package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProrrogaMesFuturoExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public ProrrogaMesFuturoExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.PRORROGA_MES_FUTURO.getLlaveMensaje();
        codigo = CodigoError.PRORROGA_MES_FUTURO.getCodigo();
    }
}
