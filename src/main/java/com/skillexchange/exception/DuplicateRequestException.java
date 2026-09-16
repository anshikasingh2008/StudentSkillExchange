package com.skillexchange.exception;

/**
 * Thrown when a student tries to send a second pending exchange
 * request to the same peer for the same skill.
 */
public class DuplicateRequestException extends SkillExchangeException {
    public DuplicateRequestException(String message) {
        super(message);
    }
}
