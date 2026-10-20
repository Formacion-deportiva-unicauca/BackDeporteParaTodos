package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

// SCRUM-80 (correccion del cliente, 15 oct 2026): el Instructor solo puede
// registrar asistencia en los dias de la semana en que el grupo tiene al
// menos una franja de horario -- no se valida la hora del dia, solo el dia.
// Coordinador/Administrador no estan sujetos a esta regla (ver
// CalculadoraDuracionClase.validarDiaConHorario).
@Setter
@Getter
public class DiaFueraDeHorarioExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public DiaFueraDeHorarioExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.DIA_FUERA_DE_HORARIO.getLlaveMensaje();
        codigo = CodigoError.DIA_FUERA_DE_HORARIO.getCodigo();
    }
}
