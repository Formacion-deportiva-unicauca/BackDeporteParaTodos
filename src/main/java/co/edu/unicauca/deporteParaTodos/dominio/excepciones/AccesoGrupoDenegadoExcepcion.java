package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

// Autorizacion por grupo (rol Instructor): el Instructor autenticado no es el
// idInstructor del grupo sobre el que intenta actuar. Coordinador y
// Administrador no estan sujetos a esta regla -- ver AutorizacionGrupoServicio.
@Setter
@Getter
public class AccesoGrupoDenegadoExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public AccesoGrupoDenegadoExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.ACCESO_GRUPO_DENEGADO.getLlaveMensaje();
        codigo = CodigoError.ACCESO_GRUPO_DENEGADO.getCodigo();
    }
}
