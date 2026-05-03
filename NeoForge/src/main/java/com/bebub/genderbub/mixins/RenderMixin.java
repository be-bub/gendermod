package com.bebub.genderbub.mixins;

import com.bebub.genderbub.GenderCore;
import com.bebub.genderbub.config.GenderConfig;
import com.bebub.genderbub.util.GenderDisplayUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
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

    private static final ResourceLocation MALE_ICON = ResourceLocation.parse("genderbub:textures/gui/male.png");
    private static final ResourceLocation FEMALE_ICON = ResourceLocation.parse("genderbub:textures/gui/female.png");
    private static final int ICON_SIZE = 12;

    @Inject(method = "render", at = @At("RETURN"))
    private void renderGenderIcon(Entity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity living)) return;
        
        if (living instanceof AgeableMob ageable && ageable.isBaby()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        ItemStack offHand = mc.player.getOffhandItem();
        if (offHand.isEmpty()) return;
        
        String itemId = BuiltInRegistries.ITEM.getKey(offHand.getItem()).toString();
        boolean isMagnifyingGlass = itemId.equals("genderbub:magnifying_glass");
        
        if (!isMagnifyingGlass) return;
        
        int maxDistance = GenderConfig.getDisplayRadius();
        if (maxDistance <= 0) return;
        
        double distance = mc.player.distanceTo(living);
        if (distance > maxDistance) return;
        
        String gender = GenderCore.getGender(living);
        if (gender.equals("none") || gender.startsWith("cached_")) return;
        
        if (gender.equals("baby")) return;
        
        ResourceLocation icon = gender.equals("male") ? MALE_ICON : FEMALE_ICON;
        int color = GenderDisplayUtil.getColor(living);
        
        poseStack.pushPose();
        
        double height = living.getBbHeight() + 0.7;
        poseStack.translate(0, height, 0);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025f, -0.025f, 0.025f);
        
        float x = -ICON_SIZE / 2f;
        float y = -ICON_SIZE / 2f;
        
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        VertexConsumer vertexConsumer = buffer.getBuffer(net.minecraft.client.renderer.RenderType.text(icon));
        org.joml.Matrix4f matrix = poseStack.last().pose();
        
        vertexConsumer.addVertex(matrix, x, y + ICON_SIZE, 0).setColor(r, g, b, 1.0f).setUv(0, 1).setLight(packedLight);
        vertexConsumer.addVertex(matrix, x + ICON_SIZE, y + ICON_SIZE, 0).setColor(r, g, b, 1.0f).setUv(1, 1).setLight(packedLight);
        vertexConsumer.addVertex(matrix, x + ICON_SIZE, y, 0).setColor(r, g, b, 1.0f).setUv(1, 0).setLight(packedLight);
        vertexConsumer.addVertex(matrix, x, y, 0).setColor(r, g, b, 1.0f).setUv(0, 0).setLight(packedLight);
        
        poseStack.popPose();
    }
}