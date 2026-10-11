package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDate;

/**
 * SCRUM-185: unica regla de disponibilidad de inscripcion de un grupo, usada
 * tanto por InscripcionServicio.inscribir() como por el catalogo de grupos
 * disponibles (GrupoServicio/GrupoGateway) -- antes habia dos implementaciones
 * independientes (una en Java que inscribir() nunca ejecutaba, otra en SQL
 * nativo con CURRENT_DATE que solo usaba el catalogo).
 */
public enum EstadoVentanaInscripcion {
    ABIERTAS,
    NO_ABIERTAS,
    CERRADAS;

    // apertura null = abierto desde siempre; cierre null = sin cierre.
    // El dia de cierre es INCLUSIVO: hoy == cierre todavia cuenta como abierto.
    public static EstadoVentanaInscripcion calcular(LocalDate apertura, LocalDate cierre, LocalDate hoy) {
        if (apertura != null && hoy.isBefore(apertura)) {
            return NO_ABIERTAS;
        }
        if (cierre != null && hoy.isAfter(cierre)) {
            return CERRADAS;
        }
        return ABIERTAS;
    }
}
