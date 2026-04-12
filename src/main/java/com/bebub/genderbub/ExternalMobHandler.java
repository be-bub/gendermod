package com.bebub.genderbub;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public class ExternalMobHandler {
    
    public static boolean isNaturalistLion(LivingEntity entity) {
        String className = entity.getClass().getName();
        return className.equals("com.starfish_studios.naturalist.common.entity.Lion");
    }
    
    public static boolean isNaturalistLionBaby(LivingEntity entity) {
        if (entity instanceof AgeableMob) {
            return ((AgeableMob) entity).isBaby();
        }
        try {
            java.lang.reflect.Method isBaby = entity.getClass().getMethod("isBaby");
            return (boolean) isBaby.invoke(entity);
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean naturalistLionHasMane(LivingEntity entity) {
        try {
            java.lang.reflect.Method hasMane = entity.getClass().getMethod("hasMane");
            return (boolean) hasMane.invoke(entity);
        } catch (Exception e) {
            return false;
        }
    }
    
    public static String determineNaturalistLionGender(LivingEntity entity) {
        if (isNaturalistLionBaby(entity)) {
            return null;
        }
        return naturalistLionHasMane(entity) ? "male" : "female";
    }
    
    public static boolean isPrimalLion(LivingEntity entity) {
        String className = entity.getClass().getName();
        return className.equals("org.primal.entity.animal.LionEntity");
    }
    
    public static boolean isPrimalLionBaby(LivingEntity entity) {
        try {
            java.lang.reflect.Method isBaby = entity.getClass().getMethod("isBaby");
            return (boolean) isBaby.invoke(entity);
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean isPrimalLionManeless(LivingEntity entity) {
        try {
            java.lang.reflect.Method isManeless = entity.getClass().getMethod("isManeless");
            return (boolean) isManeless.invoke(entity);
        } catch (Exception e) {
            return false;
        }
    }
    
    public static String determinePrimalLionGender(LivingEntity entity) {
        String existingGender = GenderGameplayEvents.getGender(entity);
        if (existingGender != null) {
            return existingGender;
        }
        return !isPrimalLionManeless(entity) ? "male" : "female";
    }
    
    public static boolean isExternalMob(LivingEntity entity) {
        return isNaturalistLion(entity) || isPrimalLion(entity);
    }
    
    public static String getExternalMobId(LivingEntity entity) {
        if (isNaturalistLion(entity)) return "naturalist:lion";
        if (isPrimalLion(entity)) return "primal:lion";
        return null;
    }
    
    public static void assignGenderIfMissing(LivingEntity entity) {
        if (isNaturalistLion(entity)) {
            String correctGender = determineNaturalistLionGender(entity);
            if (correctGender != null && GenderGameplayEvents.getGender(entity) == null) {
                GenderGameplayEvents.setGender(entity, correctGender);
                boolean sterile = GenderGameplayEvents.shouldBeSterile(new java.util.Random());
                GenderGameplayEvents.setSterile(entity, sterile);
            }
        } else if (isPrimalLion(entity)) {
            if (GenderGameplayEvents.getGender(entity) == null) {
                String correctGender = determinePrimalLionGender(entity);
                if (correctGender != null) {
                    GenderGameplayEvents.setGender(entity, correctGender);
                    boolean sterile = GenderGameplayEvents.shouldBeSterile(new java.util.Random());
                    GenderGameplayEvents.setSterile(entity, sterile);
                }
            }
        }
    }
    
    public static void handleExternalMobScanner(ServerPlayer serverPlayer, LivingEntity entity, String mobId) {
        if (isNaturalistLion(entity)) {
            if (isNaturalistLionBaby(entity)) {
                serverPlayer.displayClientMessage(Component.translatable("genderbub.gender.baby"), true);
                return;
            }
            
            String correctGender = determineNaturalistLionGender(entity);
            if (correctGender == null) return;
            
            String currentGender = GenderGameplayEvents.getGender(entity);
            
            if (currentGender == null) {
                GenderGameplayEvents.setGender(entity, correctGender);
                boolean sterile = GenderGameplayEvents.shouldBeSterile(new java.util.Random());
                GenderGameplayEvents.setSterile(entity, sterile);
            }
            
            String gender = GenderGameplayEvents.getGender(entity);
            boolean sterile = GenderGameplayEvents.isSterile(entity);
            
            if (gender != null) {
                sendGenderMessage(serverPlayer, entity.getUUID(), mobId, gender, sterile);
            }
        } else if (isPrimalLion(entity)) {
            if (isPrimalLionBaby(entity)) {
                serverPlayer.displayClientMessage(Component.translatable("genderbub.gender.baby"), true);
                return;
            }
            
            String correctGender = determinePrimalLionGender(entity);
            if (correctGender == null) return;
            
            String currentGender = GenderGameplayEvents.getGender(entity);
            
            if (currentGender == null) {
                GenderGameplayEvents.setGender(entity, correctGender);
                boolean sterile = GenderGameplayEvents.shouldBeSterile(new java.util.Random());
                GenderGameplayEvents.setSterile(entity, sterile);
            } else if (!correctGender.equals(currentGender)) {
                GenderGameplayEvents.setGender(entity, correctGender);
            }
            
            String gender = GenderGameplayEvents.getGender(entity);
            boolean sterile = GenderGameplayEvents.isSterile(entity);
            
            if (gender != null) {
                sendGenderMessage(serverPlayer, entity.getUUID(), mobId, gender, sterile);
            }
        }
    }
    
    private static void sendGenderMessage(ServerPlayer player, java.util.UUID entityId, String mobId, String gender, boolean sterile) {
        com.bebub.genderbub.network.NetworkHandler.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new com.bebub.genderbub.network.GenderSyncPacket(entityId, mobId, gender, sterile)
        );
        
        int color;
        if (sterile) {
            color = 0xAAAAAA;
        } else if (gender.equals("male")) {
            color = 0x55AAFF;
        } else {
            color = 0xFF55FF;
        }
        
        String key = sterile ? (gender.equals("male") ? "genderbub.gender.sterile.male" : "genderbub.gender.sterile.female") 
                            : (gender.equals("male") ? "genderbub.gender.male" : "genderbub.gender.female");
        Component displayComponent = Component.translatable(key).withStyle(style -> style.withColor(color));
        player.displayClientMessage(displayComponent, true);
    }
}