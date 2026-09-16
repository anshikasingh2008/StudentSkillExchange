package com.skillexchange.model;

import com.skillexchange.enums.ProficiencyLevel;
import com.skillexchange.enums.SkillCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete User type: a regular student on the platform.
 * Holds the two lists that drive matching: skills they can teach
 * (offers) and skills they want to learn (wants).
 */
public class Student extends User implements Matchable {

    private final List<SkillOffer> skillsOffered = new ArrayList<>();
    private final List<Skill> skillsWanted = new ArrayList<>();
    private final String branch; // e.g. "CSE", "ECE"
    private final int year;      // e.g. 2 for 2nd year

    public Student(int id, String name, String email, int credits, String branch, int year) {
        super(id, name, email, credits);
        this.branch = branch;
        this.year = year;
    }

    public void offerSkill(Skill skill, ProficiencyLevel level) {
        skillsOffered.add(new SkillOffer(skill, level));
    }

    public void wantSkill(Skill skill) {
        skillsWanted.add(skill);
    }

    public List<SkillOffer> getSkillsOffered() {
        return skillsOffered;
    }

    public List<Skill> getSkillsWanted() {
        return skillsWanted;
    }

    public String getBranch() {
        return branch;
    }

    public int getYear() {
        return year;
    }

    @Override
    public boolean canTeach(SkillCategory category) {
        return skillsOffered.stream().anyMatch(o -> o.getSkill().getCategory() == category);
    }

    @Override
    public boolean wantsToLearn(SkillCategory category) {
        return skillsWanted.stream().anyMatch(s -> s.getCategory() == category);
    }

    @Override
    public String getRole() {
        return "Student";
    }

    @Override
    public String displayProfile() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Student: %s | %s, Year %d | Rating: %.1f (%d reviews) | Credits: %d%n",
                getName(), branch, year, getAverageRating(), getRatingCount(), getCredits()));
        sb.append("  Can teach: ");
        if (skillsOffered.isEmpty()) {
            sb.append("(none yet)");
        } else {
            for (SkillOffer o : skillsOffered) {
                sb.append(o.getSkill().getName()).append(" [").append(o.getLevel()).append("] ");
            }
        }
        sb.append(String.format("%n  Wants to learn: "));
        if (skillsWanted.isEmpty()) {
            sb.append("(none yet)");
        } else {
            for (Skill s : skillsWanted) {
                sb.append(s.getName()).append(" ");
            }
        }
        return sb.toString();
    }
}
