package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Facultad;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FacultadDto {

    private String nombre;

    public static FacultadDto fabricarDeModelo(Facultad modelo) {
        FacultadDto dto = new FacultadDto();
        dto.setNombre(modelo.getNombre());
        return dto;
    }
}
