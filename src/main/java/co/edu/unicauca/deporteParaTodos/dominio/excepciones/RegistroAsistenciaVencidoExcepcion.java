package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegistroAsistenciaVencidoExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public RegistroAsistenciaVencidoExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.REGISTRO_ASISTENCIA_VENCIDO.getLlaveMensaje();
        codigo = CodigoError.REGISTRO_ASISTENCIA_VENCIDO.getCodigo();
    }
}
