package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

// Faltas acumuladas (etapa 4 de 5): SIN_ASISTENTES_SIN_CONFIRMAR -- nadie
// vino marcado con estaAtendido=true y el llamador no confirmo explicitamente
// confirmaTodosAusentes=true. Evita que una confirmacion accidental (p.ej.
// cuerpo vacio por error del cliente) marque a todo el grupo como ausente.
@Setter
@Getter
public class ConfirmacionSinAsistentesExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public ConfirmacionSinAsistentesExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.CONFIRMACION_SIN_ASISTENTES.getLlaveMensaje();
        codigo = CodigoError.CONFIRMACION_SIN_ASISTENTES.getCodigo();
    }
}
