package com.bebub.genderbub;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class GenderGameplayEvents {
    private static Set<String> enabledMobs = new HashSet<>();
    private static Random random = new Random();
    
    private static final Map<UUID, Long> bredThisCycle = new ConcurrentHashMap<>();
    private static final long CYCLE_TICKS = 100;
    
    private static boolean hasBredThisCycle(UUID mobId) {
        Long bredAt = bredThisCycle.get(mobId);
        if (bredAt == null) return false;
        return System.currentTimeMillis() < bredAt + CYCLE_TICKS;
    }
    
    private static void markBred(UUID mobId) {
        bredThisCycle.put(mobId, System.currentTimeMillis());
    }

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
        boolean isExternalMob = ExternalMobHandler.isExternalMob(entity);
        
        if (!isAnimal && !isVillager && !isZombieVillager && !isExternalMob) return;
        
        if ((isVillager || isZombieVillager) && !GenderConfig.isEnableVillagers()) return;
        
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (mobId == null && !isExternalMob) return;
        
        String mobIdStr = isExternalMob ? ExternalMobHandler.getExternalMobId(entity) : mobId.toString();
        if (mobIdStr == null) return;
        
        boolean isEnabled = getEnabledMobs().contains(mobIdStr) || 
                           (isZombieVillager && getEnabledMobs().contains("minecraft:villager"));
        
        if (!isEnabled && !isExternalMob) return;
        
        if (isExternalMob) {
            if ((ExternalMobHandler.isNaturalistLion(entity) && ExternalMobHandler.isNaturalistLionBaby(entity)) ||
                (ExternalMobHandler.isPrimalLion(entity) && ExternalMobHandler.isPrimalLionBaby(entity))) {
                if (getGender(entity) != null) {
                    clearGenderData(entity);
                }
                return;
            }
            ExternalMobHandler.assignGenderIfMissing(entity);
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
        boolean isExternalMob = ExternalMobHandler.isExternalMob(entity);
        
        if (!isAnimal && !isVillager && !isZombieVillager && !isExternalMob) return;
        
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
            
            String mobId = isExternalMob ? ExternalMobHandler.getExternalMobId(entity) : ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
            if (mobId == null) return;
            
            boolean isEnabled = getEnabledMobs().contains(mobId) || 
                               (isZombieVillager && getEnabledMobs().contains("minecraft:villager"));
            
            if (!isEnabled && !isExternalMob) return;
            
            if (isExternalMob) {
                ExternalMobHandler.handleExternalMobScanner(serverPlayer, entity, mobId);
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
        boolean isExternalMob = ExternalMobHandler.isExternalMob(entity);
        
        if (!isAnimal && !isVillager && !isZombieVillager && !isExternalMob) return false;
        
        if ((isVillager || isZombieVillager) && !GenderConfig.isEnableVillagers()) return false;
        
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (mobId == null && !isExternalMob) return false;
        
        String mobIdStr = isExternalMob ? ExternalMobHandler.getExternalMobId(entity) : mobId.toString();
        if (mobIdStr == null) return false;
        
        boolean isEnabled = getEnabledMobs().contains(mobIdStr) || 
                           (isZombieVillager && getEnabledMobs().contains("minecraft:villager"));
        
        if (!isEnabled && !isExternalMob) return false;
        
        if (isExternalMob) {
            if ((ExternalMobHandler.isNaturalistLion(entity) && ExternalMobHandler.isNaturalistLionBaby(entity)) ||
                (ExternalMobHandler.isPrimalLion(entity) && ExternalMobHandler.isPrimalLionBaby(entity))) {
                return false;
            }
        }
        
        String gender = getGender(entity);
        if (gender == null) return false;
        
        boolean sterile = isSterile(entity);
        
        return GenderConfig.isItemBlocked(mobIdStr, gender, sterile, item);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBabySpawn(BabyEntitySpawnEvent event) {
        if (event.getChild() != null) {
            if (ExternalMobHandler.isExternalMob(event.getChild())) {
                return;
            }
            
            LivingEntity parentA = event.getParentA();
            LivingEntity parentB = event.getParentB();
            
            if (parentA instanceof Animal) {
                if (hasBredThisCycle(parentA.getUUID())) {
                    event.setCanceled(true);
                    ((Animal) parentA).setInLoveTime(0);
                    if (parentB instanceof Animal) ((Animal) parentB).setInLoveTime(0);
                    return;
                }
                markBred(parentA.getUUID());
            }
            
            if (parentB instanceof Animal) {
                if (hasBredThisCycle(parentB.getUUID())) {
                    event.setCanceled(true);
                    ((Animal) parentB).setInLoveTime(0);
                    if (parentA instanceof Animal) ((Animal) parentA).setInLoveTime(0);
                    return;
                }
                markBred(parentB.getUUID());
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