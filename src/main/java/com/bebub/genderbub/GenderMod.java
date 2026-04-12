package com.bebub.genderbub;

import com.bebub.genderbub.client.ClientGenderCache;
import com.bebub.genderbub.client.ClientInteractionBlocker;
import com.bebub.genderbub.command.GenderCommands;
import com.bebub.genderbub.config.GenderConfig;
import com.bebub.genderbub.network.NetworkHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
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
        
        NetworkHandler.register();
        
        MinecraftForge.EVENT_BUS.register(new GenderGameplayEvents());
        MinecraftForge.EVENT_BUS.register(new GenderCommands());
        MinecraftForge.EVENT_BUS.register(ZombieVillagerHandler.class);
        
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::addCreative);
        
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("GenderMod common setup complete!");
        });
    }
    
    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientGenderCache.load();
            MinecraftForge.EVENT_BUS.register(new ClientInteractionBlocker());
            
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> {
                    try {
                        return (net.minecraft.client.gui.screens.Screen) Class.forName("com.bebub.genderbub.client.GenderConfigScreen").getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        return null;
                    }
                }));
        });
    }
    
    private void onServerStarting(ServerStartingEvent event) {
        GenderConfig.performQueuedScan();
        GenderConfig.reload();
        GenderGameplayEvents.reloadConfig();
        LOGGER.info("GenderMod server started with {} enabled mobs", GenderConfig.getEnabledMobs().size());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(GENDER_SCANNER.get());
        }
    }
}