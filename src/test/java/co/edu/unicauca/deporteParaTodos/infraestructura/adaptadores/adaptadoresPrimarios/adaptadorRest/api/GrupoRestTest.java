package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IGrupoServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ClaveGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoVentanaInscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.NivelOcupacion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.OcupacionGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PublicoCurso;
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
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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

    // ── Fase 3: cierre manual de inscripciones ──────────────────────────────

    @Test
    void patchCierreManual_cerrar_devuelveElGrupoCerrado() throws Exception {
        Grupo cerrado = grupoBase();
        cerrado.setFechaIncripcionCierre(LocalDate.of(2026, 12, 31));
        cerrado.setInscripcionesCerradasManual(true);
        when(servicio.cambiarCierreManual("Recreativo", "Natacion", 2025, 1, true)).thenReturn(cerrado);

        mockMvc.perform(patch("/api/v2/grupo/inscripciones")
                        .param("categoria", "Recreativo")
                        .param("curso", "Natacion")
                        .param("anio", "2025")
                        .param("iterable", "1")
                        .param("cerradas", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inscripcionesCerradasManual").value(true))
                .andExpect(jsonPath("$.inscripcionesAbiertas").value(false))
                .andExpect(jsonPath("$.motivoInscripcion").value("CERRADAS"));
    }

    @Test
    void patchCierreManual_reabrir_devuelveElGrupoAbierto() throws Exception {
        Grupo abierto = grupoBase();
        abierto.setFechaIncripcionCierre(LocalDate.of(2026, 12, 31));
        when(servicio.cambiarCierreManual("Recreativo", "Natacion", 2025, 1, false)).thenReturn(abierto);

        mockMvc.perform(patch("/api/v2/grupo/inscripciones")
                        .param("categoria", "Recreativo")
                        .param("curso", "Natacion")
                        .param("anio", "2025")
                        .param("iterable", "1")
                        .param("cerradas", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inscripcionesCerradasManual").value(false))
                .andExpect(jsonPath("$.inscripcionesAbiertas").value(true));
    }

    @Test
    void patchCierreManual_sinElParametroCerradas_esPeticionInvalida() throws Exception {
        mockMvc.perform(patch("/api/v2/grupo/inscripciones")
                        .param("categoria", "Recreativo")
                        .param("curso", "Natacion")
                        .param("anio", "2025")
                        .param("iterable", "1"))
                .andExpect(status().isBadRequest());
    }

    // ── Fase 4: tablero de ocupacion y periodo de inscripciones ─────────────

    @Test
    void getOcupacion_sinFiltros_devuelveLaFilaConNivelYPublico() throws Exception {
        OcupacionGrupo o = new OcupacionGrupo("Formativo", "Fulbol", 2026, 1, 1, PublicoCurso.ESTUDIANTE, 20, 17, 2,
                NivelOcupacion.POR_LLENAR, EstadoVentanaInscripcion.ABIERTAS, false,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1));
        when(servicio.obtenerOcupacion(null, null, null)).thenReturn(List.of(o));

        mockMvc.perform(get("/api/v2/grupo/ocupacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].curso").value("Fulbol"))
                .andExpect(jsonPath("$[0].nivel").value("POR_LLENAR"))
                .andExpect(jsonPath("$[0].publico").value("ESTUDIANTE"))
                .andExpect(jsonPath("$[0].inscritos").value(17))
                .andExpect(jsonPath("$[0].enEspera").value(2))
                .andExpect(jsonPath("$[0].estadoInscripcion").value("ABIERTAS"))
                .andExpect(jsonPath("$[0].fechaCierre").value("2026-07-01"));
    }

    @Test
    void getOcupacion_conFiltros_losPasaAlServicio() throws Exception {
        when(servicio.obtenerOcupacion("Formativo", "Fulbol", PublicoCurso.FUNCIONARIO)).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/grupo/ocupacion")
                        .param("categoria", "Formativo").param("curso", "Fulbol").param("publico", "FUNCIONARIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void patchPeriodo_aplicaLasFechasALosGruposSeleccionados() throws Exception {
        Grupo g = grupoBase();
        g.setFechaInscripcionApertura(LocalDate.of(2026, 7, 1));
        g.setFechaIncripcionCierre(LocalDate.of(2026, 8, 1));
        when(servicio.aplicarPeriodo(eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 8, 1)), anyList()))
                .thenReturn(List.of(g));

        String body = "{\"fechaApertura\":\"2026-07-01\",\"fechaCierre\":\"2026-08-01\","
                + "\"grupos\":[{\"categoria\":\"Recreativo\",\"curso\":\"Natacion\",\"anio\":2025,\"iterable\":1}]}";

        mockMvc.perform(patch("/api/v2/grupo/periodo").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].curso").value("Natacion"));

        org.mockito.ArgumentCaptor<List<ClaveGrupo>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(servicio).aplicarPeriodo(eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 8, 1)), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1, captor.getValue().size());
        org.junit.jupiter.api.Assertions.assertEquals("Natacion", captor.getValue().get(0).getCurso());
        org.junit.jupiter.api.Assertions.assertEquals(2025, captor.getValue().get(0).getAnio());
    }

    @Test
    void patchPeriodo_sinGrupos_respondeBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v2/grupo/periodo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaCierre\":\"2026-08-01\"}"))
                .andExpect(status().isBadRequest());
    }
}
