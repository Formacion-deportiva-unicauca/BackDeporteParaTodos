package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

// Faltas acumuladas (etapa 4 de 5): el idPerfil enviado en la confirmacion de
// asistencia no tiene una inscripcion activa en el grupo de la clase (no esta
// en el "Universo U" -- misma condicion que buscarAlumnosGrupoRaw). Se
// rechaza toda la confirmacion sin guardar nada.
@Setter
@Getter
public class AlumnoFueraDeGrupoExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public AlumnoFueraDeGrupoExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.ALUMNO_FUERA_DE_GRUPO.getLlaveMensaje();
        codigo = CodigoError.ALUMNO_FUERA_DE_GRUPO.getCodigo();
    }
}
