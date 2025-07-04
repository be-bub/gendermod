package com.bebub.gendermod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("gendermod")
public class GenderMod {
    public GenderMod() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GenderConfig.SPEC);
        
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new GenderEvents());
        
        GenderEvents.setConfig(
            GenderConfig.ENABLED_MOBS.get().stream().toList(),
            GenderConfig.GENDER_RULES.get().stream().toList()
        );
    }
}