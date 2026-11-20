package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.INovedadClaseGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NovedadAsistenciaConflictoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;

/**
 * Regla unica: para un grupo y una fecha, o hay una novedad activa (clase no realizada) o hay asistencia, nunca las dos.
 *
 * Cada metodo toma primero el bloqueo de la fila del grupo (SELECT ... FOR UPDATE), asi que la decision de registrar una novedad y la
 * de registrar asistencia del mismo grupo se serializan: el que llega segundo ve lo que dejo el primero. Debe llamarse dentro de una
 * transaccion READ_COMMITTED (con REPEATABLE READ la lectura posterior al bloqueo veria una foto anterior).
 */
@Service
public class ExclusionNovedadAsistenciaServicio {

    static final String MENSAJE_CLASE_NO_REALIZADA =
            "Esta clase fue marcada como no realizada. No se puede registrar asistencia para esa fecha";
    static final String MENSAJE_YA_HAY_ASISTENCIA =
            "Ya hay asistencia registrada para esta fecha. Corrige primero la asistencia antes de marcar la clase como no realizada";

    private final IGrupoGateway grupoGateway;
    private final INovedadClaseGateway novedadGateway;
    private final IClaseGateway claseGateway;
    private final IAsistenciaGateway asistenciaGateway;

    public ExclusionNovedadAsistenciaServicio(IGrupoGateway grupoGateway, INovedadClaseGateway novedadGateway,
            IClaseGateway claseGateway, IAsistenciaGateway asistenciaGateway) {
        this.grupoGateway = grupoGateway;
        this.novedadGateway = novedadGateway;
        this.claseGateway = claseGateway;
        this.asistenciaGateway = asistenciaGateway;
    }

    /** Antes de escribir asistencia (o crear la clase de la fecha): falla si hay una novedad activa. */
    public void exigirSinNovedad(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha) {
        grupoGateway.obtenerGrupoConLock(categoria, curso, anio, iterable);
        if (novedadGateway.existeNovedadActiva(categoria, curso, anio, iterable, fecha)) {
            throw new NovedadAsistenciaConflictoExcepcion(MENSAJE_CLASE_NO_REALIZADA);
        }
    }

    /** Antes de registrar una novedad: falla si la fecha ya tiene asistencia confirmada o alguna fila de asistencia activa. */
    public void exigirSinAsistencia(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha) {
        grupoGateway.obtenerGrupoConLock(categoria, curso, anio, iterable);
        if (!claseGateway.existeClaseEnFecha(categoria, curso, anio, iterable, fecha)) {
            return;
        }
        Clase clase = claseGateway.obtenerClaseEnFecha(categoria, curso, anio, iterable, fecha);
        boolean hayAsistencia = clase.getAsistenciaConfirmadaEn() != null
                || asistenciaGateway.obtenerAtencionesPorClase(clase.getCodigo()).stream()
                        .anyMatch(a -> !Integer.valueOf(1).equals(a.getEliminado()));
        if (hayAsistencia) {
            throw new NovedadAsistenciaConflictoExcepcion(MENSAJE_YA_HAY_ASISTENCIA);
        }
    }
}
