package com.skillexchange.service;

import com.skillexchange.model.Skill;
import com.skillexchange.model.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

/**
 * Singleton service that finds candidate teachers for a student
 * wanting to learn a given skill, ranked by rating then proficiency.
 *
 * Singleton pattern: only one MatchEngine should exist app-wide since
 * it is stateless and cheap to share (Java OOP - Other Types of Classes).
 */
public final class MatchEngine {

    private static MatchEngine instance;

    private MatchEngine() {
        // private constructor prevents external instantiation
    }

    public static synchronized MatchEngine getInstance() {
        if (instance == null) {
            instance = new MatchEngine();
        }
        return instance;
    }

    /**
     * Finds every student (other than the requester) who offers the
     * given skill, sorted best-match-first (highest rating, then
     * highest proficiency level).
     */
    public List<Student> findTeachersFor(Skill skill, Student requester, List<Student> allStudents) {
        List<Student> candidates = new ArrayList<>();
        for (Student s : allStudents) {
            if (s.equals(requester)) continue;
            boolean teaches = s.getSkillsOffered().stream()
                    .anyMatch(offer -> offer.getSkill().equals(skill));
            if (teaches) {
                candidates.add(s);
            }
        }

        candidates.sort(
                Comparator.comparingDouble(Student::getAverageRating).reversed()
                        .thenComparing((Student s) -> bestLevelFor(s, skill).getWeight(), Comparator.reverseOrder())
        );
        return candidates;
    }

    private com.skillexchange.enums.ProficiencyLevel bestLevelFor(Student s, Skill skill) {
        return s.getSkillsOffered().stream()
                .filter(o -> o.getSkill().equals(skill))
                .map(com.skillexchange.model.SkillOffer::getLevel)
                .max(Comparator.comparingInt(com.skillexchange.enums.ProficiencyLevel::getWeight))
                .orElse(com.skillexchange.enums.ProficiencyLevel.BEGINNER);
    }
}
