package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Clase {

    private Integer codigo;

    private String categoria;

    private String curso;

    private Integer anio;

    private Integer iterable;

    private String idInstructor;

    private LocalDate fecha;

    private Integer horas;

    private Integer minutos;

    private String observacion;

    private Integer eliminado;

    // SCRUM-80 (rediseno, bloque 2): obligatoria SOLO si el grupo no tiene
    // franja de horario ese dia (CalculadoraDuracionClase.Resultado sin
    // franjas validas) -- ver ClaseServicio.insertarClase().
    private String justificacion;

    // Faltas acumuladas (etapa 2 de 5): NULL = pendiente de confirmar o
    // historica (nunca genera faltas, ver etapa 5). Cada confirmacion
    // sobrescribe ambos campos -- no se guarda un historial de confirmaciones.
    private LocalDateTime asistenciaConfirmadaEn;
    private String asistenciaConfirmadaPor;
}
