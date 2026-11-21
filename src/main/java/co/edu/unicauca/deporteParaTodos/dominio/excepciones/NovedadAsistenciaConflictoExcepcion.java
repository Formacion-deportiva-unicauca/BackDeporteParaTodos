package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

/**
 * Un grupo no puede tener, para la misma fecha, una novedad de clase no realizada y asistencia registrada.
 * Se lanza al registrar asistencia con novedad activa, o al registrar una novedad cuando ya hay asistencia (responde 409).
 */
@Setter
@Getter
public class NovedadAsistenciaConflictoExcepcion extends RuntimeException {

    private final String llaveMensaje;
    private final String codigo;

    public NovedadAsistenciaConflictoExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.CONFLICTO_NOVEDAD_ASISTENCIA.getLlaveMensaje();
        codigo = CodigoError.CONFLICTO_NOVEDAD_ASISTENCIA.getCodigo();
    }
}
