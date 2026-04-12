package com.bebub.genderbub.mixins;

import com.bebub.genderbub.GenderGameplayEvents;
import com.bebub.genderbub.config.GenderConfig;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Animal.class)
public class AnimalBreedMixin {

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Animal self = (Animal) (Object) this;
        
        if (self.level().isClientSide()) return;
        if (self.isBaby()) return;
        
        ItemStack stack = player.getItemInHand(hand);
        if (!self.isFood(stack)) return;
        
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(self.getType());
        if (mobId == null) return;
        if (!GenderConfig.getEnabledMobs().contains(mobId.toString())) return;
        
        String myGender = GenderGameplayEvents.getGender(self);
        if (myGender == null) return;
        
        boolean mySterile = GenderGameplayEvents.isSterile(self);
        if (mySterile && !GenderConfig.isAllowSterileBreed()) {
            self.setInLoveTime(0);
            return;
        }
        
        String neededGender = myGender.equals("male") ? "female" : "male";
        Animal partner = findPartnerOfGender(self, neededGender);
        
        if (partner == null) {
            self.setInLoveTime(0);
            return;
        }
        
        boolean partnerSterile = GenderGameplayEvents.isSterile(partner);
        if (partnerSterile && !GenderConfig.isAllowSterileBreed()) {
            self.setInLoveTime(0);
        }
    }
    
    private Animal findPartnerOfGender(Animal self, String neededGender) {
        double range = 16.0;
        AABB searchBox = self.getBoundingBox().inflate(range, range, range);
        
        List<Animal> nearby = self.level().getEntitiesOfClass(Animal.class, searchBox, 
            other -> other != self && !other.isBaby());
        
        for (Animal other : nearby) {
            String otherGender = GenderGameplayEvents.getGender(other);
            if (otherGender == null) continue;
            if (!otherGender.equals(neededGender)) continue;
            
            boolean otherSterile = GenderGameplayEvents.isSterile(other);
            if (otherSterile && !GenderConfig.isAllowSterileBreed()) continue;
            
            return other;
        }
        return null;
    }
}