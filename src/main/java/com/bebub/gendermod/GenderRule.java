package com.bebub.gendermod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public class GenderRule {
    private final ResourceLocation mobId;
    private final String gender;
    private final ResourceLocation itemId;

    public GenderRule(String configLine) {
        String[] parts = configLine.split(",");
        this.mobId = ResourceLocation.parse(parts[0].trim());
        this.gender = parts[1].trim();
        this.itemId = ResourceLocation.parse(parts[2].trim());
    }

    public boolean matches(ResourceLocation mobId, String gender, Item item) {
        return this.mobId.equals(mobId) && 
               this.gender.equals(gender) && 
               ForgeRegistries.ITEMS.getKey(item).equals(itemId);
    }
}