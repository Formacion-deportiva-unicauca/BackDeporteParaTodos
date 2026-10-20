package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// SCRUM-80 (rediseno, bloque 3 de 4): ventana de registro de asistencia
// (GET /asistencia/ventana). fechaMinima es null para Coordinador/Administrador
// (no tienen limite inferior de plazo).
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VentanaRegistroDto {

    private String fechaMinima;
    private String fechaMaxima;
    private String rol;

    public static VentanaRegistroDto fabricarDeModelo(LocalDate fechaMinima, LocalDate fechaMaxima, String rol) {
        VentanaRegistroDto dto = new VentanaRegistroDto();
        dto.setFechaMinima(fechaMinima != null ? fechaMinima.toString() : null);
        dto.setFechaMaxima(fechaMaxima != null ? fechaMaxima.toString() : null);
        dto.setRol(rol);
        return dto;
    }
}
