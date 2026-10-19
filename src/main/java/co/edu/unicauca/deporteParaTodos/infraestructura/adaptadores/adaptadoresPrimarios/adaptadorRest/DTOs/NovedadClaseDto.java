package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
public class NovedadClaseDto {

    private Long codigo;

    @NotBlank(message = "{novedad.categoria.blank}")
    private String categoria;

    @NotBlank(message = "{novedad.curso.blank}")
    private String curso;

    @NotNull(message = "{novedad.anio.null}")
    private Integer anio;

    @NotNull(message = "{novedad.iterable.null}")
    private Integer iterable;

    @NotNull(message = "{novedad.fecha.null}")
    private LocalDate fecha;

    @NotBlank(message = "{novedad.motivo.blank}")
    private String motivo;

    private String descripcion;

    private String estado;

    private String idUsuarioCreador;

    private LocalDateTime fechaCreacion;

    private String idUsuarioModificador;

    private LocalDateTime fechaModificacion;

    private Integer version;
}
