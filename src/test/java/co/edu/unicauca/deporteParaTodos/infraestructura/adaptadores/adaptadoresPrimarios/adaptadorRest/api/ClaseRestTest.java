package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IClaseServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ClaseDto;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClaseRestTest {

    @Mock
    private IClaseServicio servicioClase;

    @InjectMocks
    private ClaseRest claseRest;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(claseRest)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    private Clase claseBase() {
        Clase c = new Clase();
        c.setCodigo(1);
        c.setCategoria("Recreativo");
        c.setCurso("Natacion");
        c.setAnio(2025);
        c.setIterable(1);
        c.setIdInstructor("INS001");
        c.setFecha(LocalDate.of(2025, 1, 15));
        c.setHoras(1);
        c.setMinutos(30);
        c.setEliminado(0);
        return c;
    }

    @Test
    void getClasesGrupo_retornaListaDto() throws Exception {
        when(servicioClase.obtenerClasesGrupo(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(claseBase()));

        // ClaseDto serializa categoria como "idGrupoCategoria" por @JsonProperty
        mockMvc.perform(get("/api/v2/clasesGrupo")
                        .param("categoria", "Recreativo")
                        .param("curso", "Natacion")
                        .param("anio", "2025")
                        .param("iterable", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idGrupoCategoria").value("Recreativo"))
                .andExpect(jsonPath("$[0].idGrupoCurso").value("Natacion"))
                .andExpect(jsonPath("$[0].codigo").value(1));
    }

    private ClaseDto claseDtoBase() {
        ClaseDto dto = new ClaseDto();
        dto.setCategoria("Recreativo");
        dto.setCurso("Natacion");
        dto.setAnio(2025);
        dto.setIterable(1);
        dto.setIdInstructor("INS001");
        dto.setFecha(LocalDate.of(2025, 1, 15));
        dto.setHoras(1);
        dto.setMinutos(30);
        return dto;
    }

    @Test
    void postClase_noExistiaClaseHoy_retornaCreated() throws Exception {
        when(servicioClase.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        when(servicioClase.insertarClase(any(Clase.class))).thenReturn(claseBase());

        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claseDtoBase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idGrupoCategoria").value("Recreativo"))
                .andExpect(jsonPath("$.codigo").value(1));
    }

    // El Instructor registra alumno por alumno; el segundo click del mismo dia no debe
    // crear una segunda clase -- insertarClase() reutiliza la existente y el controller
    // debe reflejarlo con 200, no 201 (201 significaria "se creo un recurso nuevo").
    @Test
    void postClase_yaExistiaClaseHoy_retornaOkConCodigoDeLaExistente() throws Exception {
        Clase existente = claseBase();
        existente.setCodigo(99);
        when(servicioClase.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(true);
        when(servicioClase.insertarClase(any(Clase.class))).thenReturn(existente);

        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claseDtoBase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(99));
    }

    @Test
    void deleteClase_retornaOk() throws Exception {
        when(servicioClase.eliminarClase(anyInt())).thenReturn(claseBase());

        mockMvc.perform(delete("/api/v2/clase")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.eliminado").value(0));
    }
}
