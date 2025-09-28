package com.releasescribe.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Commit {
    private String hash;
    private String message;
    private String author;
    private LocalDateTime date;
    private String component;
    private String category;

    public Commit() {}

    public Commit(String hash, String message, String author, LocalDateTime date) {
        this.hash = hash;
        this.message = message;
        this.author = author;
        this.date = date;
    }

    // Getters and setters
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Commit commit = (Commit) o;
        return Objects.equals(hash, commit.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }

    @Override
    public String toString() {
        return "Commit{" +
                "hash='" + hash + '\'' +
                ", message='" + message + '\'' +
                ", author='" + author + '\'' +
                ", date=" + date +
                '}';
    }
}

