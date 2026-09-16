package com.skillexchange.model;

import com.skillexchange.enums.SkillCategory;
import java.util.Objects;

/**
 * A skill in the platform catalog, e.g. "Python Programming" under
 * the TECHNOLOGY category. Skills are shared across all students -
 * a Student links to a Skill via a SkillOffer or SkillRequest.
 */
public final class Skill {

    private final int id;
    private final String name;
    private final SkillCategory category;
    private final String description;

    public Skill(int id, String name, SkillCategory category, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SkillCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Skill)) return false;
        Skill skill = (Skill) o;
        return id == skill.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[#%d] %s (%s)", id, name, category.getDisplayName());
    }
}
