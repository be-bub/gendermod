package com.bebub.genderbub;

import com.bebub.genderbub.config.GenderConfig;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = GenderMod.MOD_ID)
public class EggLayingHandler {
    
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;
        if (itemEntity.getItem().getItem() != Items.EGG) return;
        
        Animal nearestChicken = null;
        double nearestDist = 2.0;
        
        for (Animal animal : event.getLevel().getEntitiesOfClass(Animal.class, itemEntity.getBoundingBox().inflate(2.0))) {
            if (animal instanceof Chicken) {
                double dist = animal.distanceToSqr(itemEntity);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestChicken = animal;
                }
            }
        }
        
        if (nearestChicken == null) return;
        
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(nearestChicken.getType());
        if (mobId == null) return;
        
        if (!GenderConfig.getEnabledMobs().contains(mobId.toString())) return;
        
        String gender = GenderGameplayEvents.getGender(nearestChicken);
        if (gender == null) return;
        
        boolean sterile = GenderGameplayEvents.isSterile(nearestChicken);
        
        if (GenderConfig.isActionBlocked(mobId.toString(), gender, sterile, "lay_egg")) {
            event.setCanceled(true);
            itemEntity.discard();
        }
    }
}