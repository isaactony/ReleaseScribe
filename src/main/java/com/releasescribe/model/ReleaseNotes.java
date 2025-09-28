package com.releasescribe.model;

import java.util.List;
import java.util.Objects;

public class ReleaseNotes {
    private String changelogMd;
    private String releaseNotesMd;
    private String customerHighlightsMd;
    private List<String> breakingChanges;
    private List<String> upgradeSteps;

    public ReleaseNotes() {}

    public ReleaseNotes(String changelogMd, String releaseNotesMd, String customerHighlightsMd, 
                       List<String> breakingChanges, List<String> upgradeSteps) {
        this.changelogMd = changelogMd;
        this.releaseNotesMd = releaseNotesMd;
        this.customerHighlightsMd = customerHighlightsMd;
        this.breakingChanges = breakingChanges;
        this.upgradeSteps = upgradeSteps;
    }

    // Getters and setters
    public String getChangelogMd() { return changelogMd; }
    public void setChangelogMd(String changelogMd) { this.changelogMd = changelogMd; }

    public String getReleaseNotesMd() { return releaseNotesMd; }
    public void setReleaseNotesMd(String releaseNotesMd) { this.releaseNotesMd = releaseNotesMd; }

    public String getCustomerHighlightsMd() { return customerHighlightsMd; }
    public void setCustomerHighlightsMd(String customerHighlightsMd) { this.customerHighlightsMd = customerHighlightsMd; }

    public List<String> getBreakingChanges() { return breakingChanges; }
    public void setBreakingChanges(List<String> breakingChanges) { this.breakingChanges = breakingChanges; }

    public List<String> getUpgradeSteps() { return upgradeSteps; }
    public void setUpgradeSteps(List<String> upgradeSteps) { this.upgradeSteps = upgradeSteps; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReleaseNotes that = (ReleaseNotes) o;
        return Objects.equals(changelogMd, that.changelogMd) &&
               Objects.equals(releaseNotesMd, that.releaseNotesMd) &&
               Objects.equals(customerHighlightsMd, that.customerHighlightsMd) &&
               Objects.equals(breakingChanges, that.breakingChanges) &&
               Objects.equals(upgradeSteps, that.upgradeSteps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(changelogMd, releaseNotesMd, customerHighlightsMd, breakingChanges, upgradeSteps);
    }

    @Override
    public String toString() {
        return "ReleaseNotes{" +
                "changelogMd='" + changelogMd + '\'' +
                ", releaseNotesMd='" + releaseNotesMd + '\'' +
                ", customerHighlightsMd='" + customerHighlightsMd + '\'' +
                ", breakingChanges=" + breakingChanges +
                ", upgradeSteps=" + upgradeSteps +
                '}';
    }
}
