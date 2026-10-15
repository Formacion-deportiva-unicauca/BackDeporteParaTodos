package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProrrogaRegistroAsistenciaId implements Serializable {

    @Column(name = "PERF_ID")
    private String idInstructor;

    @Column(name = "PRR_ANIO")
    private Integer anio;

    @Column(name = "PRR_MES")
    private Integer mes;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProrrogaRegistroAsistenciaId otro = (ProrrogaRegistroAsistenciaId) o;
        return Objects.equals(idInstructor, otro.idInstructor)
                && Objects.equals(anio, otro.anio)
                && Objects.equals(mes, otro.mes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idInstructor, anio, mes);
    }
}
