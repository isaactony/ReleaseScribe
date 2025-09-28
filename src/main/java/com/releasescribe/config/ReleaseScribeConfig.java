package com.releasescribe.config;

import java.util.List;
import java.util.Map;

public class ReleaseScribeConfig {
    private int version = 1;
    private List<String> audiences;
    private List<String> sections;
    private Conventions conventions;
    private Map<String, List<String>> labelMapping;
    private Prompt prompt;
    private Limits limits;
    
    public ReleaseScribeConfig() {}
    
    // Getters and setters
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    
    public List<String> getAudiences() { return audiences; }
    public void setAudiences(List<String> audiences) { this.audiences = audiences; }
    
    public List<String> getSections() { return sections; }
    public void setSections(List<String> sections) { this.sections = sections; }
    
    public Conventions getConventions() { return conventions; }
    public void setConventions(Conventions conventions) { this.conventions = conventions; }
    
    public Map<String, List<String>> getLabelMapping() { return labelMapping; }
    public void setLabelMapping(Map<String, List<String>> labelMapping) { this.labelMapping = labelMapping; }
    
    public Prompt getPrompt() { return prompt; }
    public void setPrompt(Prompt prompt) { this.prompt = prompt; }
    
    public Limits getLimits() { return limits; }
    public void setLimits(Limits limits) { this.limits = limits; }
    
    public static class Conventions {
        private String commitStyle = "conventional";
        private Map<String, List<String>> componentPaths;
        
        public Conventions() {}
        
        public String getCommitStyle() { return commitStyle; }
        public void setCommitStyle(String commitStyle) { this.commitStyle = commitStyle; }
        
        public Map<String, List<String>> getComponentPaths() { return componentPaths; }
        public void setComponentPaths(Map<String, List<String>> componentPaths) { this.componentPaths = componentPaths; }
    }
    
    public static class Prompt {
        private String tone = "crisp";
        private String changelogStyle = "Keep a Changelog";
        private List<String> forbid;
        
        public Prompt() {}
        
        public String getTone() { return tone; }
        public void setTone(String tone) { this.tone = tone; }
        
        public String getChangelogStyle() { return changelogStyle; }
        public void setChangelogStyle(String changelogStyle) { this.changelogStyle = changelogStyle; }
        
        public List<String> getForbid() { return forbid; }
        public void setForbid(List<String> forbid) { this.forbid = forbid; }
    }
    
    public static class Limits {
        private int maxPrs = 200;
        private int maxTokens = 2000;
        
        public Limits() {}
        
        public int getMaxPrs() { return maxPrs; }
        public void setMaxPrs(int maxPrs) { this.maxPrs = maxPrs; }
        
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }
}
