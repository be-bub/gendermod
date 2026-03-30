package com.bebub.genderbub.network;

import com.bebub.genderbub.GenderMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.tryParse(GenderMod.MOD_ID + ":main"),
        () -> "1",
        "1"::equals,
        "1"::equals
    );

    public static void register() {
        CHANNEL.registerMessage(0, GenderSyncPacket.class, 
            GenderSyncPacket::encode, 
            GenderSyncPacket::decode, 
            GenderSyncPacket::handle);
    }
}