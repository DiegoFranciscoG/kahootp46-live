package com.kahootp46.live.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import com.kahootp46.live.auth.config.SecurityProperties;

/**
 * Issues and verifies the short-lived HS256 tokens used by admins (REST) and viewers (REST + STOMP).
 */
@Service
public class TokenService {

    public static final String ISSUER = "kahootp46-live";
    private static final String SCOPE_CLAIM = "scope";
    private static final String PIN_CLAIM = "pin";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final SecurityProperties properties;
    private final Clock clock;

    TokenService(JwtEncoder encoder, JwtDecoder decoder, SecurityProperties properties, Clock clock) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken issueAdminToken(UUID userId) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.adminTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(SCOPE_CLAIM, AccessRole.ADMIN.name())
                .build();
        return new IssuedToken(encode(claims), expiresAt);
    }

    public IssuedToken issueViewerToken(String pin) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.viewerTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject("viewer:" + UUID.randomUUID())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(SCOPE_CLAIM, AccessRole.VIEWER.name())
                .claim(PIN_CLAIM, pin)
                .build();
        return new IssuedToken(encode(claims), expiresAt);
    }

    /**
     * Verifies signature, issuer and expiry of a raw token (used for the STOMP CONNECT frame).
     *
     * @throws BadJwtException when the token is invalid
     */
    public AccessGrant verify(String token) {
        try {
            return toGrant(decoder.decode(token));
        } catch (JwtException ex) {
            throw new BadJwtException("Invalid token", ex);
        }
    }

    /** Converts a token already validated by the resource server filter. */
    public AccessGrant toGrant(Jwt jwt) {
        AccessRole role = AccessRole.valueOf(jwt.getClaimAsString(SCOPE_CLAIM));
        String pin = role == AccessRole.VIEWER ? jwt.getClaimAsString(PIN_CLAIM) : null;
        return new AccessGrant(jwt.getSubject(), role, pin);
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
