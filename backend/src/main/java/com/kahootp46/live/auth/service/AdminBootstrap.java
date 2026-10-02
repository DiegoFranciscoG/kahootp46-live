package com.kahootp46.live.auth.service;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kahootp46.live.auth.config.AdminProperties;
import com.kahootp46.live.auth.domain.User;
import com.kahootp46.live.auth.repository.UserRepository;

/**
 * Creates the admin account from ADMIN_EMAIL / ADMIN_PASSWORD on first start. There is no sign-up endpoint.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties admin;
    private final Clock clock;

    AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder, AdminProperties admin, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.admin = admin;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = User.normalizeEmail(admin.email());
        if (users.existsByEmail(email)) {
            return;
        }
        users.save(User.admin(email, passwordEncoder.encode(admin.password()), clock.instant()));
        log.info("Admin account created");
    }
}
