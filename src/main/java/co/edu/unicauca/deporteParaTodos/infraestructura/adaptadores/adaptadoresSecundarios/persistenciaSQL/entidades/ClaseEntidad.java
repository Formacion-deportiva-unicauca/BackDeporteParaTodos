package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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
@Table(name = "tbl_clase")
public class ClaseEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CLS_CODIGO")
    private Integer codigo;

    @Column(name ="cat_titulo")
    private String idGrupoCategoria;

    @Column(name="cur_nombre")
    private String idGrupoCurso;

    @Column(name = "grp_anio")
    private Integer idGrupoAnio;

    @Column(name = "grp_iterable")
    private Integer idGrupoIterable;

    @Column(name="perf_id")
    private String idInstructor;

    @Column(name = "CLS_FECHA")
    private LocalDate fecha;

    @Column(name="cls_duracion_horas")
    private Integer horas;

    @Column(name="cls_duracion_minutos")
    private Integer minutos;

    @Column(name = "CLS_OBSERVACION")
    private String observacion;

    @Column(name="META_ELIMINADO")
    private Integer eliminado;

}
