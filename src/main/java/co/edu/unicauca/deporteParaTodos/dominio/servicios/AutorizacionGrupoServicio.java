package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import org.springframework.stereotype.Service;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.AccesoGrupoDenegadoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;

/**
 * Autorizacion por grupo (rol Instructor): un Instructor solo puede actuar
 * sobre un grupo donde grupo.idInstructor == perf_id del token. Coordinador y
 * Administrador quedan exentos y NO disparan ninguna consulta al grupo.
 *
 * Componente unico y reutilizable: todos los servicios que necesitan esta
 * regla (ClaseServicio, AsistenciaServicio, InscripcionServicio, AlumnoServicio,
 * NovedadClaseServicio) delegan aqui en vez de repetir la comparacion.
 */
@Service
public class AutorizacionGrupoServicio {

    private final IGrupoGateway grupoGateway;

    public AutorizacionGrupoServicio(IGrupoGateway grupoGateway) {
        this.grupoGateway = grupoGateway;
    }

    private boolean esInstructor(String rolLlamador) {
        return "Instructor".equals(rolLlamador);
    }

    // Variante booleana (sin lanzar) -- para callers que necesitan evaluar la
    // propiedad de VARIOS grupos y decidir ellos mismos que excepcion lanzar
    // (p.ej. AlumnoServicio recorriendo los grupos activos del alumno).
    public boolean esPropietario(Grupo grupo, String perfIdLlamador) {
        String idInstructorGrupo = grupo != null ? grupo.getIdInstructor() : null;
        return idInstructorGrupo != null && idInstructorGrupo.equals(perfIdLlamador);
    }

    // Variante que lanza, a partir de un Grupo ya obtenido -- evita una
    // segunda consulta cuando el caller ya lo tenia (p.ej. InscripcionServicio
    // .inscribir(), que ya hizo obtenerGrupoConLock()).
    public void verificarPropietario(Grupo grupo, String perfIdLlamador, String rolLlamador) {
        if (!esInstructor(rolLlamador)) {
            return;
        }
        if (!esPropietario(grupo, perfIdLlamador)) {
            throw new AccesoGrupoDenegadoExcepcion(
                    "El Instructor autenticado no tiene a cargo el grupo indicado");
        }
    }

    // Variante que obtiene el grupo por su clave -- para callers que todavia
    // no lo han cargado. grupoGateway.obtenerGrupo() ya lanza NoExisteExcepcion
    // (404) si el grupo no existe, asi que ese comportamiento se conserva sin
    // codigo adicional aqui.
    public void verificarPropietario(String categoria, String curso, Integer anio, Integer iterable,
            String perfIdLlamador, String rolLlamador) {
        if (!esInstructor(rolLlamador)) {
            return;
        }
        Grupo grupo = grupoGateway.obtenerGrupo(categoria, curso, anio, iterable);
        verificarPropietario(grupo, perfIdLlamador, rolLlamador);
    }
}
