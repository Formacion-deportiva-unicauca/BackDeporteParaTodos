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
public class PlazoCargaAtencionesId implements Serializable {

    @Column(name = "PERF_ID")
    private String idInstructor;

    @Column(name = "PLZ_ANIO")
    private Integer anio;

    @Column(name = "PLZ_PERIODO")
    private Integer periodo;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PlazoCargaAtencionesId otro = (PlazoCargaAtencionesId) o;
        return Objects.equals(idInstructor, otro.idInstructor)
                && Objects.equals(anio, otro.anio)
                && Objects.equals(periodo, otro.periodo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idInstructor, anio, periodo);
    }
}
