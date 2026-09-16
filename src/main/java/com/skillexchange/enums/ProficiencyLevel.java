package com.skillexchange.enums;

/**
 * How skilled a student is in a given Skill they offer to teach.
 */
public enum ProficiencyLevel {
    BEGINNER(1),
    INTERMEDIATE(2),
    ADVANCED(3),
    EXPERT(4);

    private final int weight;

    ProficiencyLevel(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
