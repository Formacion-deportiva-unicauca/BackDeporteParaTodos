package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Regla de cruce de horarios (pura, sin acceso a datos).
 *
 * Una franja es (dia, inicio, fin) y es el intervalo semiabierto [inicio, fin): hay cruce entre dos franjas del MISMO dia si
 * inicioA < finB && inicioB < finA. Si una termina justo cuando empieza la otra NO es cruce.
 *
 * Los datos de horario son texto libre (la API solo exige no vacio): una franja con dia desconocido, hora mal formada o
 * fin <= inicio es INVALIDA y se ignora (fail-open): solo se bloquea un cruce demostrable, nunca por un dato sucio, y nunca hay 500.
 */
public final class CruceHorarios {

    private static final List<String> DIAS = List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO");
    private static final List<String> DIAS_VISIBLES = List.of("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo");
    private static final Pattern HORA = Pattern.compile("^\\d{1,2}:\\d{2}$");

    private CruceHorarios() {
    }

    /** Franja ya normalizada: dia con su orden en la semana y horas en minutos desde las 00:00. */
    public record Franja(int diaOrden, int inicioMin, int finMin) {
        boolean seCruzaCon(Franja otra) {
            return diaOrden == otra.diaOrden && inicioMin < otra.finMin && otra.inicioMin < finMin;
        }
    }

    /** El cruce encontrado: la franja del OTRO grupo (el que ya tiene inscrito el alumno) con la que choca. */
    public record Cruce(String categoria, String curso, int iterable, int diaOrden, int inicioMin, int finMin) {
        /** Por ejemplo "martes 10:00-12:00". */
        public String franjaTexto() {
            return DIAS_VISIBLES.get(diaOrden) + " " + hhmm(inicioMin) + "-" + hhmm(finMin);
        }

        /** Por ejemplo "Se cruza con Natación, grupo A, martes 10:00-12:00". */
        public String detalle() {
            return "Se cruza con " + curso + ", grupo " + letraDeIterable(iterable) + ", " + franjaTexto();
        }
    }

    /** Franja normalizada, o vacio si el dia no es de la semana, una hora esta mal formada, o fin <= inicio. */
    public static Optional<Franja> normalizar(String dia, String horaInicio, String horaFin) {
        int diaOrden = diaOrden(dia);
        Integer inicio = minutos(horaInicio);
        Integer fin = minutos(horaFin);
        if (diaOrden < 0 || inicio == null || fin == null || fin <= inicio) {
            return Optional.empty();
        }
        return Optional.of(new Franja(diaOrden, inicio, fin));
    }

    /** Orden del dia (LUNES=0 .. DOMINGO=6) tolerando mayusculas, espacios y tildes; -1 si no es un dia. */
    static int diaOrden(String dia) {
        if (dia == null) {
            return -1;
        }
        String limpio = Normalizer.normalize(dia.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        return DIAS.indexOf(limpio);
    }

    /** Minutos desde las 00:00 de "H:mm" o "HH:mm"; null si no es una hora valida. */
    static Integer minutos(String hora) {
        if (hora == null) {
            return null;
        }
        String limpia = hora.trim();
        if (!HORA.matcher(limpia).matches()) {
            return null;
        }
        try {
            int separador = limpia.indexOf(':');
            LocalTime t = LocalTime.of(Integer.parseInt(limpia.substring(0, separador)),
                    Integer.parseInt(limpia.substring(separador + 1)));
            return t.getHour() * 60 + t.getMinute();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Franjas validas del grupo destino (las invalidas se descartan). */
    public static List<Franja> franjasValidas(List<Horario> horarios) {
        List<Franja> franjas = new ArrayList<>();
        if (horarios != null) {
            for (Horario h : horarios) {
                normalizar(h.getDia(), h.getHoraInicio(), h.getHoraFin()).ifPresent(franjas::add);
            }
        }
        return franjas;
    }

    /**
     * Primer cruce (orden determinista: categoria, curso, grupo, dia de la semana, hora de inicio, hora de fin) entre las
     * franjas del grupo destino y las de los grupos donde el alumno ya esta inscrito. Las del propio grupo destino
     * (categoria, curso, anio, iterable) se excluyen: reactivar una fila no se compara consigo misma.
     */
    public static Optional<Cruce> primerCruce(Grupo destino, List<Franja> franjasDestino, List<HorarioInscrito> inscritos) {
        if (franjasDestino.isEmpty() || inscritos == null) {
            return Optional.empty();
        }
        List<Cruce> cruces = new ArrayList<>();
        for (HorarioInscrito h : inscritos) {
            if (mismoGrupo(destino, h)) {
                continue;
            }
            Optional<Franja> otra = normalizar(h.dia(), h.horaInicio(), h.horaFin());
            if (otra.isPresent() && franjasDestino.stream().anyMatch(f -> f.seCruzaCon(otra.get()))) {
                cruces.add(new Cruce(h.categoria(), h.curso(), h.iterable(), otra.get().diaOrden(),
                        otra.get().inicioMin(), otra.get().finMin()));
            }
        }
        return cruces.stream().min(Comparator.comparing(Cruce::categoria).thenComparing(Cruce::curso)
                .thenComparingInt(Cruce::iterable).thenComparingInt(Cruce::diaOrden)
                .thenComparingInt(Cruce::inicioMin).thenComparingInt(Cruce::finMin));
    }

    private static boolean mismoGrupo(Grupo g, HorarioInscrito h) {
        return g.getAnio() == h.anio() && g.getIterable() == h.iterable()
                && Objects.equals(g.getCategoria(), h.categoria()) && Objects.equals(g.getCurso(), h.curso());
    }

    /**
     * Misma regla que el frontend (src/app/shared/iterable-letra.util.ts): menor que 1 -> "?"; si no, el caracter 64 + n
     * (1 = A, 26 = Z, 27 = "[").
     */
    public static String letraDeIterable(int n) {
        if (n < 1) {
            return "?";
        }
        return String.valueOf((char) (64 + n));
    }

    private static String hhmm(int minutos) {
        return String.format("%02d:%02d", minutos / 60, minutos % 60);
    }
}
