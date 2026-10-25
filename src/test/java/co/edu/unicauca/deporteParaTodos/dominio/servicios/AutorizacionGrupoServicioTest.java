package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.AccesoGrupoDenegadoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;

@ExtendWith(MockitoExtension.class)
class AutorizacionGrupoServicioTest {

    @Mock
    private IGrupoGateway grupoGateway;

    @InjectMocks
    private AutorizacionGrupoServicio servicio;

    private static final String CATEGORIA = "Recreativo";
    private static final String CURSO = "Natacion";
    private static final Integer ANIO = 2026;
    private static final Integer ITERABLE = 1;
    private static final String INS = "INS001";
    private static final String OTRO_INS = "INS002";

    private Grupo grupoDe(String idInstructor) {
        Grupo g = new Grupo();
        g.setCategoria(CATEGORIA);
        g.setCurso(CURSO);
        g.setAnio(ANIO);
        g.setIterable(ITERABLE);
        g.setIdInstructor(idInstructor);
        return g;
    }

    // ── esPropietario (variante booleana, sin lanzar) ───────────────────────

    @Test
    void esPropietario_instructorTitular_esVerdadero() {
        assertTrue(servicio.esPropietario(grupoDe(INS), INS));
    }

    @Test
    void esPropietario_instructorDeOtroGrupo_esFalso() {
        assertFalse(servicio.esPropietario(grupoDe(OTRO_INS), INS));
    }

    @Test
    void esPropietario_grupoSinInstructorAsignado_esFalso() {
        assertFalse(servicio.esPropietario(grupoDe(null), INS));
    }

    @Test
    void esPropietario_grupoNulo_esFalso() {
        assertFalse(servicio.esPropietario(null, INS));
    }

    // ── verificarPropietario(Grupo, ...) ────────────────────────────────────

    @Test
    void verificarPropietarioConGrupo_instructorTitular_permite() {
        assertDoesNotThrow(() -> servicio.verificarPropietario(grupoDe(INS), INS, "Instructor"));
    }

    @Test
    void verificarPropietarioConGrupo_instructorDeOtroGrupo_lanzaAccesoGrupoDenegado() {
        assertThrows(AccesoGrupoDenegadoExcepcion.class,
                () -> servicio.verificarPropietario(grupoDe(OTRO_INS), INS, "Instructor"));
    }

    @Test
    void verificarPropietarioConGrupo_grupoSinInstructorAsignado_lanzaAccesoGrupoDenegado() {
        assertThrows(AccesoGrupoDenegadoExcepcion.class,
                () -> servicio.verificarPropietario(grupoDe(null), INS, "Instructor"));
    }

    @Test
    void verificarPropietarioConGrupo_coordinador_permiteSinImportarElInstructorDelGrupo() {
        assertDoesNotThrow(() -> servicio.verificarPropietario(grupoDe(OTRO_INS), INS, "Coordinador"));
    }

    @Test
    void verificarPropietarioConGrupo_administrador_permiteSinImportarElInstructorDelGrupo() {
        assertDoesNotThrow(() -> servicio.verificarPropietario(grupoDe(OTRO_INS), "admin-1", "Administrador"));
    }

    // ── verificarPropietario(categoria, curso, anio, iterable, ...) ────────

    @Test
    void verificarPropietarioPorClave_instructorTitular_permite() {
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(grupoDe(INS));

        assertDoesNotThrow(() -> servicio.verificarPropietario(CATEGORIA, CURSO, ANIO, ITERABLE, INS, "Instructor"));
    }

    @Test
    void verificarPropietarioPorClave_instructorDeOtroGrupo_lanzaAccesoGrupoDenegado() {
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(grupoDe(OTRO_INS));

        assertThrows(AccesoGrupoDenegadoExcepcion.class,
                () -> servicio.verificarPropietario(CATEGORIA, CURSO, ANIO, ITERABLE, INS, "Instructor"));
    }

    // Coordinador/Administrador: permitidos SIN consultar el grupo -- ni
    // siquiera se llama a grupoGateway.obtenerGrupo().
    @Test
    void verificarPropietarioPorClave_coordinador_permiteSinConsultarElGrupo() {
        assertDoesNotThrow(() -> servicio.verificarPropietario(CATEGORIA, CURSO, ANIO, ITERABLE, "coord-1", "Coordinador"));

        verify(grupoGateway, never()).obtenerGrupo(any(), any(), any(), any());
    }

    @Test
    void verificarPropietarioPorClave_administrador_permiteSinConsultarElGrupo() {
        assertDoesNotThrow(() -> servicio.verificarPropietario(CATEGORIA, CURSO, ANIO, ITERABLE, "admin-1", "Administrador"));

        verify(grupoGateway, never()).obtenerGrupo(any(), any(), any(), any());
    }

    // Grupo inexistente: el gateway ya lanza NoExisteExcepcion (404) -- se
    // conserva el comportamiento actual sin envolverla.
    @Test
    void verificarPropietarioPorClave_grupoInexistente_propagaNoExisteExcepcionDelGateway() {
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenThrow(new co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion("no existe"));

        assertThrows(co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion.class,
                () -> servicio.verificarPropietario(CATEGORIA, CURSO, ANIO, ITERABLE, INS, "Instructor"));
    }
}
