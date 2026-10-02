package com.kahootp46.live.auth;

import java.util.Objects;

/**
 * What a verified token allows. Viewers are bound to exactly one PIN; admins may watch any game.
 */
public record AccessGrant(String subject, AccessRole role, String pin) {

    public boolean canView(String requestedPin) {
        return role == AccessRole.ADMIN || Objects.equals(pin, requestedPin);
    }
}
