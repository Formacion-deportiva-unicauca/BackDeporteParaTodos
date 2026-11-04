package co.edu.unicauca.deporteParaTodos.infraestructura.mappers;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoProcesableEntidadException;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.PerfilEntidad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerfilMapperTest {

    private static final String ID       = "12345678";
    private static final String NOMBRE   = "Juan García";
    private static final String CORREO   = "juan@unicauca.edu.co";
    private static final Integer IMAGEN  = 1;
    private static final String TIPO_ID  = "CC";
    private static final String SEXO     = "M";
    private static final String ROL      = "ALUMNO";
    private static final String FACULTAD = "Ingeniería";
    private static final String TIPO_ALM = "Regular";
    private static final String ALM_COD  = "2025-001";

    private PerfilEntidad entidadBase() {
        PerfilEntidad e = new PerfilEntidad();
        e.setPerf_id(ID);
        e.setPerf_nombre(NOMBRE);
        e.setPerfcorreo(CORREO);
        e.setPerf_imagen(IMAGEN);
        e.setPerf_tipo(TIPO_ID);
        e.setPerf_Sexo(SEXO);
        e.setEliminado(0);
        return e;
    }

    private Perfil perfilBase() {
        Perfil p = new Perfil();
        p.setId(ID);
        p.setNombre(NOMBRE);
        p.setCorreo(CORREO);
        p.setImagen(IMAGEN);
        p.setTipoId(TIPO_ID);
        p.setSexo(SEXO);
        p.setRol(ROL);
        p.setFacultad(FACULTAD);
        p.setTipoAlumno(TIPO_ALM);
        p.setAlumnoCodigo(ALM_COD);
        return p;
    }

    private PerfilDto dtoBase() {
        PerfilDto d = new PerfilDto();
        d.setId(ID);
        d.setNombre(NOMBRE);
        d.setCorreo(CORREO);
        d.setTipoId(TIPO_ID);
        d.setSexo(SEXO);
        d.setRole(ROL);
        d.setFacultad(FACULTAD);
        d.setTipoAlumno(TIPO_ALM);
        d.setAlumnoCodigo(ALM_COD);
        return d;
    }

    // ────────── toDominio ──────────

    @Test
    void toDominio_entidadNula_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> PerfilMapper.toDominio(null));
    }

    @Test
    void toDominio_mapea6CamposDeEntidad() {
        Perfil resultado = PerfilMapper.toDominio(entidadBase());
        assertEquals(ID,      resultado.getId());
        assertEquals(NOMBRE,  resultado.getNombre());
        assertEquals(CORREO,  resultado.getCorreo());   // campo perfcorreo sin guión bajo
        assertEquals(IMAGEN,  resultado.getImagen());
        assertEquals(TIPO_ID, resultado.getTipoId());
        assertEquals(SEXO,    resultado.getSexo());
    }

    @Test
    void toDominio_hardcodea_rol_facultad_tipoAlumno_null() {
        Perfil resultado = PerfilMapper.toDominio(entidadBase());
        assertNull(resultado.getRol());
        assertNull(resultado.getFacultad());
        assertNull(resultado.getTipoAlumno());
    }

    // ────────── toEntidad ──────────

    @Test
    void toEntidad_perfilNulo_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> PerfilMapper.toEntidad(null));
    }

    @Test
    void toEntidad_mapea6CamposAEntidad() {
        PerfilEntidad resultado = PerfilMapper.toEntidad(perfilBase());
        assertEquals(ID,      resultado.getPerf_id());
        assertEquals(NOMBRE,  resultado.getPerf_nombre());
        assertEquals(CORREO,  resultado.getPerfcorreo());   // sin guión bajo
        assertEquals(IMAGEN,  resultado.getPerf_imagen());
        assertEquals(TIPO_ID, resultado.getPerf_tipo());
        assertEquals(SEXO,    resultado.getPerf_Sexo());    // S mayúscula
    }

    @Test
    void toEntidad_hardcodeoEliminadoCero() {
        PerfilEntidad resultado = PerfilMapper.toEntidad(perfilBase());
        assertEquals(0, resultado.getEliminado());
    }

    // ────────── toDto ──────────

    @Test
    void toDto_perfilNulo_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> PerfilMapper.toDto(null));
    }

    @Test
    void toDto_mapeaCamposCorrectos_incluyendoRolARole() {
        PerfilDto resultado = PerfilMapper.toDto(perfilBase());
        assertEquals(ID,      resultado.getId());
        assertEquals(NOMBRE,  resultado.getNombre());
        assertEquals(CORREO,  resultado.getCorreo());
        assertEquals(ROL,     resultado.getRole());     // ASIMÉTRICO: rol → role
        assertEquals(SEXO,    resultado.getSexo());
        assertEquals(TIPO_ID, resultado.getTipoId());
        assertEquals(FACULTAD, resultado.getFacultad());
        assertEquals(TIPO_ALM, resultado.getTipoAlumno());
    }

    @Test
    void toDto_noMapeaAlumnoCodigo() {
        PerfilDto resultado = PerfilMapper.toDto(perfilBase());
        assertNull(resultado.getAlumnoCodigo());
    }

    // ────────── fromDto ──────────

    @Test
    void fromDto_dtoNulo_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> PerfilMapper.fromDto(null));
    }

    @Test
    void fromDto_mapeaCamposCorrectos_incluyendoRoleARol() {
        Perfil resultado = PerfilMapper.fromDto(dtoBase());
        assertEquals(ID,       resultado.getId());
        assertEquals(NOMBRE,   resultado.getNombre());
        assertEquals(CORREO,   resultado.getCorreo());
        assertEquals(TIPO_ID,  resultado.getTipoId());
        assertEquals(SEXO,     resultado.getSexo());
        assertEquals(ROL,      resultado.getRol());     // ASIMÉTRICO: role → rol
        assertEquals(FACULTAD, resultado.getFacultad());
        assertEquals(TIPO_ALM, resultado.getTipoAlumno());
        assertEquals(ALM_COD,  resultado.getAlumnoCodigo());
    }

    @Test
    void fromDto_hardcodea_imagenNull_mapeaAlumnoCodigo() {
        Perfil resultado = PerfilMapper.fromDto(dtoBase());
        assertNull(resultado.getImagen());
        assertEquals(ALM_COD, resultado.getAlumnoCodigo());
    }

    // ── programa y dependencia (perfil por tipo de vínculo) ───────────────────

    @Test
    void fromDto_mapeaProgramaYDependencia() {
        PerfilDto dto = new PerfilDto();
        dto.setId(ID);
        dto.setNombre(NOMBRE);
        dto.setCorreo(CORREO);
        dto.setTipoId(TIPO_ID);
        dto.setSexo(SEXO);
        dto.setTipoAlumno("Administrativo");
        dto.setPrograma("Ingeniería Civil");
        dto.setDependencia("Oficina de Planeación");

        Perfil perfil = PerfilMapper.fromDto(dto);

        assertEquals("Ingeniería Civil", perfil.getPrograma());
        assertEquals("Oficina de Planeación", perfil.getDependencia());
    }

    @Test
    void toDto_mapeaProgramaYDependencia() {
        Perfil perfil = perfilBase();
        perfil.setPrograma("Ingeniería Civil");
        perfil.setDependencia("Oficina de Planeación");

        PerfilDto dto = PerfilMapper.toDto(perfil);

        assertEquals("Ingeniería Civil", dto.getPrograma());
        assertEquals("Oficina de Planeación", dto.getDependencia());
    }
}
