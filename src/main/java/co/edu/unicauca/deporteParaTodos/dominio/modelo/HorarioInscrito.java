package co.edu.unicauca.deporteParaTodos.dominio.modelo;

/**
 * Una franja horaria de un grupo en el que el alumno esta INSCRITO (vigente). Lleva los datos del grupo que hacen falta para
 * comparar (anio y periodo ya vienen filtrados) y para nombrarlo en el mensaje de cruce. Dia y horas llegan tal cual estan
 * guardados (texto libre): {@link CruceHorarios} los normaliza y descarta las que no sean validas.
 */
public record HorarioInscrito(String categoria, String curso, int anio, int iterable, int periodo,
        Integer horarioId, String dia, String horaInicio, String horaFin) {
}
