package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDate;
import java.time.YearMonth;

// SCRUM-80 (rediseno, bloque 3 de 4): estado del registro de asistencia de un
// instructor para un mes CALENDARIO, mas los criterios puros que lo alimentan.
// "Registro del mes COMPLETO" = cada grupo ACTIVO en el mes tiene al menos UNA
// clase NO eliminada con fecha dentro del mes (una clase donde todos faltaron
// cuenta como registrada: lo que importa es que la clase exista, no cuantas
// asistencias tenga). Regla pura, sin Spring, testeable con fechas fijas.
public enum EstadoRegistroMensual {
    COMPLETO,
    PENDIENTE,
    VENCIDO,
    SIN_GRUPOS;

    // Un grupo esta ACTIVO en el mes si su periodo lo SOLAPA. fechas null = sin
    // limite por ese lado (grupo sin fecha de creacion/finalizacion -> activo en
    // todos los meses) -- criterio acordado con el cliente, ver PASO 0.
    public static boolean grupoActivoEnMes(LocalDate fechaCreacion, LocalDate fechaFinalizacion, int anio, int mes) {
        LocalDate primerDia = YearMonth.of(anio, mes).atDay(1);
        LocalDate ultimoDia = YearMonth.of(anio, mes).atEndOfMonth();
        boolean empiezaAntesOEnElMes = fechaCreacion == null || !fechaCreacion.isAfter(ultimoDia);
        boolean terminaEnOTrasElMes = fechaFinalizacion == null || !fechaFinalizacion.isBefore(primerDia);
        return empiezaAntesOEnElMes && terminaEnOTrasElMes;
    }

    public static boolean claseDentroDelMes(LocalDate fecha, int anio, int mes) {
        return fecha != null && fecha.getYear() == anio && fecha.getMonthValue() == mes;
    }

    // Plazo efectivo del mes: max(fin de mes, fechaLimite de la prorroga si existe).
    // Una prorroga que reabre el mes puede empujar el limite mas alla del fin de mes.
    public static LocalDate fechaLimiteEfectiva(int anio, int mes, LocalDate limiteProrroga) {
        LocalDate finDeMes = VentanaRegistroAsistencia.finDeMes(anio, mes);
        return (limiteProrroga != null && limiteProrroga.isAfter(finDeMes)) ? limiteProrroga : finDeMes;
    }

    // Dia limite INCLUSIVO: hoy == fechaLimite todavia cuenta como PENDIENTE.
    public static EstadoRegistroMensual calcular(int gruposActivos, int gruposRegistrados,
            LocalDate fechaLimite, LocalDate hoy) {
        if (gruposActivos <= 0) {
            return SIN_GRUPOS;
        }
        if (gruposRegistrados >= gruposActivos) {
            return COMPLETO;
        }
        return hoy.isAfter(fechaLimite) ? VENCIDO : PENDIENTE;
    }
}
