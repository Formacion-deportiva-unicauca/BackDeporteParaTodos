package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DependenciaFallida;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.GrupoEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.CursoId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.GrupoId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.ICategoriaCursoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.ICursoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IGrupoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IImagenRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IInstructorRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrupoGatewayTest {

    @Mock private IGrupoRepositorio repoGrupo;
    @Mock private IImagenRepositorio repoImagen;
    @Mock private ICursoRepositorio repoCurso;
    @Mock private ICategoriaCursoRepositorio repoCategoria;
    @Mock private IInstructorRepositorio repoInstructor;

    @InjectMocks
    private GrupoGateway grupoGateway;

    private static final String CATEGORIA = "Recreativo";
    private static final String CURSO = "Natacion";

    private Grupo grupoBase(LocalDate fechaApertura, int periodo) {
        Grupo g = new Grupo();
        g.setCategoria(CATEGORIA);
        g.setCurso(CURSO);
        g.setImagenGrupo(1);
        g.setCupos(20);
        g.setIdInstructor(null);
        g.setFechaInscripcionApertura(fechaApertura);
        g.setPeriodo(periodo);
        return g;
    }

    private void mockDependencias() {
        when(repoCategoria.existsById(CATEGORIA)).thenReturn(true);
        when(repoCurso.existsById(any(CursoId.class))).thenReturn(true);
        when(repoGrupo.countByCategoriaAndCursoAndAnio(anyString(), anyString(), anyInt())).thenReturn(0);
        when(repoGrupo.existsById(any(GrupoId.class))).thenReturn(false);
        when(repoImagen.existsById(any())).thenReturn(true);
        when(repoGrupo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void insertarGrupo_calculaPeriodo1_mesesEnero_a_Junio() {
        mockDependencias();
        Grupo grupo = grupoBase(LocalDate.of(2025, 6, 1), 0);

        grupoGateway.insertarGrupo(grupo);

        ArgumentCaptor<GrupoEntidad> captor = ArgumentCaptor.forClass(GrupoEntidad.class);
        verify(repoGrupo).save(captor.capture());
        assertEquals(1, captor.getValue().getPeriodo());
        assertEquals(2025, captor.getValue().getAnio());
    }

    @Test
    void insertarGrupo_calculaPeriodo2_mesesJulio_a_Diciembre() {
        mockDependencias();
        Grupo grupo = grupoBase(LocalDate.of(2025, 7, 15), 0);

        grupoGateway.insertarGrupo(grupo);

        ArgumentCaptor<GrupoEntidad> captor = ArgumentCaptor.forClass(GrupoEntidad.class);
        verify(repoGrupo).save(captor.capture());
        assertEquals(2, captor.getValue().getPeriodo());
        assertEquals(2025, captor.getValue().getAnio());
    }

    @Test
    void insertarGrupo_sinFechaApertura_derivaAnioYPeriodoDeFechaActual() {
        mockDependencias();
        Grupo grupo = grupoBase(null, 0);
        LocalDate hoy = LocalDate.now();
        int periodoEsperado = hoy.getMonthValue() >= 7 ? 2 : 1;

        grupoGateway.insertarGrupo(grupo);

        ArgumentCaptor<GrupoEntidad> captor = ArgumentCaptor.forClass(GrupoEntidad.class);
        verify(repoGrupo).save(captor.capture());
        assertEquals(periodoEsperado, captor.getValue().getPeriodo());
        assertEquals(hoy.getYear(), captor.getValue().getAnio());
    }

    @Test
    void insertarGrupo_periodoExplicito_noSeRecalcula() {
        mockDependencias();
        // mes 3 calcularía periodo 1, pero el DTO trae periodo=2 explícito
        Grupo grupo = grupoBase(LocalDate.of(2025, 3, 1), 2);

        grupoGateway.insertarGrupo(grupo);

        ArgumentCaptor<GrupoEntidad> captor = ArgumentCaptor.forClass(GrupoEntidad.class);
        verify(repoGrupo).save(captor.capture());
        assertEquals(2, captor.getValue().getPeriodo());
    }

    // ── obtenerGrupoConLock ─────────────────────────────────────────────────

    @Test
    void obtenerGrupoConLock_grupoExistente_retornaGrupo() {
        GrupoEntidad entidad = new GrupoEntidad();
        entidad.setCategoria(CATEGORIA);
        entidad.setCurso(CURSO);
        entidad.setAnio(2026);
        entidad.setIterable(1);
        entidad.setCupos(10);
        when(repoGrupo.findByIdWithLock(CATEGORIA, CURSO, 2026, 1))
                .thenReturn(java.util.Optional.of(entidad));

        Grupo resultado = grupoGateway.obtenerGrupoConLock(CATEGORIA, CURSO, 2026, 1);

        assertNotNull(resultado);
        assertEquals(CATEGORIA, resultado.getCategoria());
        assertEquals(CURSO, resultado.getCurso());
    }

    @Test
    void obtenerGrupoConLock_grupoNoExistente_lanzaNoExisteExcepcion() {
        when(repoGrupo.findByIdWithLock(CATEGORIA, CURSO, 2026, 1))
                .thenReturn(java.util.Optional.empty());

        assertThrows(NoExisteExcepcion.class,
                () -> grupoGateway.obtenerGrupoConLock(CATEGORIA, CURSO, 2026, 1));
    }

    // ── actualizarGrupo ──────────────────────────────────────────────────────

    private GrupoEntidad entidadExistente(Integer imagenActual, String instructorActual) {
        GrupoEntidad e = new GrupoEntidad();
        e.setCategoria(CATEGORIA);
        e.setCurso(CURSO);
        e.setAnio(2026);
        e.setIterable(1);
        e.setImagenGrupo(imagenActual);
        e.setCupos(10);
        e.setIdInstructor(instructorActual);
        e.setPeriodo(1);
        e.setEliminado(0);
        return e;
    }

    @Test
    void actualizarGrupo_conInstructorValido_actualizaCorrectamente() {
        when(repoImagen.existsById(2)).thenReturn(true);
        when(repoInstructor.existsByIdPerfilAndEliminado("ins-nuevo", 0)).thenReturn(true);
        when(repoGrupo.findById(any(GrupoId.class))).thenReturn(java.util.Optional.of(entidadExistente(1, null)));
        when(repoGrupo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Grupo datos = new Grupo();
        datos.setImagenGrupo(2);
        datos.setCupos(15);
        datos.setIdInstructor("ins-nuevo");
        datos.setFechaCreacion(LocalDate.now());

        Grupo resultado = grupoGateway.actualizarGrupo(CATEGORIA, CURSO, 2026, 1, datos);

        assertEquals("ins-nuevo", resultado.getIdInstructor());
        assertEquals(2, resultado.getImagenGrupo());
        assertEquals(15, resultado.getCupos());
        verify(repoGrupo).save(any());
    }

    // idInstructor null en el PUT => se quita el instructor del grupo (PERF_ID -> NULL),
    // igual que insertarGrupo() ya tolera instructor null. No debe validarse contra
    // repoInstructor cuando no se esta asignando ninguno.
    @Test
    void actualizarGrupo_instructorNull_quitaInstructor() {
        when(repoImagen.existsById(1)).thenReturn(true);
        when(repoGrupo.findById(any(GrupoId.class))).thenReturn(java.util.Optional.of(entidadExistente(1, "ins-actual")));
        when(repoGrupo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Grupo datos = new Grupo();
        datos.setImagenGrupo(1);
        datos.setCupos(10);
        datos.setIdInstructor(null);

        Grupo resultado = grupoGateway.actualizarGrupo(CATEGORIA, CURSO, 2026, 1, datos);

        assertNull(resultado.getIdInstructor());
        verify(repoInstructor, never()).existsByIdPerfilAndEliminado(any(), any());
    }

    @Test
    void actualizarGrupo_instructorInexistente_lanzaDependenciaFallida() {
        when(repoImagen.existsById(1)).thenReturn(true);
        when(repoInstructor.existsByIdPerfilAndEliminado("no-existe", 0)).thenReturn(false);

        Grupo datos = new Grupo();
        datos.setImagenGrupo(1);
        datos.setIdInstructor("no-existe");

        assertThrows(DependenciaFallida.class,
                () -> grupoGateway.actualizarGrupo(CATEGORIA, CURSO, 2026, 1, datos));
        verify(repoGrupo, never()).save(any());
    }

    // imagenGrupo no enviada (null) en el PUT => se conserva la imagen ya asignada al
    // grupo, no se valida contra repoImagen ni se sobreescribe con null.
    @Test
    void actualizarGrupo_sinImagen_conservaImagenActual() {
        when(repoInstructor.existsByIdPerfilAndEliminado("ins-actual", 0)).thenReturn(true);
        when(repoGrupo.findById(any(GrupoId.class))).thenReturn(java.util.Optional.of(entidadExistente(7, "ins-actual")));
        when(repoGrupo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Grupo datos = new Grupo();
        datos.setImagenGrupo(null);
        datos.setCupos(10);
        datos.setIdInstructor("ins-actual");

        Grupo resultado = grupoGateway.actualizarGrupo(CATEGORIA, CURSO, 2026, 1, datos);

        assertEquals(7, resultado.getImagenGrupo());
        verify(repoImagen, never()).existsById(any());
    }

    // cupos/fechaCreacion/fechaFinalizacion no enviados (null) en el PUT => se conserva
    // el valor ya asignado al grupo, igual que imagenGrupo. Antes se sobreescribian con
    // null incondicionalmente.

    @Test
    void actualizarGrupo_sinCupos_conservaCuposActuales() {
        when(repoImagen.existsById(1)).thenReturn(true);
        when(repoInstructor.existsByIdPerfilAndEliminado("ins-actual", 0)).thenReturn(true);
        when(repoGrupo.findById(any(GrupoId.class))).thenReturn(java.util.Optional.of(entidadExistente(1, "ins-actual")));
        when(repoGrupo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Grupo datos = new Grupo();
        datos.setImagenGrupo(1);
        datos.setIdInstructor("ins-actual");
        datos.setCupos(null);

        Grupo resultado = grupoGateway.actualizarGrupo(CATEGORIA, CURSO, 2026, 1, datos);

        assertEquals(10, resultado.getCupos());
    }

    @Test
    void actualizarGrupo_sinFechaCreacion_conservaFechaCreacionActual() {
        when(repoImagen.existsById(1)).thenReturn(true);
        when(repoInstructor.existsByIdPerfilAndEliminado("ins-actual", 0)).thenReturn(true);
        GrupoEntidad existente = entidadExistente(1, "ins-actual");
        existente.setFechaCreacion(LocalDate.of(2020, 1, 1));
        when(repoGrupo.findById(any(GrupoId.class))).thenReturn(java.util.Optional.of(existente));
        when(repoGrupo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Grupo datos = new Grupo();
        datos.setImagenGrupo(1);
        datos.setIdInstructor("ins-actual");
        datos.setFechaCreacion(null);

        Grupo resultado = grupoGateway.actualizarGrupo(CATEGORIA, CURSO, 2026, 1, datos);

        assertEquals(LocalDate.of(2020, 1, 1), resultado.getFechaCreacion());
    }

    @Test
    void actualizarGrupo_sinFechaFinalizacion_conservaFechaFinalizacionActual() {
        when(repoImagen.existsById(1)).thenReturn(true);
        when(repoInstructor.existsByIdPerfilAndEliminado("ins-actual", 0)).thenReturn(true);
        GrupoEntidad existente = entidadExistente(1, "ins-actual");
        existente.setFechaFinalizacion(LocalDate.of(2026, 12, 31));
        when(repoGrupo.findById(any(GrupoId.class))).thenReturn(java.util.Optional.of(existente));
        when(repoGrupo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Grupo datos = new Grupo();
        datos.setImagenGrupo(1);
        datos.setIdInstructor("ins-actual");
        datos.setFechaFinalizacion(null);

        Grupo resultado = grupoGateway.actualizarGrupo(CATEGORIA, CURSO, 2026, 1, datos);

        assertEquals(LocalDate.of(2026, 12, 31), resultado.getFechaFinalizacion());
    }

    // fechaInscripcionApertura/fechaIncripcionCierre: antes del fix, actualizarGrupo()
    // nunca las leia de datosGrupo -- el PUT respondia 200 pero la fila quedaba
    // intacta. Mismo criterio null=conserva que cupos/fechaCreacion/fechaFinalizacion.
    @Test
    void actualizarGrupo_conFechasDeInscripcionEnviadas_lasPersiste() {
        when(repoImagen.existsById(1)).thenReturn(true);
        when(repoInstructor.existsByIdPerfilAndEliminado("ins-actual", 0)).thenReturn(true);
        when(repoGrupo.findById(any(GrupoId.class))).thenReturn(java.util.Optional.of(entidadExistente(1, "ins-actual")));
        when(repoGrupo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Grupo datos = new Grupo();
        datos.setImagenGrupo(1);
        datos.setIdInstructor("ins-actual");
        datos.setFechaInscripcionApertura(LocalDate.of(2026, 2, 1));
        datos.setFechaIncripcionCierre(LocalDate.of(2026, 2, 28));

        Grupo resultado = grupoGateway.actualizarGrupo(CATEGORIA, CURSO, 2026, 1, datos);

        assertEquals(LocalDate.of(2026, 2, 1), resultado.getFechaInscripcionApertura());
        assertEquals(LocalDate.of(2026, 2, 28), resultado.getFechaIncripcionCierre());
    }

    @Test
    void actualizarGrupo_sinFechasDeInscripcion_conservaLasActuales() {
        when(repoImagen.existsById(1)).thenReturn(true);
        when(repoInstructor.existsByIdPerfilAndEliminado("ins-actual", 0)).thenReturn(true);
        GrupoEntidad existente = entidadExistente(1, "ins-actual");
        existente.setFechaInscripcionApertura(LocalDate.of(2025, 1, 1));
        existente.setFechaIncripcionCierre(LocalDate.of(2025, 1, 31));
        when(repoGrupo.findById(any(GrupoId.class))).thenReturn(java.util.Optional.of(existente));
        when(repoGrupo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Grupo datos = new Grupo();
        datos.setImagenGrupo(1);
        datos.setIdInstructor("ins-actual");
        datos.setFechaInscripcionApertura(null);
        datos.setFechaIncripcionCierre(null);

        Grupo resultado = grupoGateway.actualizarGrupo(CATEGORIA, CURSO, 2026, 1, datos);

        assertEquals(LocalDate.of(2025, 1, 1), resultado.getFechaInscripcionApertura());
        assertEquals(LocalDate.of(2025, 1, 31), resultado.getFechaIncripcionCierre());
    }

    // ── existeGrupoEliminado ─────────────────────────────────────────────────

    @Test
    void existeGrupoEliminado_grupoActivo_retornaFalse() {
        GrupoEntidad activo = entidadExistente(1, "ins-actual");
        activo.setEliminado(0);
        when(repoGrupo.findById(any(GrupoId.class))).thenReturn(java.util.Optional.of(activo));

        assertFalse(grupoGateway.existeGrupoEliminado(CATEGORIA, CURSO, 2026, 1));
    }

    @Test
    void existeGrupoEliminado_grupoYaEliminado_retornaTrue() {
        GrupoEntidad eliminado = entidadExistente(1, "ins-actual");
        eliminado.setEliminado(1);
        when(repoGrupo.findById(any(GrupoId.class))).thenReturn(java.util.Optional.of(eliminado));

        assertTrue(grupoGateway.existeGrupoEliminado(CATEGORIA, CURSO, 2026, 1));
    }

    @Test
    void existeGrupoEliminado_grupoNoExiste_retornaFalse() {
        when(repoGrupo.findById(any(GrupoId.class))).thenReturn(java.util.Optional.empty());

        assertFalse(grupoGateway.existeGrupoEliminado(CATEGORIA, CURSO, 2026, 1));
    }
}
