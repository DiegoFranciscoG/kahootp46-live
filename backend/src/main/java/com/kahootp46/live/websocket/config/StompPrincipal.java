package com.kahootp46.live.websocket.config;

import java.security.Principal;

import com.kahootp46.live.auth.AccessGrant;

record StompPrincipal(AccessGrant grant) implements Principal {

    @Override
    public String getName() {
        return grant.subject();
    }
}
