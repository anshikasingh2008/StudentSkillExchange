package com.skillexchange.model;

import com.skillexchange.enums.ProficiencyLevel;

/** Links a Skill a student can teach with how proficient they are at it. */
public class SkillOffer {
    private final Skill skill;
    private final ProficiencyLevel level;

    public SkillOffer(Skill skill, ProficiencyLevel level) {
        this.skill = skill;
        this.level = level;
    }

    public Skill getSkill() {
        return skill;
    }

    public ProficiencyLevel getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return skill.getName() + " (" + level + ")";
    }
}
