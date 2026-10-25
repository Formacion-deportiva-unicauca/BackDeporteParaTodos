package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

// Decision (ver informe de entrega): el correo del alumno afecta su inicio de
// sesion, por lo que solo el Coordinador puede modificarlo. El Instructor
// puede seguir actualizando nombre y tipoAlumno.
@Setter
@Getter
public class CorreoNoModificablePorInstructorExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public CorreoNoModificablePorInstructorExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.CORREO_NO_MODIFICABLE_INSTRUCTOR.getLlaveMensaje();
        codigo = CodigoError.CORREO_NO_MODIFICABLE_INSTRUCTOR.getCodigo();
    }
}
