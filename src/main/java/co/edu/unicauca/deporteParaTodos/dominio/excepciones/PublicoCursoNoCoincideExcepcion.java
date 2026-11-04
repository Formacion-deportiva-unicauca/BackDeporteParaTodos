package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

/** El curso esta dirigido a otro tipo de vinculo que el del alumno (HTTP 422, codigo GC-0030). */
@Setter
@Getter
public class PublicoCursoNoCoincideExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public PublicoCursoNoCoincideExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.PUBLICO_CURSO_NO_COINCIDE.getLlaveMensaje();
        codigo = CodigoError.PUBLICO_CURSO_NO_COINCIDE.getCodigo();
    }
}
