package com.bebub.gendermod.config;

import java.util.List;

public class GenderModConfig {
    private static ModConfiguration configuration;
    
    public static void setConfiguration(ModConfiguration config) {
        configuration = config;
    }
    
    public static ModConfiguration getConfiguration() {
        return configuration;
    }
    
    public static List<String> getEnabledMobs() {
        return configuration != null ? configuration.getEnabledMobs() : List.of();
    }
    
    public static List<ModConfiguration.GenderRuleConfig> getGenderRules() {
        return configuration != null ? configuration.getGenderRules() : List.of();
    }
} 