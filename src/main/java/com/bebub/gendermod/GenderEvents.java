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
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GenderEvents {
    private static final HashMap<UUID, String> genders = new HashMap<>();
    private static final TextColor MALE_COLOR = TextColor.parseColor("#55AAFF");
    private static final TextColor FEMALE_COLOR = TextColor.parseColor("#FF55FF");
    private static final ParticleOptions BLOCKED_BREEDING_PARTICLES = ParticleTypes.ANGRY_VILLAGER;
    private static List<? extends String> enabledMobs;
    private static List<GenderRule> genderRules;

    public static void setConfig(List<? extends String> mobs, List<? extends String> rules) {
        enabledMobs = mobs;
        genderRules = rules.stream()
            .map(GenderRule::new)
            .collect(Collectors.toList());
    }

    @SubscribeEvent
    public void onEntitySpawn(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Animal animal) {
            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (entityId != null && enabledMobs.contains(entityId.toString())) {
                if (!genders.containsKey(animal.getUUID())) {
                    String gender = Math.random() < 0.5 ? "male" : "female";
                    genders.put(animal.getUUID(), gender);
                }
            }
        }
    }

    @SubscribeEvent
    public void onAnimalBreed(BabyEntitySpawnEvent event) {
        if (event.getParentA() instanceof Animal parent1 && event.getParentB() instanceof Animal parent2) {
            String gender1 = genders.get(parent1.getUUID());
            String gender2 = genders.get(parent2.getUUID());
            
            if (gender1 != null && gender2 != null && gender1.equals(gender2)) {
                event.setCanceled(true);
                
                Level level = parent1.level();
                if (!level.isClientSide) {
                    for (int i = 0; i < 5; i++) {
                        Vec3 pos1 = parent1.position().add(0, 1.5, 0)
                            .add(level.random.nextGaussian() * 0.5, 0, level.random.nextGaussian() * 0.5);
                        ((ServerLevel)level).sendParticles(BLOCKED_BREEDING_PARTICLES, pos1.x, pos1.y, pos1.z, 1, 0, 0, 0, 0);
                        
                        Vec3 pos2 = parent2.position().add(0, 1.5, 0)
                            .add(level.random.nextGaussian() * 0.5, 0, level.random.nextGaussian() * 0.5);
                        ((ServerLevel)level).sendParticles(BLOCKED_BREEDING_PARTICLES, pos2.x, pos2.y, pos2.z, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            Item item = event.getItemStack().getItem();
            if (event.getTarget() instanceof Animal animal) {
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
                String gender = genders.get(animal.getUUID());
                
                if (entityId != null && gender != null) {
                    boolean shouldCancel = genderRules.stream()
                        .anyMatch(rule -> rule.matches(entityId, gender, item));
                    
                    if (shouldCancel) {
                        event.setCanceled(true);
                        return;
                    }

                    if (item == Items.STICK) {
                        MutableComponent message = Component.literal("Gender: ")
                            .append(createGenderText(gender));
                        event.getEntity().displayClientMessage(message, true);
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    private static MutableComponent createGenderText(String gender) {
        TextColor color = gender.equals("male") ? MALE_COLOR : FEMALE_COLOR;
        return Component.literal(gender).withStyle(style -> style.withColor(color));
    }
}