package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;

public interface IAsistenciaGateway {

    public boolean existeAsistencia(String perfId, int clsId);
    
    public List<Asistencia> obtenerAsistencias();

    public List<Asistencia> obtenerAtencionesPorClase(Integer claseId);

    public Optional<Asistencia> obtenerAsistencia(String perfId, int clsId);

    public Asistencia InsertarAsistencia(Asistencia datosAsistencia);

    public Asistencia eliminarAsitencia(String perfId, int clsId);

    public Asistencia eliminarAsistencia(String perfId, Long clsCodigo);

    // SCRUM-80 (rediseno, bloque 2): reactiva una fila con META_ELIMINADO=1
    // (eliminado=0) -- PASO 0 confirmo que existeAsistencia() no distinguia
    // eliminado=0 de eliminado=1, asi que esa fila nunca se reactivaba.
    public Asistencia reactivarAsistencia(String perfId, int clsId);

    // Faltas acumuladas (etapa 4 de 5): upsert explicito a PRESENTE/AUSENTE
    // (crea la fila si no existe, o la actualiza) -- usados SOLO por el flujo
    // de confirmacion y por los endpoints viejos cuando la clase YA esta
    // confirmada (ver AsistenciaServicio). El flujo viejo (clase sin
    // confirmar) sigue usando InsertarAsistencia/reactivarAsistencia/
    // eliminarAsitencia/eliminarAsistencia sin cambios.
    public Asistencia marcarPresente(String perfId, int clsId);

    public Asistencia marcarAusente(String perfId, int clsId);

    // Faltas acumuladas (etapa 5 de 5): UNA consulta agregada por grupo,
    // perfId -> cantidad de faltas. Sin alcance de inscripcion actual.
    public Map<String, Integer> contarFaltasPorGrupo(String categoria, String curso, Integer anio, Integer iterable,
            LocalDate hoy);
}
