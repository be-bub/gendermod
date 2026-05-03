package com.bebub.genderbub.mixins;

import com.bebub.genderbub.GenderCore;
import com.bebub.genderbub.config.GenderConfig;
import com.bebub.genderbub.util.GenderDisplayUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class RenderMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void renderGenderIcon(Entity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity living)) return;
        
        if (living instanceof AgeableMob ageable && ageable.isBaby()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack offHand = mc.player.getOffhandItem();
        if (offHand.isEmpty()) return;

        String itemId = BuiltInRegistries.ITEM.getKey(offHand.getItem()).toString();
        if (!itemId.equals("genderbub:magnifying_glass")) return;

        int maxDistance = GenderConfig.getDisplayRadius();
        if (maxDistance <= 0) return;

        if (mc.player.distanceTo(living) > maxDistance) return;

        String gender = GenderCore.getGender(living);
        if (gender.equals("none") || gender.startsWith("cached_")) return;
        
        if (gender.equals("baby")) return;

        String symbol = gender.equals("male") ? "♂" : "♀";
        int color = GenderDisplayUtil.getColor(living);

        Font font = mc.font;

        poseStack.pushPose();

        double height = living.getBbHeight() + 0.8;
        poseStack.translate(0, height, 0);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.03F, -0.03F, 0.03F);

        float x = -font.width(symbol) / 2f;

        font.drawInBatch(symbol, x, 0, color, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }
}