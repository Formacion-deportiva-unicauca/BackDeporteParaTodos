package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Novedad de una clase no realizada. Entidad independiente de tbl_clase y
 * tbl_asistencia. Registra que una clase programada no se imparti� y el motivo.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NovedadClase {

    private Long codigo;

    private String categoria;

    private String curso;

    private Integer anio;

    private Integer iterable;

    private LocalDate fecha;

    private String motivo;

    private String descripcion;

    private String idUsuarioCreador;

    private LocalDateTime fechaCreacion;

    private String idUsuarioModificador;

    private LocalDateTime fechaModificacion;

    private Integer version;

    private Integer eliminado;
}
