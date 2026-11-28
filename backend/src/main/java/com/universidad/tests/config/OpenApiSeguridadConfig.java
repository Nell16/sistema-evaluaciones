package com.universidad.tests.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

// GEN-33-BE: esquema bearerAuth del contrato (docs/api/openapi.yaml) en springdoc, para que el
// boton «Authorize» de Swagger envie Authorization: Bearer <token emitido por E14>.
// GEN-50-FIX: servidor relativo para que «Try it out» use la misma direccion desde la que se abre
// Swagger (la IP publicada o localhost) y no el nombre interno de Docker (http://backend:8080).
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Sistema Integrado de Evaluaciones", version = "0.1.0"),
        servers = @Server(url = "/", description = "Mismo servidor donde se abre Swagger"),
        security = @SecurityRequirement(name = OpenApiSeguridadConfig.ESQUEMA_BEARER))
@SecurityScheme(
        name = OpenApiSeguridadConfig.ESQUEMA_BEARER,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        description = "Token emitido por E14.")
public class OpenApiSeguridadConfig {

    public static final String ESQUEMA_BEARER = "bearerAuth";
}
