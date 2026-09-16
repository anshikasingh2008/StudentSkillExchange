package com.skillexchange.model;

import com.skillexchange.enums.SkillCategory;

/**
 * Implemented by anything the MatchEngine can score against a
 * category of interest (currently just Student, but keeping this
 * as an interface lets future user types - e.g. a "Club" - plug
 * into matching without changing the engine).
 */
public interface Matchable {
    boolean canTeach(SkillCategory category);
    boolean wantsToLearn(SkillCategory category);
}
