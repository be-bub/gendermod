package com.bebub.genderbub.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import com.bebub.genderbub.client.ClientGenderCache;

import java.util.UUID;
import java.util.function.Supplier;

public class GenderSyncPacket {
    private final UUID animalId;
    private final String gender;
    private final boolean sterile;

    public GenderSyncPacket(UUID animalId, String gender, boolean sterile) {
        this.animalId = animalId;
        this.gender = gender;
        this.sterile = sterile;
    }

    public static void encode(GenderSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.animalId);
        buffer.writeUtf(packet.gender);
        buffer.writeBoolean(packet.sterile);
    }

    public static GenderSyncPacket decode(FriendlyByteBuf buffer) {
        return new GenderSyncPacket(buffer.readUUID(), buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(GenderSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientGenderCache.put(packet.animalId, packet.gender, packet.sterile);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}