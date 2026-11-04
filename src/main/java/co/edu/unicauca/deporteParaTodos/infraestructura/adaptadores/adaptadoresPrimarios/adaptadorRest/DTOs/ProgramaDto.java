package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Programa;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProgramaDto {

    private String nombre;

    private String facultad;

    public static ProgramaDto fabricarDeModelo(Programa modelo) {
        String facultad = modelo.getFacultad() != null ? modelo.getFacultad().getNombre() : null;
        return new ProgramaDto(modelo.getPrg_nombre(), facultad);
    }
}
