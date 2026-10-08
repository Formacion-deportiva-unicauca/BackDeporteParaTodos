package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.GrupoEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.CursoId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.GrupoId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.ICategoriaCursoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.ICursoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IGrupoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IImagenRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IInstructorRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.GrupoMapper;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DependenciaFallida;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.InsercionFallidaExepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;

@Service
public class GrupoGateway implements IGrupoGateway {

    @Autowired
    private IGrupoRepositorio repoGrupo;

    @Autowired
    private IImagenRepositorio repoImagen;

    @Autowired
    private ICursoRepositorio repoCurso;

    @Autowired
    private ICategoriaCursoRepositorio repoCategoria;

    @Autowired
    private IInstructorRepositorio repoInstructor;

    public boolean existeGrupo(String categoria, String curso, Integer anio, Integer secuencial) {
        GrupoId id = new GrupoId(categoria, curso, anio, secuencial);
        return repoGrupo.existsById(id);
    }

    public List<Grupo> obtenerTodosGrupos() {
        return repoGrupo.findByEliminado(0).stream()
                .map(GrupoMapper::toDominio)
                .collect(Collectors.toList());
    }

    public List<Grupo> obtenerGruposDisponibles() {
        return repoGrupo.findByEliminado(0).stream()
                .map(GrupoMapper::toDominio)
                .collect(Collectors.toList());
    }

    public List<Grupo> obtenerGruposDeCurso(String categoria, String curso) {
        return repoGrupo.findByCategoriaAndCursoAndEliminado(categoria, curso, 0).stream()
                .map(GrupoMapper::toDominio)
                .collect(Collectors.toList());
    }

    public List<Grupo> obtenerGruposInscripcionDisponible() {
        return repoGrupo.obtenerGruposConInscripcionDisponibleNativo().stream()
                .map(GrupoMapper::toDominio)
                .collect(Collectors.toList());
    }

    public List<Grupo> obtenerGruposInstructor(String idInstructor) {
        return repoGrupo.obtenerGruposPorInstructor(idInstructor).stream()
                .map(GrupoMapper::toDominio)
                .collect(Collectors.toList());
    }

    public Grupo insertarGrupo(Grupo datosGrupo) {
        if (!repoCategoria.existsById(datosGrupo.getCategoria())) {
            throw new NoExisteExcepcion("la categoria a la que intenta insertar un grupo no existe");
        }
        CursoId cursoId = new CursoId(datosGrupo.getCategoria(), datosGrupo.getCurso());
        if (!repoCurso.existsById(cursoId)) {
            throw new NoExisteExcepcion("el curso al que intenta insertar un nuevo grupo no existe");
        }
        // fuente única: fechaInscripcionApertura si existe, si no LocalDate.now()
        LocalDate fechaRef = datosGrupo.getFechaInscripcionApertura() != null
                ? datosGrupo.getFechaInscripcionApertura()
                : LocalDate.now();
        Integer anio = fechaRef.getYear();
        datosGrupo.setAnio(anio);
        Integer iterable = repoGrupo.countByCategoriaAndCursoAndAnio(datosGrupo.getCategoria(), datosGrupo.getCurso(), anio) + 1;
        datosGrupo.setIterable(iterable);
        GrupoId id = new GrupoId(datosGrupo.getCategoria(), datosGrupo.getCurso(), anio, iterable);
        if (repoGrupo.existsById(id)) {
            throw new YaExisteElementoExcepcion("el grupo especificado ya existe en el sistema");
        }
        if (!repoImagen.existsById(datosGrupo.getImagenGrupo())) {
            throw new DependenciaFallida("la imagen identificado con " + datosGrupo.getImagenGrupo() + " no existe en el sistema");
        }
        if (datosGrupo.getIdInstructor() != null) {
            if (!repoInstructor.existsByIdPerfilAndEliminado(datosGrupo.getIdInstructor(), 0)) {
                throw new DependenciaFallida("el instructor identificado con " + datosGrupo.getIdInstructor() + " no existe en el sistema");
            }
        }
        if (datosGrupo.getPeriodo() <= 0) {
            datosGrupo.setPeriodo(fechaRef.getMonthValue() >= 7 ? 2 : 1);
        }
        GrupoEntidad entidad = GrupoMapper.toEntidad(datosGrupo);
        try {
            entidad.setEliminado(0);
            System.out.println(entidad.getCategoria() + "-" + entidad.getCurso() + "-" + entidad.getAnio() + "-" + entidad.getIterable() + "-" + entidad.getCupos() + "-" + entidad.getIdInstructor() + "-" + entidad.getImagenGrupo() + "-" + entidad.getFechaCreacion() + "-" + entidad.getFechaFinalizacion() + "-" + entidad.getEliminado());
            GrupoEntidad guardado = repoGrupo.save(entidad);
            return GrupoMapper.toDominio(guardado);
        } catch (Exception e) {
            throw new InsercionFallidaExepcion("no se ha logrado insertar en gateway");
        }
    }

    public Grupo obtenerGrupoPorId(String categoria, String curso, Integer anio, Integer iterable) {
        GrupoId id = new GrupoId(categoria, curso, anio, iterable);
        GrupoEntidad entidad = repoGrupo.findById(id)
                .orElseThrow(() -> new NoExisteExcepcion("El elemento objetivo no existe en el sistema"));
        return GrupoMapper.toDominio(entidad);
    }

    public Grupo actualizarGrupo(String categoria, String curso, Integer anio, Integer iterable, Grupo datosGrupo) {
        GrupoId id = new GrupoId(categoria, curso, anio, iterable);
        // imagenGrupo no enviada (null) => se conserva la imagen ya asignada al grupo.
        if (datosGrupo.getImagenGrupo() != null && !repoImagen.existsById(datosGrupo.getImagenGrupo())) {
            throw new DependenciaFallida("la imagen identificado con " + datosGrupo.getImagenGrupo() + " no existe en el sistema");
        }
        // idInstructor null => se quita el instructor del grupo (mismo criterio que insertarGrupo()).
        if (datosGrupo.getIdInstructor() != null
                && !repoInstructor.existsByIdPerfilAndEliminado(datosGrupo.getIdInstructor(), 0)) {
            throw new DependenciaFallida("el instructor identificado con " + datosGrupo.getIdInstructor() + " no existe en el sistema");
        }
        GrupoEntidad entidad = repoGrupo.findById(id)
                .orElseThrow(() -> new NoExisteExcepcion("el grupo no se encuentra registrado en el sistema"));
        // cupos/fechaCreacion/fechaFinalizacion no enviados (null) => se conserva el valor
        // actual del grupo, igual que imagenGrupo -- antes se sobreescribian con null.
        if (datosGrupo.getCupos() != null) {
            entidad.setCupos(datosGrupo.getCupos());
        }
        entidad.setEliminado(0);
        if (datosGrupo.getFechaCreacion() != null) {
            entidad.setFechaCreacion(datosGrupo.getFechaCreacion());
        }
        if (datosGrupo.getFechaFinalizacion() != null) {
            entidad.setFechaFinalizacion(datosGrupo.getFechaFinalizacion());
        }
        // fechaInscripcionApertura/fechaIncripcionCierre no enviadas (null) => se
        // conservan, mismo criterio que fechaCreacion/fechaFinalizacion arriba. Antes
        // no se leian en absoluto y el PUT las descartaba en silencio.
        // Nota: quitar una fecha de cierre ya asignada (volverla null) vía PUT queda
        // fuera de alcance -- mismo límite que ya existe hoy para cupos/fechaCreacion/
        // fechaFinalizacion.
        if (datosGrupo.getFechaInscripcionApertura() != null) {
            entidad.setFechaInscripcionApertura(datosGrupo.getFechaInscripcionApertura());
        }
        if (datosGrupo.getFechaIncripcionCierre() != null) {
            entidad.setFechaIncripcionCierre(datosGrupo.getFechaIncripcionCierre());
        }
        entidad.setIdInstructor(datosGrupo.getIdInstructor());
        if (datosGrupo.getImagenGrupo() != null) {
            entidad.setImagenGrupo(datosGrupo.getImagenGrupo());
        }
        try {
            GrupoEntidad guardado = repoGrupo.save(entidad);
            return GrupoMapper.toDominio(guardado);
        } catch (Exception e) {
            throw new InsercionFallidaExepcion("No se ha logrado realizar el registro en gateway");
        }
    }

    public Grupo eliminarGrupo(String categoria, String curso, Integer anio, Integer iterable) {
        GrupoId id = new GrupoId(categoria, curso, anio, iterable);
        GrupoEntidad entidad = repoGrupo.findById(id)
                .orElseThrow(() -> new NoExisteExcepcion("La entidad objetivo no existe"));
        entidad.setEliminado(1);
        GrupoEntidad respuesta = repoGrupo.save(entidad);
        return GrupoMapper.toDominio(respuesta);
    }

    @Override
    public boolean existeGrupoEliminado(String categoria, String curso, Integer anio, Integer iterable) {
        GrupoId id = new GrupoId(categoria, curso, anio, iterable);
        return repoGrupo.findById(id)
                .map(entidad -> Integer.valueOf(1).equals(entidad.getEliminado()))
                .orElse(false);
    }

    @Override
    public Grupo obtenerGrupo(String categoria, String curso, Integer anio, Integer iterable) {
        GrupoId id = new GrupoId(categoria, curso, anio, iterable);
        GrupoEntidad entidad = repoGrupo.findById(id)
                .orElseThrow(() -> new NoExisteExcepcion("el grupo buscado no existe en el sistema"));
        return GrupoMapper.toDominio(entidad);
    }

    @Override
    public Grupo obtenerGrupoConLock(String categoria, String curso, Integer anio, Integer iterable) {
        GrupoEntidad entidad = repoGrupo.findByIdWithLock(categoria, curso, anio, iterable)
                .orElseThrow(() -> new NoExisteExcepcion("el grupo buscado no existe en el sistema"));
        return GrupoMapper.toDominio(entidad);
    }
}
