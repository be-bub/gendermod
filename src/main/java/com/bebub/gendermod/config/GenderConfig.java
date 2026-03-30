package com.bebub.genderbub.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.bebub.genderbub.GenderMod;
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
        public List<ActionRule> actionRules = new ArrayList<>();
        
        public ConfigData() {
            settings.maleChance = 45;
            settings.femaleChance = 45;
            settings.displayRadius = 24;
            settings.allowMaleMaleBreed = false;
            settings.allowFemaleFemaleBreed = false;
            settings.allowSterileBreed = false;
            
            mobs.add(new MobSettings("minecraft:cow", 
                Arrays.asList(
                    new InteractionRule("male", "minecraft:bucket"),
                    new InteractionRule("sterile", "minecraft:bucket")
                )));
            
            actionRules.add(new ActionRule("minecraft:chicken", "male", "lay_egg", true));
            actionRules.add(new ActionRule("minecraft:chicken", "sterile", "lay_egg", true));
        }
    }
    
    public static class GenderSettings {
        public int maleChance = 45;
        public int femaleChance = 45;
        public int displayRadius = 24;
        public boolean allowMaleMaleBreed = false;
        public boolean allowFemaleFemaleBreed = false;
        public boolean allowSterileBreed = false;
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
        public List<String> itemIds = new ArrayList<>();
        
        public InteractionRule(String gender, String itemId) {
            this.gender = gender;
            this.itemIds.add(itemId);
        }
        
        public InteractionRule(String gender, List<String> itemIds) {
            this.gender = gender;
            this.itemIds = itemIds;
        }
        
        public InteractionRule() {}
        
        public boolean isItemMatch(String itemId) {
            for (String id : itemIds) {
                if (id.equals(itemId)) {
                    return true;
                }
                if (id.startsWith("#")) {
                    TagKey<Item> tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), ResourceLocation.tryParse(id.substring(1)));
                    ResourceLocation itemLoc = ResourceLocation.tryParse(itemId);
                    if (tag != null && itemLoc != null) {
                        Item item = ForgeRegistries.ITEMS.getValue(itemLoc);
                        if (item != null && item.builtInRegistryHolder().is(tag)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        
        public boolean isTag() {
            return !itemIds.isEmpty() && itemIds.get(0).startsWith("#");
        }
    }
    
    public static class ActionRule {
        public String mobId;
        public String gender;
        public String action;
        public List<String> itemIds;
        public boolean blocked;
        
        public ActionRule(String mobId, String gender, String action, boolean blocked) {
            this.mobId = mobId;
            this.gender = gender;
            this.action = action;
            this.blocked = blocked;
            this.itemIds = null;
        }
        
        public ActionRule(String mobId, String gender, String action, String itemId, boolean blocked) {
            this.mobId = mobId;
            this.gender = gender;
            this.action = action;
            this.itemIds = new ArrayList<>();
            this.itemIds.add(itemId);
            this.blocked = blocked;
        }
        
        public ActionRule(String mobId, String gender, String action, List<String> itemIds, boolean blocked) {
            this.mobId = mobId;
            this.gender = gender;
            this.action = action;
            this.itemIds = itemIds;
            this.blocked = blocked;
        }
        
        public ActionRule() {}
        
        public boolean isItemMatch(String itemId) {
            if (itemIds == null) return true;
            for (String id : itemIds) {
                if (id.equals(itemId)) {
                    return true;
                }
                if (id.startsWith("#")) {
                    TagKey<Item> tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), ResourceLocation.tryParse(id.substring(1)));
                    ResourceLocation itemLoc = ResourceLocation.tryParse(itemId);
                    if (tag != null && itemLoc != null) {
                        Item item = ForgeRegistries.ITEMS.getValue(itemLoc);
                        if (item != null && item.builtInRegistryHolder().is(tag)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    public static void init() {
        configPath = FMLPaths.CONFIGDIR.get().resolve("bebub/genderbub.json");
        try {
            configPath.getParent().toFile().mkdirs();
        } catch (Exception e) {}
        load();
    }

    public static void load() {
        if (!configPath.toFile().exists()) {
            data = new ConfigData();
            save();
            GenderMod.LOGGER.info("Created new config file with default settings");
        } else {
            try (Reader reader = new FileReader(configPath.toFile())) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                data = new ConfigData();
                
                if (root.has("settings")) {
                    JsonObject settings = root.getAsJsonObject("settings");
                    if (settings.has("maleChance")) data.settings.maleChance = settings.get("maleChance").getAsInt();
                    if (settings.has("femaleChance")) data.settings.femaleChance = settings.get("femaleChance").getAsInt();
                    if (settings.has("displayRadius")) data.settings.displayRadius = settings.get("displayRadius").getAsInt();
                    if (settings.has("allowMaleMaleBreed")) data.settings.allowMaleMaleBreed = settings.get("allowMaleMaleBreed").getAsBoolean();
                    if (settings.has("allowFemaleFemaleBreed")) data.settings.allowFemaleFemaleBreed = settings.get("allowFemaleFemaleBreed").getAsBoolean();
                    if (settings.has("allowSterileBreed")) data.settings.allowSterileBreed = settings.get("allowSterileBreed").getAsBoolean();
                    if (settings.has("enabledMobs")) {
                        data.settings.enabledMobs = new ArrayList<>();
                        for (JsonElement e : settings.getAsJsonArray("enabledMobs")) {
                            data.settings.enabledMobs.add(e.getAsString());
                        }
                    }
                }
                
                if (root.has("mobs")) {
                    data.mobs = GSON.fromJson(root.get("mobs"), new com.google.gson.reflect.TypeToken<List<MobSettings>>(){}.getType());
                    if (data.mobs == null) data.mobs = new ArrayList<>();
                }
                
                if (root.has("actionRules")) {
                    data.actionRules = GSON.fromJson(root.get("actionRules"), new com.google.gson.reflect.TypeToken<List<ActionRule>>(){}.getType());
                    if (data.actionRules == null) data.actionRules = new ArrayList<>();
                }
                
                save();
                GenderMod.LOGGER.info("Config loaded and updated with new settings");
            } catch (Exception e) {
                GenderMod.LOGGER.error("Failed to load config, creating new one", e);
                data = new ConfigData();
                save();
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
    public static boolean isAllowMaleMaleBreed() { return data.settings.allowMaleMaleBreed; }
    public static boolean isAllowFemaleFemaleBreed() { return data.settings.allowFemaleFemaleBreed; }
    public static boolean isAllowSterileBreed() { return data.settings.allowSterileBreed; }
    
    public static List<MobSettings> getMobs() { return data.mobs; }
    public static List<ActionRule> getActionRules() { return data.actionRules; }
    
    public static void addMob(String mobId) {
        if (!data.settings.enabledMobs.contains(mobId)) {
            data.settings.enabledMobs.add(mobId);
        }
    }
    
    public static void removeMob(String mobId) {
        data.settings.enabledMobs.remove(mobId);
    }
    
    public static void addMobRule(String mobId, InteractionRule rule) {
        for (MobSettings mob : data.mobs) {
            if (mob.mobId.equals(mobId)) {
                mob.rules.add(rule);
                return;
            }
        }
        MobSettings newMob = new MobSettings(mobId, new ArrayList<>());
        newMob.rules.add(rule);
        data.mobs.add(newMob);
    }
    
    public static void addActionRule(ActionRule rule) {
        data.actionRules.add(rule);
    }
    
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
        String itemIdStr = itemId.toString();
        
        for (InteractionRule rule : rules) {
            if (!rule.gender.equals(effectiveGender)) continue;
            if (rule.isItemMatch(itemIdStr)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isActionBlocked(String mobId, String gender, boolean sterile, String action) {
        if (data.actionRules == null) return false;
        
        String effectiveGender = sterile ? "sterile" : gender;
        
        for (ActionRule rule : data.actionRules) {
            if (!rule.mobId.equals(mobId)) continue;
            if (!rule.gender.equals(effectiveGender) && !rule.gender.equals("any")) continue;
            if (!rule.action.equals(action) && !rule.action.equals("any")) continue;
            if (rule.blocked) return true;
        }
        return false;
    }
    
    public static boolean isActionBlocked(String mobId, String gender, boolean sterile, String action, Item item) {
        if (data.actionRules == null) return false;
        
        String effectiveGender = sterile ? "sterile" : gender;
        ResourceLocation itemId = item != null ? ForgeRegistries.ITEMS.getKey(item) : null;
        String itemIdStr = itemId != null ? itemId.toString() : null;
        
        for (ActionRule rule : data.actionRules) {
            if (!rule.mobId.equals(mobId)) continue;
            if (!rule.gender.equals(effectiveGender) && !rule.gender.equals("any")) continue;
            if (!rule.action.equals(action) && !rule.action.equals("any")) continue;
            
            if (rule.itemIds != null && item != null) {
                if (rule.isItemMatch(itemIdStr)) {
                    if (rule.blocked) return true;
                }
            } else if (rule.itemIds == null) {
                if (rule.blocked) return true;
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
    
    public static Path getConfigPath() {
        return configPath;
    }
    
    public static void resetToDefault() {
        data = new ConfigData();
        save();
    }
}