package com.skillexchange.model;

/**
 * Implemented by anything that accumulates a star rating,
 * e.g. a User after completed skill exchanges.
 */
public interface Rateable {
    void addRating(double stars);
    double getAverageRating();
    int getRatingCount();
}
