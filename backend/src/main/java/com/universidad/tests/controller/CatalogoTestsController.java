package com.universidad.tests.controller;

import com.universidad.tests.application.service.CatalogoTestsService;
import com.universidad.tests.dto.response.ErrorResponse;
import com.universidad.tests.dto.response.TestDetalleResponse;
import com.universidad.tests.dto.response.TestResumenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tests")
@Tag(name = "Catálogo", description = "Documento BFA §5.3")
public class CatalogoTestsController {

    // GEN-49-BE: ejemplos ficticios con la forma del contrato (docs/api/openapi.yaml)
    private static final String EJEMPLO_E2_200 =
            """
            {
              "id": 1,
              "codigo": "BFA",
              "nombre": "Batería Factorial de Aptitudes",
              "versionFormularioId": 10,
              "subpruebas": [
                {
                  "codigo": "VOC1",
                  "nombre": "Vocabulario - Forma A",
                  "orden": 1,
                  "factor": "VOC1",
                  "tiempoLimiteSegundos": 300,
                  "tipoRespuesta": "SELECCION",
                  "opciones": 5,
                  "numeroPreguntas": 37,
                  "maximoEntradas": null,
                  "tienePractica": true
                },
                {
                  "codigo": "FV",
                  "nombre": "Fluidez Verbal",
                  "orden": 13,
                  "factor": "FV",
                  "tiempoLimiteSegundos": 300,
                  "tipoRespuesta": "TEXTO_LIBRE",
                  "opciones": null,
                  "numeroPreguntas": 0,
                  "maximoEntradas": 75,
                  "tienePractica": true
                }
              ]
            }
            """;

    private static final String EJEMPLO_E2_401 =
            """
            {"timestamp": "2026-11-04T18:00:00Z", "status": 401, "codigo": "NO_AUTENTICADO",
             "mensaje": "No hay una sesion valida", "ruta": "/api/v1/tests/1"}
            """;

    private static final String EJEMPLO_E2_403 =
            """
            {"timestamp": "2026-11-04T18:00:00Z", "status": 403, "codigo": "SIN_PERMISO",
             "mensaje": "No tiene permiso para realizar esta operacion", "ruta": "/api/v1/tests/1"}
            """;

    private static final String EJEMPLO_E2_404 =
            """
            {"timestamp": "2026-11-04T18:00:00Z", "status": 404, "codigo": "TEST_NO_ENCONTRADO",
             "mensaje": "No existe el test con ID: 99", "ruta": "/api/v1/tests/99"}
            """;

    private static final String EJEMPLO_E2_500 =
            """
            {"timestamp": "2026-11-04T18:00:00Z", "status": 500, "codigo": "ERROR_INTERNO",
             "mensaje": "Error interno", "ruta": "/api/v1/tests/1"}
            """;

    private final CatalogoTestsService catalogoService;

    public CatalogoTestsController(CatalogoTestsService catalogoService) {
        this.catalogoService = catalogoService;
    }

    // E1
    @GetMapping
    public List<TestResumenResponse> listarTests() {
        return catalogoService.listarTests();
    }

    // E2
    @Operation(
            operationId = "obtenerTest",
            summary = "E2 — Consultar el test y sus subpruebas",
            description =
                    "Devuelve el test y las configuraciones de la edición publicada identificada,"
                            + " en orden de aplicación, sin preguntas ni claves. numeroPreguntas es"
                            + " el conteo de reactivos reales (FV tiene 0); maximoEntradas vale 75"
                            + " en FV y nulo en selección. La edición identificada es la publicada"
                            + " más reciente del test. CU-BFA-P02.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(
            responseCode = "200",
            description = "Test con sus subpruebas.",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TestDetalleResponse.class),
                            examples = @ExampleObject(name = "BFA", value = EJEMPLO_E2_200)))
    @ApiResponse(
            responseCode = "401",
            description = "NO_AUTENTICADO — no hay sesión válida.",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = EJEMPLO_E2_401)))
    @ApiResponse(
            responseCode = "403",
            description = "SIN_PERMISO — sin rol activo, permiso o alcance sobre el recurso.",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = EJEMPLO_E2_403)))
    @ApiResponse(
            responseCode = "404",
            description =
                    "TEST_NO_ENCONTRADO — el test no existe, está inactivo o no tiene edición"
                            + " publicada.",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = EJEMPLO_E2_404)))
    @ApiResponse(
            responseCode = "500",
            description = "ERROR_INTERNO — error no previsto; nunca expone detalles internos.",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = EJEMPLO_E2_500)))
    @GetMapping("/{testId}")
    public TestDetalleResponse obtenerTest(
            @Parameter(description = "Identificador del test", example = "1") @PathVariable
                    Long testId) {
        return catalogoService.obtenerTest(testId);
    }
}
