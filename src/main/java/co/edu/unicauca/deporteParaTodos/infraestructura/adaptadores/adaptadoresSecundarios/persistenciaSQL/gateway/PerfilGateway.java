package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.dominio.servicios.valores.Roles;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.AlumnoEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.PerfilEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IAlumnoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.ICoordinadorRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IImagenRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IInstructorRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IPerfilRepositorio;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DependenciaFallida;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.PerfilMapper;

@Service
public class PerfilGateway implements IPerfilGateway {

    @Autowired
    private IPerfilRepositorio repoPerfil;

    @Autowired
    private ICoordinadorRepositorio repoCoordinador;

    @Autowired
    private IInstructorRepositorio repoInstructor;
    
    @Autowired
    private IAlumnoRepositorio repoAlumno;

    @Autowired
    private IImagenRepositorio repoImagen;

    @Qualifier("modelMapperGenerico")
    @Autowired
    private ModelMapper mapper;

    @Override
    public boolean existePerfil(String perfilId) {
        return repoPerfil.existsById(perfilId);
    }

    @Override
    public List<Perfil> obtenerPerfiles() {
        Iterable<PerfilEntidad> respuesta = repoPerfil.findAll();
        List<Perfil> perfiles = new ArrayList<>();
        perfiles = mapper.map(respuesta, new TypeToken<List<Perfil>>() {
        }.getType());
        return perfiles;
    }

    @Override
    public Perfil insertarPerfil(Perfil perfil) {
        if(existePerfil(perfil.getId())){
            throw new YaExisteElementoExcepcion("El perfil con la identificacion ya se encuentra registrado");
        }
        if(!repoImagen.existsById(perfil.getImagen())){
            throw new DependenciaFallida("la imgen no se encuentra registrada");
        }
        PerfilEntidad entidadInsertar = PerfilMapper.toEntidad(perfil);
        PerfilEntidad guardado = repoPerfil.save(entidadInsertar);
        return PerfilMapper.toDominio(guardado);
    }

    @Override
    @Transactional
    public Perfil registrarAlumno(Perfil perfil) {
        if (existePerfil(perfil.getId())) {
            throw new YaExisteElementoExcepcion("El perfil con la identificacion ya se encuentra registrado");
        }

        PerfilEntidad entidadPerfil = PerfilMapper.toEntidad(perfil);

        AlumnoEntidad entidadAlumno = new AlumnoEntidad();
        entidadAlumno.setIdPerfil(perfil.getId());
        entidadAlumno.setAlm_codigo(perfil.getAlumnoCodigo());
        entidadAlumno.setTipoAlumno(perfil.getTipoAlumno());
        entidadAlumno.setEliminado(0);

        try {
            PerfilEntidad perfilGuardado = repoPerfil.save(entidadPerfil);
            AlumnoEntidad alumnoGuardado = repoAlumno.save(entidadAlumno);

            if (perfilGuardado == null || alumnoGuardado == null) {
                throw new InternalError("No se ha logrado registrar el alumno");
            }

            Perfil perfilRegistrado = PerfilMapper.toDominio(perfilGuardado);
            perfilRegistrado.setRol(Roles.ALUMNO.getValor());
            perfilRegistrado.setTipoAlumno(alumnoGuardado.getTipoAlumno());
            perfilRegistrado.setFacultad(null);
            return perfilRegistrado;
        } catch (DataIntegrityViolationException e) {
            throw new YaExisteElementoExcepcion("El correo o identificacion ya se encuentra registrado en el sistema");
        }
    }

    @Override
    public Optional<Perfil> obtenerPerfil(String perfilId) {
        Optional<PerfilEntidad> entidadRecuperada = repoPerfil.findById(perfilId);
        return entidadRecuperada.map(perfilEndidad -> mapper.map(perfilEndidad, Perfil.class));
    }

    @Override
    public Perfil actualizarPerfil(String perfilId, Perfil datosPerfil) {
        //confirmar existencia de registro
        if (!existePerfil(perfilId)) {
            throw new NoExisteExcepcion("No existe el perfil con el identificador " + perfilId);
        }
        
        //obtener datos
        PerfilEntidad entidadExistente = repoPerfil.findById(perfilId)
                .orElseThrow(() -> new NoExisteExcepcion("No exoste el perfil"));

        entidadExistente.setPerf_nombre(datosPerfil.getNombre());
        entidadExistente.setPerfcorreo(datosPerfil.getCorreo());
        //solo actualizar imagen en caso de existir dicha informacion
        if(datosPerfil.getImagen()!=null){
            if(!repoImagen.existsById(datosPerfil.getImagen())){
                throw new DependenciaFallida("la imagen a actualizar no existe");
            }
            entidadExistente.setPerf_imagen(datosPerfil.getImagen());
        }
        // solo actualizar tipo e id si vienen informados (null = no modificar)
        if (datosPerfil.getTipoId() != null) {
            entidadExistente.setPerf_tipo(datosPerfil.getTipoId());
        }
        if (datosPerfil.getSexo() != null) {
            entidadExistente.setPerf_Sexo(datosPerfil.getSexo());
        }
        entidadExistente.setEliminado(0);

        // SCRUM-182: UQ_PERFIL_CORREO solo se verifica en el flush. save() normal no
        // lo dispara aqui (quedaria pendiente hasta el commit de la transaccion, fuera
        // de este metodo, donde el catch ya no lo veria) -- saveAndFlush() lo fuerza de
        // forma sincrona para poder capturarlo. Mismo patron que registrarAlumno().
        // Cambiar el correo al mismo que ya tenia (o no tocarlo) nunca viola la
        // restriccion UNIQUE, porque la fila que ya posee ese valor es esta misma.
        try {
            PerfilEntidad perfilActualizado = repoPerfil.saveAndFlush(entidadExistente);
            return mapper.map(perfilActualizado, Perfil.class);
        } catch (DataIntegrityViolationException e) {
            throw new YaExisteElementoExcepcion(
                    "El correo " + datosPerfil.getCorreo() + " ya esta en uso por otro perfil");
        }
    }

    @Override
    public Perfil eliminarPerfil(String perfilId) {
        Optional<PerfilEntidad> entidadExistente = repoPerfil.findById(perfilId);
        if (entidadExistente.isPresent()) {
            PerfilEntidad entidad = entidadExistente.get();
            repoPerfil.delete(entidad);
            return mapper.map(entidad, Perfil.class);
        }
        throw new NoExisteExcepcion("No existe el perfil con el identificador " + perfilId);
    }

    @Override
    public Perfil obtenerUsuario(String email) {
        Optional<PerfilEntidad> perfOpt = repoPerfil.findByPerfcorreo(email);
        if(perfOpt.isPresent()){
            PerfilEntidad entidad = perfOpt.get();
            Perfil perfil = PerfilMapper.toDominio(entidad);
            String id = perfil.getId();
            if(repoCoordinador.existsById(id)){
                perfil.setRol(Roles.ADMINISTRADOR.getValor());
                return perfil;
            }
            if(repoInstructor.existsById(id)){
                perfil.setRol(Roles.INSTRUCTOR.getValor());
                return perfil;
            }
            if(repoAlumno.existsById(id)){
                AlumnoEntidad alumno = repoAlumno.findById(id).orElse(null);
                perfil.setRol(Roles.ALUMNO.getValor());
                if(alumno != null){
                    perfil.setTipoAlumno(alumno.getTipoAlumno());
                }
                perfil.setFacultad(repoAlumno.obtenerFacultadPorPerfilId(id));
                return perfil;
            }
        }
        return null;
    }

}
