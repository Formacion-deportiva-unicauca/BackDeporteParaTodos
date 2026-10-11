package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IGrupoServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.GrupoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GrupoRestTest {

    @Mock
    private IGrupoServicio servicio;

    @Mock
    private Clock clock;

    @InjectMocks
    private GrupoRest grupoRest;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // SCRUM-185: GrupoRest ahora necesita un Clock para calcular inscripcionesAbiertas/
        // motivoInscripcion en cada respuesta -- lenient() porque no es el foco de estos tests.
        lenient().when(clock.instant())
                .thenReturn(LocalDate.of(2026, 6, 15).atStartOfDay(ZoneId.of("America/Bogota")).toInstant());
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("America/Bogota"));

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(grupoRest)
                .setMessageConverters(converter)
                .build();
    }

    private Grupo grupoBase() {
        Grupo g = new Grupo();
        g.setCategoria("Recreativo");
        g.setCurso("Natacion");
        g.setAnio(2025);
        g.setIterable(1);
        g.setImagenGrupo(1);
        g.setCupos(20);
        g.setIdInstructor("INS001");
        g.setFechaCreacion(LocalDate.of(2025, 1, 15));
        g.setPeriodo(1);
        return g;
    }

    @Test
    void obtenerGrupos_retornaListaDto() throws Exception {
        when(servicio.obtenerTodosGrupos()).thenReturn(List.of(grupoBase()));

        mockMvc.perform(get("/api/v2/grupos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria").value("Recreativo"))
                .andExpect(jsonPath("$[0].curso").value("Natacion"));
    }

    @Test
    void obtenerGruposNoEliminados_retornaListaDto() throws Exception {
        when(servicio.obtenerGruposDisponibles()).thenReturn(List.of(grupoBase()));

        mockMvc.perform(get("/api/v2/gruposNoEliminados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria").value("Recreativo"));
    }

    @Test
    void obtenerGruposCurso_retornaListaDto() throws Exception {
        when(servicio.obtenerGruposDeCurso(anyString(), anyString())).thenReturn(List.of(grupoBase()));

        mockMvc.perform(get("/api/v2/gruposCurso")
                        .param("prmCategoria", "Recreativo")
                        .param("prmCurso", "Natacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].curso").value("Natacion"));
    }

    @Test
    void obtenerGruposInscripcion_retornaListaDto() throws Exception {
        when(servicio.obtenerGruposInscripcionDisponible()).thenReturn(List.of(grupoBase()));

        mockMvc.perform(get("/api/v2/gruposInscripcion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria").value("Recreativo"));
    }

    @Test
    void obtenerGruposInstructor_retornaListaDto() throws Exception {
        when(servicio.obtenerGruposInstructor(anyString())).thenReturn(List.of(grupoBase()));

        mockMvc.perform(get("/api/v2/gruposInstructor")
                        .param("idInstructor", "INS001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idInstructor").value("INS001"));
    }

    @Test
    void postGrupo_retornaCreated() throws Exception {
        when(servicio.insertarGrupo(any(Grupo.class))).thenReturn(grupoBase());

        GrupoDto dto = new GrupoDto();
        dto.setCategoria("Recreativo");
        dto.setCurso("Natacion");
        dto.setImagenGrupo(1);
        dto.setCupos(20);
        dto.setFechaCreacion(LocalDate.of(2025, 1, 15));

        mockMvc.perform(post("/api/v2/grupo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").value("Recreativo"))
                .andExpect(jsonPath("$.curso").value("Natacion"));
    }

    @Test
    void obtenerGrupo_retornaDto() throws Exception {
        when(servicio.obtenerGrupo(anyString(), anyString(), anyInt(), anyInt())).thenReturn(grupoBase());

        mockMvc.perform(get("/api/v2/grupo")
                        .param("categoria", "Recreativo")
                        .param("curso", "Natacion")
                        .param("anio", "2025")
                        .param("iterable", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("Recreativo"))
                .andExpect(jsonPath("$.curso").value("Natacion"));
    }

    // SCRUM-185: inscripcionesAbiertas/motivoInscripcion son @JsonProperty(access =
    // READ_ONLY) en GrupoDto -- un PUT /grupo que los traiga en el body debe
    // responder 200 normalmente y Jackson debe ignorarlos al deserializar, sin que
    // lleguen nunca a actualizarGrupo() (Grupo, el modelo de dominio, ni los tiene).
    @Test
    void putGrupo_bodyConCamposDeSoloLecturaFalsos_respondeOkYLosIgnora() throws Exception {
        when(servicio.actualizarGrupo(anyString(), anyString(), anyInt(), anyInt(), any(Grupo.class)))
                .thenReturn(grupoBase());

        String body = "{\"imagenGrupo\":1,\"cupos\":20,\"fechaCreacion\":\"2025-01-15\","
                + "\"inscripcionesAbiertas\":true,\"motivoInscripcion\":\"CERRADAS\"}";

        mockMvc.perform(put("/api/v2/grupo")
                        .param("categoria", "Recreativo")
                        .param("curso", "Natacion")
                        .param("anio", "2025")
                        .param("iterable", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("Recreativo"));

        // Confirma el efecto real de @JsonProperty(access = READ_ONLY): decodificando
        // el MISMO json de entrada con el ObjectMapper de la app, los campos de solo
        // lectura nunca se poblaron, sin importar el valor "falso" que trajera el body.
        GrupoDto dtoDeserializado = objectMapper.readValue(body, GrupoDto.class);
        assertFalse(dtoDeserializado.isInscripcionesAbiertas());
        assertNull(dtoDeserializado.getMotivoInscripcion());
    }
}
