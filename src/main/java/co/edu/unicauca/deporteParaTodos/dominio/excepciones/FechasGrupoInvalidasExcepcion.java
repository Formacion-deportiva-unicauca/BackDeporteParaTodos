package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FechasGrupoInvalidasExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public FechasGrupoInvalidasExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.FECHAS_GRUPO_INVALIDAS.getLlaveMensaje();
        codigo = CodigoError.FECHAS_GRUPO_INVALIDAS.getCodigo();
    }
}
