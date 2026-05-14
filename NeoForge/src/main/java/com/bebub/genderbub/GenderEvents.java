package com.bebub.genderbub;

import com.bebub.genderbub.compat.GenderAddon;
import com.bebub.genderbub.config.GenderConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = GenderMod.MOD_ID)
public class GenderEvents {

    private static boolean isMCAVillager(LivingEntity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) return false;
        String str = id.toString();
        return str.equals("mca:male_villager") || str.equals("mca:female_villager") ||
               str.equals("mca:male_zombie_villager") || str.equals("mca:female_zombie_villager");
    }

    private static String getMCAGender(LivingEntity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) return null;
        String str = id.toString();
        if (str.equals("mca:male_villager") || str.equals("mca:male_zombie_villager")) return "male";
        if (str.equals("mca:female_villager") || str.equals("mca:female_zombie_villager")) return "female";
        return null;
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().isClientSide()) return;

        if (isMCAVillager(entity)) {
            if (GenderConfig.isEnableVillagers()) {
                String gender = getMCAGender(entity);
                if (gender != null && GenderCore.getGender(entity).equals("none")) {
                    GenderCore.setGender(entity, gender);
                    GenderCore.setSterile(entity, false);
                }
            } else {
                String current = GenderCore.getGender(entity);
                if (!current.equals("none") && !current.startsWith("cached_")) {
                    GenderCore.setGender(entity, "cached_" + current);
                }
            }
            return;
        }

        if (GenderAddon.isIceFireDragon(entity)) {
            String correct = GenderAddon.getIceFireDragonGender(entity);
            if (correct != null && !GenderCore.getGender(entity).equals(correct)) {
                GenderCore.setGender(entity, correct);
                GenderCore.setSterile(entity, false);
            }
            return;
        }

        if (entity instanceof ZombieVillager) {
            if (GenderConfig.isEnableVillagers()) {
                if (GenderCore.getGender(entity).equals("none")) {
                    String[] res = GenderConfig.getRandomGenderWithSterile();
                    if (!res[0].equals("none")) {
                        GenderCore.setGender(entity, res[0]);
                        GenderCore.setSterile(entity, Boolean.parseBoolean(res[1]));
                    }
                }
            }
            return;
        }

        if (entity instanceof Villager) {
            if (!GenderConfig.isEnableVillagers()) return;
            if (GenderCore.getGender(entity).equals("none")) {
                String[] res = GenderConfig.getRandomGenderWithSterile();
                if (!res[0].equals("none")) {
                    GenderCore.setGender(entity, res[0]);
                    GenderCore.setSterile(entity, Boolean.parseBoolean(res[1]));
                }
            }
            return;
        }

        boolean isAnimal = entity instanceof Animal;
        boolean isExternal = GenderAddon.isExternalMob(entity);
        
        if (!isAnimal && !isExternal) return;

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String mobId = isExternal ? GenderAddon.getExternalMobId(entity) : (id != null ? id.toString() : null);
        if (mobId == null) return;
        
        if (!GenderConfig.isMobEnabled(mobId)) return;

        if (GenderConfig.getGenderOnlyMobs().contains(mobId)) {
            if (GenderCore.getGender(entity).equals("none")) {
                String[] res = GenderConfig.getRandomGenderWithSterile();
                if (!res[0].equals("none")) {
                    GenderCore.setGender(entity, res[0]);
                    GenderCore.setSterile(entity, false);
                }
            }
            return;
        }

        if (isExternal) {
            if ((GenderAddon.isNaturalistLion(entity) && GenderAddon.isNaturalistLionBaby(entity)) ||
                (GenderAddon.isPrimalLion(entity) && GenderAddon.isPrimalLionBaby(entity))) {
                String current = GenderCore.getGender(entity);
                if (!current.equals("baby")) {
                    GenderCore.setGender(entity, "baby");
                    GenderCore.setSterile(entity, false);
                }
                return;
            }
            GenderAddon.assignGenderIfMissing(entity);
        } else if (GenderCore.getGender(entity).equals("none")) {
            String[] res = GenderConfig.getRandomGenderWithSterile();
            if (!res[0].equals("none")) {
                GenderCore.setGender(entity, res[0]);
                GenderCore.setSterile(entity, Boolean.parseBoolean(res[1]));
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof LivingEntity entity)) return;
        
        if (shouldCancelInteraction(entity, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getTarget() instanceof LivingEntity entity)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack held = event.getItemStack();
        if (held.isEmpty()) return;

        if (shouldCancelInteraction(entity, held)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        boolean animal = entity instanceof Animal;
        boolean villager = (entity instanceof Villager || entity instanceof ZombieVillager) && !isMCAVillager(entity);
        boolean external = GenderAddon.isExternalMob(entity);
        if (!animal && !villager && !external) return;
        if (villager && !GenderConfig.isEnableVillagers()) return;

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String mobId = external ? GenderAddon.getExternalMobId(entity) : (id != null ? id.toString() : null);
        if (mobId == null) return;
        if (!GenderConfig.isMobEnabled(mobId)) return;
    }

    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (event.getChild() == null) return;
        if (event.getChild().level().isClientSide()) return;

        if (isMCAVillager(event.getChild())) {
            if (GenderConfig.isEnableVillagers()) {
                String gender = getMCAGender(event.getChild());
                if (gender != null && GenderCore.getGender(event.getChild()).equals("none")) {
                    GenderCore.setGender(event.getChild(), gender);
                    GenderCore.setSterile(event.getChild(), false);
                }
            } else {
                String current = GenderCore.getGender(event.getChild());
                if (!current.equals("none") && !current.startsWith("cached_")) {
                    GenderCore.setGender(event.getChild(), "cached_" + current);
                }
            }
            return;
        }

        if (GenderAddon.isExternalMob(event.getChild())) return;

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getChild().getType());
        if (id == null) return;
        String mobId = id.toString();

        boolean villager = mobId.equals("minecraft:villager") || mobId.equals("minecraft:zombie_villager");
        if (villager && !GenderConfig.isEnableVillagers()) return;
        if (!GenderConfig.isMobEnabled(mobId)) return;

        String[] res = GenderConfig.getRandomGenderWithSterile();
        if (!res[0].equals("none")) {
            GenderCore.setGender(event.getChild(), res[0]);
            GenderCore.setSterile(event.getChild(), Boolean.parseBoolean(res[1]));
        }
    }

    private static boolean shouldCancelInteraction(LivingEntity entity, ItemStack stack) {
        boolean animal = entity instanceof Animal;
        boolean villager = (entity instanceof Villager || entity instanceof ZombieVillager) && !isMCAVillager(entity);
        boolean external = GenderAddon.isExternalMob(entity);
        if (!animal && !villager && !external) return false;
        if (villager && !GenderConfig.isEnableVillagers()) return false;

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String mobId = external ? GenderAddon.getExternalMobId(entity) : (id != null ? id.toString() : null);
        if (mobId == null) return false;
        if (!GenderConfig.isMobEnabled(mobId)) return false;

        if (external && ((GenderAddon.isNaturalistLion(entity) && GenderAddon.isNaturalistLionBaby(entity)) ||
                         (GenderAddon.isPrimalLion(entity) && GenderAddon.isPrimalLionBaby(entity)))) {
            return false;
        }

        String gender = GenderCore.getGender(entity);
        if (gender.equals("none")) return false;
        return GenderConfig.isItemBlocked(mobId, gender, GenderCore.isSterile(entity), stack.getItem());
    }
}