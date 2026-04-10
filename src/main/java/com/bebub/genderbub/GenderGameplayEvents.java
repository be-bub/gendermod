package com.bebub.genderbub;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.InteractionResult;
import com.bebub.genderbub.config.GenderConfig;
import com.bebub.genderbub.network.GenderSyncPacket;
import com.bebub.genderbub.network.EnabledMobsSyncPacket;
import com.bebub.genderbub.network.NetworkHandler;
import com.bebub.genderbub.TurtleTracker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GenderGameplayEvents {
    private static Set<String> enabledMobs = new HashSet<>();
    private static Random random = new Random();
    private static Map<UUID, UUID> breedingPair = new ConcurrentHashMap<>();

    public static void reloadConfig() {
        enabledMobs = new HashSet<>(GenderConfig.getEnabledMobs());
    }

    private static Set<String> getEnabledMobs() {
        if (enabledMobs == null) {
            enabledMobs = new HashSet<>(GenderConfig.getEnabledMobs());
        }
        return enabledMobs;
    }

    public static String getGender(LivingEntity entity) {
        CompoundTag nbt = entity.getPersistentData();
        return nbt.contains("GenderMod_Gender") ? nbt.getString("GenderMod_Gender") : null;
    }
    
    public static boolean isSterile(LivingEntity entity) {
        CompoundTag nbt = entity.getPersistentData();
        return nbt.contains("GenderMod_Sterile") && nbt.getBoolean("GenderMod_Sterile");
    }
    
    public static void setGender(LivingEntity entity, String gender) {
        entity.getPersistentData().putString("GenderMod_Gender", gender);
    }
    
    public static void setSterile(LivingEntity entity, boolean sterile) {
        entity.getPersistentData().putBoolean("GenderMod_Sterile", sterile);
    }
    
    public static void clearGenderData(LivingEntity entity) {
        entity.getPersistentData().remove("GenderMod_Gender");
        entity.getPersistentData().remove("GenderMod_Sterile");
    }
    
    public static void copyGenderData(LivingEntity from, LivingEntity to) {
        String gender = getGender(from);
        boolean sterile = isSterile(from);
        if (gender != null) {
            setGender(to, gender);
            setSterile(to, sterile);
        }
    }
    
    public static boolean isNaturalistLion(LivingEntity entity) {
        String className = entity.getClass().getName();
        return className.equals("com.starfish_studios.naturalist.common.entity.Lion");
    }
    
    public static boolean isLionBaby(LivingEntity entity) {
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
    
    public static boolean lionHasMane(LivingEntity entity) {
        try {
            java.lang.reflect.Method hasMane = entity.getClass().getMethod("hasMane");
            return (boolean) hasMane.invoke(entity);
        } catch (Exception e) {
            return false;
        }
    }
    
    public static String determineLionGender(LivingEntity entity) {
        if (isLionBaby(entity)) {
            return null;
        }
        return lionHasMane(entity) ? "male" : "female";
    }
    
    public static double getSterileChance() {
        int maleChance = GenderConfig.getMaleChance();
        int femaleChance = GenderConfig.getFemaleChance();
        int total = maleChance + femaleChance;
        if (total >= 100) {
            return 0.0;
        }
        return (100 - total) / 100.0;
    }
    
    public static boolean shouldBeSterile(Random random) {
        double sterileChance = getSterileChance();
        return sterileChance > 0 && random.nextDouble() < sterileChance;
    }

    private boolean canBreed(LivingEntity parentA, LivingEntity parentB) {
        String g1 = getGender(parentA);
        String g2 = getGender(parentB);
        boolean sterile1 = isSterile(parentA);
        boolean sterile2 = isSterile(parentB);
        
        if (g1 == null || g2 == null) return true;
        
        if (g1.equals(g2)) {
            boolean isMalePair = g1.equals("male");
            if ((isMalePair && !GenderConfig.isAllowMaleMaleBreed()) ||
                (!isMalePair && !GenderConfig.isAllowFemaleFemaleBreed())) return false;
        }
        
        if (sterile1 || sterile2) {
            return GenderConfig.isAllowSterileBreed();
        }
        
        return true;
    }

    public static void storeBreedingPair(Animal parentA, Animal parentB) {
        UUID idA = parentA.getUUID();
        UUID idB = parentB.getUUID();
        breedingPair.put(idA, idB);
        breedingPair.put(idB, idA);
    }

    public static void checkAndCancelBreeding(Animal parentA, Animal parentB, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        UUID idA = parentA.getUUID();
        UUID idB = parentB.getUUID();
        
        UUID storedMate = breedingPair.get(idA);
        if (storedMate == null || !storedMate.equals(idB)) {
            return;
        }
        
        breedingPair.remove(idA);
        breedingPair.remove(idB);
        
        if (!canBreedStatic(parentA, parentB)) {
            ci.cancel();
            parentA.setInLoveTime(0);
            parentB.setInLoveTime(0);
            if (parentA.level() instanceof ServerLevel sl) {
                spawnAngryParticles(parentA, sl);
                spawnAngryParticles(parentB, sl);
            }
        }
    }

    private static boolean canBreedStatic(LivingEntity parentA, LivingEntity parentB) {
        String g1 = getGender(parentA);
        String g2 = getGender(parentB);
        boolean sterile1 = isSterile(parentA);
        boolean sterile2 = isSterile(parentB);
        
        if (g1 == null || g2 == null) return true;
        
        if (g1.equals(g2)) {
            boolean isMalePair = g1.equals("male");
            if ((isMalePair && !GenderConfig.isAllowMaleMaleBreed()) ||
                (!isMalePair && !GenderConfig.isAllowFemaleFemaleBreed())) return false;
        }
        
        if (sterile1 || sterile2) {
            return GenderConfig.isAllowSterileBreed();
        }
        
        return true;
    }

    public static boolean isAllowMaleMaleBreed() {
        return GenderConfig.isAllowMaleMaleBreed();
    }

    public static boolean isAllowFemaleFemaleBreed() {
        return GenderConfig.isAllowFemaleFemaleBreed();
    }

    public static boolean isAllowSterileBreed() {
        return GenderConfig.isAllowSterileBreed();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        
        Set<String> enabledMobsSet = new HashSet<>(GenderConfig.getEnabledMobs());
        
        NetworkHandler.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer) event.getEntity()),
            new EnabledMobsSyncPacket(enabledMobsSet)
        );
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        
        boolean isAnimal = entity instanceof Animal;
        boolean isVillager = entity instanceof Villager;
        boolean isZombieVillager = entity instanceof ZombieVillager;
        boolean isNaturalistLion = isNaturalistLion(entity);
        
        if (!isAnimal && !isVillager && !isZombieVillager && !isNaturalistLion) return;
        
        if ((isVillager || isZombieVillager) && !GenderConfig.isEnableVillagers()) return;
        
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (mobId == null && !isNaturalistLion) return;
        
        String mobIdStr = isNaturalistLion ? "naturalist:lion" : mobId.toString();
        boolean isEnabled = getEnabledMobs().contains(mobIdStr) || 
                           (isZombieVillager && getEnabledMobs().contains("minecraft:villager"));
        
        if (!isEnabled && !isNaturalistLion) return;
        
        if (isNaturalistLion) {
            if (isLionBaby(entity)) {
                if (getGender(entity) != null) {
                    clearGenderData(entity);
                }
                return;
            }
            String currentGender = getGender(entity);
            String correctGender = determineLionGender(entity);
            if (correctGender == null) {
                return;
            }
            if (currentGender == null || !correctGender.equals(currentGender)) {
                setGender(entity, correctGender);
                boolean sterile = shouldBeSterile(new Random());
                setSterile(entity, sterile);
            }
        } else if (!entity.getPersistentData().contains("GenderMod_Gender")) {
            String[] result = GenderConfig.getRandomGenderWithSterile();
            if (!result[0].equals("none")) {
                setGender(entity, result[0]);
                setSterile(entity, Boolean.parseBoolean(result[1]));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof LivingEntity entity)) return;
        
        boolean isAnimal = entity instanceof Animal;
        boolean isVillager = entity instanceof Villager;
        boolean isZombieVillager = entity instanceof ZombieVillager;
        boolean isNaturalistLion = isNaturalistLion(entity);
        
        if (!isAnimal && !isVillager && !isZombieVillager && !isNaturalistLion) return;
        
        if ((isVillager || isZombieVillager) && !GenderConfig.isEnableVillagers()) return;
        
        net.minecraft.world.entity.player.Player player = event.getEntity();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        boolean hasScannerInMain = GenderConfig.isScannerItem(mainHand.getItem());
        boolean hasScannerInOff = GenderConfig.isScannerItem(offHand.getItem());
        boolean hasScanner = hasScannerInMain || hasScannerInOff;
        
        boolean hasForbiddenInMain = false;
        boolean hasForbiddenInOff = false;
        
        if (!mainHand.isEmpty()) {
            if (shouldCancelInteraction(entity, mainHand.getItem())) {
                hasForbiddenInMain = true;
            }
        }
        
        if (!offHand.isEmpty()) {
            if (shouldCancelInteraction(entity, offHand.getItem())) {
                hasForbiddenInOff = true;
            }
        }
        
        boolean hasForbidden = hasForbiddenInMain || hasForbiddenInOff;
        
        if (hasForbidden) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        
        if (hasScanner) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            
            if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;
            
            String mobId = isNaturalistLion ? "naturalist:lion" : ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
            boolean isEnabled = getEnabledMobs().contains(mobId) || 
                               (isZombieVillager && getEnabledMobs().contains("minecraft:villager"));
            
            if (!isEnabled && !isNaturalistLion) return;
            
            if (isNaturalistLion) {
                if (isLionBaby(entity)) {
                    serverPlayer.displayClientMessage(Component.translatable("genderbub.gender.baby"), true);
                    return;
                }
                
                String correctGender = determineLionGender(entity);
                if (correctGender == null) return;
                
                String currentGender = getGender(entity);
                
                if (currentGender == null) {
                    setGender(entity, correctGender);
                    boolean sterile = shouldBeSterile(new Random());
                    setSterile(entity, sterile);
                }
                
                String gender = getGender(entity);
                boolean sterile = isSterile(entity);
                
                if (gender != null) {
                    NetworkHandler.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new GenderSyncPacket(entity.getUUID(), mobId, gender, sterile)
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
                    serverPlayer.displayClientMessage(displayComponent, true);
                }
            } else {
                String currentGender = getGender(entity);
                boolean sterile = isSterile(entity);
                boolean isBaby = false;
                
                if (entity instanceof Villager) {
                    isBaby = ((Villager) entity).isBaby();
                } else if (entity instanceof ZombieVillager) {
                    isBaby = ((ZombieVillager) entity).isBaby();
                }
                if (currentGender == null) {
                    String[] result = GenderConfig.getRandomGenderWithSterile();
                    if (!result[0].equals("none")) {
                        setGender(entity, result[0]);
                        setSterile(entity, Boolean.parseBoolean(result[1]));
                        currentGender = result[0];
                        sterile = Boolean.parseBoolean(result[1]);
                    }
                }
                
                if (currentGender != null && !currentGender.equals("none")) {
                    NetworkHandler.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new GenderSyncPacket(entity.getUUID(), mobId, currentGender, sterile)
                    );
                    
                    int color;
                    if (sterile) {
                        color = 0xAAAAAA;
                    } else if (currentGender.equals("male")) {
                        color = 0x55AAFF;
                    } else {
                        color = 0xFF55FF;
                    }
                    
                    Component displayComponent;
                    if (isVillager || isZombieVillager) {
                        String key;
                        if (sterile) {
                            key = isBaby ? (currentGender.equals("male") ? "genderbub.villager.sterile.boy" : "genderbub.villager.sterile.girl")
                                        : (currentGender.equals("male") ? "genderbub.villager.sterile.male" : "genderbub.villager.sterile.female");
                        } else {
                            key = isBaby ? (currentGender.equals("male") ? "genderbub.villager.boy" : "genderbub.villager.girl")
                                        : (currentGender.equals("male") ? "genderbub.villager.male" : "genderbub.villager.female");
                        }
                        displayComponent = Component.translatable(key).withStyle(style -> style.withColor(color));
                    } else {
                        String key = sterile ? (currentGender.equals("male") ? "genderbub.gender.sterile.male" : "genderbub.gender.sterile.female") 
                                            : (currentGender.equals("male") ? "genderbub.gender.male" : "genderbub.gender.female");
                        displayComponent = Component.translatable(key).withStyle(style -> style.withColor(color));
                    }
                    serverPlayer.displayClientMessage(displayComponent, true);
                }
            }
        }
    }

    public static boolean shouldCancelInteraction(LivingEntity entity, Item item) {
        boolean isAnimal = entity instanceof Animal;
        boolean isVillager = entity instanceof Villager;
        boolean isZombieVillager = entity instanceof ZombieVillager;
        boolean isNaturalistLion = isNaturalistLion(entity);
        
        if (!isAnimal && !isVillager && !isZombieVillager && !isNaturalistLion) return false;
        
        if ((isVillager || isZombieVillager) && !GenderConfig.isEnableVillagers()) return false;
        
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (mobId == null && !isNaturalistLion) return false;
        
        String mobIdStr = isNaturalistLion ? "naturalist:lion" : mobId.toString();
        boolean isEnabled = getEnabledMobs().contains(mobIdStr) || 
                           (isZombieVillager && getEnabledMobs().contains("minecraft:villager"));
        
        if (!isEnabled && !isNaturalistLion) return false;
        
        if (isNaturalistLion && isLionBaby(entity)) return false;
        
        String gender = getGender(entity);
        if (gender == null) return false;
        
        boolean sterile = isSterile(entity);
        
        return GenderConfig.isItemBlocked(mobIdStr, gender, sterile, item);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBabySpawn(BabyEntitySpawnEvent event) {
        LivingEntity parentA = event.getParentA();
        LivingEntity parentB = event.getParentB();
        if (parentA == null || parentB == null) return;
        if (!(parentA instanceof Animal) || !(parentB instanceof Animal)) return;
        Animal animalA = (Animal) parentA;
        Animal animalB = (Animal) parentB;
        
        ResourceLocation mobIdA = ForgeRegistries.ENTITY_TYPES.getKey(animalA.getType());
        ResourceLocation mobIdB = ForgeRegistries.ENTITY_TYPES.getKey(animalB.getType());
        if (mobIdA == null || mobIdB == null) return;
        if (!getEnabledMobs().contains(mobIdA.toString()) || !getEnabledMobs().contains(mobIdB.toString())) return;
        
        if (!canBreed(animalA, animalB)) {
            event.setCanceled(true);
            animalA.setInLoveTime(0);
            animalB.setInLoveTime(0);
            if (animalA.level() instanceof ServerLevel sl) {
                spawnAngryParticles(animalA, sl);
                spawnAngryParticles(animalB, sl);
            }
            return;
        }
        
        if (event.getChild() != null) {
            if (isNaturalistLion(event.getChild())) {
                return;
            }
            String[] result = GenderConfig.getRandomGenderWithSterile();
            if (!result[0].equals("none")) {
                event.getChild().getPersistentData().putString("GenderMod_Gender", result[0]);
                event.getChild().getPersistentData().putBoolean("GenderMod_Sterile", Boolean.parseBoolean(result[1]));
            }
        }
    }

    public static void spawnAngryParticles(LivingEntity entity, ServerLevel level) {
        for (int i = 0; i < 2; i++) {
            Vec3 pos = entity.position().add(0, 1.5, 0)
                .add(level.random.nextGaussian() * 0.6, level.random.nextGaussian() * 0.4, level.random.nextGaussian() * 0.6);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }
}