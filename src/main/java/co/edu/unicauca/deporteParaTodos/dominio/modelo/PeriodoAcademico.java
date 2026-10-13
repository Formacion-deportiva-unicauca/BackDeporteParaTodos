package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.Clock;
import java.time.LocalDate;

// SCRUM-80 (HE-14 HU-02): periodo academico vigente, calculado con el Clock
// inyectado (zona America/Bogota via ClockConfig). Reutiliza EXACTAMENTE la
// misma regla que GrupoGateway.insertarGrupo() (mes >= 7 -> periodo 2, si no
// periodo 1) pero con "hoy" tomado del Clock en vez de LocalDate.now() sin
// zona -- GrupoGateway no se modifica, sigue con su comportamiento actual.
public final class PeriodoAcademico {

    private final int anio;
    private final int periodo;

    private PeriodoAcademico(int anio, int periodo) {
        this.anio = anio;
        this.periodo = periodo;
    }

    public static PeriodoAcademico vigente(Clock clock) {
        LocalDate hoy = LocalDate.now(clock);
        int periodo = hoy.getMonthValue() >= 7 ? 2 : 1;
        return new PeriodoAcademico(hoy.getYear(), periodo);
    }

    public int getAnio() {
        return anio;
    }

    public int getPeriodo() {
        return periodo;
    }
}
