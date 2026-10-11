package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InscripcionFueraDeFechasExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public InscripcionFueraDeFechasExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.INSCRIPCION_FUERA_DE_FECHAS.getLlaveMensaje();
        codigo = CodigoError.INSCRIPCION_FUERA_DE_FECHAS.getCodigo();
    }
}
