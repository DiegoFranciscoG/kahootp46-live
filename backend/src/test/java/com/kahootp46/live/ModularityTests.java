package com.kahootp46.live;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Fails the build if a module reaches into another module's internals or modules form a cycle.
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(Kahootp46LiveApplication.class);

    @Test
    void modulesRespectTheirBoundaries() {
        modules.verify();
    }
}
