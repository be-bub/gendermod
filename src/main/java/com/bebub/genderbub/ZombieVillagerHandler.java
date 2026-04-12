package com.bebub.genderbub;

import com.bebub.genderbub.config.GenderConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = GenderMod.MOD_ID)
public class ZombieVillagerHandler {
    
    private static final ConcurrentHashMap<UUID, CompoundTag> PENDING_GENDER = new ConcurrentHashMap<>();
    
    @SubscribeEvent
    public static void onConversionPre(LivingConversionEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) return;
        
        if (!GenderConfig.isKeepVillagerGender()) return;
        
        Entity entity = event.getEntity();
        
        if (entity instanceof Villager || entity instanceof ZombieVillager) {
            if (entity.getPersistentData().contains("GenderMod_Gender")) {
                CompoundTag genderData = new CompoundTag();
                genderData.putString("GenderMod_Gender", entity.getPersistentData().getString("GenderMod_Gender"));
                genderData.putBoolean("GenderMod_Sterile", entity.getPersistentData().getBoolean("GenderMod_Sterile"));
                PENDING_GENDER.put(entity.getUUID(), genderData);
            }
        }
    }
    
    @SubscribeEvent
    public static void onConversionPost(LivingConversionEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        
        if (!GenderConfig.isKeepVillagerGender()) return;
        
        Entity original = event.getEntity();
        Entity outcome = event.getOutcome();
        
        if (outcome instanceof Villager || outcome instanceof ZombieVillager) {
            CompoundTag genderData = PENDING_GENDER.remove(original.getUUID());
            
            if (genderData != null && genderData.contains("GenderMod_Gender")) {
                outcome.getPersistentData().putString("GenderMod_Gender", genderData.getString("GenderMod_Gender"));
                outcome.getPersistentData().putBoolean("GenderMod_Sterile", genderData.getBoolean("GenderMod_Sterile"));
            }
        }
    }
}