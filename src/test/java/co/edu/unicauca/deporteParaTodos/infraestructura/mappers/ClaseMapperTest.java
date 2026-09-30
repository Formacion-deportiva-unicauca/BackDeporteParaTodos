package co.edu.unicauca.deporteParaTodos.infraestructura.mappers;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoProcesableEntidadException;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ClaseDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ClaseEntidad;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ClaseMapperTest {

    private static final String CATEGORIA  = "Recreativo";
    private static final String CURSO      = "Natacion";
    private static final Integer ANIO      = 2025;
    private static final Integer ITERABLE  = 1;
    private static final LocalDate FECHA   = LocalDate.of(2025, 1, 15);

    private ClaseEntidad entidadBase() {
        ClaseEntidad e = new ClaseEntidad();
        e.setCodigo(10);
        e.setIdGrupoCategoria(CATEGORIA);
        e.setIdGrupoCurso(CURSO);
        e.setIdGrupoAnio(ANIO);
        e.setIdGrupoIterable(ITERABLE);
        e.setIdInstructor("INS001");
        e.setFecha(FECHA);
        e.setHoras(1);
        e.setMinutos(30);
        e.setObservacion("obs");
        e.setEliminado(0);
        return e;
    }

    private Clase claseBase() {
        Clase c = new Clase();
        c.setCodigo(10);
        c.setCategoria(CATEGORIA);
        c.setCurso(CURSO);
        c.setAnio(ANIO);
        c.setIterable(ITERABLE);
        c.setIdInstructor("INS001");
        c.setFecha(FECHA);
        c.setHoras(1);
        c.setMinutos(30);
        c.setObservacion("obs");
        c.setEliminado(0);
        return c;
    }

    // ────────── toDominio ──────────

    @Test
    void toDominio_renombraCamposDeEntidadADominio() {
        // idGrupoCategoria → categoria, idGrupoCurso → curso, idGrupoAnio → anio, idGrupoIterable → iterable
        Clase resultado = ClaseMapper.toDominio(entidadBase());
        assertEquals(CATEGORIA, resultado.getCategoria());
        assertEquals(CURSO,     resultado.getCurso());
        assertEquals(ANIO,      resultado.getAnio());
        assertEquals(ITERABLE,  resultado.getIterable());
    }

    @Test
    void toDominio_preservaCamposDirectos() {
        Clase resultado = ClaseMapper.toDominio(entidadBase());
        assertEquals(10,       resultado.getCodigo());
        assertEquals("INS001", resultado.getIdInstructor());
        assertEquals(FECHA,    resultado.getFecha());
        assertEquals(1,        resultado.getHoras());
        assertEquals(30,       resultado.getMinutos());
        assertEquals(0,        resultado.getEliminado());
    }

    @Test
    void toDominio_entidadNula_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> ClaseMapper.toDominio(null));
    }

    // ────────── toEntidad ──────────

    @Test
    void toEntidad_renombraCamposDeDominioAEntidad() {
        // categoria → idGrupoCategoria, curso → idGrupoCurso, anio → idGrupoAnio, iterable → idGrupoIterable
        ClaseEntidad resultado = ClaseMapper.toEntidad(claseBase());
        assertEquals(CATEGORIA, resultado.getIdGrupoCategoria());
        assertEquals(CURSO,     resultado.getIdGrupoCurso());
        assertEquals(ANIO,      resultado.getIdGrupoAnio());
        assertEquals(ITERABLE,  resultado.getIdGrupoIterable());
    }

    @Test
    void toEntidad_preservaCamposDirectos() {
        ClaseEntidad resultado = ClaseMapper.toEntidad(claseBase());
        assertEquals(10,       resultado.getCodigo());
        assertEquals("INS001", resultado.getIdInstructor());
        assertEquals(FECHA,    resultado.getFecha());
        assertEquals(1,        resultado.getHoras());
        assertEquals(30,       resultado.getMinutos());
    }

    @Test
    void toEntidad_claseNula_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> ClaseMapper.toEntidad(null));
    }

    // ────────── toDto ──────────

    @Test
    void toDto_claseValida_mapeaCamposCorrectamente() {
        ClaseDto resultado = ClaseMapper.toDto(claseBase());
        assertEquals(CATEGORIA, resultado.getCategoria());
        assertEquals(CURSO,     resultado.getCurso());
        assertEquals(ANIO,      resultado.getAnio());
        assertEquals(ITERABLE,  resultado.getIterable());
        assertEquals(10,        resultado.getCodigo());
        assertEquals(FECHA,     resultado.getFecha());
    }

    @Test
    void toDto_claseNula_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> ClaseMapper.toDto(null));
    }

    // ────────── fromDto ──────────

    @Test
    void fromDto_dtoValido_mapeaCamposCorrectamente() {
        ClaseDto dto = new ClaseDto();
        dto.setCodigo(10);
        dto.setCategoria(CATEGORIA);
        dto.setCurso(CURSO);
        dto.setAnio(ANIO);
        dto.setIterable(ITERABLE);
        dto.setIdInstructor("INS001");
        dto.setFecha(FECHA);
        dto.setHoras(1);
        dto.setMinutos(30);

        Clase resultado = ClaseMapper.fromDto(dto);

        assertEquals(CATEGORIA, resultado.getCategoria());
        assertEquals(CURSO,     resultado.getCurso());
        assertEquals(ANIO,      resultado.getAnio());
        assertEquals(10,        resultado.getCodigo());
        assertEquals(FECHA,     resultado.getFecha());
    }

    @Test
    void fromDto_preservaObservacionYEliminado() {
        ClaseDto dto = new ClaseDto();
        dto.setCategoria(CATEGORIA);
        dto.setCurso(CURSO);
        dto.setAnio(ANIO);
        dto.setIterable(ITERABLE);
        dto.setObservacion("nota extra");
        dto.setEliminado(0);

        Clase resultado = ClaseMapper.fromDto(dto);

        assertEquals("nota extra", resultado.getObservacion());
        assertEquals(0,            resultado.getEliminado());
    }

    @Test
    void fromDto_dtoNulo_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> ClaseMapper.fromDto(null));
    }
}
