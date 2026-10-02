package com.kahootp46.live.auth;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only view of admin accounts for other modules (e.g. the demo data seeder).
 */
public interface AdminAccounts {

    /** Id of the account created from ADMIN_EMAIL at startup. */
    Optional<UUID> findBootstrapAdminId();
}
