package com.bebub.gendermod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.bebub.gendermod.GenderMod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class GenderConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;
    private static ConfigData data;

    public static class ConfigData {
        public GenderSettings settings = new GenderSettings();
        public List<MobSettings> mobs = new ArrayList<>();
        
        public ConfigData() {
            settings.maleChance = 47;
            settings.femaleChance = 47;
            settings.displayRadius = 24;
            
            mobs.add(new MobSettings("minecraft:cow", 
                Arrays.asList(
                    new InteractionRule("male", "minecraft:bucket"),
                    new InteractionRule("male", "#forge:buckets"),
                    new InteractionRule("sterile", "minecraft:bucket"),
                    new InteractionRule("sterile", "#forge:buckets")
                )));
        }
    }
    
    public static class GenderSettings {
        public int maleChance = 45;
        public int femaleChance = 45;
        public int displayRadius = 24;
        public List<String> enabledMobs = new ArrayList<>(Arrays.asList(
            "minecraft:cow", "minecraft:sheep", "minecraft:pig", "minecraft:chicken",
            "minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:panda",
            "minecraft:turtle", "minecraft:cat", "minecraft:fox", "minecraft:rabbit",
            "minecraft:parrot", "minecraft:llama", "minecraft:polar_bear", "minecraft:wolf",
            "minecraft:goat", "minecraft:mooshroom"
        ));
    }
    
    public static class MobSettings {
        public String mobId;
        public List<InteractionRule> rules = new ArrayList<>();
        
        public MobSettings(String mobId, List<InteractionRule> rules) {
            this.mobId = mobId;
            this.rules = rules;
        }
        public MobSettings() {}
    }
    
    public static class InteractionRule {
        public String gender;
        public String itemId;
        
        public InteractionRule(String gender, String itemId) {
            this.gender = gender;
            this.itemId = itemId;
        }
        public InteractionRule() {}
        
        public boolean isTag() {
            return itemId.startsWith("#");
        }
        
        public String getTagId() {
            return isTag() ? itemId.substring(1) : itemId;
        }
    }

    public static void init() {
        configPath = FMLPaths.CONFIGDIR.get().resolve("bebub/gendermod.json");
        try {
            configPath.getParent().toFile().mkdirs();
        } catch (Exception e) {}
        load();
    }

    public static void load() {
        if (!configPath.toFile().exists()) {
            data = new ConfigData();
            save();
        } else {
            try (Reader reader = new FileReader(configPath.toFile())) {
                data = GSON.fromJson(reader, ConfigData.class);
                if (data.settings == null) data.settings = new GenderSettings();
                if (data.settings.enabledMobs == null) data.settings.enabledMobs = new ArrayList<>();
                if (data.mobs == null) data.mobs = new ArrayList<>();
            } catch (Exception e) {
                GenderMod.LOGGER.error("Failed to load config", e);
                data = new ConfigData();
            }
        }
    }

    public static void save() {
        try (Writer writer = new FileWriter(configPath.toFile())) {
            GSON.toJson(data, writer);
        } catch (Exception e) {
            GenderMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static void reload() {
        load();
    }

    public static int getMaleChance() { return data.settings.maleChance; }
    public static int getFemaleChance() { return data.settings.femaleChance; }
    public static int getDisplayRadius() { return data.settings.displayRadius; }
    public static List<String> getEnabledMobs() { return data.settings.enabledMobs; }
    public static List<InteractionRule> getRulesForMob(String mobId) {
        for (MobSettings mob : data.mobs) {
            if (mob.mobId.equals(mobId)) {
                return mob.rules;
            }
        }
        return new ArrayList<>();
    }
    
    public static boolean isItemBlocked(String mobId, String gender, boolean sterile, Item item) {
        List<InteractionRule> rules = getRulesForMob(mobId);
        String effectiveGender = sterile ? "sterile" : gender;
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        
        if (itemId == null) return false;
        
        for (InteractionRule rule : rules) {
            if (!rule.gender.equals(effectiveGender)) continue;
            
            if (rule.isTag()) {
                TagKey<Item> tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), new ResourceLocation(rule.getTagId()));
                if (item.builtInRegistryHolder().is(tag)) {
                    return true;
                }
            } else if (rule.itemId.equals(itemId.toString())) {
                return true;
            }
        }
        return false;
    }
    
    public static String[] getRandomGenderWithSterile() {
        int male = data.settings.maleChance;
        int female = data.settings.femaleChance;
        int total = male + female;
        
        if (total > 100) {
            male = (male * 100) / total;
            female = 100 - male;
            total = 100;
        }
        
        int sterile = 100 - male - female;
        if (sterile < 0) sterile = 0;
        
        int roll = new Random().nextInt(100);
        boolean isSterile = false;
        String gender;
        
        if (roll < male) {
            gender = "male";
            isSterile = false;
        } else if (roll < male + female) {
            gender = "female";
            isSterile = false;
        } else {
            if (male > female) {
                gender = "male";
            } else if (female > male) {
                gender = "female";
            } else {
                gender = new Random().nextBoolean() ? "male" : "female";
            }
            isSterile = true;
        }
        
        return new String[]{gender, String.valueOf(isSterile)};
    }
}