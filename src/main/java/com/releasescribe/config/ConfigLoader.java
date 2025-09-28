package com.releasescribe.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigLoader {
    
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    
    public static ReleaseScribeConfig loadConfig(Path configFile) throws IOException {
        if (!Files.exists(configFile)) {
            return getDefaultConfig();
        }
        
        try {
            return YAML_MAPPER.readValue(configFile.toFile(), ReleaseScribeConfig.class);
        } catch (Exception e) {
            System.err.println("Failed to load config file: " + e.getMessage());
            return getDefaultConfig();
        }
    }
    
    public static ReleaseScribeConfig getDefaultConfig() {
        ReleaseScribeConfig config = new ReleaseScribeConfig();
        
        // Set default audiences
        config.setAudiences(List.of("dev", "customer"));
        
        // Set default sections
        config.setSections(List.of("features", "fixes", "perf", "security", "breaking", "upgrade", "docs"));
        
        // Set default conventions
        ReleaseScribeConfig.Conventions conventions = new ReleaseScribeConfig.Conventions();
        conventions.setCommitStyle("conventional");
        
        Map<String, List<String>> componentPaths = new HashMap<>();
        componentPaths.put("api", List.of("service-api/**", "src/main/java/com/example/api/**"));
        componentPaths.put("ui", List.of("web/**", "frontend/**"));
        componentPaths.put("infra", List.of("helm/**", "terraform/**"));
        conventions.setComponentPaths(componentPaths);
        
        config.setConventions(conventions);
        
        // Set default label mapping
        Map<String, List<String>> labelMapping = new HashMap<>();
        labelMapping.put("feature", List.of("feature", "enhancement", "feat"));
        labelMapping.put("fix", List.of("bug", "fix", "bugfix"));
        labelMapping.put("security", List.of("security", "deps:security"));
        labelMapping.put("perf", List.of("performance", "perf"));
        labelMapping.put("docs", List.of("docs"));
        config.setLabelMapping(labelMapping);
        
        // Set default prompt settings
        ReleaseScribeConfig.Prompt prompt = new ReleaseScribeConfig.Prompt();
        prompt.setTone("crisp");
        prompt.setChangelogStyle("Keep a Changelog");
        prompt.setForbid(List.of("marketing fluff", "unverifiable claims"));
        config.setPrompt(prompt);
        
        // Set default limits
        ReleaseScribeConfig.Limits limits = new ReleaseScribeConfig.Limits();
        limits.setMaxPrs(200);
        limits.setMaxTokens(2000);
        config.setLimits(limits);
        
        return config;
    }
    
    public static void saveDefaultConfig(Path configFile) throws IOException {
        ReleaseScribeConfig defaultConfig = getDefaultConfig();
        YAML_MAPPER.writeValue(configFile.toFile(), defaultConfig);
    }
}
