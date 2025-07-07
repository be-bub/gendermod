package com.bebub.gendermod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModConfiguration {
    private static final String CONFIG_FILE_NAME = "gendermod.json";
    private static final String CONFIG_FOLDER = "config/bub_addition/gendermod";
    
    private List<String> enabledMobs;
    private List<GenderRuleConfig> genderRules;
    
    public ModConfiguration() {
        this.enabledMobs = new ArrayList<>(Arrays.asList(
            "minecraft:cow"
        ));
        this.genderRules = new ArrayList<>(Arrays.asList(
            new GenderRuleConfig("minecraft:cow", "male", "minecraft:bucket")
        ));
    }
    
    public static void register() {
        ModConfiguration config = loadConfiguration();
        GenderModConfig.setConfiguration(config);
    }
    
    public static ModConfiguration loadConfiguration() {
        Path configPath = getConfigPath();
        File configFile = configPath.toFile();
        
        if (!configFile.exists()) {
            ModConfiguration defaultConfig = new ModConfiguration();
            saveConfiguration(defaultConfig);
            return defaultConfig;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            
            ModConfiguration config = new ModConfiguration();
            
            if (jsonObject.has("enabled_mobs")) {
                config.enabledMobs = new ArrayList<>();
                jsonObject.getAsJsonArray("enabled_mobs").forEach(element -> 
                    config.enabledMobs.add(element.getAsString())
                );
            }
            
            if (jsonObject.has("gender_rules")) {
                config.genderRules = new ArrayList<>();
                jsonObject.getAsJsonArray("gender_rules").forEach(element -> {
                    JsonObject ruleObj = element.getAsJsonObject();
                    GenderRuleConfig rule = new GenderRuleConfig(
                        ruleObj.get("mob_id").getAsString(),
                        ruleObj.get("gender").getAsString(),
                        ruleObj.get("item_id").getAsString()
                    );
                    config.genderRules.add(rule);
                });
            }
            
            return config;
        } catch (IOException e) {
            return new ModConfiguration();
        }
    }
    
    public static void saveConfiguration(ModConfiguration config) {
        Path configPath = getConfigPath();
        File configFile = configPath.toFile();
        
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            
            JsonObject jsonObject = new JsonObject();
            
            var mobsArray = new com.google.gson.JsonArray();
            config.enabledMobs.forEach(mobsArray::add);
            jsonObject.add("enabled_mobs", mobsArray);
            
            var rulesArray = new com.google.gson.JsonArray();
            config.genderRules.forEach(rule -> {
                JsonObject ruleObj = new JsonObject();
                ruleObj.addProperty("mob_id", rule.getMobId());
                ruleObj.addProperty("gender", rule.getGender());
                ruleObj.addProperty("item_id", rule.getItemId());
                rulesArray.add(ruleObj);
            });
            jsonObject.add("gender_rules", rulesArray);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(jsonObject, writer);
            }
            
        } catch (IOException e) {
        }
    }
    
    public static Path getConfigPath() {
        return FMLPaths.GAMEDIR.get().resolve(CONFIG_FOLDER).resolve(CONFIG_FILE_NAME);
    }
    
    public List<String> getEnabledMobs() {
        return enabledMobs;
    }
    
    public void setEnabledMobs(List<String> enabledMobs) {
        this.enabledMobs = enabledMobs;
    }
    
    public List<GenderRuleConfig> getGenderRules() {
        return genderRules;
    }
    
    public void setGenderRules(List<GenderRuleConfig> genderRules) {
        this.genderRules = genderRules;
    }
    
    public static class GenderRuleConfig {
        private final String mobId;
        private final String gender;
        private final String itemId;
        
        public GenderRuleConfig(String mobId, String gender, String itemId) {
            this.mobId = mobId;
            this.gender = gender;
            this.itemId = itemId;
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
} 