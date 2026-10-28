package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClaseDto {

    private Integer codigo;

    @NotBlank(message = "{clase.categoria.blank}")
    @JsonAlias("idGrupoCategoria")
    @JsonProperty("idGrupoCategoria")
    private String categoria;

    @NotBlank(message = "{clase.curso.blank}")
    @JsonAlias("idGrupoCurso")
    @JsonProperty("idGrupoCurso")
    private String curso;

    @NotNull(message = "{clase.anio.null}")
    @JsonAlias("idGrupoAnio")
    @JsonProperty("idGrupoAnio")
    private Integer anio;

    @NotNull(message = "{clase.iterable.null}")
    @JsonAlias("idGrupoIterable")
    @JsonProperty("idGrupoIterable")
    private Integer iterable;

    @NotBlank(message = "{clase.instructor.blank}")
    private String idInstructor;

    @NotNull(message = "{clase.fecha.null}")
    private LocalDate fecha;

    @NotNull(message = "{clase.horas.null}")
    private Integer horas;

    @NotNull(message = "{clase.minutos.null}")
    private Integer minutos;

    private String observacion;

    private Integer eliminado;

    // SCRUM-80 (rediseno, bloque 2): opcional en la peticion -- solo es
    // obligatoria cuando el grupo no tiene franja de horario ese dia (lo
    // valida ClaseServicio, no una anotacion aqui, porque la obligatoriedad
    // depende de datos que el DTO no conoce). La respuesta siempre la incluye.
    private String justificacion;

// Solo de SALIDA: cuando se confirmo la asistencia de la clase (null = pendiente o historica). Nunca se lee del cliente
// (ClaseMapper.fromDto la ignora).
private LocalDateTime asistenciaConfirmadaEn;
}
