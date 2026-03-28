package com.bebub.gendermod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;
import com.bebub.gendermod.config.GenderConfig;

@OnlyIn(Dist.CLIENT)
public class ClientInteractionBlocker {
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        if (event.isUseItem()) {
            HitResult hit = mc.hitResult;
            if (hit instanceof EntityHitResult) {
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity target = entityHit.getEntity();
                if (target instanceof Animal) {
                    Animal animal = (Animal) target;
                    Player player = mc.player;
                    ItemStack stack = player.getMainHandItem();
                    
                    if (stack.isEmpty()) return;
                    
                    ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
                    if (mobId == null) return;
                    
                    String mobIdStr = mobId.toString();
                    if (!GenderConfig.getEnabledMobs().contains(mobIdStr)) return;
                    
                    String gender = ClientGenderCache.getGender(animal.getUUID());
                    if (gender == null) return;
                    
                    boolean sterile = ClientGenderCache.isSterile(animal.getUUID());
                    
                    if (GenderConfig.isItemBlocked(mobIdStr, gender, sterile, stack.getItem())) {
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
}