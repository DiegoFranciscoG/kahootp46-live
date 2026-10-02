package com.kahootp46.live.shared.error;

/**
 * A request that is well formed but conflicts with the current state of a resource (HTTP 409).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
