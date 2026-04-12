package com.bebub.genderbub.network;

import com.bebub.genderbub.GenderMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static int packetId = 0;
    
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(GenderMod.MOD_ID, "main"),
        () -> "1",
        "1"::equals,
        "1"::equals
    );

    public static void register() {
        CHANNEL.registerMessage(packetId++, GenderSyncPacket.class, 
            GenderSyncPacket::encode, 
            GenderSyncPacket::decode, 
            GenderSyncPacket::handle);
        
        CHANNEL.registerMessage(packetId++, InteractionRequestPacket.class, 
            InteractionRequestPacket::encode, 
            InteractionRequestPacket::decode, 
            InteractionRequestPacket::handle);
        
        CHANNEL.registerMessage(packetId++, EnabledMobsSyncPacket.class, 
            EnabledMobsSyncPacket::encode, 
            EnabledMobsSyncPacket::decode, 
            EnabledMobsSyncPacket::handle);
    }
}