package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.AlumnoEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.InstructorEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.PerfilEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IAlumnoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IInstructorRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IPerfilRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorGatewayTest {

    @Mock
    private IInstructorRepositorio repoInstructor;

    @Mock
    private IPerfilRepositorio repoPerfil;

    @Mock
    private IAlumnoRepositorio repoAlumno;

    @InjectMocks
    private InstructorGateway instructorGateway;

    private static final String PERF_ID  = "12345678";
    private static final String NOMBRE   = "Prof. García";
    private static final String CORREO   = "garcia@unicauca.edu.co";
    private static final String SEXO     = "M";
    private static final String TIPO_ALM = "Instructor";

    private PerfilEntidad perfilEntidadBase() {
        PerfilEntidad e = new PerfilEntidad();
        e.setPerf_id(PERF_ID);
        e.setPerf_nombre(NOMBRE);
        e.setPerfcorreo(CORREO);
        e.setPerf_tipo("CC");
        e.setPerf_Sexo(SEXO);
        e.setEliminado(0);
        return e;
    }

    private InstructorEntidad instructorEntidadBase() {
        InstructorEntidad e = new InstructorEntidad();
        e.setIdPerfil(PERF_ID);
        e.setEliminado(0);
        e.setPerfil(perfilEntidadBase());
        return e;
    }

    private Perfil perfilBase() {
        Perfil p = new Perfil();
        p.setId(PERF_ID);
        p.setNombre(NOMBRE);
        p.setCorreo(CORREO);
        p.setTipoId("CC");
        p.setSexo(SEXO);
        return p;
    }

    // Riesgo real detectado al activar DELETE /instructor: la lista publica usaba
    // findAll() (sin filtro), por lo que un instructor eliminado seguiria apareciendo
    // en GET /instructores. Ahora delega en findByEliminado(0).
    @Test
    void obtenerInstructores_delegaEnFindByEliminado0_excluyeEliminados() {
        when(repoInstructor.findByEliminado(0)).thenReturn(List.of(instructorEntidadBase()));

        List<Instructor> resultado = instructorGateway.obtenerInstructores();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertNotNull(resultado.get(0).getPerfil());
        assertEquals(PERF_ID, resultado.get(0).getPerfil().getId());
        assertEquals(CORREO,  resultado.get(0).getPerfil().getCorreo());
        verify(repoInstructor).findByEliminado(0);
        verify(repoInstructor, never()).findAll();
    }

    @Test
    void obtenerInstructor_existente_retornaOptionalConInstructor() {
        when(repoInstructor.existsById(PERF_ID)).thenReturn(true);
        when(repoInstructor.findById(PERF_ID)).thenReturn(Optional.of(instructorEntidadBase()));

        Optional<Instructor> resultado = instructorGateway.obtenerInstructor(PERF_ID);

        assertTrue(resultado.isPresent());
        assertEquals(PERF_ID, resultado.get().getPerfil().getId());
    }

    @Test
    void obtenerInstructor_noExistente_retornaEmpty() {
        when(repoInstructor.existsById(anyString())).thenReturn(false);

        Optional<Instructor> resultado = instructorGateway.obtenerInstructor("desconocido");

        assertTrue(resultado.isEmpty());
        verify(repoInstructor, never()).findById(any());
    }

    // SCRUM-138: eliminarInstructor() hacia DELETE fisico (repoInstructor.delete()),
    // riesgo real de romper FK_GRUPO_INSTRUCTOR (RESTRICT) y de borrar silenciosamente
    // la atribucion historica en tbl_clase (FK_CLASE_INSTRUCTOR ON DELETE SET NULL).
    // Ahora es borrado logico: setEliminado(1) + save(), mismo patron exacto de
    // HorarioGateway.eliminarHorario() -- nunca debe llamar a delete().
    @Test
    void eliminarInstructor_existente_marcaEliminadoYNuncaBorraFisicamente() {
        InstructorEntidad entidad = instructorEntidadBase();
        InstructorEntidad guardado = instructorEntidadBase();
        guardado.setEliminado(1);
        when(repoInstructor.findById(PERF_ID)).thenReturn(Optional.of(entidad));
        when(repoInstructor.save(entidad)).thenReturn(guardado);

        Instructor resultado = instructorGateway.eliminarInstructor(PERF_ID);

        assertNotNull(resultado);
        assertEquals(1, entidad.getEliminado(), "La entidad pasada a save() debe quedar con eliminado=1");
        // El mapeo manual (mapearEntidadADominio -> PerfilMapper) debe poblar el
        // perfil completo -- a diferencia de mapper.map() generico, que en
        // InstructorIT se confirmo que dejaba nombre/correo en null.
        assertNotNull(resultado.getPerfil(), "El perfil debe venir poblado, no null");
        assertEquals(PERF_ID, resultado.getPerfil().getId());
        assertEquals(NOMBRE,  resultado.getPerfil().getNombre());
        assertEquals(CORREO,  resultado.getPerfil().getCorreo());
        verify(repoInstructor).save(entidad);
        verify(repoInstructor, never()).delete(any());
    }

    @Test
    void eliminarInstructor_noExistente_lanzaNoExisteExcepcion() {
        when(repoInstructor.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(NoExisteExcepcion.class,
                () -> instructorGateway.eliminarInstructor("desconocido"));

        verify(repoInstructor, never()).delete(any());
    }

    @Test
    void registrarInstructor_guardaPerfilAlumnoEInstructor() {
        PerfilEntidad perfilGuardado = perfilEntidadBase();
        InstructorEntidad instructorGuardado = new InstructorEntidad();
        instructorGuardado.setIdPerfil(PERF_ID);

        when(repoPerfil.save(any(PerfilEntidad.class))).thenReturn(perfilGuardado);
        when(repoAlumno.save(any(AlumnoEntidad.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repoInstructor.save(any(InstructorEntidad.class))).thenReturn(instructorGuardado);

        Instructor resultado = instructorGateway.registrarInstructor(perfilBase(), TIPO_ALM);

        assertNotNull(resultado);
        assertEquals(PERF_ID, resultado.getInst_codigo());
        assertNotNull(resultado.getPerfil());
        assertEquals(CORREO, resultado.getPerfil().getCorreo());
        assertEquals(TIPO_ALM, resultado.getPerfil().getTipoAlumno());
        verify(repoPerfil).save(any(PerfilEntidad.class));
        verify(repoAlumno).save(any(AlumnoEntidad.class));
        verify(repoInstructor).save(any(InstructorEntidad.class));
    }
}
