package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICategoriaCursoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICursoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IGrupoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IImagenServicio;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Categoria;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;

/**
 * Test de integracion (BD MySQL real, via HTTP con MockMvc) para PUT /api/v2/grupo.
 * Fixtures via los servicios reales de la app, no SQL directo -- el grupo arranca
 * CON un instructor asignado (seed de DDL-MYSQL.sql) para poder probar que quitarlo
 * (idInstructor null) deja PERF_ID en NULL de verdad en la BD.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class GrupoIT {

    private static final String CATEGORIA = "Deportes";
    private static final String CURSO = "GrupoIT";
    private static final int ANIO = LocalDate.now().getYear();
    private static final int ITERABLE = 1;
    private static final String DEPORTE_SEED = "Natacion";
    // Instructor ya insertado por el seed de DDL-MYSQL.sql.
    private static final String INSTRUCTOR_SEED_ID = "2";

    @Autowired
    private ICategoriaCursoServicio categoriaCursoServicio;
    @Autowired
    private ICursoServicio cursoServicio;
    @Autowired
    private IGrupoServicio grupoServicio;
    @Autowired
    private IImagenServicio imagenServicio;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private Integer imagenId;

    @BeforeAll
    void crearFixtures() {
        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-grupoit.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {9, 9, 9}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para GrupoIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        try {
            Curso curso = new Curso();
            curso.setNombre(CURSO);
            curso.setCategoriaCurso(CATEGORIA);
            curso.setDescripcion("Curso de prueba para GrupoIT");
            curso.setImagenId(imagenId);
            curso.setDeporte(DEPORTE_SEED);
            curso.setEstadoCurso(EstadoCurso.ACTIVO);
            curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
            cursoServicio.insertarCurso(curso);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior contra BD persistente; se reutiliza.
        }

        try {
            grupoServicio.obtenerGrupoPorId(CATEGORIA, CURSO, ANIO, ITERABLE);
        } catch (NoExisteExcepcion e) {
            Grupo grupo = new Grupo();
            grupo.setCategoria(CATEGORIA);
            grupo.setCurso(CURSO);
            grupo.setAnio(ANIO);
            grupo.setIterable(ITERABLE);
            grupo.setImagenGrupo(imagenId);
            grupo.setIdInstructor(INSTRUCTOR_SEED_ID); // arranca CON instructor asignado
            grupo.setCupos(1);
            grupo.setFechaCreacion(LocalDate.now());
            grupo.setPeriodo(1);
            grupoServicio.insertarGrupo(grupo);
        }
    }

    private String buildJwt(String rol, String perfId) {
        byte[] keyBytes = Base64.getDecoder().decode("dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=");
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(new OctetSequenceKey.Builder(key).build()));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(perfId + "@it.unicauca.edu.co")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("rol", rol)
                .claim("perf_id", perfId)
                .build();
        return new NimbusJwtEncoder(source)
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    void putGrupo_instructorNull_dejaPerfIdNullEnBd() throws Exception {
        String body = "{\"categoria\":\"" + CATEGORIA + "\",\"curso\":\"" + CURSO
                + "\",\"imagenGrupo\":" + imagenId + ",\"cupos\":1,\"idInstructor\":null,"
                + "\"fechaCreacion\":\"" + LocalDate.now() + "\"}";

        mockMvc.perform(put("/api/v2/grupo")
                .param("categoria", CATEGORIA).param("curso", CURSO)
                .param("anio", String.valueOf(ANIO)).param("iterable", String.valueOf(ITERABLE))
                .contentType("application/json")
                .content(body)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk());

        String perfId = jdbc.queryForObject(
                "SELECT PERF_ID FROM tbl_grupo WHERE CAT_TITULO = ? AND CUR_NOMBRE = ? "
                        + "AND GRP_ANIO = ? AND GRP_ITERABLE = ?",
                String.class, CATEGORIA, CURSO, ANIO, ITERABLE);
        assertNull(perfId, "PERF_ID debe quedar NULL en la BD tras quitar el instructor");
    }

    // Grupo propio por test (iterable asignado por insertarGrupo()) para no interferir
    // con el grupo ITERABLE=1 que usa putGrupo_instructorNull_dejaPerfIdNullEnBd --
    // eliminarGrupo() no tiene reversa, asi que borrarlo dejaria ese otro test sin fixture.
    private int crearGrupoFresco() {
        Grupo grupo = new Grupo();
        grupo.setCategoria(CATEGORIA);
        grupo.setCurso(CURSO);
        grupo.setImagenGrupo(imagenId);
        grupo.setIdInstructor(INSTRUCTOR_SEED_ID);
        grupo.setCupos(1);
        grupo.setFechaCreacion(LocalDate.now());
        grupo.setPeriodo(1);
        return grupoServicio.insertarGrupo(grupo).getIterable();
    }

    private boolean apareceEnGruposDisponibles(int iterable) {
        return grupoServicio.obtenerGruposDisponibles().stream()
                .anyMatch(g -> CATEGORIA.equals(g.getCategoria()) && CURSO.equals(g.getCurso())
                        && g.getIterable() == iterable);
    }

    @Test
    void deleteGrupo_marcaEliminadoYDesaparaceDeGruposDisponibles() throws Exception {
        int iterable = crearGrupoFresco();
        assertTrue(apareceEnGruposDisponibles(iterable),
                "El grupo recien creado debe aparecer en obtenerGruposDisponibles() antes del DELETE");

        mockMvc.perform(delete("/api/v2/grupo")
                .param("categoria", CATEGORIA).param("curso", CURSO)
                .param("anio", String.valueOf(ANIO)).param("iterable", String.valueOf(iterable))
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk());

        assertFalse(apareceEnGruposDisponibles(iterable),
                "El grupo eliminado no debe aparecer en obtenerGruposDisponibles() -- sin filtro adicional");
    }

    @Test
    void deleteGrupo_segundaVezSobreElMismoGrupo_retorna409() throws Exception {
        int iterable = crearGrupoFresco();

        mockMvc.perform(delete("/api/v2/grupo")
                .param("categoria", CATEGORIA).param("curso", CURSO)
                .param("anio", String.valueOf(ANIO)).param("iterable", String.valueOf(iterable))
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v2/grupo")
                .param("categoria", CATEGORIA).param("curso", CURSO)
                .param("anio", String.valueOf(ANIO)).param("iterable", String.valueOf(iterable))
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isConflict());
    }
}
