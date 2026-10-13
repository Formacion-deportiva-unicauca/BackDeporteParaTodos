package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDate;

// SCRUM-80 (HE-14 HU-02): estado del reporte de carga de atenciones de un
// instructor para el periodo vigente. GENERADO prevalece sobre cualquier otro
// estado (el instructor ya reporto, sin importar el plazo); plazoVencido() es
// independiente del estado y refleja solo si la fecha limite ya paso.
public enum EstadoReporte {
    SIN_PLAZO,
    PENDIENTE,
    VENCIDO,
    GENERADO;

    // Dia limite INCLUSIVO: hoy == fechaLimite todavia cuenta como PENDIENTE.
    public static EstadoReporte calcular(LocalDate fechaLimite, int clasesConAsistencia, LocalDate hoy) {
        if (clasesConAsistencia > 0) {
            return GENERADO;
        }
        if (fechaLimite == null) {
            return SIN_PLAZO;
        }
        return hoy.isAfter(fechaLimite) ? VENCIDO : PENDIENTE;
    }

    public static boolean plazoVencido(LocalDate fechaLimite, LocalDate hoy) {
        return fechaLimite != null && hoy.isAfter(fechaLimite);
    }
}
