package com.bebub.genderbub.client;

import com.bebub.genderbub.GenderMod;
import com.bebub.genderbub.GenderGameplayEvents;
import com.bebub.genderbub.config.GenderConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = GenderMod.MOD_ID, value = Dist.CLIENT)
public class ClientGenderHandler {
    private static Entity lastLookedEntity = null;
    private static final float ICON_OFFSET = 0.7f;

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        
        boolean isAnimal = living instanceof Animal;
        boolean isVillager = living instanceof Villager;
        boolean isZombieVillager = living instanceof ZombieVillager;
        boolean isNaturalistLion = GenderGameplayEvents.isNaturalistLion(living);
        
        if (!isAnimal && !isVillager && !isZombieVillager && !isNaturalistLion) return;
        
        if (isNaturalistLion && GenderGameplayEvents.isLionBaby(living)) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        ItemStack offhand = mc.player.getOffhandItem();
        ItemStack mainHand = mc.player.getMainHandItem();
        
        boolean isScannerOffhand = isScannerItem(offhand);
        boolean isScannerMainHand = isScannerItem(mainHand);
        
        boolean isLookingAt = false;
        if (mc.hitResult instanceof EntityHitResult) {
            Entity target = ((EntityHitResult) mc.hitResult).getEntity();
            if (target == living) {
                isLookingAt = true;
            }
        }
        
        boolean showIcon = false;
        
        if (isLookingAt && (isScannerMainHand || isScannerOffhand)) {
            showIcon = true;
        } else if (isScannerOffhand && !isLookingAt) {
            int maxDistance = GenderConfig.getDisplayRadius();
            if (maxDistance > 0) {
                if (maxDistance > 256) {
                    maxDistance = 256;
                }
                double distance = mc.player.distanceTo(living);
                if (distance <= maxDistance) {
                    showIcon = true;
                }
            }
        }
        
        if (!showIcon) return;
        
        String gender = ClientGenderCache.getGender(living.getUUID());
        if (gender == null || gender.equals("none")) return;
        
        boolean sterile = ClientGenderCache.isSterile(living.getUUID());
        
        String text = gender.equals("male") ? "♂" : "♀";
        int color;
        
        if (sterile) {
            color = 0xAAAAAA;
        } else if (gender.equals("male")) {
            color = 0x55AAFF;
        } else {
            color = 0xFF55FF;
        }
        
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        
        poseStack.pushPose();
        
        float yOffset = living.getBbHeight() + ICON_OFFSET;
        
        boolean hasCustomName = living.hasCustomName();
        if (hasCustomName) {
            yOffset += 0.3f;
        }
        
        poseStack.translate(0, yOffset, 0);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        
        float scale = 0.025f;
        poseStack.scale(-scale, -scale, scale);
        
        float x = -mc.font.width(text) / 2f;
        
        int packedLight = 0xF000F0;
        
        mc.font.drawInBatch(text, x, 0, color, false, poseStack.last().pose(), buffer, 
            net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight);
        
        poseStack.popPose();
    }
    
    private static boolean isScannerItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return false;
        return itemId.toString().equals("genderbub:magnifying_glass");
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        
        HitResult hit = mc.hitResult;
        Entity target = hit instanceof EntityHitResult ? ((EntityHitResult) hit).getEntity() : null;
        
        if (target != lastLookedEntity) {
            lastLookedEntity = target;
        }
    }
}