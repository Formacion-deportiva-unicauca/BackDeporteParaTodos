package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAlumnoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.AccesoGrupoDenegadoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.CorreoNoModificablePorInstructorExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AlumnoDto;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlumnoServicioTest {

    @Mock private IAlumnoGateway alumnoGateway;
    @Mock private IPerfilGateway perfilGateway;
    @Mock private IInscripcionGateway inscripcionGateway;
    @Mock private IGrupoGateway grupoGateway;
    @Mock private AutorizacionGrupoServicio autorizacionGrupo;
    @InjectMocks private AlumnoServicio alumnoServicio;

    private static final String  ID       = "12345678";
    private static final String  CATEGORIA = "Recreativo";
    private static final String  CURSO    = "Natacion";
    private static final Integer ANIO     = 2025;
    private static final Integer ITERABLE = 1;
    private static final String  INS      = "INS001";
    private static final String  COORD    = "coord-1";

    private Alumno alumnoModelo() {
        Perfil perfil = new Perfil();
        perfil.setId(ID);
        perfil.setNombre("Juan Test");
        perfil.setCorreo("juan@unicauca.edu.co");

        Alumno alumno = new Alumno();
        alumno.setPerfil(perfil);
        alumno.setAlm_codigo("2025-REC-001");
        alumno.setTipoAlumno("Estudiante");
        return alumno;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerAlumnos
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerAlumnos_listaNoVacia_retornaLista() {
        when(alumnoGateway.obtenerAlumnos()).thenReturn(List.of(alumnoModelo()));

        List<Alumno> resultado = alumnoServicio.obtenerAlumnos();

        assertEquals(1, resultado.size());
        verify(alumnoGateway).obtenerAlumnos();
    }

    @Test
    void obtenerAlumnos_listaVacia_lanzaListadoVacioExcepcion() {
        when(alumnoGateway.obtenerAlumnos()).thenReturn(List.of());

        assertThrows(ListadoVacioExcepcion.class, () -> alumnoServicio.obtenerAlumnos());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerAlumnosGrupo
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerAlumnosGrupo_listaNoVacia_retornaDtosMapeados() {
        // NOTA: AlumnoDto.fabricarDeModelo puede retornar null si el modelo es invalido,
        // y el servicio lo agregaria a la lista sin null-check (comportamiento B).
        // Con modelos validos el mapeo funciona correctamente.
        when(alumnoGateway.obtenerAlumnosGrupo(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(List.of(alumnoModelo()));

        List<AlumnoDto> resultado = alumnoServicio.obtenerAlumnosGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);

        assertEquals(1, resultado.size());
        verify(alumnoGateway).obtenerAlumnosGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);
    }

    @Test
    void obtenerAlumnosGrupo_listaVacia_lanzaListadoVacioExcepcion() {
        when(alumnoGateway.obtenerAlumnosGrupo(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(List.of());

        assertThrows(ListadoVacioExcepcion.class,
                () -> alumnoServicio.obtenerAlumnosGrupo(CATEGORIA, CURSO, ANIO, ITERABLE));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // insertAlumno
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void insertAlumno_retornaNullSinDelegarAlGateway_comportamientoActual() {
        // COMPORTAMIENTO CUESTIONABLE: insertAlumno no delega al gateway ni lanza
        // excepcion; simplemente retorna null. No hay endpoint REST que lo exponga
        // en AlumnoRest. Codigo muerto identico al patron de insertarInstructor.
        Alumno resultado = alumnoServicio.insertAlumno(new Alumno());

        assertNull(resultado, "insertAlumno siempre retorna null sin llamar al gateway");
        verifyNoInteractions(alumnoGateway);
        verifyNoInteractions(perfilGateway);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerAlumno
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerAlumno_existente_retornaAlumno() {
        when(alumnoGateway.obtenerAlumno(ID)).thenReturn(Optional.of(alumnoModelo()));

        Alumno resultado = alumnoServicio.obtenerAlumno(ID);

        assertNotNull(resultado);
        assertEquals("2025-REC-001", resultado.getAlm_codigo());
    }

    @Test
    void obtenerAlumno_noExiste_lanzaNoExisteExcepcion() {
        when(alumnoGateway.obtenerAlumno(ID)).thenReturn(Optional.empty());

        assertThrows(NoExisteExcepcion.class, () -> alumnoServicio.obtenerAlumno(ID));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // actualizarAlumno
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void actualizarAlumno_noExiste_lanzaNoExisteExcepcion() {
        when(alumnoGateway.existeAlumno(ID)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
                () -> alumnoServicio.actualizarAlumno(ID, alumnoModelo(), COORD, "Coordinador"));

        verify(alumnoGateway, never()).actualizarAlumno(any(), any());
        verifyNoInteractions(perfilGateway);
    }

    @Test
    void actualizarAlumno_existente_coordinador_actualizaPerfilYAlumno() {
        // Verifica que el servicio coordina ambos gateways: primero actualiza el perfil
        // (nombre/correo en tbl_perfil via perfilGateway) y luego el alumno (tipo en
        // tbl_alumno via alumnoGateway). Ambas llamadas son requeridas para que SCRUM-66
        // PUT /alumnos/{id} funcione correctamente. Coordinador: sin restricciones.
        when(alumnoGateway.existeAlumno(ID)).thenReturn(true);
        when(alumnoGateway.actualizarAlumno(eq(ID), any(Alumno.class))).thenReturn(alumnoModelo());

        Alumno resultado = alumnoServicio.actualizarAlumno(ID, alumnoModelo(), COORD, "Coordinador");

        assertNotNull(resultado);
        verify(perfilGateway).actualizarPerfil(eq(ID), any(Perfil.class));
        verify(alumnoGateway).actualizarAlumno(eq(ID), any(Alumno.class));
        verifyNoInteractions(inscripcionGateway);
    }

    // ── Autorizacion por grupo (caso 3 del ticket): Instructor ──────────────────

    private Inscripcion inscripcionActiva() {
        return new Inscripcion(ID, CATEGORIA, CURSO, ANIO, ITERABLE,
                Timestamp.valueOf("2026-01-01 00:00:00"), null, "INSCRITO");
    }

    private Grupo grupoDe(String idInstructor) {
        Grupo g = new Grupo();
        g.setCategoria(CATEGORIA);
        g.setCurso(CURSO);
        g.setAnio(ANIO);
        g.setIterable(ITERABLE);
        g.setIdInstructor(idInstructor);
        return g;
    }

    @Test
    void actualizarAlumno_instructorConAlumnoACargo_permiteActualizarNombreYTipo() {
        Alumno datosNuevos = alumnoModelo(); // mismo correo que el actual -- no se intenta cambiar
        when(alumnoGateway.existeAlumno(ID)).thenReturn(true);
        when(inscripcionGateway.obtenerInscripcionesAlumno(ID)).thenReturn(List.of(inscripcionActiva()));
        when(inscripcionGateway.existeInscripcionActiva(ID, CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(true);
        Grupo grupo = grupoDe(INS);
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(grupo);
        when(autorizacionGrupo.esPropietario(grupo, INS)).thenReturn(true);
        when(perfilGateway.obtenerPerfil(ID)).thenReturn(Optional.of(alumnoModelo().getPerfil()));
        when(alumnoGateway.actualizarAlumno(eq(ID), any(Alumno.class))).thenReturn(datosNuevos);

        Alumno resultado = alumnoServicio.actualizarAlumno(ID, datosNuevos, INS, "Instructor");

        assertNotNull(resultado);
        verify(alumnoGateway).actualizarAlumno(eq(ID), any(Alumno.class));
    }

    // "Grupo sin instructor asignado: denegado para el Instructor" -- cubierto
    // aqui porque grupoDe(null) nunca hace match con esPropietario (delegado al
    // componente real en AutorizacionGrupoServicioTest); el mock de
    // autorizacionGrupo.esPropietario(...) default false ya representa ese caso.
    @Test
    void actualizarAlumno_instructorSinAlumnoACargo_lanzaAccesoGrupoDenegado() {
        when(alumnoGateway.existeAlumno(ID)).thenReturn(true);
        when(inscripcionGateway.obtenerInscripcionesAlumno(ID)).thenReturn(List.of(inscripcionActiva()));
        when(inscripcionGateway.existeInscripcionActiva(ID, CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(true);
        Grupo grupoDeOtro = grupoDe("OTRO_INS");
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(grupoDeOtro);
        when(autorizacionGrupo.esPropietario(grupoDeOtro, INS)).thenReturn(false);

        assertThrows(AccesoGrupoDenegadoExcepcion.class,
                () -> alumnoServicio.actualizarAlumno(ID, alumnoModelo(), INS, "Instructor"));

        verify(alumnoGateway, never()).actualizarAlumno(any(), any());
        verifyNoInteractions(perfilGateway);
    }

    @Test
    void actualizarAlumno_instructorSinInscripcionesDelAlumno_lanzaAccesoGrupoDenegado() {
        when(alumnoGateway.existeAlumno(ID)).thenReturn(true);
        when(inscripcionGateway.obtenerInscripcionesAlumno(ID)).thenReturn(List.of());

        assertThrows(AccesoGrupoDenegadoExcepcion.class,
                () -> alumnoServicio.actualizarAlumno(ID, alumnoModelo(), INS, "Instructor"));

        verify(alumnoGateway, never()).actualizarAlumno(any(), any());
        verify(grupoGateway, never()).obtenerGrupo(any(), any(), any(), any());
    }

    // Inscripcion desvinculada (o EN_ESPERA) no cuenta como "a cargo".
    @Test
    void actualizarAlumno_instructorConInscripcionNoActiva_noCuentaComoACargo_lanzaAccesoGrupoDenegado() {
        when(alumnoGateway.existeAlumno(ID)).thenReturn(true);
        when(inscripcionGateway.obtenerInscripcionesAlumno(ID)).thenReturn(List.of(inscripcionActiva()));
        when(inscripcionGateway.existeInscripcionActiva(ID, CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(false);

        assertThrows(AccesoGrupoDenegadoExcepcion.class,
                () -> alumnoServicio.actualizarAlumno(ID, alumnoModelo(), INS, "Instructor"));

        verify(grupoGateway, never()).obtenerGrupo(any(), any(), any(), any());
        verify(alumnoGateway, never()).actualizarAlumno(any(), any());
    }

    // Decision documentada: el Instructor NO puede cambiar el correo del alumno
    // (afecta su inicio de sesion) -- 422 con codigo propio, sin tocar nada.
    @Test
    void actualizarAlumno_instructorIntentaCambiarCorreo_lanzaCorreoNoModificable() {
        Alumno actual = alumnoModelo(); // correo actual: juan@unicauca.edu.co
        Alumno datosConCorreoNuevo = alumnoModelo();
        datosConCorreoNuevo.getPerfil().setCorreo("otro-correo@unicauca.edu.co");

        when(alumnoGateway.existeAlumno(ID)).thenReturn(true);
        when(inscripcionGateway.obtenerInscripcionesAlumno(ID)).thenReturn(List.of(inscripcionActiva()));
        when(inscripcionGateway.existeInscripcionActiva(ID, CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(true);
        Grupo grupo = grupoDe(INS);
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(grupo);
        when(autorizacionGrupo.esPropietario(grupo, INS)).thenReturn(true);
        when(perfilGateway.obtenerPerfil(ID)).thenReturn(Optional.of(actual.getPerfil()));

        assertThrows(CorreoNoModificablePorInstructorExcepcion.class,
                () -> alumnoServicio.actualizarAlumno(ID, datosConCorreoNuevo, INS, "Instructor"));

        verify(alumnoGateway, never()).actualizarAlumno(any(), any());
        verify(perfilGateway, never()).actualizarPerfil(any(), any());
    }

    @Test
    void actualizarAlumno_instructorEnviaElMismoCorreoActual_noLanzaNada() {
        Alumno actual = alumnoModelo(); // correo: juan@unicauca.edu.co
        Alumno datosMismoCorreo = alumnoModelo(); // mismo correo exacto

        when(alumnoGateway.existeAlumno(ID)).thenReturn(true);
        when(inscripcionGateway.obtenerInscripcionesAlumno(ID)).thenReturn(List.of(inscripcionActiva()));
        when(inscripcionGateway.existeInscripcionActiva(ID, CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(true);
        Grupo grupo = grupoDe(INS);
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(grupo);
        when(autorizacionGrupo.esPropietario(grupo, INS)).thenReturn(true);
        when(perfilGateway.obtenerPerfil(ID)).thenReturn(Optional.of(actual.getPerfil()));
        when(alumnoGateway.actualizarAlumno(eq(ID), any(Alumno.class))).thenReturn(datosMismoCorreo);

        assertDoesNotThrow(() -> alumnoServicio.actualizarAlumno(ID, datosMismoCorreo, INS, "Instructor"));
        verify(alumnoGateway).actualizarAlumno(eq(ID), any(Alumno.class));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // eliminarAlumno
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void eliminarAlumno_noExiste_lanzaNoExisteExcepcion() {
        when(alumnoGateway.existeAlumno(ID)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class, () -> alumnoServicio.eliminarAlumno(ID));

        verify(alumnoGateway, never()).eliminarAlumno(any());
    }

    @Test
    void eliminarAlumno_exitoso_delegaAlGateway() {
        when(alumnoGateway.existeAlumno(ID)).thenReturn(true);
        when(alumnoGateway.eliminarAlumno(ID)).thenReturn(alumnoModelo());

        Alumno resultado = alumnoServicio.eliminarAlumno(ID);

        assertNotNull(resultado);
        verify(alumnoGateway).eliminarAlumno(ID);
    }
}
