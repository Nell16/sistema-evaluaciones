package com.universidad.tests.controller;

import com.universidad.tests.application.service.CatalogoTestsService;
import com.universidad.tests.dto.response.TestDetalleResponse;
import com.universidad.tests.dto.response.TestResumenResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tests")
public class CatalogoTestsController {

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
    @GetMapping("/{testId}")
    public TestDetalleResponse obtenerTest(@PathVariable Long testId) {
        return catalogoService.obtenerTest(testId);
    }
}
