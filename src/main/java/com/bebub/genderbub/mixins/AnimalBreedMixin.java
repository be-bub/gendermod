package com.bebub.genderbub.mixins;

import com.bebub.genderbub.GenderGameplayEvents;
import com.bebub.genderbub.TurtleTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

@Mixin(Animal.class)
public class AnimalBreedMixin {
    
    private boolean hasEggTag(Animal animal) {
        CompoundTag fullNBT = new CompoundTag();
        animal.saveWithoutId(fullNBT);
        return fullNBT.contains("HasEgg");
    }
    
    private void spawnAngryParticles(Animal animal) {
        if (animal.level() instanceof ServerLevel level) {
            for (int i = 0; i < 2; i++) {
                double x = animal.getX() + (level.random.nextDouble() - 0.5) * 0.8;
                double y = animal.getY() + animal.getBbHeight() + level.random.nextDouble() * 0.5;
                double z = animal.getZ() + (level.random.nextDouble() - 0.5) * 0.8;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }
    
    private void setBreedingCooldown(Animal animal) {
        animal.setInLoveTime(0);
        animal.setAge(6000);
    }
    
    @Inject(method = "canMate", at = @At("HEAD"), cancellable = true)
    private void onCanMate(Animal otherAnimal, CallbackInfoReturnable<Boolean> cir) {
        Animal thisAnimal = (Animal) (Object) this;
        
        if (!hasEggTag(thisAnimal)) {
            return;
        }
        
        UUID selfId = thisAnimal.getUUID();
        long currentTime = thisAnimal.level().getGameTime();
        
        if (!TurtleTracker.isTracking(selfId)) {
            TurtleTracker.startTracking(selfId, currentTime);
            return;
        }
        
        if (TurtleTracker.isTimeExpired(selfId, currentTime)) {
            TurtleTracker.clear(selfId);
            if (thisAnimal.isInLove()) {
                thisAnimal.setInLoveTime(0);
            }
            return;
        }
        
        double distance = thisAnimal.distanceTo(otherAnimal);
        if (distance > 5.0) {
            return;
        }
        
        if (!thisAnimal.isInLove() || !otherAnimal.isInLove()) {
            return;
        }
        
        String genderSelf = GenderGameplayEvents.getGender(thisAnimal);
        String genderOther = GenderGameplayEvents.getGender(otherAnimal);
        boolean sterileSelf = GenderGameplayEvents.isSterile(thisAnimal);
        boolean sterileOther = GenderGameplayEvents.isSterile(otherAnimal);
        
        if (genderSelf == null || genderOther == null) {
            return;
        }
        
        boolean cancel = false;
        
        if (genderSelf.equals(genderOther)) {
            boolean isMalePair = genderSelf.equals("male");
            if ((isMalePair && !GenderGameplayEvents.isAllowMaleMaleBreed()) ||
                (!isMalePair && !GenderGameplayEvents.isAllowFemaleFemaleBreed())) {
                cancel = true;
            }
        }
        
        if (!cancel && (sterileSelf || sterileOther)) {
            if (!GenderGameplayEvents.isAllowSterileBreed()) {
                cancel = true;
            }
        }
        
        if (cancel) {
            setBreedingCooldown(thisAnimal);
            setBreedingCooldown(otherAnimal);
            spawnAngryParticles(thisAnimal);
            spawnAngryParticles(otherAnimal);
            TurtleTracker.clear(selfId);
            cir.setReturnValue(false);
            cir.cancel();
        } else {
            TurtleTracker.clear(selfId);
        }
    }
}