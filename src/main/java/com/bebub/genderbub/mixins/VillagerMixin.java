package com.bebub.genderbub.mixins;

import com.bebub.genderbub.GenderGameplayEvents;
import com.bebub.genderbub.config.GenderConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Villager.class)
public class VillagerMixin {
    
    @Inject(method = "canBreed", at = @At("RETURN"), cancellable = true)
    private void onCanBreed(CallbackInfoReturnable<Boolean> cir) {
        Villager villager = (Villager) (Object) this;
        
        if (!GenderConfig.isEnableVillagers()) return;
        if (!cir.getReturnValue()) return;
        
        String gender = GenderGameplayEvents.getGender(villager);
        boolean sterile = GenderGameplayEvents.isSterile(villager);
        
        if (sterile && !GenderConfig.isAllowSterileBreed()) {
            cir.setReturnValue(false);
            applyVanillaCooldown(villager);
            spawnAngryParticles(villager);
            return;
        }
        
        if (gender == null) return;
        
        Optional<AgeableMob> partnerOpt = villager.getBrain().getMemory(MemoryModuleType.BREED_TARGET);
        if (!partnerOpt.isPresent()) return;
        
        AgeableMob partnerMob = partnerOpt.get();
        if (!(partnerMob instanceof Villager partner)) return;
        
        String partnerGender = GenderGameplayEvents.getGender(partner);
        boolean partnerSterile = GenderGameplayEvents.isSterile(partner);
        
        if (partnerSterile && !GenderConfig.isAllowSterileBreed()) {
            cir.setReturnValue(false);
            applyVanillaCooldown(villager);
            applyVanillaCooldown(partner);
            spawnAngryParticles(villager);
            spawnAngryParticles(partner);
            return;
        }
        
        if (gender.equals(partnerGender)) {
            if (gender.equals("male") && !GenderConfig.isAllowMaleMaleBreed()) {
                cir.setReturnValue(false);
                applyVanillaCooldown(villager);
                applyVanillaCooldown(partner);
                spawnAngryParticles(villager);
                spawnAngryParticles(partner);
            } else if (gender.equals("female") && !GenderConfig.isAllowFemaleFemaleBreed()) {
                cir.setReturnValue(false);
                applyVanillaCooldown(villager);
                applyVanillaCooldown(partner);
                spawnAngryParticles(villager);
                spawnAngryParticles(partner);
            }
        }
    }
    
    private void applyVanillaCooldown(Villager villager) {
        villager.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
        villager.setAge(6000);
    }
    
    private void spawnAngryParticles(Villager villager) {
        if (villager.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 8; i++) {
                Vec3 pos = villager.position().add(0, 1.2, 0)
                    .add(serverLevel.random.nextGaussian() * 0.5, 0, serverLevel.random.nextGaussian() * 0.5);
                serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }
    }
}