package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICursoServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PublicoCurso;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.time.Instant;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.CursoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CursoRestTest {

    @Mock
    private ICursoServicio servicio;

    @InjectMocks
    private CursoRest cursoRest;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cursoRest)
            .setMessageConverters(new MappingJackson2HttpMessageConverter())
            .build();
    }

    private Curso cursoBase() {
        Curso c = new Curso();
        c.setNombre("Natacion");
        c.setCategoriaCurso("Acuatico");
        c.setDescripcion("Descripcion");
        c.setDeporte("Natacion");
        c.setImagenId(1);
        c.setEstadoCurso(EstadoCurso.ACTIVO);
        c.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
        return c;
    }

    @Test
    void obtenerCursos_retornaListaDto() throws Exception {
        when(servicio.recuperarCursos()).thenReturn(List.of(cursoBase()));

        mockMvc.perform(get("/api/v2/cursos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("Natacion"))
            .andExpect(jsonPath("$[0].categoriaCurso").value("Acuatico"));
    }

    @Test
    void obtenerCursosPorCategoria_retornaListaDto() throws Exception {
        when(servicio.cursosDeCategoria("Acuatico")).thenReturn(List.of(cursoBase()));

        mockMvc.perform(get("/api/v2/cursosbycategoria").param("prmCategoria", "Acuatico"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].categoriaCurso").value("Acuatico"));
    }

    @Test
    void obtenerCurso_retornaCursoDto() throws Exception {
        when(servicio.obtenerCurso("Acuatico", "Natacion")).thenReturn(cursoBase());

        mockMvc.perform(get("/api/v2/curso")
                .param("prmCategoria", "Acuatico")
                .param("prmCurso", "Natacion"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Natacion"));
    }

    @Test
    void postAgregarCurso_retornaCreated() throws Exception {
        when(servicio.insertarCurso(any())).thenReturn(cursoBase());

        CursoDto dto = new CursoDto();
        dto.setNombre("Natacion");
        dto.setDeporte("Natacion");
        dto.setCategoriaCurso("Acuatico");
        dto.setDescripcion("Descripcion");
        dto.setIdImagen(1);

        mockMvc.perform(post("/api/v2/curso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombre").value("Natacion"));
    }

    @Test
    void cambiarEstadoCurso_retornaOk() throws Exception {
        Curso inactivo = cursoBase();
        inactivo.setEstadoCurso(EstadoCurso.INACTIVO);
        when(servicio.cambiarEstadoCurso("Acuatico", "Natacion", EstadoCurso.INACTIVO))
            .thenReturn(inactivo);

        mockMvc.perform(patch("/api/v2/curso/estado")
                .param("prmCategoria", "Acuatico")
                .param("prmCurso", "Natacion")
                .param("prmEstado", "INACTIVO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estadoCurso").value("INACTIVO"));
    }

    // ── El Alumno solo ve los cursos que le corresponden; los demas roles ven todo ─────────────────

    private JwtAuthenticationToken token(String rol, String perfId) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "HS256")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).claim("rol", rol);
        if (perfId != null) b.claim("perf_id", perfId);
        return new JwtAuthenticationToken(b.build(), List.of(new SimpleGrantedAuthority(rol)));
    }

    @Test
    void obtenerCursos_conAlumno_usaElListadoParaAlumno() throws Exception {
        when(servicio.recuperarCursosParaAlumno("alum1")).thenReturn(List.of(cursoBase()));

        mockMvc.perform(get("/api/v2/cursos").principal(token("Alumno", "alum1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("Natacion"));

        org.mockito.Mockito.verify(servicio, org.mockito.Mockito.never()).recuperarCursos();
    }

    @Test
    void obtenerCursosPorCategoria_conAlumno_usaElListadoParaAlumno() throws Exception {
        when(servicio.cursosDeCategoriaParaAlumno("Acuatico", "alum1")).thenReturn(List.of(cursoBase()));

        mockMvc.perform(get("/api/v2/cursosbycategoria").param("prmCategoria", "Acuatico")
                        .principal(token("Alumno", "alum1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("Natacion"));

        org.mockito.Mockito.verify(servicio, org.mockito.Mockito.never()).cursosDeCategoria(any());
    }

    @Test
    void obtenerCursosPorCategoria_conCoordinadorInstructorOAdministrador_veTodo() throws Exception {
        when(servicio.cursosDeCategoria("Acuatico")).thenReturn(List.of(cursoBase()));

        for (String rol : List.of("Coordinador", "Instructor", "Administrador")) {
            mockMvc.perform(get("/api/v2/cursosbycategoria").param("prmCategoria", "Acuatico")
                            .principal(token(rol, "u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Natacion"));
        }
        org.mockito.Mockito.verify(servicio, org.mockito.Mockito.never()).cursosDeCategoriaParaAlumno(any(), any());
    }

    @Test
    void obtenerCursos_conAlumnoSinPerfId_loTrataComoAlumnoSinTipo() throws Exception {
        when(servicio.recuperarCursosParaAlumno(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/cursos").principal(token("Alumno", null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void obtenerCursos_incluyeElPublicoEnElDto() throws Exception {
        Curso curso = cursoBase();
        curso.setPublico(PublicoCurso.FUNCIONARIO);
        when(servicio.recuperarCursos()).thenReturn(List.of(curso));

        mockMvc.perform(get("/api/v2/cursos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].publico").value("FUNCIONARIO"));
    }
}
