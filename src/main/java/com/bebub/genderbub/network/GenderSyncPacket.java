package com.bebub.genderbub.network;

import com.bebub.genderbub.client.ClientGenderCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class GenderSyncPacket {
    private final UUID entityId;
    private final String mobId;
    private final String gender;
    private final boolean sterile;

    public GenderSyncPacket(UUID entityId, String mobId, String gender, boolean sterile) {
        this.entityId = entityId;
        this.mobId = mobId;
        this.gender = gender;
        this.sterile = sterile;
    }

    public static void encode(GenderSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.entityId);
        buffer.writeUtf(packet.mobId);
        buffer.writeUtf(packet.gender);
        buffer.writeBoolean(packet.sterile);
    }

    public static GenderSyncPacket decode(FriendlyByteBuf buffer) {
        return new GenderSyncPacket(buffer.readUUID(), buffer.readUtf(), buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(GenderSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientGenderCache.put(packet.entityId, packet.mobId, packet.gender, packet.sterile);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}