package com.skillexchange.exception;

/** Thrown when a student does not have enough credits to request a skill exchange. */
public class InsufficientCreditsException extends SkillExchangeException {
    public InsufficientCreditsException(String message) {
        super(message);
    }
}
