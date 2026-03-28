package com.bebub.gendermod;

import com.bebub.gendermod.config.GenderConfig;
import com.bebub.gendermod.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bebub.gendermod.item.GenderScannerItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("bub_addition")
public class GenderMod {
    public static final String MOD_ID = "bub_addition";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<Item> GENDER_SCANNER = ITEMS.register("magnifying_glass", GenderScannerItem::new);

    public GenderMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        
        GenderConfig.init();
        GenderGameplayEvents.reloadConfig();
        
        MinecraftForge.EVENT_BUS.register(new GenderGameplayEvents());
        modBus.addListener(this::setup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::addCreative);
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }
    
    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MinecraftForge.EVENT_BUS.register(new com.bebub.gendermod.client.ClientInteractionBlocker());
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(GENDER_SCANNER.get());
        }
    }
}