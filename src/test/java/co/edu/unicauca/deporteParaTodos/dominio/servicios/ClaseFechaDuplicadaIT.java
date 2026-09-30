package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import org.springframework.test.web.servlet.MvcResult;

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
 * Test de integracion (BD MySQL real, via HTTP con MockMvc) para dos hallazgos ligados:
 *
 * 1. Desfase de fecha: ClaseDto.fecha era java.sql.Date; Jackson lo deserializaba
 *    interpretando el string ISO como medianoche UTC, y al convertir esa instancia a
 *    la zona horaria por defecto de la JVM (America/Bogota, UTC-5) el componente de
 *    fecha retrocedia un dia. Confirmado en aislado: "2026-09-29" -> java.sql.Date
 *    "2026-09-28". El fix cambia Clase/ClaseDto/ClaseEntidad.fecha a java.time.LocalDate,
 *    que Jackson deserializa directo desde el string ISO sin pasar por un instante ni
 *    por zona horaria alguna.
 *
 * 2. Registro de asistencia duplicado: el Instructor registra asistencia alumno por
 *    alumno, con un click en "Registrar asistencia" por cada uno (no marca varios
 *    checkboxes y envia una sola vez). Cada click repite POST /claseGrupo + POST
 *    /atenciones. Nada impedia crear una segunda Clase para el mismo grupo en la misma
 *    fecha (ni a nivel de codigo -- ClaseServicio/ClaseGateway guardaban sin validar --
 *    ni de esquema -- tbl_clase solo tiene PK en CLS_CODIGO autoincremental). Rechazar
 *    con 409 el segundo click (primer diseno) rompia el flujo real: la intencion era
 *    agregar el alumno 2 a la MISMA clase de hoy, no crear una segunda. El fix final
 *    hace que insertarClase() reutilice la clase existente (200) en vez de rechazar.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class ClaseFechaDuplicadaIT {

    private static final String CATEGORIA = "Deportes";
    private static final String CURSO = "ClaseFechaDuplicadaIT";
    // GrupoGateway.insertarGrupo() ignora el anio pedido y crea el grupo en el anio ACTUAL.
    private static final int ANIO = LocalDate.now().getYear();
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
        imagenDto.setNombre("imagen-clase-fecha-duplicada-it.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {7, 7, 7}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para ClaseFechaDuplicadaIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        try {
            Curso curso = new Curso();
            curso.setNombre(CURSO);
            curso.setCategoriaCurso(CATEGORIA);
            curso.setDescripcion("Curso de prueba para ClaseFechaDuplicadaIT");
            curso.setImagenId(imagenId);
            curso.setDeporte(DEPORTE_SEED);
            curso.setEstadoCurso(EstadoCurso.ACTIVO);
            curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
            cursoServicio.insertarCurso(curso);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior contra BD persistente; se reutiliza.
        }
    }

    // Un grupo nuevo por test (iterable propio, asignado por insertarGrupo()) para que
    // ningun test dependa de fechas usadas por otro en corridas anteriores.
    private int crearGrupoFresco() {
        Grupo grupo = new Grupo();
        grupo.setCategoria(CATEGORIA);
        grupo.setCurso(CURSO);
        grupo.setImagenGrupo(imagenId);
        grupo.setIdInstructor(INSTRUCTOR_SEED_ID);
        grupo.setCupos(5);
        grupo.setFechaCreacion(LocalDate.now());
        grupo.setPeriodo(1);
        return grupoServicio.insertarGrupo(grupo).getIterable();
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

    private String bodyClase(int iterable, String fechaIso, String observacion) {
        return "{\"idGrupoCategoria\":\"" + CATEGORIA + "\",\"idGrupoCurso\":\"" + CURSO
                + "\",\"idGrupoAnio\":" + ANIO + ",\"idGrupoIterable\":" + iterable
                + ",\"idInstructor\":\"" + INSTRUCTOR_SEED_ID + "\",\"fecha\":\"" + fechaIso + "\","
                + "\"horas\":1,\"minutos\":0,\"observacion\":\"" + observacion + "\",\"eliminado\":0}";
    }

    private String fechaPersistida(int iterable) {
        return jdbc.queryForObject(
                "SELECT CLS_FECHA FROM tbl_clase WHERE CAT_TITULO = ? AND CUR_NOMBRE = ? "
                        + "AND GRP_ANIO = ? AND GRP_ITERABLE = ?",
                String.class, CATEGORIA, CURSO, ANIO, iterable);
    }

    /**
     * Reproduce el desfase confirmado manualmente: POST /claseGrupo con fecha
     * "2026-09-29" debia dejar CLS_FECHA = '2026-09-29' en BD; con java.sql.Date +
     * Jackson (sin fix) quedaba en '2026-09-28'.
     */
    @Test
    void postClase_http_fechaEnviada_sePersisteExactamenteIgualEnBd() throws Exception {
        int iterable = crearGrupoFresco();
        String fechaEnviada = "2026-11-11";

        mockMvc.perform(post("/api/v2/claseGrupo")
                .contentType("application/json")
                .content(bodyClase(iterable, fechaEnviada, "verificacion desfase de fecha"))
                .header("Authorization", "Bearer " + buildJwt("Instructor", INSTRUCTOR_SEED_ID)))
                .andExpect(status().isCreated());

        assertEquals(fechaEnviada, fechaPersistida(iterable),
                "La fecha persistida debe ser exactamente la enviada, sin desfase de zona horaria");
    }

    private Integer filasClaseEnFecha(int iterable, String fecha) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_clase WHERE CAT_TITULO = ? AND CUR_NOMBRE = ? "
                        + "AND GRP_ANIO = ? AND GRP_ITERABLE = ? AND CLS_FECHA = ?",
                Integer.class, CATEGORIA, CURSO, ANIO, iterable, fecha);
    }

    /**
     * Reconstruccion del flujo real: el Instructor registra alumno por alumno, un
     * click por alumno en momentos distintos -- no marca varios checkboxes y envia
     * una sola vez. El segundo click (segundo alumno) repite POST /claseGrupo para
     * el mismo grupo/fecha; debe REUTILIZAR la clase del primer click (200, mismo
     * codigo), no rechazar con 409 ni crear una segunda fila.
     */
    @Test
    void postClase_http_fechaDuplicadaMismoGrupo_reutilizaClaseExistente() throws Exception {
        int iterable = crearGrupoFresco();
        String fecha = "2026-12-01";
        ObjectMapper mapper = new ObjectMapper();

        MvcResult primero = mockMvc.perform(post("/api/v2/claseGrupo")
                .contentType("application/json")
                .content(bodyClase(iterable, fecha, "click alumno 1"))
                .header("Authorization", "Bearer " + buildJwt("Instructor", INSTRUCTOR_SEED_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        int codigoPrimero = mapper.readTree(primero.getResponse().getContentAsString()).get("codigo").asInt();

        MvcResult segundo = mockMvc.perform(post("/api/v2/claseGrupo")
                .contentType("application/json")
                .content(bodyClase(iterable, fecha, "click alumno 2"))
                .header("Authorization", "Bearer " + buildJwt("Instructor", INSTRUCTOR_SEED_ID)))
                .andExpect(status().isOk())
                .andReturn();
        int codigoSegundo = mapper.readTree(segundo.getResponse().getContentAsString()).get("codigo").asInt();

        assertEquals(codigoPrimero, codigoSegundo,
                "El segundo click debe devolver el codigo de la clase YA EXISTENTE, no uno nuevo");
        assertEquals(1, filasClaseEnFecha(iterable, fecha),
                "Debe existir una unica fila en tbl_clase para ese grupo+fecha, no dos");
    }

    // Camino normal: mismo grupo, fechas distintas -- ambas deben aceptarse sin cambios.
    @Test
    void postClase_http_fechasDistintasMismoGrupo_aceptaAmbas() throws Exception {
        int iterable = crearGrupoFresco();

        mockMvc.perform(post("/api/v2/claseGrupo")
                .contentType("application/json")
                .content(bodyClase(iterable, "2026-12-15", "clase dia 1"))
                .header("Authorization", "Bearer " + buildJwt("Instructor", INSTRUCTOR_SEED_ID)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v2/claseGrupo")
                .contentType("application/json")
                .content(bodyClase(iterable, "2026-12-16", "clase dia 2"))
                .header("Authorization", "Bearer " + buildJwt("Instructor", INSTRUCTOR_SEED_ID)))
                .andExpect(status().isCreated());
    }
}
