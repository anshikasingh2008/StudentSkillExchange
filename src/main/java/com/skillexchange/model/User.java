package com.skillexchange.model;

import java.util.Objects;

/**
 * Abstract base class for anyone using the platform.
 * Demonstrates: encapsulation, constructors, `this`, `final` fields,
 * and abstract methods overridden by subclasses (polymorphism).
 */
public abstract class User implements Rateable {

    private final int id;
    private final String name;
    private final String email;
    protected int credits;

    private double totalRatingPoints = 0.0;
    private int ratingCount = 0;

    public User(int id, String name, String email, int credits) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.credits = credits;
    }

    // Abstract method - every concrete subclass must define its own
    // profile summary (polymorphism at work when printed from a
    // List<User> containing mixed subclasses).
    public abstract String displayProfile();

    // Abstract hook subclasses use to describe their own role.
    public abstract String getRole();

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public int getCredits() {
        return credits;
    }

    public void addCredits(int amount) {
        this.credits += amount;
    }

    public void deductCredits(int amount) {
        this.credits -= amount;
    }

    @Override
    public void addRating(double stars) {
        totalRatingPoints += stars;
        ratingCount++;
    }

    @Override
    public double getAverageRating() {
        if (ratingCount == 0) return 0.0;
        return totalRatingPoints / ratingCount;
    }

    @Override
    public int getRatingCount() {
        return ratingCount;
    }

    /**
     * Used only by the DAO layer to rehydrate accumulated rating
     * totals when loading a User back out of the database, without
     * going through addRating() (which represents one new review).
     */
    public void restoreRatingStats(double totalRatingPoints, int ratingCount) {
        this.totalRatingPoints = totalRatingPoints;
        this.ratingCount = ratingCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[#%d] %s (%s) - %d credits", id, name, getRole(), credits);
    }
}
