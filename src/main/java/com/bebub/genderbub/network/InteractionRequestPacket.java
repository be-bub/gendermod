package com.bebub.genderbub.network;

import com.bebub.genderbub.GenderGameplayEvents;
import com.bebub.genderbub.config.GenderConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class InteractionRequestPacket {
    private final int entityId;
    private final int slot;
    private final String mobId;
    private final String itemId;
    private final boolean isOffhand;

    public InteractionRequestPacket(int entityId, int slot, String mobId, String itemId, boolean isOffhand) {
        this.entityId = entityId;
        this.slot = slot;
        this.mobId = mobId;
        this.itemId = itemId;
        this.isOffhand = isOffhand;
    }

    public static void encode(InteractionRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeInt(packet.slot);
        buffer.writeUtf(packet.mobId);
        buffer.writeUtf(packet.itemId);
        buffer.writeBoolean(packet.isOffhand);
    }

    public static InteractionRequestPacket decode(FriendlyByteBuf buffer) {
        return new InteractionRequestPacket(buffer.readInt(), buffer.readInt(), buffer.readUtf(), buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(InteractionRequestPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            Entity entity = player.level().getEntity(packet.entityId);
            if (!(entity instanceof Animal animal)) return;
            
            ResourceLocation mobIdLoc = ResourceLocation.tryParse(packet.mobId);
            ResourceLocation itemIdLoc = ResourceLocation.tryParse(packet.itemId);
            
            if (mobIdLoc == null || itemIdLoc == null) return;
            
            ItemStack stack;
            if (packet.isOffhand) {
                stack = player.getOffhandItem();
            } else {
                if (packet.slot < 0 || packet.slot >= player.getInventory().items.size()) return;
                stack = player.getInventory().items.get(packet.slot);
            }
            
            if (!stack.getItem().equals(ForgeRegistries.ITEMS.getValue(itemIdLoc))) return;
            
            String gender = GenderGameplayEvents.getGender(animal);
            if (gender == null) return;
            
            boolean sterile = GenderGameplayEvents.isSterile(animal);
            
            if (GenderConfig.isItemBlocked(packet.mobId, gender, sterile, stack.getItem())) {
                return;
            }
            
            InteractionHand hand = packet.isOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            animal.mobInteract(player, hand);
        });
        ctx.get().setPacketHandled(true);
    }
}