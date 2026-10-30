package com.universidad.tests;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArquitecturaTest {
    private static final JavaClasses CLASES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.universidad.tests");
    private static final List<String> MODULOS = List.of("bfa", "weill", "thurstone", "allport", "vocacional");

    @Test
    void dominioNoDependeDeSpringNiJpa() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..")
                .allowEmptyShould(true).check(CLASES);
    }

    @Test
    void controllerNoAccedeAInfraestructura() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .allowEmptyShould(true).check(CLASES);
    }

    @Test
    void modulosNoSeUsanEntreSi() {
        for (String modulo : MODULOS) {
            String[] otros = MODULOS.stream().filter(o -> !o.equals(modulo)).map(o -> ".." + o + "..").toArray(String[]::new);
            noClasses().that().resideInAPackage(".." + modulo + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(otros)
                    .allowEmptyShould(true).check(CLASES);
        }
    }
}
