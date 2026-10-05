package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionEnEspera;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.InscripcionEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.InscripcionId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IInscripcionRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.InscripcionMapper;

@Service
public class InscripcionGateway implements IInscripcionGateway {

    private final IInscripcionRepositorio repoInscrp;

    public InscripcionGateway(IInscripcionRepositorio repoInscrp) {
        this.repoInscrp = repoInscrp;
    }

    @Override
    public boolean existeInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable) {
        InscripcionId id = new InscripcionId(categoria, curso, anio, iterable, alumnoId);
        return repoInscrp.existsById(id);
    }

    @Override
    public boolean existeInscripcionSinDesvincular(String alumnoId, String categoria, String curso, int anio, int iterable) {
        return repoInscrp.existeInscripcionSinDesvincular(alumnoId, categoria, curso, anio, iterable);
    }

    @Override
    public boolean existeInscripcionActiva(String alumnoId, String categoria, String curso, int anio, int iterable) {
        return repoInscrp.existeInscripcionActiva(alumnoId, categoria, curso, anio, iterable);
    }

    @Override
    public boolean existeEnEspera(String alumnoId, String categoria, String curso, int anio, int iterable) {
        return repoInscrp.existeEnEspera(alumnoId, categoria, curso, anio, iterable);
    }

    @Override
    public Inscripcion obtenerInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable) {
        InscripcionId id = new InscripcionId(categoria, curso, anio, iterable, alumnoId);
        Optional<InscripcionEntidad> op = repoInscrp.findById(id);
        if (op.isEmpty()) {
            throw new NoExisteExcepcion("La inscripcion buscada no existe");
        }
        return InscripcionMapper.toDominio(op.get());
    }

    @Override
    public Inscripcion guardarInscripcion(Inscripcion inscripcion) {
        InscripcionEntidad entidad = InscripcionMapper.toEntidad(inscripcion);
        InscripcionEntidad guardada = repoInscrp.save(entidad);
        return InscripcionMapper.toDominio(guardada);
    }

    @Override
    public long contarInscripcionesActivasGrupo(String categoria, String curso, int anio, int iterable) {
        return repoInscrp.contarInscripcionesActivasGrupo(categoria, curso, anio, iterable);
    }

    @Override
    public long contarCursosActivosAlumno(String alumnoId) {
        return repoInscrp.contarCursosActivosAlumno(alumnoId);
    }

    @Override
    public long contarEnEsperaGrupo(String categoria, String curso, int anio, int iterable) {
        return repoInscrp.contarEnEsperaGrupo(categoria, curso, anio, iterable);
    }

    @Override
    public Inscripcion desvincularInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable) {
        InscripcionId id = new InscripcionId(categoria, curso, anio, iterable, alumnoId);
        Optional<InscripcionEntidad> op = repoInscrp.findById(id);
        if (op.isEmpty()) {
            throw new NoExisteExcepcion("La inscripcion a desvincular no existe");
        }
        InscripcionEntidad entidad = op.get();
        // 'estado' se deja intencionalmente SIN actualizar aqui: queda como registro
        // historico del rol que tenia la inscripcion (INSCRITO/EN_ESPERA) antes de
        // desvincularse. La fuente de verdad de "esta activa" es fechaDesvinculacion
        // IS NULL, que es lo que verifican todas las queries de conteo/activos de
        // IInscripcionRepositorio. No "corregir" este campo sin auditar y ajustar
        // tambien esas queries (y el guard existeInscripcionSinDesvincular).
        entidad.setFechaDesvinculacion(Timestamp.from(Instant.now()));
        InscripcionEntidad guardada = repoInscrp.save(entidad);
        return InscripcionMapper.toDominio(guardada);
    }

    @Override
    public void promoverPrimeroEnEspera(String categoria, String curso, int anio, int iterable) {
        List<InscripcionEntidad> enEspera = repoInscrp.findEnEsperaOrdenados(categoria, curso, anio, iterable);
        if (!enEspera.isEmpty()) {
            InscripcionEntidad primero = enEspera.get(0);
            primero.setEstado("INSCRITO");
            primero.setFechaInscripcion(Timestamp.from(Instant.now()));
            repoInscrp.save(primero);
        }
    }

    @Override
    public Inscripcion promoverInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable) {
        InscripcionId id = new InscripcionId(categoria, curso, anio, iterable, alumnoId);
        InscripcionEntidad entidad = repoInscrp.findById(id)
                .orElseThrow(() -> new NoExisteExcepcion("La inscripcion no existe"));
        entidad.setEstado("INSCRITO");
        entidad.setFechaInscripcion(Timestamp.from(Instant.now()));
        InscripcionEntidad guardada = repoInscrp.save(entidad);
        return InscripcionMapper.toDominio(guardada);
    }

    @Override
    public List<InscripcionEnEspera> listarEnEspera(String categoria, String curso, int anio, int iterable) {
        return repoInscrp.findEnEsperaConPerfil(categoria, curso, anio, iterable).stream()
                .map(row -> new InscripcionEnEspera(
                        (String) row[0],
                        (String) row[1],
                        (String) row[2],
                        (Timestamp) row[3]))
                .collect(Collectors.toList());
    }

    @Override
    public List<Inscripcion> obtenerInscripcionesAlumno(String alumnoId) {
        return repoInscrp.findMisCursos(alumnoId).stream()
                .map(InscripcionMapper::toDominio)
                .collect(Collectors.toList());
    }
}
