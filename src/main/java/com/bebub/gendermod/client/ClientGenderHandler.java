package com.bebub.genderbub.client;

import com.bebub.genderbub.GenderMod;
import com.bebub.genderbub.item.GenderScannerItem;
import com.bebub.genderbub.config.GenderConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GenderMod.MOD_ID, value = Dist.CLIENT)
public class ClientGenderHandler {
    private static Entity lastLookedEntity = null;

    @SubscribeEvent
    public static void onRender(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        ItemStack offhand = mc.player.getOffhandItem();
        ItemStack mainHand = mc.player.getMainHandItem();
        
        boolean showIcon = false;
        
        if (offhand.getItem() instanceof GenderScannerItem) {
            showIcon = true;
        } else if (mainHand.getItem() instanceof GenderScannerItem) {
            Entity target = mc.hitResult instanceof EntityHitResult ehr ? ehr.getEntity() : null;
            if (target == mob) {
                showIcon = true;
            }
        }
        
        if (!showIcon) return;
        
        int radius = GenderConfig.getDisplayRadius();
        if (mob.distanceToSqr(mc.player) > radius * radius) return;
        
        String gender = ClientGenderCache.getGender(mob.getUUID());
        boolean sterile = ClientGenderCache.isSterile(mob.getUUID());
        if (gender == null) return;
        
        String text = gender.equals("male") ? "♂" : "♀";
        int color;
        
        if (sterile) {
            color = 0xAAAAAA;
        } else {
            color = gender.equals("male") ? 0x55AAFF : 0xFF55FF;
        }
        
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        
        float yOffset;
        
        if (mob instanceof Animal) {
            yOffset = mob.getBbHeight() + 0.5f;
        } else if (mob instanceof Villager) {
            yOffset = mob.getBbHeight() + 0.3f;
        } else if (mob instanceof Monster) {
            yOffset = mob.getBbHeight() + 0.4f;
        } else {
            yOffset = mob.getBbHeight() + 0.4f;
        }
        
        if (mob.hasCustomName()) {
            yOffset += 0.3f;
        }
        
        pose.translate(0, yOffset, 0);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        
        float scale = sterile ? 0.035f : 0.025f;
        pose.scale(-scale, -scale, scale);
        
        float x = -mc.font.width(text) / 2f;
        mc.font.drawInBatch(text, x, 0, color, false, pose.last().pose(), 
            event.getMultiBufferSource(), net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 0xF000F0);
        
        pose.popPose();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        
        HitResult hit = mc.hitResult;
        Entity target = hit instanceof EntityHitResult ehr ? ehr.getEntity() : null;
        
        if (target != lastLookedEntity) {
            lastLookedEntity = target;
        }
    }
}