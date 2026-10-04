package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInstructorGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.AlumnoEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.InstructorEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.PerfilEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IAlumnoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IInstructorRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IPerfilRepositorio;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.infraestructura.controladorExcepciones.excepciones.NoImplementadoException;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.PerfilMapper;

@Service
public class InstructorGateway implements IInstructorGateway {

    @Autowired
    private IInstructorRepositorio repoInstructor;

    @Autowired
    private IPerfilRepositorio repoPerfil;

    @Autowired
    private IAlumnoRepositorio repoAlumno;

    // ── Mapeo manual Entidad → Dominio ──────────────────────────────────────
    private Instructor mapearEntidadADominio(InstructorEntidad entidad) {
        Instructor instructor = new Instructor();
        instructor.setInst_codigo(entidad.getIdPerfil());
        instructor.setEliminado(entidad.getEliminado());

        if (entidad.getPerfil() != null) {
            instructor.setPerfil(PerfilMapper.toDominio(entidad.getPerfil()));
        }

        return instructor;
    }

    // Nueva Implementacion para obtener instructores
    @Override
    public List<Instructor> obtenerInstructores() {
        List<Instructor> lista = new ArrayList<>();
        // Lista publica: excluye eliminados (mismo criterio que CursoServicio para
        // listados). obtenerInstructor() NO se filtra -- lo usa el guard de 409 de
        // eliminarInstructor() para leer el estado de un instructor ya eliminado.
        repoInstructor.findByEliminado(0)
                .forEach(entidad -> lista.add(mapearEntidadADominio(entidad)));
        return lista;
    }

    @Override
    public boolean existeInstructor(String instructorId) {
        return repoInstructor.existsById(instructorId);
    }

    @Override
    public Instructor insertarInstructor(Instructor datosInstructor) {
        throw new NoImplementadoException();
    }

    @Override
    public Optional<Instructor> obtenerInstructor(String instructorId) {
        if (existeInstructor(instructorId)) {
            Optional<InstructorEntidad> entidadRecuperada = repoInstructor.findById(instructorId);
            return entidadRecuperada.map(this::mapearEntidadADominio);
        }
        return Optional.empty();
    }

    @Override
    public Instructor actualizarInstructor(String instructorId, Instructor datosInstructor) {
        throw new NoImplementadoException();
    }

    @Override
    public Instructor eliminarInstructor(String instructorId) {
        Optional<InstructorEntidad> entidadExistente = repoInstructor.findById(instructorId);
        if (entidadExistente.isPresent()) {
            InstructorEntidad entidad = entidadExistente.get();
            entidad.setEliminado(1);
            InstructorEntidad guardado = repoInstructor.save(entidad);
            // mapper.map() generico (ModelMapper) no mapeaba bien el perfil anidado
            // (nombre/correo quedaban null -- confirmado con test de integracion real,
            // ver InstructorIT) por los getters irregulares de PerfilEntidad
            // (getPerfcorreo() sin guion bajo, getPerf_Sexo() con S mayuscula). Se usa
            // el mismo mapeo manual que ya usan obtenerInstructor()/obtenerInstructores().
            return mapearEntidadADominio(guardado);
        }
        throw new NoExisteExcepcion();
    }

    @Override
    public Instructor registrarInstructor(Perfil perfil, String tipoAlumno) {
        // [1] Crear perfil
        PerfilEntidad entidadPerfil = PerfilMapper.toEntidad(perfil);
        PerfilEntidad perfilGuardado = repoPerfil.save(entidadPerfil);
        String perfilId = perfilGuardado.getPerf_id();

        // [2] Registrar como alumno (vinculación al programa) - usando solo el ID
        AlumnoEntidad entidadAlumno = new AlumnoEntidad();
        entidadAlumno.setIdPerfil(perfilId);
        entidadAlumno.setTipoAlumno(tipoAlumno);
        entidadAlumno.setEliminado(0);
        repoAlumno.save(entidadAlumno);

        // [3] Registrar como instructor - usando solo el ID
        InstructorEntidad entidadInstructor = new InstructorEntidad();
        entidadInstructor.setIdPerfil(perfilId);
        entidadInstructor.setEliminado(0);
        InstructorEntidad instructorGuardado = repoInstructor.save(entidadInstructor);

        // Construir objeto de dominio para retornar
        Instructor instructorRegistrado = new Instructor();
        instructorRegistrado.setInst_codigo(instructorGuardado.getIdPerfil());
        instructorRegistrado.setPerfil(PerfilMapper.toDominio(perfilGuardado));
        instructorRegistrado.getPerfil().setTipoAlumno(tipoAlumno);

        return instructorRegistrado;
    }

}
