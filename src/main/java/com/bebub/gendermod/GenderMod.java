package com.bebub.genderbub;

import com.bebub.genderbub.config.GenderConfig;
import com.bebub.genderbub.network.NetworkHandler;
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
import com.bebub.genderbub.item.GenderScannerItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("genderbub")
public class GenderMod {
    public static final String MOD_ID = "genderbub";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<Item> GENDER_SCANNER = ITEMS.register("magnifying_glass", GenderScannerItem::new);

    public GenderMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        
        GenderConfig.init();
        GenderGameplayEvents.reloadConfig();
        
        MinecraftForge.EVENT_BUS.register(new GenderGameplayEvents());
        MinecraftForge.EVENT_BUS.register(EggLayingHandler.class);
        MinecraftForge.EVENT_BUS.register(com.bebub.genderbub.command.GenderCommands.class);
        
        modBus.addListener(this::setup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::addCreative);
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }
    
    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MinecraftForge.EVENT_BUS.register(new com.bebub.genderbub.client.ClientInteractionBlocker());
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(GENDER_SCANNER.get());
        }
    }
}