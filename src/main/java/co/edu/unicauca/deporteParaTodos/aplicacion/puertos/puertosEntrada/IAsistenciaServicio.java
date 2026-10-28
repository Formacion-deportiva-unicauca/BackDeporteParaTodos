package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ResultadoConfirmacionAsistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AtencionDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ItemConfirmacionAsistenciaDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.VentanaRegistroDto;

public interface IAsistenciaServicio {

    public List<Asistencia> obtenerAsistencias();

    // TODO: obtenerAtencionesPorClase y registrarAtencionesPorClase exponen AtencionDto intencionalmente
    // en esta interfaz; refactorizarlas requiere cambiar también los callers en AtencionRest (fuera de alcance).
    public List<AtencionDto> obtenerAtencionesPorClase(Integer claseId);

    // SCRUM-80 (rediseno, bloque 2 de 3): perfIdLlamador/rolLlamador salen del
    // JWT en el controller -- VentanaRegistroAsistencia se evalua con el
    // perf_id de QUIEN LLAMA (no el instructor asignado al grupo), contra la
    // fecha de la CLASE (no la de "hoy").
    public void registrarAtencionesPorClase(List<AtencionDto> atenciones, Integer claseId,
            String perfIdLlamador, String rolLlamador);

    public Asistencia obtenerAsistencia(String perfId, int clsId);

    public Asistencia InsertarAsistencia(Asistencia datosAsistencia);

    public Asistencia eliminarAsitencia(String perfId, int clsId);

    public Asistencia eliminarAsistencia(String perfId, Long clsCodigo, String perfIdLlamador, String rolLlamador);

    // SCRUM-80 (rediseno, bloque 3 de 4): ventana de registro de asistencia para
    // quien llama. perfIdLlamador/rolLlamador salen del JWT en el controller.
    // Instructor: fechaMinima = 1o del mes actual (o del anterior si tiene
    // prorroga vigente), fechaMaxima = hoy. Coordinador/Administrador:
    // fechaMinima null (sin limite inferior), fechaMaxima = hoy.
    public VentanaRegistroDto obtenerVentanaRegistro(String perfIdLlamador, String rolLlamador);

    // Faltas acumuladas (etapa 4 de 5): PUT /clase/{codigo}/asistencia.
    public ResultadoConfirmacionAsistencia confirmarAsistencias(Integer claseId,
            List<ItemConfirmacionAsistenciaDto> asistencias, boolean confirmaTodosAusentes,
            String perfIdLlamador, String rolLlamador);
}
