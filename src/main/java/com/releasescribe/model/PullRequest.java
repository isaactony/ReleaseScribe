package com.releasescribe.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class PullRequest {
    private int number;
    private String title;
    private String body;
    private String author;
    private LocalDateTime mergedAt;
    private List<String> labels;
    private List<String> linkedIssues;
    private String component;
    private String category;
    private boolean breakingChange;

    public PullRequest() {}

    public PullRequest(int number, String title, String body, String author, 
                      LocalDateTime mergedAt, List<String> labels, List<String> linkedIssues) {
        this.number = number;
        this.title = title;
        this.body = body;
        this.author = author;
        this.mergedAt = mergedAt;
        this.labels = labels;
        this.linkedIssues = linkedIssues;
    }

    // Getters and setters
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public LocalDateTime getMergedAt() { return mergedAt; }
    public void setMergedAt(LocalDateTime mergedAt) { this.mergedAt = mergedAt; }

    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }

    public List<String> getLinkedIssues() { return linkedIssues; }
    public void setLinkedIssues(List<String> linkedIssues) { this.linkedIssues = linkedIssues; }

    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isBreakingChange() { return breakingChange; }
    public void setBreakingChange(boolean breakingChange) { this.breakingChange = breakingChange; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PullRequest that = (PullRequest) o;
        return number == that.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }

    @Override
    public String toString() {
        return "PullRequest{" +
                "number=" + number +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", mergedAt=" + mergedAt +
                '}';
    }
}

