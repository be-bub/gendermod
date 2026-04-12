package com.bebub.genderbub.network;

import com.bebub.genderbub.client.ClientGenderCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class EnabledMobsSyncPacket {
    private final Set<String> enabledMobs;

    public EnabledMobsSyncPacket(Set<String> enabledMobs) {
        this.enabledMobs = enabledMobs;
    }

    public static void encode(EnabledMobsSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.enabledMobs.size());
        for (String mobId : packet.enabledMobs) {
            buffer.writeUtf(mobId);
        }
    }

    public static EnabledMobsSyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        Set<String> enabledMobs = new HashSet<>();
        for (int i = 0; i < size; i++) {
            enabledMobs.add(buffer.readUtf());
        }
        return new EnabledMobsSyncPacket(enabledMobs);
    }

    public static void handle(EnabledMobsSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientGenderCache.cleanupByMobList(packet.enabledMobs);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}