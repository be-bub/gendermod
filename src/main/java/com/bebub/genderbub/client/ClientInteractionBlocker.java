package com.bebub.genderbub.client;

import com.bebub.genderbub.config.GenderConfig;
import com.bebub.genderbub.network.InteractionRequestPacket;
import com.bebub.genderbub.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;

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
                    
                    ItemStack mainHand = player.getMainHandItem();
                    ItemStack offHand = player.getOffhandItem();
                    
                    ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
                    if (mobId == null) return;
                    String mobIdStr = mobId.toString();
                    
                    boolean hasRules = false;
                    for (GenderConfig.MobRule mob : GenderConfig.getMobs()) {
                        if (mob.mobId.equals(mobIdStr)) {
                            hasRules = true;
                            break;
                        }
                    }
                    if (!hasRules) return;
                    
                    boolean hasForbiddenInMain = false;
                    boolean hasForbiddenInOff = false;
                    int slotToUse = -1;
                    boolean useOffhand = false;
                    String itemIdToCheck = null;
                    
                    if (!mainHand.isEmpty()) {
                        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(mainHand.getItem());
                        if (itemId != null) {
                            String itemIdStr = itemId.toString();
                            for (GenderConfig.MobRule mob : GenderConfig.getMobs()) {
                                if (mob.mobId.equals(mobIdStr)) {
                                    if (mob.isItemMatch(itemIdStr)) {
                                        hasForbiddenInMain = true;
                                        slotToUse = player.getInventory().selected;
                                        useOffhand = false;
                                        itemIdToCheck = itemIdStr;
                                        break;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (!offHand.isEmpty()) {
                        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(offHand.getItem());
                        if (itemId != null) {
                            String itemIdStr = itemId.toString();
                            for (GenderConfig.MobRule mob : GenderConfig.getMobs()) {
                                if (mob.mobId.equals(mobIdStr)) {
                                    if (mob.isItemMatch(itemIdStr)) {
                                        hasForbiddenInOff = true;
                                        slotToUse = 40;
                                        useOffhand = true;
                                        itemIdToCheck = itemIdStr;
                                        break;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    
                    boolean hasScannerInMain = GenderConfig.isScannerItem(mainHand.getItem());
                    boolean hasScannerInOff = GenderConfig.isScannerItem(offHand.getItem());
                    
                    boolean hasForbidden = hasForbiddenInMain || hasForbiddenInOff;
                    boolean hasScanner = hasScannerInMain || hasScannerInOff;
                    
                    if (hasScanner && !hasForbidden) {
                        return;
                    }
                    
                    if (hasForbidden) {
                        event.setCanceled(true);
                        NetworkHandler.CHANNEL.sendToServer(new InteractionRequestPacket(animal.getId(), slotToUse, mobIdStr, itemIdToCheck, useOffhand));
                    }
                }
            }
        }
    }
}