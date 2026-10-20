package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import java.util.List;
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
}
