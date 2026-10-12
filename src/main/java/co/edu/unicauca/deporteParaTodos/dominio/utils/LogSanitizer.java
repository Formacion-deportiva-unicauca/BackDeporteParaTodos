package co.edu.unicauca.deporteParaTodos.dominio.utils;

/**
 * Sonar java:S5145 (log forging): un solo punto, Java puro sin dependencias de
 * Spring, para limpiar CR/LF de cualquier valor proveniente del cliente (variable
 * de ruta, parametro, cuerpo, claim de JWT) antes de pasarlo a un logger. Lo usan
 * tanto servicios de dominio (InstructorServicio) como controladores REST
 * (GrupoRest, InstructoresRest), evitando duplicar la logica de saneo.
 */
public final class LogSanitizer {

    private LogSanitizer() {
    }

    public static String sanitizar(String valor) {
        return valor == null ? null : valor.replace("\r", "_").replace("\n", "_");
    }
}
