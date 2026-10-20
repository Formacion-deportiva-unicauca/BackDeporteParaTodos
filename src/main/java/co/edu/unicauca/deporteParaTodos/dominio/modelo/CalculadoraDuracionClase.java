package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DiaFueraDeHorarioExcepcion;

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

    // SCRUM-80 (correccion del cliente, 15 oct 2026): "dia con horario" = al
    // menos una franja VALIDA (fin > inicio) ese dia de la semana. No se mira
    // la hora del dia en ningun punto -- solo si el dia, como tal, tiene horario.
    public static boolean hayFranjaValidaEnFecha(List<Horario> franjasDelGrupo, LocalDate fecha) {
        String dia = nombreDia(fecha.getDayOfWeek());
        List<Horario> franjasDelDia = franjasDelGrupo.stream()
                .filter(h -> dia.equalsIgnoreCase(h.getDia()))
                .toList();
        return calcular(franjasDelDia).tieneFranjasValidas();
    }

    // Dias (LUNES..DOMINGO, orden natural de la semana) en que el grupo tiene
    // al menos una franja valida -- usado SOLO para el mensaje de error, nunca
    // para decidir (esa decision es hayFranjaValidaEnFecha/validarDiaConHorario).
    public static List<String> diasConFranjaValida(List<Horario> franjasDelGrupo) {
        List<String> dias = new ArrayList<>();
        for (DayOfWeek diaSemana : DayOfWeek.values()) {
            String nombre = nombreDia(diaSemana);
            List<Horario> franjasDelDia = franjasDelGrupo.stream()
                    .filter(h -> nombre.equalsIgnoreCase(h.getDia()))
                    .toList();
            if (calcular(franjasDelDia).tieneFranjasValidas()) {
                dias.add(nombre);
            }
        }
        return dias;
    }

    // SCRUM-80 (correccion del cliente, 15 oct 2026): el Instructor solo puede
    // registrar asistencia (crear clase o marcar atenciones) en los dias de la
    // semana en que el grupo tiene horario asignado -- Coordinador/Administrador
    // NO estan sujetos a esta regla (el llamador decide si la invoca). El
    // mensaje menciona los dias reales del grupo, nunca datos crudos del
    // cliente (la fecha que llego en la peticion no se repite en el mensaje).
    public static void validarDiaConHorario(List<Horario> franjasDelGrupo, LocalDate fecha) {
        if (hayFranjaValidaEnFecha(franjasDelGrupo, fecha)) {
            return;
        }
        List<String> dias = diasConFranjaValida(franjasDelGrupo);
        String mensaje = dias.isEmpty()
                ? "Este grupo no tiene ningun horario asignado -- solicite al Coordinador que configure el horario antes de registrar asistencia."
                : "El Instructor solo puede registrar asistencia los dias en que el grupo tiene horario asignado: "
                        + String.join(", ", dias) + ".";
        throw new DiaFueraDeHorarioExcepcion(mensaje);
    }
}
