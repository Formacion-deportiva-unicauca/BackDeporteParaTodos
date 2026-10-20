package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "tbl_novedad_clase")
public class NovedadClaseEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOV_CODIGO")
    private Long codigo;

    @Column(name = "CAT_TITULO")
    private String categoria;

    @Column(name = "CUR_NOMBRE")
    private String curso;

    @Column(name = "GRP_ANIO")
    private Integer anio;

    @Column(name = "GRP_ITERABLE")
    private Integer iterable;

    @Column(name = "NOV_FECHA")
    private LocalDate fecha;

    @Column(name = "NOV_MOTIVO")
    private String motivo;

    @Column(name = "NOV_DESCRIPCION")
    private String descripcion;

    @Column(name = "NOV_USUARIO_CREADOR")
    private String idUsuarioCreador;

    @Column(name = "NOV_FECHA_CREACION")
    private LocalDateTime fechaCreacion;

    @Column(name = "NOV_USUARIO_MODIFICADOR")
    private String idUsuarioModificador;

    @Column(name = "NOV_FECHA_MODIFICACION")
    private LocalDateTime fechaModificacion;

    @Column(name = "NOV_VERSION")
    private Integer version;

    @Column(name = "META_ELIMINADO")
    private Integer eliminado;
}
