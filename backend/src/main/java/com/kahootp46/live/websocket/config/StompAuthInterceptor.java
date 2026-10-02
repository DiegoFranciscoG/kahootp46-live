package com.kahootp46.live.websocket.config;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.kahootp46.live.auth.TokenService;

/**
 * Authenticates the STOMP CONNECT frame with the bearer token and authorizes every SUBSCRIBE against the
 * PIN bound to that token. The protocol is read-only: clients cannot SEND anything.
 */
@Component
class StompAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER = "Bearer ";
    private static final Pattern ALLOWED_SUBSCRIPTION =
            Pattern.compile("^(?:/topic/games/(\\d{6})|/app/games/(\\d{6})/state)$");

    private final TokenService tokens;

    StompAuthInterceptor(TokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        switch (command) {
            case CONNECT, STOMP -> accessor.setUser(authenticate(accessor));
            case SUBSCRIBE -> authorizeSubscription(accessor.getUser(), accessor.getDestination());
            case SEND -> throw new AccessDeniedException("Read-only protocol");
            default -> {
                // UNSUBSCRIBE, DISCONNECT, ACK/NACK need no authorization.
            }
        }
        return message;
    }

    private StompPrincipal authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            throw new AccessDeniedException("Missing bearer token");
        }
        return new StompPrincipal(tokens.verify(header.substring(BEARER.length())));
    }

    private static void authorizeSubscription(Principal user, String destination) {
        if (!(user instanceof StompPrincipal principal) || destination == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        Matcher matcher = ALLOWED_SUBSCRIPTION.matcher(destination);
        if (!matcher.matches()) {
            throw new AccessDeniedException("Destination not allowed");
        }
        String pin = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        if (!principal.grant().canView(pin)) {
            throw new AccessDeniedException("Token not valid for this game");
        }
    }
}
