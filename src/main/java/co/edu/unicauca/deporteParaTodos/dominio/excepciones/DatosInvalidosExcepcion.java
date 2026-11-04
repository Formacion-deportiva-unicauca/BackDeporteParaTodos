package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

/** Datos enviados por el cliente que no cumplen las reglas del negocio (HTTP 422, codigo GC-0029). */
@Setter
@Getter
public class DatosInvalidosExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public DatosInvalidosExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.DATOS_INVALIDOS.getLlaveMensaje();
        codigo = CodigoError.DATOS_INVALIDOS.getCodigo();
    }
}
