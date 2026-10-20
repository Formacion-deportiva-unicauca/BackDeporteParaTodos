package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Deporte;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeporteDto {

    private String nombre;

    public static DeporteDto fabricarDeModelo(Deporte modelo) {
        DeporteDto dto = new DeporteDto();
        dto.setNombre(modelo.getNombre());
        return dto;
    }
}
