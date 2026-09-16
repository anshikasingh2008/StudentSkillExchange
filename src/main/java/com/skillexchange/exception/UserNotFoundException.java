package com.skillexchange.exception;

/** Thrown when a requested user id/email cannot be located. */
public class UserNotFoundException extends SkillExchangeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
