package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAlumnoGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.AlumnoEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IAlumnoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IPerfilRepositorio;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;

@Service
public class AlumnoGateway implements IAlumnoGateway {

    @Autowired
    private IAlumnoRepositorio repoAlumno;

    @Autowired
    private IPerfilRepositorio repoPerfil;

    @Qualifier("modelMapperGenerico")
    @Autowired
    private ModelMapper mapper;

    @Override
    public boolean existeAlumno(String alumnoId) {
        return repoAlumno.existsById(alumnoId);
    }

    @Override
    public List<Alumno> obtenerAlumnos() {
        Iterable<AlumnoEntidad> entidades = repoAlumno.findAll();
        List<Alumno> alumnos = new ArrayList<>();
        alumnos = mapper.map(entidades, new TypeToken<List<Alumno>>() {
        }.getType());
        return alumnos;
    }

    @Override
    public List<Alumno> obtenerAlumnosGrupo(String categoria, String curso, Integer anio, Integer iterable) {
        List<Object[]> resultados = repoAlumno.buscarAlumnosGrupoRaw(categoria, curso, anio, iterable, 0);
        List<Alumno> alumnos = new ArrayList<>();
        resultados.forEach(registro -> {
            Alumno alumno = construirAlumnoDesdeRegistro(registro);
            if (alumno != null) {
                alumnos.add(alumno);
            }
        });
        return alumnos;
    }

    @Override
    public Alumno insertAlumno(Alumno datosAlumno) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'insertAlumno'");
    }

    @Override
    public Optional<Alumno> obtenerAlumno(String alumnoId) {
        if (existeAlumno(alumnoId)) {
            Object[] registro = repoAlumno.buscarAlumnoPorIdRaw(alumnoId);
            // Con la base real, una consulta nativa que retorna Object[] entrega a veces la fila envuelta en otro arreglo
            // de una sola posicion (Object[]{ Object[]{columnas} }): se desenvuelve para leer las columnas.
            if (registro != null && registro.length == 1 && registro[0] instanceof Object[] fila) {
                registro = fila;
            }
            Alumno alumno = construirAlumnoDesdeRegistro(registro);
            return Optional.ofNullable(alumno);
        }
        return Optional.empty();
    }

    @Override
    public Alumno actualizarAlumno(String alumnoId, Alumno datosAlumno) {
        // Actualizar alm_tipo en tbl_alumno
        repoAlumno.actualizarTipoAlumno(alumnoId, datosAlumno.getTipoAlumno());
        // Construir la respuesta desde los datos en memoria para evitar el
        // ArrayIndexOutOfBoundsException que produce Hibernate al releer con
        // native query dentro de una transacción @Transactional activa.
        // Los 3 campos editables (nombre, correo, tipoAlumno) ya están en datosAlumno.
        Perfil perfil = datosAlumno.getPerfil();
        perfil.setId(alumnoId); // necesario para que AlumnoDto serialice el id
        Alumno resultado = new Alumno();
        resultado.setTipoAlumno(datosAlumno.getTipoAlumno());
        resultado.setPerfil(perfil);
        return resultado;
    }

    @Override
    public Alumno eliminarAlumno(String alumnoId) {
        // Borrado lógico: META_ELIMINADO=1 en tbl_alumno y tbl_perfil
        // NUNCA DELETE físico — las FK tienen ON DELETE SET NULL (tablas hijas no se tocan)
        repoAlumno.eliminarAlumnoLogico(alumnoId);
        repoPerfil.findById(alumnoId).ifPresent(perfil -> {
            perfil.setEliminado(1);
            repoPerfil.save(perfil);
        });
        // Retornar el estado final del alumno (con meta_eliminado=1)
        Object[] registro = repoAlumno.buscarAlumnoPorIdRaw(alumnoId);
        Alumno alumnoEliminado = construirAlumnoDesdeRegistro(registro);
        if (alumnoEliminado == null) {
            throw new NoExisteExcepcion("No se pudo recuperar el alumno eliminado con id " + alumnoId);
        }
        return alumnoEliminado;
    }

    private Alumno construirAlumnoDesdeRegistro(Object[] registro) {
        if (registro == null) {
            return null;
        }
        Perfil perfil = new Perfil();
        perfil.setId((String) registro[1]);
        perfil.setNombre((String) registro[4]);
        perfil.setCorreo((String) registro[5]);
        perfil.setSexo((String) registro[6]);
        perfil.setTipoId((String) registro[7]);
        perfil.setImagen(registro[8] != null ? ((Number) registro[8]).intValue() : null);

        Alumno alumno = new Alumno();
        alumno.setEliminadoestado(registro[0] != null ? ((Number) registro[0]).intValue() : null);
        alumno.setAlm_codigo((String) registro[2]);
        alumno.setTipoAlumno((String) registro[3]);
        alumno.setPerfil(perfil);
        return alumno;
    }

}
