package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAlumnoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.ICursoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PublicoCurso;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CursoServicioTest {

    @Mock
    private ICursoGateway cursoGateway;

    @Mock
    private IAlumnoGateway alumnoGateway;

    @Mock
    private IGrupoGateway grupoGateway;

    @InjectMocks
    private CursoServicio cursoServicio;

    // Fase 3: los cursos que salen del servicio llevan el estado de inscripciones derivado de sus grupos, que usa el reloj.
    @org.junit.jupiter.api.BeforeEach
    void relojPorDefecto() {
        relojFijo();
    }

    private static final String CATEGORIA = "Acuatico";
    private static final String NOMBRE    = "Natacion";

    @Test
    void cambiarEstadoCurso_exitoso_retornaDtoConEstadoActualizado() {
        Curso cursoActualizado = new Curso();
        cursoActualizado.setNombre(NOMBRE);
        cursoActualizado.setCategoriaCurso(CATEGORIA);
        cursoActualizado.setDescripcion("Descripcion");
        cursoActualizado.setDeporte("Natacion");
        cursoActualizado.setImagenId(1);
        cursoActualizado.setEstadoCurso(EstadoCurso.INACTIVO);

        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.cambiarEstadoCurso(CATEGORIA, NOMBRE, EstadoCurso.INACTIVO))
            .thenReturn(cursoActualizado);

        Curso resultado = cursoServicio.cambiarEstadoCurso(CATEGORIA, NOMBRE, EstadoCurso.INACTIVO);

        assertNotNull(resultado);
        assertEquals(EstadoCurso.INACTIVO, resultado.getEstadoCurso());
        verify(cursoGateway).cambiarEstadoCurso(CATEGORIA, NOMBRE, EstadoCurso.INACTIVO);
    }

    @Test
    void cambiarEstadoCurso_cursoNoExiste_lanzaNoExisteExcepcion() {
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
            () -> cursoServicio.cambiarEstadoCurso(CATEGORIA, NOMBRE, EstadoCurso.INACTIVO));

        verify(cursoGateway, never()).cambiarEstadoCurso(any(), any(), any());
    }

    // --- eliminarCursoPermanente ---

    @Test
    void eliminarCursoPermanente_exitoso_retornaDtoConEstadoInactivo() {
        Curso cursoActivo = new Curso();
        cursoActivo.setNombre(NOMBRE);
        cursoActivo.setCategoriaCurso(CATEGORIA);
        cursoActivo.setDescripcion("Descripcion");
        cursoActivo.setDeporte("Natacion");
        cursoActivo.setImagenId(1);
        cursoActivo.setEstadoCurso(EstadoCurso.ACTIVO);

        Curso cursoEliminado = new Curso();
        cursoEliminado.setNombre(NOMBRE);
        cursoEliminado.setCategoriaCurso(CATEGORIA);
        cursoEliminado.setDescripcion("Descripcion");
        cursoEliminado.setDeporte("Natacion");
        cursoEliminado.setImagenId(1);
        cursoEliminado.setEstadoCurso(EstadoCurso.INACTIVO);

        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.obtenerCurso(CATEGORIA, NOMBRE)).thenReturn(cursoActivo);
        when(cursoGateway.eliminarCursoPermanente(CATEGORIA, NOMBRE)).thenReturn(cursoEliminado);

        Curso resultado = cursoServicio.eliminarCursoPermanente(CATEGORIA, NOMBRE);

        assertNotNull(resultado);
        assertEquals(EstadoCurso.INACTIVO, resultado.getEstadoCurso());
        verify(cursoGateway).eliminarCursoPermanente(CATEGORIA, NOMBRE);
    }

    @Test
    void eliminarCursoPermanente_cursoNoExiste_lanzaNoExisteExcepcion() {
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
            () -> cursoServicio.eliminarCursoPermanente(CATEGORIA, NOMBRE));

        verify(cursoGateway, never()).eliminarCursoPermanente(any(), any());
    }

    @Test
    void eliminarCursoPermanente_yaEstaEliminado_lanzaYaExisteElementoExcepcion() {
        Curso cursoYaInactivo = new Curso();
        cursoYaInactivo.setNombre(NOMBRE);
        cursoYaInactivo.setCategoriaCurso(CATEGORIA);
        cursoYaInactivo.setDescripcion("Descripcion");
        cursoYaInactivo.setDeporte("Natacion");
        cursoYaInactivo.setImagenId(1);
        cursoYaInactivo.setEstadoCurso(EstadoCurso.INACTIVO);

        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.obtenerCurso(CATEGORIA, NOMBRE)).thenReturn(cursoYaInactivo);

        assertThrows(YaExisteElementoExcepcion.class,
            () -> cursoServicio.eliminarCursoPermanente(CATEGORIA, NOMBRE));

        verify(cursoGateway, never()).eliminarCursoPermanente(any(), any());
    }

    // SonarCloud javabugs:S2259: existeCurso() y obtenerCurso() son 2 consultas
    // separadas (TOCTOU) -- si el curso desaparece justo entre ambas,
    // obtenerCurso() retorna null y actual.getEstadoCurso() reventaba con NPE
    // real en vez de la NoExisteExcepcion de negocio esperada.
    @Test
    void eliminarCursoPermanente_obtenerCursoRetornaNull_lanzaNoExisteExcepcionSinNpe() {
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.obtenerCurso(CATEGORIA, NOMBRE)).thenReturn(null);

        assertThrows(NoExisteExcepcion.class,
            () -> cursoServicio.eliminarCursoPermanente(CATEGORIA, NOMBRE));

        verify(cursoGateway, never()).eliminarCursoPermanente(any(), any());
    }

    // --- cursosDeCategoria (regresión) ---

    @Test
    void cursosDeCategoria_sigueExcluyendoEliminados() {
        Curso cursoActivo = new Curso();
        cursoActivo.setNombre("Natacion");
        cursoActivo.setCategoriaCurso(CATEGORIA);
        cursoActivo.setDescripcion("Desc");
        cursoActivo.setDeporte("Natacion");
        cursoActivo.setImagenId(1);
        cursoActivo.setEstadoCurso(EstadoCurso.ACTIVO);

        when(cursoGateway.obtenerCursosDeCategoria(CATEGORIA))
            .thenReturn(java.util.List.of(cursoActivo));

        java.util.List<Curso> resultado = cursoServicio.cursosDeCategoria(CATEGORIA);

        assertEquals(1, resultado.size());
        assertTrue(resultado.stream().allMatch(d -> EstadoCurso.ACTIVO.equals(d.getEstadoCurso())),
            "cursosDeCategoria no debe retornar cursos INACTIVOS");
        verify(cursoGateway).obtenerCursosDeCategoria(CATEGORIA);
        verify(cursoGateway, never()).obtenerTodosCursosDeCategoria(any());
    }

    // --- todosLosCursosDeCategoria ---

    @Test
    void todosLosCursosDeCategoria_incluyeCursosEliminados() {
        Curso cursoActivo = new Curso();
        cursoActivo.setNombre("Natacion");
        cursoActivo.setCategoriaCurso(CATEGORIA);
        cursoActivo.setDescripcion("Desc");
        cursoActivo.setDeporte("Natacion");
        cursoActivo.setImagenId(1);
        cursoActivo.setEstadoCurso(EstadoCurso.ACTIVO);

        Curso cursoInactivo = new Curso();
        cursoInactivo.setNombre("Futbol");
        cursoInactivo.setCategoriaCurso(CATEGORIA);
        cursoInactivo.setDescripcion("Desc");
        cursoInactivo.setDeporte("Futbol");
        cursoInactivo.setImagenId(1);
        cursoInactivo.setEstadoCurso(EstadoCurso.INACTIVO);

        when(cursoGateway.obtenerTodosCursosDeCategoria(CATEGORIA))
            .thenReturn(java.util.List.of(cursoActivo, cursoInactivo));

        java.util.List<Curso> resultado = cursoServicio.todosLosCursosDeCategoria(CATEGORIA);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().anyMatch(d -> EstadoCurso.INACTIVO.equals(d.getEstadoCurso())),
            "Debe incluir al menos un curso con estadoCurso INACTIVO");
        verify(cursoGateway).obtenerTodosCursosDeCategoria(CATEGORIA);
        verify(cursoGateway, never()).obtenerCursosDeCategoria(any());
    }

    // --- recuperarCursos ---

    @Test
    void recuperarCursos_delegaAlGateway() {
        Curso curso = new Curso();
        curso.setNombre(NOMBRE);
        curso.setCategoriaCurso(CATEGORIA);
        when(cursoGateway.obtenerCursos()).thenReturn(java.util.List.of(curso));

        java.util.List<Curso> resultado = cursoServicio.recuperarCursos();

        assertEquals(1, resultado.size());
        verify(cursoGateway).obtenerCursos();
    }

    // --- obtenerCurso ---

    @Test
    void obtenerCurso_existente_retornaCurso() {
        Curso curso = new Curso();
        curso.setNombre(NOMBRE);
        curso.setCategoriaCurso(CATEGORIA);
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.obtenerCurso(CATEGORIA, NOMBRE)).thenReturn(curso);

        Curso resultado = cursoServicio.obtenerCurso(CATEGORIA, NOMBRE);

        assertNotNull(resultado);
        assertEquals(NOMBRE, resultado.getNombre());
        verify(cursoGateway).obtenerCurso(CATEGORIA, NOMBRE);
    }

    // --- insertarCurso ---

    @Test
    void insertarCurso_nuevo_retornaCursoInsertado() {
        Curso nuevo = new Curso();
        nuevo.setNombre(NOMBRE);
        nuevo.setCategoriaCurso(CATEGORIA);
        nuevo.setDescripcion("Descripcion");
        nuevo.setDeporte("Natacion");
        nuevo.setImagenId(1);
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(false);
        when(cursoGateway.insertarCurso(nuevo)).thenReturn(nuevo);

        Curso resultado = cursoServicio.insertarCurso(nuevo);

        assertNotNull(resultado);
        assertEquals(NOMBRE, resultado.getNombre());
        verify(cursoGateway).insertarCurso(nuevo);
    }

    // --- actualizarCurso ---

    @Test
    void actualizarCurso_existente_retornaCursoActualizado() {
        Curso datos = new Curso();
        datos.setNombre(NOMBRE);
        datos.setCategoriaCurso(CATEGORIA);
        datos.setDescripcion("Nueva descripcion");
        datos.setDeporte("Natacion");
        datos.setImagenId(1);
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.actualizarCurso(CATEGORIA, NOMBRE, datos)).thenReturn(datos);

        Curso resultado = cursoServicio.actualizarCurso(CATEGORIA, NOMBRE, datos);

        assertNotNull(resultado);
        assertEquals("Nueva descripcion", resultado.getDescripcion());
        verify(cursoGateway).actualizarCurso(CATEGORIA, NOMBRE, datos);
    }

    // --- eliminarCurso ---

    @Test
    void eliminarCurso_existente_retornaCursoEliminado() {
        Curso cursoEliminado = new Curso();
        cursoEliminado.setNombre(NOMBRE);
        cursoEliminado.setCategoriaCurso(CATEGORIA);
        cursoEliminado.setEstadoCurso(EstadoCurso.INACTIVO);
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.eliminarCurso(CATEGORIA, NOMBRE)).thenReturn(cursoEliminado);

        Curso resultado = cursoServicio.eliminarCurso(CATEGORIA, NOMBRE);

        assertNotNull(resultado);
        assertEquals(EstadoCurso.INACTIVO, resultado.getEstadoCurso());
        verify(cursoGateway).eliminarCurso(CATEGORIA, NOMBRE);
    }

    // ── Cursos visibles para un Alumno: su tipo de vinculo + algun grupo con inscripciones abiertas ──

    private static final LocalDate HOY = LocalDate.of(2026, 11, 3);

    private void relojFijo() {
        ReflectionTestUtils.setField(cursoServicio, "clock", Clock.fixed(
                HOY.atStartOfDay(ZoneId.of("America/Bogota")).toInstant(), ZoneId.of("America/Bogota")));
    }

    private Curso curso(String nombre, PublicoCurso publico) {
        Curso c = new Curso();
        c.setNombre(nombre);
        c.setCategoriaCurso(CATEGORIA);
        c.setPublico(publico);
        c.setEstadoCurso(EstadoCurso.ACTIVO);
        return c;
    }

    private Grupo grupo(LocalDate apertura, LocalDate cierre) {
        Grupo g = new Grupo();
        g.setFechaInscripcionApertura(apertura);
        g.setFechaIncripcionCierre(cierre);
        return g;
    }

    private Grupo grupoAbierto() {
        return grupo(HOY.minusDays(5), HOY.plusDays(5));
    }

    private void alumnoDeTipo(String tipo) {
        Alumno a = new Alumno();
        a.setTipoAlumno(tipo);
        when(alumnoGateway.obtenerAlumno("alum1")).thenReturn(Optional.of(a));
    }

    private void gruposDe(String curso, Grupo... grupos) {
        when(grupoGateway.obtenerGruposDeCurso(CATEGORIA, curso)).thenReturn(List.of(grupos));
    }

    private List<String> nombres(List<Curso> cursos) {
        return cursos.stream().map(Curso::getNombre).toList();
    }

    @Test
    void cursosParaAlumno_estudiante_veSoloEstudianteYMixto() {
        relojFijo();
        alumnoDeTipo("Estudiante");
        when(cursoGateway.obtenerCursosDeCategoria(CATEGORIA)).thenReturn(List.of(
                curso("ParaEstudiantes", PublicoCurso.ESTUDIANTE),
                curso("ParaFuncionarios", PublicoCurso.FUNCIONARIO),
                curso("ParaTodos", PublicoCurso.MIXTO)));
        gruposDe("ParaEstudiantes", grupoAbierto());
        gruposDe("ParaTodos", grupoAbierto());

        List<Curso> resultado = cursoServicio.cursosDeCategoriaParaAlumno(CATEGORIA, "alum1");

        assertEquals(List.of("ParaEstudiantes", "ParaTodos"), nombres(resultado));
        verify(grupoGateway, never()).obtenerGruposDeCurso(CATEGORIA, "ParaFuncionarios");
    }

    @Test
    void cursosParaAlumno_docenteAdministrativoYContratista_veFuncionarioYMixto() {
        relojFijo();
        for (String tipo : List.of("Docente", "Administrativo", "Contratista")) {
            org.mockito.Mockito.reset(alumnoGateway, cursoGateway, grupoGateway);
            alumnoDeTipo(tipo);
            when(cursoGateway.obtenerCursosDeCategoria(CATEGORIA)).thenReturn(List.of(
                    curso("ParaEstudiantes", PublicoCurso.ESTUDIANTE),
                    curso("ParaFuncionarios", PublicoCurso.FUNCIONARIO),
                    curso("ParaTodos", PublicoCurso.MIXTO)));
            gruposDe("ParaFuncionarios", grupoAbierto());
            gruposDe("ParaTodos", grupoAbierto());

            assertEquals(List.of("ParaFuncionarios", "ParaTodos"),
                    nombres(cursoServicio.cursosDeCategoriaParaAlumno(CATEGORIA, "alum1")), tipo);
        }
    }

    @Test
    void cursosParaAlumno_unCursoSinGrupoAbiertoPorFechaSeOculta() {
        relojFijo();
        alumnoDeTipo("Estudiante");
        when(cursoGateway.obtenerCursosDeCategoria(CATEGORIA)).thenReturn(List.of(
                curso("Cerrado", PublicoCurso.MIXTO),
                curso("Programado", PublicoCurso.MIXTO),
                curso("SinGrupos", PublicoCurso.MIXTO),
                curso("Abierto", PublicoCurso.MIXTO)));
        gruposDe("Cerrado", grupo(HOY.minusDays(30), HOY.minusDays(1)));
        gruposDe("Programado", grupo(HOY.plusDays(2), HOY.plusDays(20)));
        gruposDe("SinGrupos");
        gruposDe("Abierto", grupoAbierto());

        assertEquals(List.of("Abierto"), nombres(cursoServicio.cursosDeCategoriaParaAlumno(CATEGORIA, "alum1")));
    }

    @Test
    void cursosParaAlumno_conUnGrupoAbiertoYOtroCerrado_esVisible() {
        relojFijo();
        alumnoDeTipo("Docente");
        when(cursoGateway.obtenerCursosDeCategoria(CATEGORIA)).thenReturn(List.of(curso("Mixto", PublicoCurso.MIXTO)));
        gruposDe("Mixto", grupo(HOY.minusDays(30), HOY.minusDays(1)), grupoAbierto());

        assertEquals(List.of("Mixto"), nombres(cursoServicio.cursosDeCategoriaParaAlumno(CATEGORIA, "alum1")));
    }

    @Test
    void cursosParaAlumno_gruposSinFechas_cuentanComoAbiertos() {
        relojFijo();
        alumnoDeTipo("Estudiante");
        when(cursoGateway.obtenerCursosDeCategoria(CATEGORIA)).thenReturn(List.of(curso("SinFechas", PublicoCurso.MIXTO)));
        gruposDe("SinFechas", grupo(null, null));

        assertEquals(List.of("SinFechas"), nombres(cursoServicio.cursosDeCategoriaParaAlumno(CATEGORIA, "alum1")));
    }

    @Test
    void cursosParaAlumno_ultimoDiaDeInscripcion_sigueAbierto() {
        relojFijo();
        alumnoDeTipo("Estudiante");
        when(cursoGateway.obtenerCursosDeCategoria(CATEGORIA)).thenReturn(List.of(curso("UltimoDia", PublicoCurso.MIXTO)));
        gruposDe("UltimoDia", grupo(HOY.minusDays(10), HOY));

        assertEquals(List.of("UltimoDia"), nombres(cursoServicio.cursosDeCategoriaParaAlumno(CATEGORIA, "alum1")));
    }

    @Test
    void cursosParaAlumno_alumnoNoEncontrado_soloVeLosMixtos() {
        relojFijo();
        when(alumnoGateway.obtenerAlumno("alum1")).thenReturn(Optional.empty());
        when(cursoGateway.obtenerCursosDeCategoria(CATEGORIA)).thenReturn(List.of(
                curso("ParaEstudiantes", PublicoCurso.ESTUDIANTE),
                curso("ParaTodos", PublicoCurso.MIXTO)));
        gruposDe("ParaTodos", grupoAbierto());

        assertEquals(List.of("ParaTodos"), nombres(cursoServicio.cursosDeCategoriaParaAlumno(CATEGORIA, "alum1")));
    }

    @Test
    void cursosParaAlumno_cursoSinPublicoSeTrataComoMixto() {
        relojFijo();
        alumnoDeTipo("Contratista");
        when(cursoGateway.obtenerCursosDeCategoria(CATEGORIA)).thenReturn(List.of(curso("SinPublico", null)));
        gruposDe("SinPublico", grupoAbierto());

        assertEquals(List.of("SinPublico"), nombres(cursoServicio.cursosDeCategoriaParaAlumno(CATEGORIA, "alum1")));
    }

    @Test
    void recuperarCursosParaAlumno_aplicaLasMismasReglasYOcultaLosInactivos() {
        relojFijo();
        alumnoDeTipo("Estudiante");
        Curso inactivo = curso("Inactivo", PublicoCurso.MIXTO);
        inactivo.setEstadoCurso(EstadoCurso.INACTIVO);
        when(cursoGateway.obtenerCursos()).thenReturn(List.of(
                curso("Visible", PublicoCurso.ESTUDIANTE),
                curso("NoCorresponde", PublicoCurso.FUNCIONARIO),
                inactivo));
        gruposDe("Visible", grupoAbierto());

        assertEquals(List.of("Visible"), nombres(cursoServicio.recuperarCursosParaAlumno("alum1")));
        verify(grupoGateway, never()).obtenerGruposDeCurso(CATEGORIA, "Inactivo");
    }

    @Test
    void cursosParaAlumno_sinCursos_retornaListaVacia() {
        relojFijo();
        alumnoDeTipo("Estudiante");
        when(cursoGateway.obtenerCursosDeCategoria(CATEGORIA)).thenReturn(List.of());

        assertTrue(cursoServicio.cursosDeCategoriaParaAlumno(CATEGORIA, "alum1").isEmpty());
    }

    // ── Fase 3: el estado de inscripciones del curso se calcula a partir de sus grupos ──

    private Curso cursoConInterruptorViejo(String nombre, EstadoInscripciones valorViejo) {
        Curso c = curso(nombre, PublicoCurso.MIXTO);
        c.setEstadoInscripciones(valorViejo);
        return c;
    }

    @Test
    void estadoInscripciones_conUnGrupoAbierto_esABIERTO_aunqueElInterruptorViejoDiga_CERRADO() {
        relojFijo();
        when(cursoGateway.obtenerCursos()).thenReturn(List.of(cursoConInterruptorViejo("Fulbol", EstadoInscripciones.CERRADO)));
        gruposDe("Fulbol", grupo(HOY.minusDays(30), HOY.minusDays(1)), grupoAbierto());

        assertEquals(EstadoInscripciones.ABIERTO, cursoServicio.recuperarCursos().get(0).getEstadoInscripciones());
    }

    @Test
    void estadoInscripciones_todosLosGruposVencidos_esCERRADO_aunqueElInterruptorViejoDiga_ABIERTO() {
        relojFijo();
        when(cursoGateway.obtenerCursos()).thenReturn(List.of(cursoConInterruptorViejo("Fulbol", EstadoInscripciones.ABIERTO)));
        gruposDe("Fulbol", grupo(HOY.minusDays(30), HOY.minusDays(1)));

        assertEquals(EstadoInscripciones.CERRADO, cursoServicio.recuperarCursos().get(0).getEstadoInscripciones());
    }

    @Test
    void estadoInscripciones_elUnicoGrupoAbiertoCerradoAMano_esCERRADO() {
        relojFijo();
        Grupo cerradoAMano = grupoAbierto();
        cerradoAMano.setInscripcionesCerradasManual(true);
        when(cursoGateway.obtenerCursos()).thenReturn(List.of(cursoConInterruptorViejo("Fulbol", EstadoInscripciones.ABIERTO)));
        gruposDe("Fulbol", cerradoAMano);

        assertEquals(EstadoInscripciones.CERRADO, cursoServicio.recuperarCursos().get(0).getEstadoInscripciones());
    }

    @Test
    void estadoInscripciones_unGrupoCerradoAManoYOtroAbierto_esABIERTO() {
        relojFijo();
        Grupo cerradoAMano = grupoAbierto();
        cerradoAMano.setInscripcionesCerradasManual(true);
        when(cursoGateway.obtenerCursos()).thenReturn(List.of(cursoConInterruptorViejo("Fulbol", EstadoInscripciones.CERRADO)));
        gruposDe("Fulbol", cerradoAMano, grupoAbierto());

        assertEquals(EstadoInscripciones.ABIERTO, cursoServicio.recuperarCursos().get(0).getEstadoInscripciones());
    }

    @Test
    void estadoInscripciones_grupoQueAunNoAbre_noCuentaComoAbierto() {
        relojFijo();
        when(cursoGateway.obtenerCursos()).thenReturn(List.of(cursoConInterruptorViejo("Fulbol", EstadoInscripciones.ABIERTO)));
        gruposDe("Fulbol", grupo(HOY.plusDays(2), HOY.plusDays(20)));

        assertEquals(EstadoInscripciones.CERRADO, cursoServicio.recuperarCursos().get(0).getEstadoInscripciones());
    }

    @Test
    void estadoInscripciones_cursoSinGrupos_esSIN_GRUPOS() {
        relojFijo();
        when(cursoGateway.obtenerCursos()).thenReturn(List.of(cursoConInterruptorViejo("Nuevo", EstadoInscripciones.ABIERTO)));
        gruposDe("Nuevo");

        assertEquals(EstadoInscripciones.SIN_GRUPOS, cursoServicio.recuperarCursos().get(0).getEstadoInscripciones());
    }

    @Test
    void estadoInscripciones_seDerivaEnTodosLosListadosYEnElDetalle() {
        relojFijo();
        Curso base = cursoConInterruptorViejo("Fulbol", EstadoInscripciones.CERRADO);
        when(cursoGateway.obtenerCursosDeCategoria(CATEGORIA)).thenReturn(List.of(base));
        when(cursoGateway.obtenerTodosCursosDeCategoria(CATEGORIA)).thenReturn(List.of(base));
        when(cursoGateway.existeCurso(CATEGORIA, "Fulbol")).thenReturn(true);
        when(cursoGateway.obtenerCurso(CATEGORIA, "Fulbol")).thenReturn(base);
        gruposDe("Fulbol", grupoAbierto());

        assertEquals(EstadoInscripciones.ABIERTO, cursoServicio.cursosDeCategoria(CATEGORIA).get(0).getEstadoInscripciones());
        assertEquals(EstadoInscripciones.ABIERTO, cursoServicio.todosLosCursosDeCategoria(CATEGORIA).get(0).getEstadoInscripciones());
        assertEquals(EstadoInscripciones.ABIERTO, cursoServicio.obtenerCurso(CATEGORIA, "Fulbol").getEstadoInscripciones());
    }

    @Test
    void estadoInscripciones_alCrearYActualizarUnCurso_tambienSeDeriva() {
        relojFijo();
        Curso nuevo = cursoConInterruptorViejo("Nuevo", EstadoInscripciones.ABIERTO);
        when(cursoGateway.existeCurso(CATEGORIA, "Nuevo")).thenReturn(false, true);
        when(cursoGateway.insertarCurso(any())).thenReturn(nuevo);
        when(cursoGateway.actualizarCurso(eq(CATEGORIA), eq("Nuevo"), any())).thenReturn(nuevo);
        gruposDe("Nuevo");

        assertEquals(EstadoInscripciones.SIN_GRUPOS, cursoServicio.insertarCurso(nuevo).getEstadoInscripciones());
        assertEquals(EstadoInscripciones.SIN_GRUPOS, cursoServicio.actualizarCurso(CATEGORIA, "Nuevo", nuevo).getEstadoInscripciones());
    }
}
