package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInstructorGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InstructorDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorServicioTest {

    @Mock
    private IInstructorGateway instructorsGateway;

    @InjectMocks
    private InstructorServicio instructorServicio;

    private static final String ID      = "98765432";
    private static final String NOMBRE  = "Prof. García";
    private static final String CORREO  = "garcia@unicauca.edu.co";
    private static final String TIPO_ID = "CC";
    private static final String SEXO    = "M";

    private Instructor instructorConPerfil() {
        Perfil perfil = new Perfil();
        perfil.setId(ID);
        perfil.setNombre(NOMBRE);
        perfil.setCorreo(CORREO);
        perfil.setSexo(SEXO);
        Instructor inst = new Instructor();
        inst.setInst_codigo(ID);
        inst.setPerfil(perfil);
        return inst;
    }

    private PerfilDto dtoBase() {
        PerfilDto dto = new PerfilDto();
        dto.setId(ID);
        dto.setNombre(NOMBRE);
        dto.setCorreo(CORREO);
        dto.setTipoId(TIPO_ID);
        dto.setSexo(SEXO);
        dto.setTipoAlumno("Instructor");
        return dto;
    }

    // ── obtenerInstructores ─────────────────────────────────────────────────

    @Test
    void obtenerInstructores_conDatos_retornaListaDtos() {
        when(instructorsGateway.obtenerInstructores()).thenReturn(List.of(instructorConPerfil()));

        List<InstructorDto> resultado = instructorServicio.obtenerInstructores();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(ID,     resultado.get(0).getId());
        assertEquals(CORREO, resultado.get(0).getCorreo());
    }

    @Test
    void obtenerInstructores_sinDatos_lanzaListadoVacioExcepcion() {
        when(instructorsGateway.obtenerInstructores()).thenReturn(List.of());

        assertThrows(ListadoVacioExcepcion.class,
                () -> instructorServicio.obtenerInstructores());
    }

    // ── obtenerInstructor ───────────────────────────────────────────────────

    @Test
    void obtenerInstructor_existente_retornaInstructorDto() {
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(instructorConPerfil()));

        InstructorDto resultado = instructorServicio.obtenerInstructor(ID);

        assertNotNull(resultado);
        assertEquals(ID,     resultado.getId());
        assertEquals(NOMBRE, resultado.getNombre());
    }

    @Test
    void obtenerInstructor_noExistente_lanzaNoExisteExcepcion() {
        when(instructorsGateway.obtenerInstructor(anyString())).thenReturn(Optional.empty());

        assertThrows(NoExisteExcepcion.class,
                () -> instructorServicio.obtenerInstructor("desconocido"));
    }

    // ── registrarInstructor ─────────────────────────────────────────────────

    @Test
    void registrarInstructor_sinAlumnoCodigo_seRegistraCorrectamente() {
        PerfilDto dto = new PerfilDto();
        dto.setId(ID);
        dto.setNombre(NOMBRE);
        dto.setCorreo(CORREO);
        dto.setTipoId(TIPO_ID);
        dto.setSexo(SEXO);
        dto.setTipoAlumno("Instructor");
        // alumnoCodigo NOT set — remains null (regression: SCRUM-137)

        when(instructorsGateway.existeInstructor(dto.getId())).thenReturn(false);
        when(instructorsGateway.registrarInstructor(any(Perfil.class), eq("Instructor")))
                .thenReturn(new Instructor());

        InstructorDto resultado = instructorServicio.registrarInstructor(dto);

        assertNotNull(resultado);
        verify(instructorsGateway).registrarInstructor(any(Perfil.class), eq("Instructor"));
    }

    @Test
    void registrarInstructor_yaExistente_lanzaYaExisteElementoExcepcion() {
        when(instructorsGateway.existeInstructor(ID)).thenReturn(true);

        assertThrows(YaExisteElementoExcepcion.class,
                () -> instructorServicio.registrarInstructor(dtoBase()));

        verify(instructorsGateway, never()).registrarInstructor(any(), any());
    }

    // ── eliminarInstructor ──────────────────────────────────────────────────
    // SCRUM-138: eliminarInstructor() hacia DELETE fisico (riesgo real de romper
    // FK_GRUPO_INSTRUCTOR -- RESTRICT -- y de borrar silenciosamente la atribucion
    // historica en tbl_clase via FK_CLASE_INSTRUCTOR ON DELETE SET NULL). Mismo
    // patron exacto de HorarioServicio.eliminarHorario(): existeInstructor() (404),
    // luego obtenerInstructor() para leer el estado actual y guardar contra 409.

    @Test
    void eliminarInstructor_noExiste_lanzaNoExisteExcepcion() {
        when(instructorsGateway.existeInstructor(ID)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class, () -> instructorServicio.eliminarInstructor(ID));

        verify(instructorsGateway, never()).eliminarInstructor(any());
    }

    @Test
    void eliminarInstructor_exitoso_delegaAlGateway() {
        Instructor activo = instructorConPerfil();
        activo.setEliminado(0);
        Instructor eliminado = instructorConPerfil();
        eliminado.setEliminado(1);
        when(instructorsGateway.existeInstructor(ID)).thenReturn(true);
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(activo));
        when(instructorsGateway.eliminarInstructor(ID)).thenReturn(eliminado);

        Instructor resultado = instructorServicio.eliminarInstructor(ID);

        assertNotNull(resultado);
        verify(instructorsGateway).eliminarInstructor(ID);
    }

    // Llamar eliminarInstructor() dos veces seguidas: la primera exitosa, la segunda
    // debe dar 409 en vez de reintentar el borrado silenciosamente.
    @Test
    void eliminarInstructor_yaEliminado_lanzaYaExisteElementoExcepcion() {
        Instructor yaEliminado = instructorConPerfil();
        yaEliminado.setEliminado(1);
        when(instructorsGateway.existeInstructor(ID)).thenReturn(true);
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(yaEliminado));

        YaExisteElementoExcepcion ex = assertThrows(YaExisteElementoExcepcion.class,
                () -> instructorServicio.eliminarInstructor(ID));

        assertEquals("El instructor ya se encuentra eliminado", ex.getMessage());
        verify(instructorsGateway, never()).eliminarInstructor(any());
    }
}
