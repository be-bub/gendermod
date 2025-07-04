package com.bebub.gendermod;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Arrays;
import java.util.List;

public class GenderConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENABLED_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GENDER_RULES;

    static {
        BUILDER.push("Gender Mod Configuration");
        
        ENABLED_MOBS = BUILDER.comment(
                "List of mobs that will have genders",
                "Format: 'modid:mobname'")
            .defineList("enabled_mobs", 
                Arrays.asList(
                    "minecraft:cow",
                    "minecraft:pig",
                    "minecraft:sheep"
                ), 
                entry -> entry instanceof String);
        
        GENDER_RULES = BUILDER.comment(
                "Gender interaction rules",
                "Format: 'mob,gender,item'")
            .defineList("gender_rules", 
                Arrays.asList(
                    "minecraft:cow,male,minecraft:bucket"
                ), 
                entry -> entry instanceof String);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}