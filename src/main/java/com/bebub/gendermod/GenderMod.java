package com.bebub.gendermod;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.bebub.gendermod.config.ModConfiguration;
import com.bebub.gendermod.config.GenderModConfig;
import com.bebub.gendermod.GenderGameplayEvents;

@Mod("bub_addition")
public class GenderMod {
    public GenderMod() {
        ModConfiguration.register();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new GenderGameplayEvents());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            GenderGameplayEvents.setConfiguration(
                GenderModConfig.getEnabledMobs(),
                GenderModConfig.getGenderRules()
            );
        });
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Animal animal) {
            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
            if (entityId != null && GenderModConfig.getEnabledMobs().contains(entityId.toString())) {
                animal.setPersistenceRequired();
            }
        }
    }
}