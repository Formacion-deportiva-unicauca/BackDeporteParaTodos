package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDate;
import java.time.YearMonth;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechaClaseFuturaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.RegistroAsistenciaVencidoExcepcion;

// SCRUM-80 (rediseno, bloque 1 de 3): regla de negocio pura (sin Spring) para
// decidir si se puede registrar la asistencia de una clase. Plazo base = fin
// del mes CALENDARIO de la fecha de la clase, inclusive, igual para todos.
// Prorroga = la asigna el Coordinador, por instructor y mes; su limite
// efectivo ya viene PRECALCULADO (ver calcularLimiteProrroga) al momento de
// la asignacion -- aqui solo se compara contra el fin de mes con max(). El
// Coordinador no tiene limite de plazo, pero SI esta sujeto a "no fecha
// futura" (nadie la tiene, sin excepcion de rol).
public final class VentanaRegistroAsistencia {

    private VentanaRegistroAsistencia() {
    }

    public static LocalDate finDeMes(int anio, int mes) {
        return YearMonth.of(anio, mes).atEndOfMonth();
    }

    // Limite efectivo de una prorroga EN EL MOMENTO EN QUE EL COORDINADOR LA
    // ASIGNA: max(finDeMes, fechaAsignacion) + dias. Usar la fecha de
    // asignacion como base (en vez de finDeMes) cuando esta es posterior
    // permite "reabrir" un mes que ya vencio.
    public static LocalDate calcularLimiteProrroga(LocalDate finDeMes, LocalDate fechaAsignacion, int dias) {
        LocalDate base = fechaAsignacion.isAfter(finDeMes) ? fechaAsignacion : finDeMes;
        return base.plusDays(dias);
    }

    // Lanza FechaClaseFuturaExcepcion o RegistroAsistenciaVencidoExcepcion si
    // NO se puede registrar; no hace nada si si se puede.
    public static void validarRegistro(LocalDate fechaClase, LocalDate hoy, boolean esCoordinador,
            LocalDate limiteProrroga) {
        if (fechaClase.isAfter(hoy)) {
            throw new FechaClaseFuturaExcepcion(
                    "La fecha de la clase (" + fechaClase + ") no puede ser posterior a hoy (" + hoy + ")");
        }
        if (esCoordinador) {
            return;
        }
        LocalDate finMes = finDeMes(fechaClase.getYear(), fechaClase.getMonthValue());
        LocalDate limiteEfectivo = (limiteProrroga != null && limiteProrroga.isAfter(finMes)) ? limiteProrroga : finMes;
        if (hoy.isAfter(limiteEfectivo)) {
            throw new RegistroAsistenciaVencidoExcepcion(
                    "El plazo para registrar la asistencia de la clase del " + fechaClase
                            + " vencio el " + limiteEfectivo);
        }
    }
}
