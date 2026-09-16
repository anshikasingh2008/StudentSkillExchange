package com.skillexchange.enums;

/**
 * Broad categories a Skill can belong to.
 * Used for filtering and matching students together.
 */
public enum SkillCategory {
    TECHNOLOGY("Technology & Programming"),
    MUSIC("Music & Performing Arts"),
    SPORTS("Sports & Fitness"),
    ART_AND_DESIGN("Art & Design"),
    LANGUAGE("Language Learning"),
    ACADEMICS("Academics & Tutoring"),
    LIFE_SKILLS("Life Skills & Hobbies"),
    OTHER("Other");

    private final String displayName;

    SkillCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
