package com.kahootp46.live.shared.web;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(
        title = "KahootP46 Live API",
        version = "v1",
        description = "Real-time quiz answer viewer. Live events travel over STOMP (/ws); see docs/protocolo-websocket.md.",
        license = @License(name = "MIT")))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class OpenApiConfig {
}
