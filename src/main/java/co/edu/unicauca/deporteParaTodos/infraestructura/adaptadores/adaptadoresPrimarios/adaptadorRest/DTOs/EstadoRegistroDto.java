package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import java.time.LocalDate;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoRegistroMensual;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// SCRUM-80 (rediseno, bloque 3 de 4): una fila de GET /instructores/estado-registro.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EstadoRegistroDto {

    private String idInstructor;
    private String nombre;
    private int anio;
    private int mes;
    private String estado;
    private int gruposActivos;
    private int gruposRegistrados;
    private String fechaLimite;
    private String hoy;

    // Los conteos, el estado, el limite y "hoy" son datos AGREGADOS que el
    // Servicio calcula con gateways + el Clock: no viven en Instructor, se
    // reciben aparte (mismo criterio que PlazoCargaDto.fabricarDeModelo).
    public static EstadoRegistroDto fabricarDeModelo(Instructor instructor, int anio, int mes,
            EstadoRegistroMensual estado, int gruposActivos, int gruposRegistrados,
            LocalDate fechaLimite, LocalDate hoy) {
        EstadoRegistroDto dto = new EstadoRegistroDto();
        if (instructor != null) {
            dto.setIdInstructor(instructor.getInst_codigo());
            if (instructor.getPerfil() != null) {
                dto.setNombre(instructor.getPerfil().getNombre());
            }
        }
        dto.setAnio(anio);
        dto.setMes(mes);
        dto.setEstado(estado != null ? estado.name() : null);
        dto.setGruposActivos(gruposActivos);
        dto.setGruposRegistrados(gruposRegistrados);
        dto.setFechaLimite(fechaLimite != null ? fechaLimite.toString() : null);
        dto.setHoy(hoy != null ? hoy.toString() : null);
        return dto;
    }
}
