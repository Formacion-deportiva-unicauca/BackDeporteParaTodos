package co.edu.unicauca.deporteParaTodos.dominio.modelo;

/**
 * A quien va dirigido un curso. Se compara con el tipo de vinculo del alumno (Estudiante, Docente, Administrativo,
 * Contratista): los tres ultimos son "Funcionarios". MIXTO es toda la comunidad universitaria.
 */
public enum PublicoCurso {
    ESTUDIANTE,
    FUNCIONARIO,
    MIXTO;

    public boolean admite(String tipoAlumno) {
        return switch (this) {
            case MIXTO -> true;
            case ESTUDIANTE -> "Estudiante".equals(tipoAlumno);
            case FUNCIONARIO -> "Docente".equals(tipoAlumno)
                    || "Administrativo".equals(tipoAlumno)
                    || "Contratista".equals(tipoAlumno);
        };
    }

    /** Un valor ausente es MIXTO; uno desconocido lanza IllegalArgumentException. */
    public static PublicoCurso desdeTexto(String texto) {
        return texto == null || texto.isBlank() ? MIXTO : valueOf(texto.trim());
    }
}
