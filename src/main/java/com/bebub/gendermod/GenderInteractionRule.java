package com.bebub.gendermod;

import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import com.bebub.gendermod.config.ModConfiguration.GenderRuleConfig;

public class GenderInteractionRule {
    private final String mobId;
    private final String gender;
    private final String itemId;

    public GenderInteractionRule(GenderRuleConfig config) {
        this.mobId = config.getMobId();
        this.gender = config.getGender();
        this.itemId = config.getItemId();
    }

    public boolean matches(ResourceLocation entityId, String gender, Item item) {
        if (!entityId.toString().equals(mobId) || !gender.equals(this.gender)) {
            return false;
        }
        ResourceLocation itemResource = ForgeRegistries.ITEMS.getKey(item);
        return itemResource != null && itemResource.toString().equals(itemId);
    }
    
    public String getMobId() {
        return mobId;
    }
    
    public String getGender() {
        return gender;
    }
    
    public String getItemId() {
        return itemId;
    }
} 