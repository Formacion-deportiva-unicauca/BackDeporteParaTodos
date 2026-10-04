package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

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
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IImagenServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Categoria;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;

/**
 * Test de integracion (BD MySQL real, via HTTP con MockMvc) para SCRUM-138:
 * exponer DELETE /api/v2/instructor con borrado logico, y los 2 riesgos
 * confirmados antes de activarlo:
 *   1. GET /instructores debe excluir instructores ya eliminados.
 *   2. Asignar un instructor ya eliminado a un grupo nuevo debe rechazarse
 *      (antes del fix, GrupoGateway usaba existsById() sin filtrar eliminado).
 * Tambien confirma (no se asume) que el mapeo ModelMapper de
 * InstructorEntidad.perfil -> Instructor.perfil funciona end-to-end: el body
 * de la respuesta del DELETE debe traer id/nombre/correo/sexo poblados.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class InstructorIT {

    private static final String CATEGORIA = "Deportes";
    private static final String CURSO = "InstructorIT";
    private static final String DEPORTE_SEED = "Natacion";

    @Autowired
    private IInstructorServicio instructorServicio;

    @Autowired
    private ICategoriaCursoServicio categoriaCursoServicio;

    @Autowired
    private ICursoServicio cursoServicio;

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
        imagenDto.setNombre("imagen-instructor-it.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {3, 3, 3}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para InstructorIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        try {
            Curso curso = new Curso();
            curso.setNombre(CURSO);
            curso.setCategoriaCurso(CATEGORIA);
            curso.setDescripcion("Curso de prueba para InstructorIT");
            curso.setImagenId(imagenId);
            curso.setDeporte(DEPORTE_SEED);
            curso.setEstadoCurso(EstadoCurso.ACTIVO);
            curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
            cursoServicio.insertarCurso(curso);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior contra BD persistente; se reutiliza.
        }
    }

    // Instructor fresco por test (id/correo unicos) para no interferir con el
    // instructor seed ("2") que usan GrupoIT/InscripcionConcurrenciaIT/etc.
    private String crearInstructorFresco() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String id = "instructor-it-" + sufijo;
        PerfilDto dto = new PerfilDto();
        dto.setId(id);
        dto.setNombre("Prof. Prueba " + sufijo);
        dto.setCorreo("instructor-it-" + sufijo + "@unicauca.edu.co");
        dto.setTipoId("CC");
        dto.setSexo("M");
        // tbl_alumno.ALM_TIPO tiene CHECK (IN 'Estudiante','Administrativo','Docente').
        dto.setTipoAlumno("Docente");
        instructorServicio.registrarInstructor(dto);
        return id;
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

    private Integer metaEliminadoEnBd(String instructorId) {
        return jdbc.queryForObject(
                "SELECT META_ELIMINADO FROM tbl_instructor WHERE PERF_ID = ?",
                Integer.class, instructorId);
    }

    /**
     * No se asume que el mapeo ModelMapper de InstructorEntidad.perfil funciona --
     * se confirma que el body de la respuesta trae el perfil completo, y que
     * META_ELIMINADO queda en 1 en la BD real.
     */
    @Test
    void deleteInstructor_http_marcaEliminadoEnBdYRetornaPerfilCompleto() throws Exception {
        String instructorId = crearInstructorFresco();

        mockMvc.perform(delete("/api/v2/instructor")
                .param("instructorId", instructorId)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(instructorId))
                .andExpect(jsonPath("$.nombre").exists())
                .andExpect(jsonPath("$.correo").exists())
                .andExpect(jsonPath("$.sexo").exists());

        assertEquals(1, metaEliminadoEnBd(instructorId),
                "META_ELIMINADO debe quedar en 1 en la BD real tras el DELETE");
    }

    @Test
    void deleteInstructor_segundaVezSobreElMismoInstructor_retorna409() throws Exception {
        String instructorId = crearInstructorFresco();

        mockMvc.perform(delete("/api/v2/instructor")
                .param("instructorId", instructorId)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v2/instructor")
                .param("instructorId", instructorId)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isConflict());
    }

    // Riesgo #1 confirmado: GET /instructores usaba findAll() sin filtro.
    @Test
    void getInstructores_http_excluyeInstructorEliminado() throws Exception {
        String instructorId = crearInstructorFresco();
        String jwtCoordinador = "Bearer " + buildJwt("Coordinador", "coord-it");

        mockMvc.perform(get("/api/v2/instructores").header("Authorization", jwtCoordinador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + instructorId + "')]").exists());

        mockMvc.perform(delete("/api/v2/instructor")
                .param("instructorId", instructorId)
                .header("Authorization", jwtCoordinador))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v2/instructores").header("Authorization", jwtCoordinador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + instructorId + "')]").doesNotExist());
    }

    // Riesgo #2 confirmado: GrupoGateway.insertarGrupo() usaba
    // repoInstructor.existsById() sin filtrar eliminado -- un instructor ya
    // eliminado podia asignarse igual a un grupo nuevo.
    @Test
    void postGrupo_conInstructorEliminado_seRechazaConFailedDependency() throws Exception {
        String instructorId = crearInstructorFresco();
        String jwtCoordinador = "Bearer " + buildJwt("Coordinador", "coord-it");

        mockMvc.perform(delete("/api/v2/instructor")
                .param("instructorId", instructorId)
                .header("Authorization", jwtCoordinador))
                .andExpect(status().isOk());

        String body = "{\"categoria\":\"" + CATEGORIA + "\",\"curso\":\"" + CURSO
                + "\",\"imagenGrupo\":" + imagenId + ",\"cupos\":5,\"idInstructor\":\"" + instructorId + "\","
                + "\"fechaCreacion\":\"" + LocalDate.now() + "\"}";

        mockMvc.perform(post("/api/v2/grupo")
                .contentType("application/json")
                .content(body)
                .header("Authorization", jwtCoordinador))
                .andExpect(status().is(424));
    }
}
