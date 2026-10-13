package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import java.time.LocalDate;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PlazoCargaAtenciones;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// SCRUM-80 (HE-14 HU-02).
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlazoCargaDto {

    private String idInstructor;
    private Integer anio;
    private Integer periodo;
    private String fechaLimite;
    private String estadoReporte;
    private boolean plazoVencido;
    private int clasesConAsistencia;

    // clasesConAsistencia y hoy no viven en PlazoCargaAtenciones (son datos
    // agregados de OTRAS tablas y del Clock, calculados en el Servicio) -- se
    // reciben aparte en vez de forzarlos dentro del modelo de dominio.
    public static PlazoCargaDto fabricarDeModelo(PlazoCargaAtenciones modelo, int clasesConAsistencia, LocalDate hoy) {
        PlazoCargaDto dto = new PlazoCargaDto();
        dto.setIdInstructor(modelo.getIdInstructor());
        dto.setAnio(modelo.getAnio());
        dto.setPeriodo(modelo.getPeriodo());
        LocalDate fechaLimite = modelo.getFechaLimite();
        dto.setFechaLimite(fechaLimite != null ? fechaLimite.toString() : null);
        dto.setEstadoReporte(EstadoReporte.calcular(fechaLimite, clasesConAsistencia, hoy).name());
        dto.setPlazoVencido(EstadoReporte.plazoVencido(fechaLimite, hoy));
        dto.setClasesConAsistencia(clasesConAsistencia);
        return dto;
    }
}
