package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// SCRUM-80 (rediseno, bloque 2 de 3): componente puro (sin Spring) que calcula
// la duracion total de una clase sumando las franjas de tbl_horario del grupo
// para el dia de la semana de la fecha de la clase.
//
// PASO 0 (verificado, no supuesto): HR_DIA en tbl_horario se guarda SIN
// tildes ("MIERCOLES", "SABADO" -- confirmado por grep sobre los fixtures/IT
// reales del proyecto, p.ej. InscripcionServicioTest/GrupoInstructorIT usan
// literalmente "MIERCOLES"). nombreDia() usa esa misma convencion para que el
// filtrado por dia encuentre las franjas reales guardadas en BD.
public final class CalculadoraDuracionClase {

    private CalculadoraDuracionClase() {
    }

    public static String nombreDia(DayOfWeek dia) {
        switch (dia) {
            case MONDAY: return "LUNES";
            case TUESDAY: return "MARTES";
            case WEDNESDAY: return "MIERCOLES";
            case THURSDAY: return "JUEVES";
            case FRIDAY: return "VIERNES";
            case SATURDAY: return "SABADO";
            case SUNDAY: return "DOMINGO";
            default: throw new IllegalArgumentException("Dia de la semana no reconocido: " + dia);
        }
    }

    public static final class Resultado {
        private final int minutos;
        private final List<Integer> idsFranjasIgnoradas;

        private Resultado(int minutos, List<Integer> idsFranjasIgnoradas) {
            this.minutos = minutos;
            this.idsFranjasIgnoradas = idsFranjasIgnoradas;
        }

        public int getMinutos() {
            return minutos;
        }

        public List<Integer> getIdsFranjasIgnoradas() {
            return idsFranjasIgnoradas;
        }

        // Una franja invalida no cuenta como franja -- si TODAS las franjas del
        // dia son invalidas (o no hay ninguna), equivale a "sin horario ese dia".
        public boolean tieneFranjasValidas() {
            return minutos > 0;
        }
    }

    // Decision documentada: una franja con fin <= inicio es un dato de horario
    // invalido (preexistente, fuera del control de quien registra la clase
    // hoy) -- se IGNORA (no suma minutos) en vez de rechazar el registro
    // completo de la clase; su id queda en el resultado para que el llamador
    // lo deje en el log como diagnostico.
    public static Resultado calcular(List<Horario> franjasDelDia) {
        int totalMinutos = 0;
        List<Integer> ignoradas = new ArrayList<>();
        for (Horario franja : franjasDelDia) {
            LocalTime inicio = LocalTime.parse(franja.getHoraInicio());
            LocalTime fin = LocalTime.parse(franja.getHoraFin());
            if (!fin.isAfter(inicio)) {
                ignoradas.add(franja.getId());
                continue;
            }
            totalMinutos += Duration.between(inicio, fin).toMinutes();
        }
        return new Resultado(totalMinutos, Collections.unmodifiableList(ignoradas));
    }
}
