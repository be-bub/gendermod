package com.bebub.genderbub.jade;

import com.bebub.genderbub.ExternalMobHandler;
import com.bebub.genderbub.client.ClientGenderCache;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.ZombieVillager;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class GenderJade implements IWailaPlugin {
    
    private static final ResourceLocation GENDER_INFO = ResourceLocation.fromNamespaceAndPath("genderbub", "gender_info");
    
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(new GenderProvider(), LivingEntity.class);
        registration.markAsClientFeature(GENDER_INFO);
    }
    
    public static class GenderProvider implements IEntityComponentProvider {
        
        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof LivingEntity living)) return;
            
            boolean isAnimal = living instanceof Animal;
            boolean isVillager = living instanceof Villager;
            boolean isZombieVillager = living instanceof ZombieVillager;
            boolean isNaturalistLion = ExternalMobHandler.isNaturalistLion(living);
            boolean isPrimalLion = ExternalMobHandler.isPrimalLion(living);
            
            if (!isAnimal && !isVillager && !isZombieVillager && !isNaturalistLion && !isPrimalLion) return;
            
            if ((isNaturalistLion && ExternalMobHandler.isNaturalistLionBaby(living)) ||
                (isPrimalLion && ExternalMobHandler.isPrimalLionBaby(living))) return;
            
            String gender = ClientGenderCache.getGender(living.getUUID());
            if (gender == null || gender.equals("none")) return;
            
            boolean sterile = ClientGenderCache.isSterile(living.getUUID());
            boolean isBaby = living.isBaby();
            
            String symbol = gender.equals("male") ? "♂" : "♀";
            int color;
            String key;
            
            if (isBaby) {
                if (isVillager || isZombieVillager) {
                    if (sterile) {
                        key = gender.equals("male") ? "genderbub.villager.sterile.boy" : "genderbub.villager.sterile.girl";
                        color = 0xAAAAAA;
                    } else {
                        key = gender.equals("male") ? "genderbub.villager.boy" : "genderbub.villager.girl";
                        color = gender.equals("male") ? 0x55AAFF : 0xFF55FF;
                    }
                } else {
                    if (sterile) {
                        key = gender.equals("male") ? "genderbub.gender.sterile.male" : "genderbub.gender.sterile.female";
                        color = 0xAAAAAA;
                    } else {
                        key = "genderbub.gender.baby";
                        color = gender.equals("male") ? 0x55AAFF : 0xFF55FF;
                    }
                }
            } else if (sterile) {
                if (isVillager || isZombieVillager) {
                    key = gender.equals("male") ? "genderbub.villager.sterile.male" : "genderbub.villager.sterile.female";
                } else {
                    key = gender.equals("male") ? "genderbub.gender.sterile.male" : "genderbub.gender.sterile.female";
                }
                color = 0xAAAAAA;
            } else {
                if (isVillager || isZombieVillager) {
                    key = gender.equals("male") ? "genderbub.villager.male" : "genderbub.villager.female";
                } else {
                    key = gender.equals("male") ? "genderbub.gender.male" : "genderbub.gender.female";
                }
                color = gender.equals("male") ? 0x55AAFF : 0xFF55FF;
            }
            
            tooltip.add(Component.literal(symbol + " ").withStyle(style -> style.withColor(color)));
            tooltip.append(Component.translatable(key).withStyle(style -> style.withColor(color)));
        }
        
        @Override
        public ResourceLocation getUid() {
            return GENDER_INFO;
        }
    }
}