package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.AsistenciaId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "tbl_asistencia")
@IdClass(value = AsistenciaId.class)
public class AsistenciaEntidad {
    @Id
    @Column(name = "PERF_ID")
    private String perfilId;

    @Id
    @Column(name = "CLS_CODIGO")
    private Integer claseCodigo;

    @Column(name = "META_ELIMINADO")
    private Integer eliminado;

    @Column(name = "ASI_ESTADO")
    private String estado;
}
