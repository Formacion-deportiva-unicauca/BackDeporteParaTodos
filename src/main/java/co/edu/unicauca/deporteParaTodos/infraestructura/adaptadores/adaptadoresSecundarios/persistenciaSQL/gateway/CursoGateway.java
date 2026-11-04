package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.ICursoGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.CursoEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.CursoId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.ICategoriaCursoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.ICursoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IDeporteRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IImagenRepositorio;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DependenciaFallida;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.CursoMapper;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoProcesableEntidadException;

@Service
public class CursoGateway implements ICursoGateway{

    @Autowired
    private ICursoRepositorio repoCurso;

    @Autowired
    private IImagenRepositorio repoImagen;

    @Autowired 
    private IDeporteRepositorio repoDeporte;

    @Autowired
    private ICategoriaCursoRepositorio repoCategoria;

    @Qualifier("modelMapperGenerico")
    @Autowired
    private ModelMapper mapper;

    @Override
    public boolean existeCurso(String categoria, String nombreCurso) {
        CursoId id = new CursoId(categoria, nombreCurso);
        return repoCurso.existsById(id);
    }

    @Override
    public List<Curso> obtenerCursos() {
        Iterable<CursoEntidad> respuesta = repoCurso.findAll();
        List<Curso> cursos = new ArrayList<>();
        respuesta.forEach(entidad -> {
            Curso curso = CursoMapper.toDominio(entidad);
            cursos.add(curso);
        });
        return cursos;
    }

    @Override
    public Curso obtenerCurso(String categoria, String nombreCurso) {
        CursoId id = new CursoId(categoria,nombreCurso);
        Optional<CursoEntidad> resultado = repoCurso.findById(id);
        if(resultado.isPresent()){
            CursoEntidad entidad = resultado.get();
            Curso curso = CursoMapper.toDominio(entidad);
            return curso;
        }
        return null;
        
    }

    @Override
    public Curso insertarCurso(Curso curso) {
        CursoEntidad entidad = CursoMapper.toEntidad(curso);
        if (entidad==null) {
            throw new NoProcesableEntidadException("No fue posible convertir de modelo a entidad en ingreso del gateway");
        }
        
        if(!repoCategoria.existsById(entidad.getCategoriaCurso())){
            throw new DependenciaFallida("La categoria especificada no existe");
        }

        if(!repoImagen.existsById(entidad.getObjImagen())){
            throw new DependenciaFallida("fallo insercion curso, imagen adjunta con identificador "+entidad.getObjImagen()+" no existe en el sistema");
        }
        if(!repoDeporte.existsById(entidad.getDeporte())){
            throw new DependenciaFallida("no existe un deporte registrado notado como "+entidad.getDeporte());
        }
        entidad.setEliminado(0);
        CursoEntidad respuesta = repoCurso.save(entidad);
        Curso insertado = CursoMapper.toDominio(respuesta);
        if(insertado==null){
            throw new NoProcesableEntidadException("No fue posible convertir de entidad a modelo en salida del gateway");
        }
        return insertado;
    }

    @Override
    public Curso actualizarCurso(String categoria, String nombre, Curso curso) {
        CursoEntidad entidad = CursoMapper.toEntidad(curso);
        if (entidad==null) {
            throw new NoProcesableEntidadException("No fue posible convertir de modelo a entidad en ingreso del gateway");
        }
        if(!repoCategoria.existsById(entidad.getCategoriaCurso())){
            throw new DependenciaFallida("La categoria especificada no existe");
        }
        if(!repoImagen.existsById(entidad.getObjImagen())){
            throw new DependenciaFallida("fallo actualizacion curso, imagen adjunta con identificador "+entidad.getObjImagen()+" no existe en el sistema");
        }
        if(!repoDeporte.existsById(entidad.getDeporte())){
            throw new DependenciaFallida("no existe un deporte registrado notado como "+entidad.getDeporte());
        }
        CursoId id = new CursoId(categoria,nombre);
        Optional<CursoEntidad> objetivo = repoCurso.findById(id);
        if(!objetivo.isPresent()){
            throw new NoExisteExcepcion("No existe el curso que se desea actualizar");
        }
        CursoEntidad actulizacion = objetivo.get();
        actulizacion.setDeporte(entidad.getDeporte());
        actulizacion.setDescripcion(entidad.getDescripcion());
        actulizacion.setEliminado(0);
        actulizacion.setObjImagen(entidad.getObjImagen());
        actulizacion.setPublico(entidad.getPublico());
        CursoEntidad respuesta = repoCurso.save(actulizacion);
        Curso insertado = CursoMapper.toDominio(respuesta);
        if(insertado==null){
            throw new NoProcesableEntidadException("No fue posible convertir de entidad a modelo en salida del gateway");
        }
        return insertado;
    }

    @Override
    public Curso eliminarCurso(String categoria, String curso) {
        CursoId id = new CursoId(categoria,curso);
        if(!repoCurso.existsById(id)){
            throw new NoExisteExcepcion("gateway no encuentra el objetivo");
        }
        Optional<CursoEntidad> op = repoCurso.findById(id);
        if(op.isEmpty()){
            throw new NoExisteExcepcion("gateway encontro el objetivo en blanco");
        }
        CursoEntidad entidad = op.get();
        entidad.setEliminado(1);
        CursoEntidad respuesta = repoCurso.save(entidad);
        Curso respuestaModelo = CursoMapper.toDominio(respuesta);
        return respuestaModelo;
    }

    @Override
    public List<Curso> obtenerCursosDeCategoria(String nombreCategoria){
        List<CursoEntidad> entidades = repoCurso.findByCategoriaCursoAndEliminado(nombreCategoria, 0);
        List<Curso> cursos = new ArrayList<>();
        entidades.forEach(entidad -> {
            Curso curso = CursoMapper.toDominio(entidad);
            cursos.add(curso);
        });
        return cursos;
    }

    @Override
    public List<Curso> obtenerTodosCursosDeCategoria(String nombreCategoria) {
        List<CursoEntidad> entidades = repoCurso.findByCategoriaCurso(nombreCategoria);
        List<Curso> cursos = new ArrayList<>();
        entidades.forEach(entidad -> {
            Curso curso = CursoMapper.toDominio(entidad);
            cursos.add(curso);
        });
        return cursos;
    }

    @Override
    public Curso cambiarEstadoCurso(String categoria, String nombreCurso, EstadoCurso estado) {
        CursoId id = new CursoId(categoria, nombreCurso);
        Optional<CursoEntidad> op = repoCurso.findById(id);
        if (op.isEmpty()) {
            throw new NoExisteExcepcion("gateway no encuentra el curso a cambiar estado");
        }
        CursoEntidad entidad = op.get();
        entidad.setEliminado(EstadoCurso.INACTIVO.equals(estado) ? 1 : 0);
        CursoEntidad respuesta = repoCurso.save(entidad);
        return CursoMapper.toDominio(respuesta);
    }

    @Override
    public Curso eliminarCursoPermanente(String categoria, String nombreCurso) {
        CursoId id = new CursoId(categoria, nombreCurso);
        Optional<CursoEntidad> op = repoCurso.findById(id);
        if (op.isEmpty()) {
            throw new NoExisteExcepcion("gateway no encuentra el curso a eliminar");
        }
        CursoEntidad entidad = op.get();
        entidad.setEliminado(1);
        CursoEntidad respuesta = repoCurso.save(entidad);
        return CursoMapper.toDominio(respuesta);
    }
}
