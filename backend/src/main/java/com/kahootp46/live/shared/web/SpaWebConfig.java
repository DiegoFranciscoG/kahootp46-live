package com.kahootp46.live.shared.web;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the Angular build bundled in {@code classpath:/static} from the same origin as the API
 * (single-container deployment). Disabled by default: with docker compose nginx serves the SPA instead.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.serve-spa", havingValue = "true")
class SpaWebConfig implements WebMvcConfigurer {

    private static final Set<String> BACKEND_ROOTS = Set.of("api", "actuator", "v3", "ws");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Hashed build artifacts never change under the same name.
        registry.addResourceHandler("/*.js", "/*.css")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).immutable());
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    /**
     * Returns the requested file or, for client-side routes such as {@code /live/582941}, {@code index.html}.
     * Backend paths and missing files with an extension stay 404 so API typos never answer with HTML.
     */
    static final class SpaFallbackResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            if (isBackendPath(resourcePath) || resourcePath.contains(".")) {
                return null;
            }
            Resource index = new ClassPathResource("static/index.html");
            return index.exists() ? index : null;
        }

        static boolean isBackendPath(String resourcePath) {
            String firstSegment = resourcePath.split("/", 2)[0];
            return BACKEND_ROOTS.contains(firstSegment) || firstSegment.startsWith("swagger-ui");
        }
    }
}
