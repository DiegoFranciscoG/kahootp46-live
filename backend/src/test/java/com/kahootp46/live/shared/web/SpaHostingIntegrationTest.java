package com.kahootp46.live.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.kahootp46.live.support.IntegrationTestSupport;

class SpaHostingIntegrationTest extends IntegrationTestSupport {

    @Test
    void rootAndClientSideRoutesServeIndexHtml() {
        for (String path : new String[] {"/", "/live/582941", "/admin", "/admin/quizzes/new"}) {
            RawResult page = raw(path, null);

            assertThat(page.status()).as(path).isEqualTo(200);
            assertThat(page.body()).as(path).contains("<app-root>");
            assertThat(page.header("Cache-Control")).as(path).contains("no-cache");
        }
    }

    @Test
    void headRequestsToThePublicBundleWorkButStayProtectedUnderApi() {
        assertThat(headStatus("/", null)).isEqualTo(200);
        assertThat(headStatus("/main-TEST1234.js", null)).isEqualTo(200);
        assertThat(headStatus("/api/quizzes", null)).isEqualTo(401);
        assertThat(headStatus("/actuator/env", null)).isEqualTo(401);
    }

    @Test
    void hashedAssetsAreCachedForever() {
        RawResult script = raw("/main-TEST1234.js", null);

        assertThat(script.status()).isEqualTo(200);
        assertThat(script.header("Cache-Control")).contains("immutable");
    }

    @Test
    void missingFilesAndUnknownApiPathsNeverAnswerWithHtml() {
        assertThat(raw("/missing-file.js", null).status()).isEqualTo(404);
        assertThat(raw("/api/does-not-exist", null).status()).isEqualTo(401);

        RawResult authenticatedTypo = raw("/api/does-not-exist", adminToken());
        assertThat(authenticatedTypo.status()).isEqualTo(404);
        assertThat(authenticatedTypo.body()).doesNotContain("<app-root>");
    }

    @Test
    void actuatorStaysClosedExceptHealth() {
        assertThat(raw("/actuator/health", null).status()).isEqualTo(200);
        assertThat(raw("/actuator/env", null).status()).isEqualTo(401);
        assertThat(raw("/actuator/beans", null).status()).isEqualTo(401);
    }

    @Test
    void securityHeadersAllowTheBaseElementOfTheSpa() {
        RawResult page = raw("/", null);

        assertThat(page.header("Content-Security-Policy")).contains("default-src 'self'", "base-uri 'self'",
                "frame-ancestors 'none'");
        assertThat(page.header("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(page.header("X-Frame-Options")).isEqualTo("DENY");
    }

    @Test
    void backendPathDetection() {
        assertThat(SpaWebConfig.SpaFallbackResolver.isBackendPath("api/games/123456")).isTrue();
        assertThat(SpaWebConfig.SpaFallbackResolver.isBackendPath("swagger-ui/index.html")).isTrue();
        assertThat(SpaWebConfig.SpaFallbackResolver.isBackendPath("ws")).isTrue();
        assertThat(SpaWebConfig.SpaFallbackResolver.isBackendPath("live/582941")).isFalse();
        assertThat(SpaWebConfig.SpaFallbackResolver.isBackendPath("apiary")).isFalse();
    }
}
