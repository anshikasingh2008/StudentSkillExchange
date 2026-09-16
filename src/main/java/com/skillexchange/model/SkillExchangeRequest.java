package com.skillexchange.model;

import com.skillexchange.enums.ExchangeStatus;

import java.time.LocalDateTime;

/**
 * Represents one student requesting to learn a skill from another
 * student, in exchange for credits. Tracks status through its
 * lifecycle: PENDING -> ACCEPTED -> COMPLETED (or REJECTED/CANCELLED).
 */
public class SkillExchangeRequest {

    private final int id;
    private final Student requester;   // wants to learn
    private final Student provider;    // will teach
    private final Skill skill;
    private final int creditsOffered;
    private final LocalDateTime createdAt;
    private ExchangeStatus status;

    public SkillExchangeRequest(int id, Student requester, Student provider,
                                 Skill skill, int creditsOffered) {
        this.id = id;
        this.requester = requester;
        this.provider = provider;
        this.skill = skill;
        this.creditsOffered = creditsOffered;
        this.status = ExchangeStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public Student getRequester() {
        return requester;
    }

    public Student getProvider() {
        return provider;
    }

    public Skill getSkill() {
        return skill;
    }

    public int getCreditsOffered() {
        return creditsOffered;
    }

    public ExchangeStatus getStatus() {
        return status;
    }

    public void setStatus(ExchangeStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return String.format("Request#%d: %s -> %s wants '%s' for %d credits [%s]",
                id, requester.getName(), provider.getName(), skill.getName(),
                creditsOffered, status);
    }
}
