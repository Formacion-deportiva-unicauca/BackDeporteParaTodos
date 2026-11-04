package co.edu.unicauca.deporteParaTodos.infraestructura.mappers;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PublicoCurso;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.CursoDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.CursoEntidad;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoProcesableEntidadException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CursoMapperTest {

    private CursoEntidad entidadBase() {
        CursoEntidad e = new CursoEntidad();
        e.setNombre("Natacion");
        e.setCategoriaCurso("Acuatico");
        e.setDescripcion("Descripcion");
        e.setDeporte("Natacion");
        e.setObjImagen(1);
        return e;
    }

    private Curso cursoBase() {
        Curso c = new Curso();
        c.setNombre("Natacion");
        c.setCategoriaCurso("Acuatico");
        c.setDescripcion("Descripcion");
        c.setDeporte("Natacion");
        c.setImagenId(1);
        c.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
        return c;
    }

    private CursoDto dtoBase() {
        CursoDto dto = new CursoDto();
        dto.setNombre("Natacion");
        dto.setCategoriaCurso("Acuatico");
        dto.setDescripcion("Descripcion");
        dto.setDeporte("Natacion");
        dto.setIdImagen(1);
        dto.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
        return dto;
    }

    // ── toDominio ──────────────────────────────────────────────────────────────

    @Test
    void toDominio_eliminadoCero_debeMapearseComoActivo() {
        CursoEntidad entidad = entidadBase();
        entidad.setEliminado(0);

        Curso curso = CursoMapper.toDominio(entidad);

        assertEquals(EstadoCurso.ACTIVO, curso.getEstadoCurso());
    }

    @Test
    void toDominio_eliminadoUno_debeMapearseComoInactivo() {
        CursoEntidad entidad = entidadBase();
        entidad.setEliminado(1);

        Curso curso = CursoMapper.toDominio(entidad);

        assertEquals(EstadoCurso.INACTIVO, curso.getEstadoCurso());
    }

    @Test
    void toDominio_eliminadoNulo_debeMapearseComoActivo() {
        CursoEntidad entidad = entidadBase();
        entidad.setEliminado(null);

        Curso curso = CursoMapper.toDominio(entidad);

        assertEquals(EstadoCurso.ACTIVO, curso.getEstadoCurso());
    }

    @Test
    void toDominio_estadoInscripcionesNulo_defaultAbierto() {
        CursoEntidad entidad = entidadBase();
        entidad.setEliminado(0);
        entidad.setEstadoInscripciones(null);

        Curso curso = CursoMapper.toDominio(entidad);

        assertEquals(EstadoInscripciones.ABIERTO, curso.getEstadoInscripciones());
    }

    // ── toEntidad ──────────────────────────────────────────────────────────────

    @Test
    void toEntidad_mapeaCamposCorrectamente() {
        Curso curso = cursoBase();

        CursoEntidad entidad = CursoMapper.toEntidad(curso);

        assertNotNull(entidad);
        assertEquals("Natacion", entidad.getNombre());
        assertEquals("Acuatico", entidad.getCategoriaCurso());
        assertEquals("Descripcion", entidad.getDescripcion());
        assertEquals("Natacion", entidad.getDeporte());
        assertEquals(Integer.valueOf(1), entidad.getObjImagen());
        assertEquals("ABIERTO", entidad.getEstadoInscripciones());
    }

    @Test
    void toEntidad_estadoInscripcionesNulo_defaultAbierto() {
        Curso curso = cursoBase();
        curso.setEstadoInscripciones(null);

        CursoEntidad entidad = CursoMapper.toEntidad(curso);

        assertEquals("ABIERTO", entidad.getEstadoInscripciones());
    }

    // ── toDto ──────────────────────────────────────────────────────────────────

    @Test
    void toDto_mapeaCamposCorrectamente() {
        Curso curso = cursoBase();
        curso.setEstadoCurso(EstadoCurso.ACTIVO);

        CursoDto dto = CursoMapper.toDto(curso);

        assertNotNull(dto);
        assertEquals("Natacion", dto.getNombre());
        assertEquals("Acuatico", dto.getCategoriaCurso());
        assertEquals("Descripcion", dto.getDescripcion());
        assertEquals("Natacion", dto.getDeporte());
        assertEquals(Integer.valueOf(1), dto.getIdImagen());
        assertEquals(EstadoCurso.ACTIVO, dto.getEstadoCurso());
        assertEquals(EstadoInscripciones.ABIERTO, dto.getEstadoInscripciones());
    }

    // ── fromDto ────────────────────────────────────────────────────────────────

    @Test
    void fromDto_mapeaCamposCorrectamente() {
        CursoDto dto = dtoBase();

        Curso curso = CursoMapper.fromDto(dto);

        assertNotNull(curso);
        assertEquals("Natacion", curso.getNombre());
        assertEquals("Acuatico", curso.getCategoriaCurso());
        assertEquals("Descripcion", curso.getDescripcion());
        assertEquals("Natacion", curso.getDeporte());
        assertEquals(Integer.valueOf(1), curso.getImagenId());
        assertEquals(EstadoInscripciones.ABIERTO, curso.getEstadoInscripciones());
    }

    @Test
    void fromDto_nulo_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> CursoMapper.fromDto(null));
    }

    // ── catch blocks: throw NoProcesableEntidadException ─────────────────────

    @Test
    void toDominio_entidadNula_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> CursoMapper.toDominio(null));
    }

    @Test
    void toEntidad_cursoNulo_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> CursoMapper.toEntidad(null));
    }

    @Test
    void toDto_cursoNulo_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> CursoMapper.toDto(null));
    }

    // ── publico del curso (ESTUDIANTE, FUNCIONARIO, MIXTO) ────────────────────

    @Test
    void toDominio_publicoNuloEnLaBase_seTomaComoMixto() {
        CursoEntidad e = entidadBase();
        e.setPublico(null);

        assertEquals(PublicoCurso.MIXTO, CursoMapper.toDominio(e).getPublico());
    }

    @Test
    void toDominio_mapeaElPublicoGuardado() {
        CursoEntidad e = entidadBase();
        e.setPublico("FUNCIONARIO");

        assertEquals(PublicoCurso.FUNCIONARIO, CursoMapper.toDominio(e).getPublico());
    }

    @Test
    void toDominio_publicoDesconocidoEnLaBase_lanzaNoProcesable() {
        CursoEntidad e = entidadBase();
        e.setPublico("DOCENTES");

        assertThrows(NoProcesableEntidadException.class, () -> CursoMapper.toDominio(e));
    }

    @Test
    void toEntidad_publicoNuloSeGuardaComoMixto() {
        Curso c = cursoBase();
        c.setPublico(null);

        assertEquals("MIXTO", CursoMapper.toEntidad(c).getPublico());
    }

    @Test
    void toEntidad_guardaElPublicoElegido() {
        Curso c = cursoBase();
        c.setPublico(PublicoCurso.ESTUDIANTE);

        assertEquals("ESTUDIANTE", CursoMapper.toEntidad(c).getPublico());
    }

    @Test
    void toDto_incluyeElPublico() {
        Curso c = cursoBase();
        c.setPublico(PublicoCurso.FUNCIONARIO);

        assertEquals(PublicoCurso.FUNCIONARIO, CursoMapper.toDto(c).getPublico());
    }

    @Test
    void fromDto_mapeaElPublico() {
        CursoDto dto = dtoBase();
        dto.setPublico(PublicoCurso.ESTUDIANTE);

        assertEquals(PublicoCurso.ESTUDIANTE, CursoMapper.fromDto(dto).getPublico());
    }

    @Test
    void fromDto_sinPublico_quedaNuloYSeGuardaraComoMixto() {
        CursoDto dto = dtoBase();
        dto.setPublico(null);

        Curso c = CursoMapper.fromDto(dto);

        assertEquals(null, c.getPublico());
        assertEquals("MIXTO", CursoMapper.toEntidad(c).getPublico());
    }
}
