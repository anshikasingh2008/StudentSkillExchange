package com.skillexchange.exception;

/**
 * Base checked exception for all domain-specific errors in the
 * Student Skill Exchange application. Keeping a common parent lets
 * calling code catch broadly with `catch (SkillExchangeException e)`
 * or narrowly with the specific subclasses below.
 */
public class SkillExchangeException extends Exception {

    public SkillExchangeException(String message) {
        super(message);
    }

    public SkillExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
