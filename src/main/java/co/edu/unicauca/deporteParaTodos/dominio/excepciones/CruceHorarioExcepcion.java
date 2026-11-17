package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

/** El horario del grupo se cruza con el de otro grupo en el que el alumno ya esta inscrito (HTTP 422). */
@Setter
@Getter
public class CruceHorarioExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public CruceHorarioExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.CRUCE_HORARIO.getLlaveMensaje();
        codigo = CodigoError.CRUCE_HORARIO.getCodigo();
    }
}
