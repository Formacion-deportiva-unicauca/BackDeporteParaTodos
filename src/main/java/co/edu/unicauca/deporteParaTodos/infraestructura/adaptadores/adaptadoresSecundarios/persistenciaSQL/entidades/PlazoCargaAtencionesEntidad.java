package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades;

import java.time.LocalDate;
import java.time.LocalDateTime;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.PlazoCargaAtencionesId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "tbl_plazo_carga_atenciones")
@IdClass(value = PlazoCargaAtencionesId.class)
public class PlazoCargaAtencionesEntidad {

    @Id
    @Column(name = "PERF_ID")
    private String idInstructor;

    @Id
    @Column(name = "PLZ_ANIO")
    private Integer anio;

    @Id
    @Column(name = "PLZ_PERIODO")
    private Integer periodo;

    @Column(name = "PLZ_FECHA_LIMITE")
    private LocalDate fechaLimite;

    @Column(name = "PLZ_COORDINADOR")
    private String idCoordinador;

    @Column(name = "PLZ_FECHA_ASIGNACION")
    private LocalDateTime fechaAsignacion;

    @Column(name = "META_ELIMINADO")
    private Integer eliminado;
}
