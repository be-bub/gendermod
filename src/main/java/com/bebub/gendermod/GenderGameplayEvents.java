package com.bebub.gendermod;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import com.bebub.gendermod.config.GenderModConfig;
import com.bebub.gendermod.config.ModConfiguration.GenderRuleConfig;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;
import java.util.stream.Collectors;

public class GenderGameplayEvents {
    private static final TextColor MALE_COLOR = TextColor.parseColor("#55AAFF");
    private static final TextColor FEMALE_COLOR = TextColor.parseColor("#FF55FF");
    private static final ParticleOptions BLOCKED_BREEDING_PARTICLES = ParticleTypes.ANGRY_VILLAGER;
    private static List<String> enabledMobs;
    private static List<GenderInteractionRule> genderRules;

    public static void setConfiguration(List<String> mobs, List<GenderRuleConfig> rules) {
        enabledMobs = mobs;
        genderRules = rules.stream().map(GenderInteractionRule::new).collect(Collectors.toList());
    }

    @SubscribeEvent
    public void onEntitySpawn(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Animal animal) {
            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (entityId != null && enabledMobs.contains(entityId.toString())) {
                CompoundTag nbt = animal.getPersistentData();
                if (!nbt.contains("GenderMod_Gender")) {
                    String gender = Math.random() < 0.5 ? "male" : "female";
                    nbt.putString("GenderMod_Gender", gender);
                }
            }
        }
    }

    private String getGenderFromNBT(Animal animal) {
        CompoundTag nbt = animal.getPersistentData();
        if (nbt.contains("GenderMod_Gender")) {
            return nbt.getString("GenderMod_Gender");
        }
        return null;
    }

    @SubscribeEvent
    public void onAnimalBreed(BabyEntitySpawnEvent event) {
        if (event.getParentA() instanceof Animal parent1 && event.getParentB() instanceof Animal parent2) {
            String gender1 = getGenderFromNBT(parent1);
            String gender2 = getGenderFromNBT(parent2);
            if (gender1 != null && gender2 != null && gender1.equals(gender2)) {
                event.setCanceled(true);
                Level level = parent1.level();
                if (!level.isClientSide) {
                    spawnAngryParticles(parent1, (ServerLevel)level);
                    spawnAngryParticles(parent2, (ServerLevel)level);
                }
            }
        }
    }

    private void spawnAngryParticles(Animal animal, ServerLevel level) {
        for (int i = 0; i < 5; i++) {
            Vec3 pos = animal.position().add(0, 1.5, 0).add(level.random.nextGaussian() * 0.5, 0, level.random.nextGaussian() * 0.5);
            level.sendParticles(BLOCKED_BREEDING_PARTICLES, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            Item item = event.getItemStack().getItem();
            if (event.getTarget() instanceof Animal animal) {
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
                String gender = getGenderFromNBT(animal);
                if (entityId != null && gender != null) {
                    if (shouldCancelInteraction(entityId, gender, item)) {
                        event.setCanceled(true);
                        event.setResult(Event.Result.DENY);
                        return;
                    }
                    if (item == Items.STICK && !event.getEntity().level().isClientSide) {
                        showGenderMessage(event, gender);
                        event.setCanceled(true);
                        event.setResult(Event.Result.DENY);
                    }
                }
            }
        }
    }

    private boolean shouldCancelInteraction(ResourceLocation entityId, String gender, Item item) {
        return genderRules.stream().anyMatch(rule -> rule.matches(entityId, gender, item));
    }

    private void showGenderMessage(PlayerInteractEvent.EntityInteract event, String gender) {
        MutableComponent genderText = createGenderText(gender);
        MutableComponent message = Component.translatable("gendermod.message.gender", genderText);
        event.getEntity().displayClientMessage(message, true);
    }

    private MutableComponent createGenderText(String gender) {
        TextColor color = gender.equals("male") ? MALE_COLOR : FEMALE_COLOR;
        String translationKey = gender.equals("male") ? "gendermod.gender.male" : "gendermod.gender.female";
        return Component.translatable(translationKey).withStyle(style -> style.withColor(color));
    }
} 