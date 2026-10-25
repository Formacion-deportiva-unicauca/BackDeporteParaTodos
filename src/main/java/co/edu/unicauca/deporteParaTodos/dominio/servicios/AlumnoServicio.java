package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAlumnoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAlumnoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.AccesoGrupoDenegadoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.CorreoNoModificablePorInstructorExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AlumnoDto;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;

@Service
public class AlumnoServicio implements IAlumnoServicio {

    @Autowired
    private IAlumnoGateway alumnoGateway;

    @Autowired
    private IPerfilGateway perfilGateway;

    @Autowired
    private IInscripcionGateway inscripcionGateway;

    @Autowired
    private IGrupoGateway grupoGateway;

    @Autowired
    private AutorizacionGrupoServicio autorizacionGrupo;

    @Override
    public List<Alumno> obtenerAlumnos() {

        List<Alumno> listAlumnos = alumnoGateway.obtenerAlumnos();
        if (listAlumnos.isEmpty()) {
            throw new ListadoVacioExcepcion("No se encuentran alumnos registrados");
        }
        return listAlumnos;
    }

    @Override
    public List<AlumnoDto> obtenerAlumnosGrupo(String categoria, String curso, Integer anio, Integer iterable) {
        List<Alumno> alumnos = alumnoGateway.obtenerAlumnosGrupo(categoria, curso, anio, iterable);
        if (alumnos.isEmpty()) {
            throw new ListadoVacioExcepcion("No se encuentran alumnos registrados para el grupo consultado");
        }
        List<AlumnoDto> listaDtos = new ArrayList<>();
        alumnos.forEach(modelo -> {
            AlumnoDto dto = AlumnoDto.fabricarDeModelo(modelo);
            listaDtos.add(dto);
        });
        return listaDtos;
    }

    @Override
    public Alumno insertAlumno(Alumno datosAlumno) {
        return null;
    }

    @Override
    public Alumno obtenerAlumno(String alumnoId) {
        return alumnoGateway.obtenerAlumno(alumnoId).orElseThrow(
                () -> new NoExisteExcepcion("No exoste el alumno con el identificador " + alumnoId));
    }

    @Override
    @Transactional
    public Alumno actualizarAlumno(String alumnoId, Alumno datosAlumno, String perfIdLlamador, String rolLlamador) {
        if (!alumnoGateway.existeAlumno(alumnoId)) {
            throw new NoExisteExcepcion("No existe el alumno con el identificador " + alumnoId);
        }

        if ("Instructor".equals(rolLlamador)) {
            if (!tieneAlumnoACargo(alumnoId, perfIdLlamador)) {
                throw new AccesoGrupoDenegadoExcepcion(
                        "El alumno no tiene una inscripcion activa en ningun grupo a cargo del Instructor");
            }
            // Decision (ver informe de entrega): el correo del alumno afecta su
            // inicio de sesion -- solo el Coordinador puede modificarlo. El
            // Instructor puede seguir actualizando nombre y tipoAlumno.
            String correoActual = alumnoGateway.obtenerAlumno(alumnoId)
                    .map(Alumno::getPerfil)
                    .map(Perfil::getCorreo)
                    .orElse(null);
            String correoNuevo = datosAlumno.getPerfil() != null ? datosAlumno.getPerfil().getCorreo() : null;
            if (correoNuevo != null && !correoNuevo.equals(correoActual)) {
                throw new CorreoNoModificablePorInstructorExcepcion(
                        "El Instructor no puede modificar el correo del alumno");
            }
        }

        // 1. Actualizar nombre y correo en tbl_perfil
        //    Se pasa un Perfil con solo nombre y correo; perf_tipo y perf_sexo
        //    vienen null — PerfilGateway los ignorará gracias a los null-checks
        Perfil datosPerfilActualizar = new Perfil();
        datosPerfilActualizar.setNombre(datosAlumno.getPerfil().getNombre());
        datosPerfilActualizar.setCorreo(datosAlumno.getPerfil().getCorreo());
        perfilGateway.actualizarPerfil(alumnoId, datosPerfilActualizar);

        // 2. Actualizar alm_tipo en tbl_alumno y retornar el alumno completo
        return alumnoGateway.actualizarAlumno(alumnoId, datosAlumno);
    }

    // Autorizacion por grupo (caso 3 del ticket): el alumno esta "a cargo" de
    // un Instructor si tiene AL MENOS una inscripcion activa (INSCRITO, sin
    // desvincular) en un grupo cuyo idInstructor es ese Instructor. Una
    // inscripcion desvinculada o EN_ESPERA no cuenta -- existeInscripcionActiva()
    // ya aplica esa misma definicion (ver IInscripcionRepositorio).
    private boolean tieneAlumnoACargo(String alumnoId, String perfIdLlamador) {
        List<Inscripcion> inscripciones = inscripcionGateway.obtenerInscripcionesAlumno(alumnoId);
        for (Inscripcion inscripcion : inscripciones) {
            boolean activa = inscripcionGateway.existeInscripcionActiva(alumnoId,
                    inscripcion.getCategoria(), inscripcion.getCurso(), inscripcion.getAnio(), inscripcion.getIterable());
            if (!activa) {
                continue;
            }
            Grupo grupo = grupoGateway.obtenerGrupo(
                    inscripcion.getCategoria(), inscripcion.getCurso(), inscripcion.getAnio(), inscripcion.getIterable());
            if (autorizacionGrupo.esPropietario(grupo, perfIdLlamador)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Alumno eliminarAlumno(String alumnoId) {
        if (!alumnoGateway.existeAlumno(alumnoId)) {
            throw new NoExisteExcepcion("No exoste el alumno con el identificador " + alumnoId);
        }
        return alumnoGateway.eliminarAlumno(alumnoId);
    }

}
