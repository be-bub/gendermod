package com.bebub.genderbub;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.InteractionResult;
import com.bebub.genderbub.config.GenderConfig;
import com.bebub.genderbub.network.GenderSyncPacket;
import com.bebub.genderbub.network.NetworkHandler;

import java.util.*;

public class GenderGameplayEvents {
    private static final ParticleOptions BLOCKED_BREEDING_PARTICLES = ParticleTypes.ANGRY_VILLAGER;
    private static Set<String> enabledMobs;
    private static Map<String, List<GenderConfig.InteractionRule>> rulesByMob;

    public static void reloadConfig() {
        enabledMobs = new HashSet<>(GenderConfig.getEnabledMobs());
        rulesByMob = new HashMap<>();
        for (String mobId : enabledMobs) {
            List<GenderConfig.InteractionRule> rules = GenderConfig.getRulesForMob(mobId);
            if (!rules.isEmpty()) {
                rulesByMob.put(mobId, rules);
            }
        }
    }

    private static boolean isAnimalEnabled(Animal animal) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (id == null) return false;
        return enabledMobs.contains(id.toString());
    }

    public static String getGender(Animal animal) {
        CompoundTag nbt = animal.getPersistentData();
        return nbt.contains("GenderMod_Gender") ? nbt.getString("GenderMod_Gender") : null;
    }
    
    public static boolean isSterile(Animal animal) {
        CompoundTag nbt = animal.getPersistentData();
        return nbt.contains("GenderMod_Sterile") && nbt.getBoolean("GenderMod_Sterile");
    }
    
    private void setSterile(Animal animal, boolean sterile) {
        animal.getPersistentData().putBoolean("GenderMod_Sterile", sterile);
    }

    private void setGender(Animal animal, String gender) {
        animal.getPersistentData().putString("GenderMod_Gender", gender);
    }

    public static boolean shouldCancelInteraction(Animal animal, Item item) {
        if (!isAnimalEnabled(animal)) return false;
        
        String gender = getGender(animal);
        if (gender == null) return false;
        
        boolean sterile = isSterile(animal);
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (mobId == null) return false;
        
        return GenderConfig.isItemBlocked(mobId.toString(), gender, sterile, item);
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Animal animal)) return;
        
        if (!isAnimalEnabled(animal)) return;
        
        if (!animal.getPersistentData().contains("GenderMod_Gender")) {
            String[] result = GenderConfig.getRandomGenderWithSterile();
            setGender(animal, result[0]);
            setSterile(animal, Boolean.parseBoolean(result[1]));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAnimalBreed(BabyEntitySpawnEvent event) {
        LivingEntity parentA = event.getParentA();
        LivingEntity parentB = event.getParentB();
        
        if (!(parentA instanceof Animal a) || !(parentB instanceof Animal b)) return;
        
        if (!isAnimalEnabled(a) || !isAnimalEnabled(b)) return;
        
        String g1 = getGender(a);
        String g2 = getGender(b);
        boolean sterile1 = isSterile(a);
        boolean sterile2 = isSterile(b);
        
        if (g1 == null || g2 == null) return;
        
        boolean cancel = false;
        
        if (sterile1 || sterile2) {
            cancel = true;
        } else if (g1.equals(g2)) {
            boolean isMalePair = g1.equals("male");
            if ((isMalePair && !GenderConfig.isAllowMaleMaleBreed()) ||
                (!isMalePair && !GenderConfig.isAllowFemaleFemaleBreed())) {
                cancel = true;
            }
        }
        
        if (cancel) {
            event.setCanceled(true);
            Level level = a.level();
            if (level instanceof ServerLevel sl) {
                spawnAngryParticles(a, sl);
                spawnAngryParticles(b, sl);
            }
            return;
        }
        
        if (event.getChild() != null) {
            String[] result = GenderConfig.getRandomGenderWithSterile();
            event.getChild().getPersistentData().putString("GenderMod_Gender", result[0]);
            event.getChild().getPersistentData().putBoolean("GenderMod_Sterile", Boolean.parseBoolean(result[1]));
        }
    }

    public static void spawnAngryParticles(Animal animal, ServerLevel level) {
        for (int i = 0; i < 5; i++) {
            Vec3 pos = animal.position().add(0, 1.5, 0)
                .add(level.random.nextGaussian() * 0.5, 0, level.random.nextGaussian() * 0.5);
            level.sendParticles(BLOCKED_BREEDING_PARTICLES, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof Animal animal)) return;
        
        Item item = event.getItemStack().getItem();
        
        if (shouldCancelInteraction(animal, item)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        
        if (item == GenderMod.GENDER_SCANNER.get()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                String gender = getGender(animal);
                boolean sterile = isSterile(animal);
                if (gender != null) {
                    NetworkHandler.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new GenderSyncPacket(animal.getUUID(), gender, sterile)
                    );
                    
                    Component displayComponent;
                    
                    if (sterile) {
                        displayComponent = Component.translatable(
                            gender.equals("male") ? "genderbub.gender.sterile.male" : "genderbub.gender.sterile.female",
                            Component.translatable(gender.equals("male") ? "genderbub.gender.male" : "genderbub.gender.female")
                        );
                    } else {
                        displayComponent = Component.translatable(gender.equals("male") ? "genderbub.gender.male" : "genderbub.gender.female");
                    }
                    
                    player.displayClientMessage(displayComponent, true);
                }
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof Animal animal)) return;
        
        Item item = event.getItemStack().getItem();
        
        if (shouldCancelInteraction(animal, item)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static boolean shouldBlockAction(Animal animal, String action) {
        if (!isAnimalEnabled(animal)) return false;
        
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (mobId == null) return false;
        
        String gender = getGender(animal);
        if (gender == null) return false;
        
        boolean sterile = isSterile(animal);
        
        return GenderConfig.isActionBlocked(mobId.toString(), gender, sterile, action);
    }
}