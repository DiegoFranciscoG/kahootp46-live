package com.kahootp46.live.auth.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kahootp46.live.auth.AdminAccounts;
import com.kahootp46.live.auth.IssuedToken;
import com.kahootp46.live.auth.TokenService;
import com.kahootp46.live.auth.config.AdminProperties;
import com.kahootp46.live.auth.domain.User;
import com.kahootp46.live.auth.dto.LoginRequest;
import com.kahootp46.live.auth.dto.LoginResponse;
import com.kahootp46.live.auth.repository.UserRepository;
import com.kahootp46.live.shared.ratelimit.RateLimitProperties;
import com.kahootp46.live.shared.ratelimit.RateLimiter;

@Service
@Transactional(readOnly = true)
public class AuthService implements AdminAccounts {

    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(1);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties limits;
    private final AdminProperties admin;
    private final String timingEqualizerHash;

    AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens,
            RateLimiter rateLimiter, RateLimitProperties limits, AdminProperties admin) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.rateLimiter = rateLimiter;
        this.limits = limits;
        this.admin = admin;
        this.timingEqualizerHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public LoginResponse login(LoginRequest request, String clientIp) {
        rateLimiter.acquire("login:" + clientIp, limits.loginPerMinute(), LOGIN_WINDOW);

        Optional<User> user = users.findByEmail(User.normalizeEmail(request.email())).filter(User::isEnabled);
        // Always run one hash comparison so response time does not reveal whether the email exists.
        String hash = user.map(User::getPasswordHash).orElse(timingEqualizerHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !matches) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        IssuedToken token = tokens.issueAdminToken(user.get().getId());
        return new LoginResponse(token.value(), "Bearer", token.expiresAt());
    }

    @Override
    public Optional<UUID> findBootstrapAdminId() {
        return users.findByEmail(User.normalizeEmail(admin.email())).map(User::getId);
    }
}
