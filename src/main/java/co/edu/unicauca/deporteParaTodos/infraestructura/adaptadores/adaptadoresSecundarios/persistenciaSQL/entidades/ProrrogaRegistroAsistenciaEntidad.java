package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades;

import java.time.LocalDate;
import java.time.LocalDateTime;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.ProrrogaRegistroAsistenciaId;
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
@Table(name = "tbl_prorroga_registro_asistencia")
@IdClass(value = ProrrogaRegistroAsistenciaId.class)
public class ProrrogaRegistroAsistenciaEntidad {

    @Id
    @Column(name = "PERF_ID")
    private String idInstructor;

    @Id
    @Column(name = "PRR_ANIO")
    private Integer anio;

    @Id
    @Column(name = "PRR_MES")
    private Integer mes;

    @Column(name = "PRR_DIAS")
    private Integer dias;

    @Column(name = "PRR_FECHA_LIMITE")
    private LocalDate fechaLimite;

    @Column(name = "PRR_COORDINADOR")
    private String idCoordinador;

    @Column(name = "PRR_FECHA_ASIGNACION")
    private LocalDateTime fechaAsignacion;

    @Column(name = "META_ELIMINADO")
    private Integer eliminado;
}
