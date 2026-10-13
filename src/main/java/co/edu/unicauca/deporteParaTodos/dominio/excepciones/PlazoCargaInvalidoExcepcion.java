package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PlazoCargaInvalidoExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public PlazoCargaInvalidoExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.PLAZO_CARGA_INVALIDO.getLlaveMensaje();
        codigo = CodigoError.PLAZO_CARGA_INVALIDO.getCodigo();
    }
}
