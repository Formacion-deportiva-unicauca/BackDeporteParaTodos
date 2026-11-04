package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "tbl_alumno")
public class AlumnoEntidad {
    @Id
    @Column(name = "perf_id")
    private String idPerfil;

    @Column(name = "ALM_CODIGO", unique = true, length = 20, nullable = true)
    private String alm_codigo;

    @Column(name = "ALM_TIPO")
    private String tipoAlumno;

    @Column(name = "ALM_DEPENDENCIA", length = 150, nullable = true)
    private String dependencia;

    @Column(name= "META_ELIMINADO")
    private Integer eliminado;

}
