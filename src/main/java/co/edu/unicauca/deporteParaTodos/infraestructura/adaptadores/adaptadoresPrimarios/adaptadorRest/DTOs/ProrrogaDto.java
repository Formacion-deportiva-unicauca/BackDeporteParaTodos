package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// SCRUM-80 (rediseno, bloque 1 de 3). fechaLimite es null cuando dias=0
// (la prorroga se elimino -- la respuesta del PUT confirma el borrado con
// dias=0/fechaLimite=null en vez de omitir ese instructor de la lista).
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProrrogaDto {

    private String idInstructor;
    private Integer dias;
    private String fechaLimite;

    public static ProrrogaDto fabricarDeModelo(ProrrogaRegistroAsistencia modelo) {
        ProrrogaDto dto = new ProrrogaDto();
        dto.setIdInstructor(modelo.getIdInstructor());
        dto.setDias(modelo.getDias());
        dto.setFechaLimite(modelo.getFechaLimite() != null ? modelo.getFechaLimite().toString() : null);
        return dto;
    }
}
