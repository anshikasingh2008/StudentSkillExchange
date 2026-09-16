package com.skillexchange.exception;

/** Thrown when a requested skill id/name does not exist in the catalog. */
public class SkillNotFoundException extends SkillExchangeException {
    public SkillNotFoundException(String message) {
        super(message);
    }
}
