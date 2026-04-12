package com.bebub.genderbub.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.bebub.genderbub.GenderMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
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
    private static boolean scanQueued = false;

    public static class ConfigData {
        public GenderSettings settings = new GenderSettings();
        public List<MobRule> mobs = new ArrayList<>();
        public List<EggBlockRule> eggBlockedMobs = new ArrayList<>();
        public boolean autoScanComplete = false;
        
        public ConfigData() {
            settings.maleChance = 45;
            settings.femaleChance = 45;
            settings.displayRadius = 24;
            settings.allowMaleMaleBreed = false;
            settings.allowFemaleFemaleBreed = false;
            settings.allowSterileBreed = false;
            settings.enableVillagers = true;
            settings.keepVillagerGender = true;
            settings.enabledMobs = new ArrayList<>();
            
            mobs.add(new MobRule("minecraft:cow", Arrays.asList("male", "sterile"), Arrays.asList("minecraft:bucket")));
            mobs.add(new MobRule("minecraft:goat", Arrays.asList("male", "sterile"), Arrays.asList("minecraft:bucket")));
            
            eggBlockedMobs.add(new EggBlockRule("minecraft:chicken", Arrays.asList("male", "sterile")));
        }
    }
    
    public static class GenderSettings {
        public int maleChance = 45;
        public int femaleChance = 45;
        public int displayRadius = 24;
        public boolean allowMaleMaleBreed = false;
        public boolean allowFemaleFemaleBreed = false;
        public boolean allowSterileBreed = false;
        public boolean enableVillagers = true;
        public boolean keepVillagerGender = true;
        public List<String> enabledMobs = new ArrayList<>();
    }
    
    public static class MobRule {
        public String mobId;
        public List<String> genders;
        public List<String> itemIds;
        
        public MobRule(String mobId, List<String> genders, List<String> itemIds) {
            this.mobId = mobId;
            this.genders = genders;
            this.itemIds = itemIds;
        }
        public MobRule() {}
        
        public boolean isGenderMatch(String gender) {
            return genders.contains(gender) || genders.contains("any");
        }
        
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
    }
    
    public static class EggBlockRule {
        public String mobId;
        public List<String> genders;
        
        public EggBlockRule(String mobId, List<String> genders) {
            this.mobId = mobId;
            this.genders = genders;
        }
        public EggBlockRule() {}
        
        public boolean isGenderMatch(String gender) {
            return genders.contains(gender) || genders.contains("any");
        }
    }

    public static void init() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("genderbub");
        configPath = configDir.resolve("genderbub.json");
        try {
            configDir.toFile().mkdirs();
        } catch (Exception e) {}
        load();
    }
    
    public static void queueScan() {
        scanQueued = true;
    }
    
    public static void performQueuedScan() {
        if (scanQueued) {
            scanQueued = false;
            scanAllMobsAndAddToConfig();
        }
    }

    public static void load() {
        if (!configPath.toFile().exists()) {
            data = new ConfigData();
            save();
            queueScan();
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
                    if (settings.has("enableVillagers")) data.settings.enableVillagers = settings.get("enableVillagers").getAsBoolean();
                    if (settings.has("keepVillagerGender")) data.settings.keepVillagerGender = settings.get("keepVillagerGender").getAsBoolean();
                    if (settings.has("enabledMobs")) {
                        data.settings.enabledMobs = new ArrayList<>();
                        for (JsonElement e : settings.getAsJsonArray("enabledMobs")) {
                            data.settings.enabledMobs.add(e.getAsString());
                        }
                    }
                }
                
                if (data.settings.maleChance > 50) data.settings.maleChance = 50;
                if (data.settings.maleChance < 0) data.settings.maleChance = 0;
                if (data.settings.femaleChance > 50) data.settings.femaleChance = 50;
                if (data.settings.femaleChance < 0) data.settings.femaleChance = 0;
                if (data.settings.displayRadius > 256) data.settings.displayRadius = 256;
                if (data.settings.displayRadius < 0) data.settings.displayRadius = 0;
                
                if (root.has("mobs")) {
                    data.mobs = GSON.fromJson(root.get("mobs"), new com.google.gson.reflect.TypeToken<List<MobRule>>(){}.getType());
                    if (data.mobs == null) data.mobs = new ArrayList<>();
                }
                
                if (root.has("eggBlockedMobs")) {
                    data.eggBlockedMobs = GSON.fromJson(root.get("eggBlockedMobs"), new com.google.gson.reflect.TypeToken<List<EggBlockRule>>(){}.getType());
                    if (data.eggBlockedMobs == null) data.eggBlockedMobs = new ArrayList<>();
                }
                
                if (root.has("autoScanComplete")) {
                    data.autoScanComplete = root.get("autoScanComplete").getAsBoolean();
                }
                
                sortEnabledMobsByModAndName();
                save();
            } catch (Exception e) {
                GenderMod.LOGGER.error("Failed to load config", e);
                data = new ConfigData();
                save();
            }
        }
        
        if (!data.autoScanComplete) {
            queueScan();
        }
    }
    
    private static void sortEnabledMobsByModAndName() {
        if (data.settings.enabledMobs == null || data.settings.enabledMobs.isEmpty()) return;
        
        data.settings.enabledMobs.sort((a, b) -> {
            String modA = a.contains(":") ? a.split(":")[0] : "";
            String modB = b.contains(":") ? b.split(":")[0] : "";
            String nameA = a.contains(":") ? a.split(":")[1] : a;
            String nameB = b.contains(":") ? b.split(":")[1] : b;
            
            int modCompare = modA.compareTo(modB);
            if (modCompare != 0) return modCompare;
            return nameA.compareTo(nameB);
        });
    }
    
    private static boolean isEggLayingMob(String mobId) {
        List<String> eggMobs = Arrays.asList(
            "minecraft:chicken",
            "alexsmobs:emu",
            "naturalist:duck"
        );
        return eggMobs.contains(mobId);
    }
    
    public static void scanAllMobsAndAddToConfig() {
        List<String> allMobs = getAllMobsFromRegistry();
        List<String> validAnimals = filterOnlyAnimals(allMobs);
        
        GenderMod.LOGGER.info("Found {} total mobs, {} valid animals", allMobs.size(), validAnimals.size());
        
        for (String animal : validAnimals) {
            if (!data.settings.enabledMobs.contains(animal)) {
                data.settings.enabledMobs.add(animal);
                GenderMod.LOGGER.info("Added animal: {}", animal);
            }
        }
        
        if (data.settings.enableVillagers && !data.settings.enabledMobs.contains("minecraft:villager")) {
            data.settings.enabledMobs.add("minecraft:villager");
            GenderMod.LOGGER.info("Added villager to enabled mobs");
        }
        
        for (String mobId : validAnimals) {
            if (isEggLayingMob(mobId)) {
                EggBlockRule rule = new EggBlockRule(mobId, Arrays.asList("male", "sterile"));
                boolean exists = false;
                for (EggBlockRule existing : data.eggBlockedMobs) {
                    if (existing.mobId.equals(mobId)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    data.eggBlockedMobs.add(rule);
                    GenderMod.LOGGER.info("Auto-added egg block rule for: {}", mobId);
                }
            }
        }
        
        sortEnabledMobsByModAndName();
        
        data.autoScanComplete = true;
        save();
        GenderMod.LOGGER.info("Scan complete, total {} enabled mobs", data.settings.enabledMobs.size());
    }
    
    public static List<String> getAllMobsFromRegistry() {
        List<String> allMobs = new ArrayList<>();
        
        for (var entry : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            String entityId = entry.getKey().location().toString();
            allMobs.add(entityId);
        }
        
        Collections.sort(allMobs);
        return allMobs;
    }
    
    private static boolean isProjectileOrNonLiving(String entityId) {
        return entityId.contains("arrow") || entityId.contains("fireball") ||
               entityId.contains("potion") || entityId.contains("trident") ||
               entityId.contains("snowball") || entityId.contains("egg") ||
               entityId.contains("ender_pearl") || entityId.contains("eye_of_ender") ||
               entityId.contains("firework") || entityId.contains("fishing_bobber") ||
               entityId.contains("llama_spit") || entityId.contains("shulker_bullet") ||
               entityId.contains("dragon_fireball") || entityId.contains("wither_skull") ||
               entityId.equals("minecraft:item") || entityId.equals("minecraft:experience_orb") ||
               entityId.equals("minecraft:area_effect_cloud") || entityId.equals("minecraft:marker") ||
               entityId.equals("minecraft:lightning_bolt") || entityId.equals("minecraft:falling_block") ||
               entityId.equals("minecraft:tnt") || entityId.equals("minecraft:primed_tnt") ||
               entityId.contains("boat") || entityId.contains("minecart") ||
               entityId.contains("painting") || entityId.contains("item_frame");
    }
    
    private static boolean isPartOrSegment(String entityId) {
        return entityId.contains("_part") || entityId.contains("_segment") || 
               entityId.endsWith("_part") || entityId.endsWith("_segment") ||
               entityId.contains("piece") || entityId.contains("tail") ||
               entityId.contains("limb");
    }
    
    private static boolean isHostileOrBoss(String entityId) {
        String[] hostileKeywords = {
            "zombie", "skeleton", "spider", "creeper", "enderman", "witch", "pillager",
            "vindicator", "ravager", "evoker", "vex", "phantom", "drowned", "husk",
            "stray", "warden", "blaze", "ghast", "magma_cube", "slime",
            "silverfish", "endermite", "guardian", "elder_guardian", "shulker",
            "piglin_brute", "hoglin", "zoglin", "piglin", "wither", "ender_dragon",
            "giant", "illusioner", "boss"
        };
        
        for (String keyword : hostileKeywords) {
            if (entityId.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isAnimal(EntityType<?> type, String entityId) {
        if (isProjectileOrNonLiving(entityId)) {
            return false;
        }
        
        if (isPartOrSegment(entityId)) {
            return false;
        }
        
        if (entityId.equals("minecraft:player") || entityId.equals("minecraft:armor_stand")) {
            return false;
        }
        
        if (isHostileOrBoss(entityId)) {
            return false;
        }
        
        try {
            if (type.getBaseClass() != null && Animal.class.isAssignableFrom(type.getBaseClass())) {
                return true;
            }
        } catch (Exception e) {}
        
        try {
            if (type.getBaseClass() != null && WaterAnimal.class.isAssignableFrom(type.getBaseClass())) {
                return true;
            }
        } catch (Exception e) {}
        
        if (type.getCategory() == MobCategory.CREATURE || 
            type.getCategory() == MobCategory.WATER_CREATURE || 
            type.getCategory() == MobCategory.UNDERGROUND_WATER_CREATURE) {
            return true;
        }
        
        try {
            if (type.getBaseClass() != null && AbstractHorse.class.isAssignableFrom(type.getBaseClass())) {
                return true;
            }
        } catch (Exception e) {}
        
        try {
            if (type.getBaseClass() != null && AbstractGolem.class.isAssignableFrom(type.getBaseClass())) {
                return false;
            }
        } catch (Exception e) {}
        
        if (type.getCategory() == MobCategory.MONSTER || 
            type.getCategory() == MobCategory.MISC || 
            type.getCategory() == MobCategory.AMBIENT) {
            return false;
        }
        
        return false;
    }
    
    public static List<String> filterOnlyAnimals(List<String> allMobs) {
        List<String> animals = new ArrayList<>();
        
        for (String mobId : allMobs) {
            try {
                ResourceLocation loc = ResourceLocation.tryParse(mobId);
                if (loc != null) {
                    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(loc);
                    if (type != null && isAnimal(type, mobId)) {
                        animals.add(mobId);
                    }
                }
            } catch (Exception e) {}
        }
        
        Collections.sort(animals);
        return animals;
    }
    
    public static List<String> scanAndGetNewAnimals() {
        List<String> allMobs = getAllMobsFromRegistry();
        List<String> validAnimals = filterOnlyAnimals(allMobs);
        List<String> newAnimals = new ArrayList<>();
        
        for (String animal : validAnimals) {
            if (!data.settings.enabledMobs.contains(animal)) {
                newAnimals.add(animal);
                data.settings.enabledMobs.add(animal);
            }
        }
        
        if (data.settings.enableVillagers && !data.settings.enabledMobs.contains("minecraft:villager")) {
            newAnimals.add("minecraft:villager");
            data.settings.enabledMobs.add("minecraft:villager");
        }
        
        if (!newAnimals.isEmpty()) {
            sortEnabledMobsByModAndName();
            
            boolean needSave = false;
            
            for (String mobId : newAnimals) {
                if (isEggLayingMob(mobId)) {
                    EggBlockRule rule = new EggBlockRule(mobId, Arrays.asList("male", "sterile"));
                    boolean exists = false;
                    for (EggBlockRule existing : data.eggBlockedMobs) {
                        if (existing.mobId.equals(mobId)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        data.eggBlockedMobs.add(rule);
                        needSave = true;
                        GenderMod.LOGGER.info("Auto-added egg block rule for: {}", mobId);
                    }
                }
            }
            
            if (needSave) {
                save();
            }
            
            GenderMod.LOGGER.info("Found {} new animals: {}", newAnimals.size(), newAnimals);
        }
        
        return newAnimals;
    }
    
    public static void addMob(String mobId) {
        if (!data.settings.enabledMobs.contains(mobId)) {
            data.settings.enabledMobs.add(mobId);
            sortEnabledMobsByModAndName();
            save();
        }
    }
    
    public static void removeMob(String mobId) {
        data.settings.enabledMobs.remove(mobId);
        sortEnabledMobsByModAndName();
        save();
    }

    public static void save() {
        sortEnabledMobsByModAndName();
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
    public static int getDisplayRadius() { 
        int radius = data.settings.displayRadius;
        if (radius < 0) radius = 0;
        if (radius > 256) radius = 256;
        return radius;
    }
    public static boolean isAllowMaleMaleBreed() { return data.settings.allowMaleMaleBreed; }
    public static boolean isAllowFemaleFemaleBreed() { return data.settings.allowFemaleFemaleBreed; }
    public static boolean isAllowSterileBreed() { return data.settings.allowSterileBreed; }
    public static boolean isEnableVillagers() { return data.settings.enableVillagers; }
    public static boolean isKeepVillagerGender() { return data.settings.keepVillagerGender; }
    public static List<String> getEnabledMobs() { return data.settings.enabledMobs; }
    public static List<MobRule> getMobs() { return data.mobs; }
    public static List<EggBlockRule> getEggBlockedMobs() { return data.eggBlockedMobs; }
    
    public static boolean isItemBlocked(String mobId, String gender, boolean sterile, Item item) {
        String effectiveGender = sterile ? "sterile" : gender;
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId == null) return false;
        String itemIdStr = itemId.toString();
        
        for (MobRule rule : data.mobs) {
            if (!rule.mobId.equals(mobId)) continue;
            if (!rule.isGenderMatch(effectiveGender)) continue;
            if (rule.isItemMatch(itemIdStr)) {
                return true;
            }
        }
        return false;
    }

    public static void addDefaultEggBlockRules() {
        List<EggBlockRule> defaults = new ArrayList<>();
        
        defaults.add(new EggBlockRule("minecraft:chicken", Arrays.asList("male", "sterile")));
        defaults.add(new EggBlockRule("alexsmobs:emu", Arrays.asList("male", "sterile")));
        defaults.add(new EggBlockRule("naturalist:duck", Arrays.asList("male", "sterile")));
        
        for (EggBlockRule defaultRule : defaults) {
            boolean exists = false;
            for (EggBlockRule existing : data.eggBlockedMobs) {
                if (existing.mobId.equals(defaultRule.mobId)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                data.eggBlockedMobs.add(defaultRule);
                GenderMod.LOGGER.info("Added default egg block rule for: {}", defaultRule.mobId);
            }
        }
        save();
    }
    
    public static boolean isEggLayingBlocked(String mobId, String gender, boolean sterile) {
        if (data.eggBlockedMobs == null) return false;
        
        String effectiveGender = sterile ? "sterile" : gender;
        
        for (EggBlockRule rule : data.eggBlockedMobs) {
            if (!rule.mobId.equals(mobId)) continue;
            if (rule.genders.contains(effectiveGender) || rule.genders.contains("any")) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isScannerItem(Item item) {
        if (item == null) return false;
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId == null) return false;
        return itemId.toString().equals("genderbub:magnifying_glass");
    }
    
    public static String[] getRandomGenderWithSterile() {
        int male = data.settings.maleChance;
        int female = data.settings.femaleChance;
        
        if (male < 0) male = 0;
        if (female < 0) female = 0;
        if (male > 50) male = 50;
        if (female > 50) female = 50;
        
        if (male == 0 && female == 0) {
            return new String[]{"none", "false"};
        }
        
        if (male == 0) {
            return new String[]{"female", "false"};
        }
        
        if (female == 0) {
            return new String[]{"male", "false"};
        }
        
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
        data.autoScanComplete = false;
        save();
        queueScan();
    }

    public static void setMaleChance(int value) { 
        if (value < 0) value = 0;
        if (value > 50) value = 50;
        data.settings.maleChance = value; 
        save();
    }
    
    public static void setFemaleChance(int value) { 
        if (value < 0) value = 0;
        if (value > 50) value = 50;
        data.settings.femaleChance = value; 
        save();
    }
    
    public static void setDisplayRadius(int value) { 
        if (value < 0) value = 0;
        if (value > 256) value = 256;
        data.settings.displayRadius = value; 
        save();
    }
    
    public static void setAllowMaleMaleBreed(boolean value) { 
        data.settings.allowMaleMaleBreed = value; 
        save();
    }
    
    public static void setAllowFemaleFemaleBreed(boolean value) { 
        data.settings.allowFemaleFemaleBreed = value; 
        save();
    }
    
    public static void setAllowSterileBreed(boolean value) { 
        data.settings.allowSterileBreed = value; 
        save();
    }
    
    public static void setEnableVillagers(boolean value) { 
        data.settings.enableVillagers = value; 
        save();
    }
    
    public static void setKeepVillagerGender(boolean value) { 
        data.settings.keepVillagerGender = value; 
        save();
    }
}