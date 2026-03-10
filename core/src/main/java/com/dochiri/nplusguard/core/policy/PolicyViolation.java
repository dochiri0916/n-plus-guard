package com.dochiri.nplusguard.core.policy;

import java.util.Objects;

public record PolicyViolation(
        String code,
        String message,
        int actual,
        int limit
) {

    public PolicyViolation {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
