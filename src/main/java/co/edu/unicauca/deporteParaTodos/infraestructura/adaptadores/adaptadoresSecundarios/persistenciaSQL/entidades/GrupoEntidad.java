package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades;

import java.time.LocalDate;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.GrupoId;
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
@Table(name = "tbl_grupo")
@IdClass(value = GrupoId.class)
public class GrupoEntidad {
    /*@EmbeddedId
    private GrupoId id;*/
////CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE
    @Id
    @Column(name = "CAT_TITULO")
    private String categoria;

    @Id
    @Column(name = "CUR_NOMBRE")
    private String curso;

    @Id
    @Column(name = "GRP_ANIO")
    private int anio;

    @Id
    @Column(name = "GRP_ITERABLE")
    private int iterable;

    @Column(name = "GRP_IMAGEN")
    private Integer imagenGrupo;

    @Column(name = "GRP_CUPOS")
    private Integer cupos;

    @Column(name="PERF_ID")
    private String idInstructor;

    //@Column(name = "GRP_ESTADO")
    //private String estado;

    @Column(name = "GRP_FECHACREACION")
    private LocalDate fechaCreacion;

    @Column(name = "GRP_FECHA_FINALIZACION")
    private LocalDate fechaFinalizacion;

    @Column(name = "GRP_FECHA_INSCRIP_APERTURA")
    private LocalDate fechaInscripcionApertura;

    @Column(name = "GRP_FECHA_INSCRIP_CIERRE")
    private LocalDate fechaIncripcionCierre;

    @Column(name = "GRP_PERIODO")
    private int periodo;

    @Column(name = "META_ELIMINADO")
    private Integer eliminado;

    @Column(name = "GRP_INSCRIP_CERRADA_MANUAL", nullable = false)
    private boolean inscripcionesCerradasManual;
}