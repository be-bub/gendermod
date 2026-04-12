package com.bebub.genderbub.mixins;

import com.bebub.genderbub.GenderGameplayEvents;
import com.bebub.genderbub.config.GenderConfig;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Animal.class)
public abstract class CanMateMixin {
    
    @Inject(method = "canMate", at = @At("HEAD"), cancellable = true)
    private void onCanMate(Animal other, CallbackInfoReturnable<Boolean> cir) {
        Animal self = (Animal) (Object) this;
        
        if (self.level().isClientSide()) return;
        
        String genderSelf = GenderGameplayEvents.getGender(self);
        String genderOther = GenderGameplayEvents.getGender(other);
        
        if (genderSelf == null || genderOther == null) return;
        
        if (genderSelf.equals(genderOther)) {
            boolean isMalePair = genderSelf.equals("male");
            if ((isMalePair && !GenderConfig.isAllowMaleMaleBreed()) ||
                (!isMalePair && !GenderConfig.isAllowFemaleFemaleBreed())) {
                cir.setReturnValue(false);
                cir.cancel();
                return;
            }
        }
        
        boolean sterileSelf = GenderGameplayEvents.isSterile(self);
        boolean sterileOther = GenderGameplayEvents.isSterile(other);
        
        if ((sterileSelf || sterileOther) && !GenderConfig.isAllowSterileBreed()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}