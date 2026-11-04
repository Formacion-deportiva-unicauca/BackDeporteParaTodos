package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PublicoCursoTest {

    @ParameterizedTest
    @ValueSource(strings = {"Estudiante", "Docente", "Administrativo", "Contratista"})
    void mixto_admiteATodosLosTipos(String tipo) {
        assertTrue(PublicoCurso.MIXTO.admite(tipo));
    }

    @Test
    void mixto_admiteTambienUnTipoDesconocido() {
        assertTrue(PublicoCurso.MIXTO.admite(null));
        assertTrue(PublicoCurso.MIXTO.admite("Otro"));
    }

    @Test
    void estudiante_soloAdmiteEstudiantes() {
        assertTrue(PublicoCurso.ESTUDIANTE.admite("Estudiante"));
        assertFalse(PublicoCurso.ESTUDIANTE.admite("Docente"));
        assertFalse(PublicoCurso.ESTUDIANTE.admite("Administrativo"));
        assertFalse(PublicoCurso.ESTUDIANTE.admite("Contratista"));
    }

    @Test
    void funcionario_admiteDocentesAdministrativosYContratistas() {
        assertTrue(PublicoCurso.FUNCIONARIO.admite("Docente"));
        assertTrue(PublicoCurso.FUNCIONARIO.admite("Administrativo"));
        assertTrue(PublicoCurso.FUNCIONARIO.admite("Contratista"));
        assertFalse(PublicoCurso.FUNCIONARIO.admite("Estudiante"));
    }

    @Test
    void conPublicoEspecifico_unTipoNuloODesconocidoNoSeAdmite() {
        assertFalse(PublicoCurso.ESTUDIANTE.admite(null));
        assertFalse(PublicoCurso.FUNCIONARIO.admite(null));
        assertFalse(PublicoCurso.FUNCIONARIO.admite("Otro"));
    }

    @Test
    void desdeTexto_nuloOEnBlancoEsMixto() {
        assertEquals(PublicoCurso.MIXTO, PublicoCurso.desdeTexto(null));
        assertEquals(PublicoCurso.MIXTO, PublicoCurso.desdeTexto("  "));
    }

    @Test
    void desdeTexto_convierteLosValoresValidos() {
        assertEquals(PublicoCurso.ESTUDIANTE, PublicoCurso.desdeTexto("ESTUDIANTE"));
        assertEquals(PublicoCurso.FUNCIONARIO, PublicoCurso.desdeTexto("FUNCIONARIO"));
        assertEquals(PublicoCurso.MIXTO, PublicoCurso.desdeTexto("MIXTO"));
    }

    @Test
    void desdeTexto_valorDesconocidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> PublicoCurso.desdeTexto("DOCENTES"));
    }
}
