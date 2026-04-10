package com.bebub.genderbub.mixins;

import com.bebub.genderbub.GenderGameplayEvents;
import com.bebub.genderbub.config.GenderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(Animal.class)
public class UniversalEggMixin {
    
    private static final int CHECK_THRESHOLD = 1000;
    
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci) {
        Animal animal = (Animal) (Object) this;
        
        if (animal.level().isClientSide()) return;
        if (animal.isBaby()) return;
        
        int currentEggTime = getEggTime(animal);
        
        if (currentEggTime == -1) return;
        if (currentEggTime > CHECK_THRESHOLD) return;
        
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (mobId == null) {
            resetEggTime(animal);
            return;
        }
        
        String mobIdStr = mobId.toString();
        
        if (!GenderConfig.getEnabledMobs().contains(mobIdStr)) return;
        
        String gender = GenderGameplayEvents.getGender(animal);
        if (gender == null) return;
        
        boolean sterile = GenderGameplayEvents.isSterile(animal);
        
        if (GenderConfig.isEggLayingBlocked(mobIdStr, gender, sterile)) {
            resetEggTime(animal);
        }
    }
    
    private int getEggTime(Animal animal) {
        if (animal instanceof Chicken) {
            return ((Chicken) animal).eggTime;
        }
        
        try {
            Field field = animal.getClass().getDeclaredField("eggTime");
            field.setAccessible(true);
            return field.getInt(animal);
        } catch (Exception e) {}
        
        try {
            Field field = animal.getClass().getDeclaredField("timeUntilNextEgg");
            field.setAccessible(true);
            return field.getInt(animal);
        } catch (Exception e) {}
        
        try {
            Field field = animal.getClass().getDeclaredField("eggLayTime");
            field.setAccessible(true);
            return field.getInt(animal);
        } catch (Exception e) {}
        
        return -1;
    }
    
    private void resetEggTime(Animal animal) {
        int newTime = animal.getRandom().nextInt(6000) + 6000;
        
        if (animal instanceof Chicken) {
            ((Chicken) animal).eggTime = newTime;
            return;
        }
        
        try {
            Field field = animal.getClass().getDeclaredField("eggTime");
            field.setAccessible(true);
            field.setInt(animal, newTime);
            return;
        } catch (Exception e) {}
        
        try {
            Field field = animal.getClass().getDeclaredField("timeUntilNextEgg");
            field.setAccessible(true);
            field.setInt(animal, newTime);
            return;
        } catch (Exception e) {}
        
        try {
            Field field = animal.getClass().getDeclaredField("eggLayTime");
            field.setAccessible(true);
            field.setInt(animal, newTime);
            return;
        } catch (Exception e) {}
    }
}