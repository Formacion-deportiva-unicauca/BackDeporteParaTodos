package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterAll;
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

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICategoriaCursoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICursoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IGrupoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IImagenServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Categoria;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;

/**
 * SCRUM-80 (rediseno, bloque 3 de 4): GET /instructores/estado-registro,
 * GET /instructor/avisos-registro y GET /asistencia/ventana (test de integracion
 * contra la BD MySQL real).
 *
 * Fixtures deterministas con sufijo unico por corrida:
 *  - Instructor A -> grupo activo en el mes actual CON una clase en el mes
 *    => estado COMPLETO / sin avisos.
 *  - Instructor B -> grupo activo en el mes actual SIN clase
 *    => estado PENDIENTE.
 *  - Instructor C -> sin grupos => estado SIN_GRUPOS.
 *  - Instructor D -> grupo cuyo periodo cae ENTERO en el mes anterior, sin clase
 *    => aviso VENCIDO para el mes anterior.
 *
 * Ejecutar DOS veces seguidas debe dar el mismo resultado (limpieza en
 * @BeforeAll/@AfterAll + ids unicos por corrida). "hoy" SIEMPRE sale del Clock
 * inyectado (America/Bogota), nunca de la zona del sistema.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class EstadoRegistroInstructorIT {

    private static final String CATEGORIA = "Deportes";
    private static final String PREFIJO_CURSO = "EstadoRegIT-%";
    private static final String PREFIJO_PERFIL = "estadoreg-%";
    private static final String DEPORTE_SEED = "Natacion";
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
    private static final String COORD = "coord-it";

    @Autowired private IGrupoServicio grupoServicio;
    @Autowired private ICategoriaCursoServicio categoriaCursoServicio;
    @Autowired private ICursoServicio cursoServicio;
    @Autowired private IInstructorServicio instructorServicio;
    @Autowired private IImagenServicio imagenServicio;
    @Autowired private IClaseGateway claseGateway;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Clock clock;

    private Integer imagenId;
    private String instructorA;
    private String instructorB;
    private String instructorC;
    private String instructorD;
    private LocalDate hoy;
    private LocalDate primerDiaMesActual;
    private LocalDate primerDiaMesAnterior;
    private LocalDate ultimoDiaMesAnterior;

    @BeforeAll
    void crearFixtures() {
        limpiarRestosDeCorridasAnteriores();

        hoy = LocalDate.now(clock);
        primerDiaMesActual = hoy.withDayOfMonth(1);
        YearMonth mesAnterior = YearMonth.from(hoy.minusMonths(1));
        primerDiaMesAnterior = mesAnterior.atDay(1);
        ultimoDiaMesAnterior = mesAnterior.atEndOfMonth();

        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-estadoreg.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {8, 8, 8}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para EstadoRegistroInstructorIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        instructorA = crearInstructor("a-" + sufijo);
        instructorB = crearInstructor("b-" + sufijo);
        instructorC = crearInstructor("c-" + sufijo);
        instructorD = crearInstructor("d-" + sufijo);

        Grupo grupoA = crearGrupo("A-" + sufijo, instructorA, primerDiaMesActual, null);
        registrarClase(grupoA, instructorA, hoy);

        crearGrupo("B-" + sufijo, instructorB, primerDiaMesActual, null);

        // C: sin grupos a proposito (estado SIN_GRUPOS).

        crearGrupo("D-" + sufijo, instructorD, primerDiaMesAnterior, ultimoDiaMesAnterior);
    }

    @AfterAll
    void limpiarAlTerminar() {
        limpiarRestosDeCorridasAnteriores();
    }

    // Orden de FK: asistencia -> clase -> horario -> grupo -> prorroga -> curso -> instructor -> alumno -> perfil.
    private void limpiarRestosDeCorridasAnteriores() {
        jdbc.update("DELETE FROM tbl_asistencia WHERE PERF_ID LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_clase WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_horario WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_grupo WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_prorroga_registro_asistencia WHERE PERF_ID LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_curso WHERE cat_titulo = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_instructor WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_alumno WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_perfil WHERE perf_id LIKE ?", PREFIJO_PERFIL);
    }

    private String crearInstructor(String sufijoId) {
        String id = "estadoreg-" + sufijoId;
        PerfilDto dto = new PerfilDto();
        dto.setId(id);
        dto.setNombre("Prof. EstadoReg " + sufijoId);
        dto.setCorreo(id + "@it.unicauca.edu.co");
        dto.setTipoId("CC");
        dto.setSexo("M");
        dto.setTipoAlumno("Docente");
        instructorServicio.registrarInstructor(dto);
        return id;
    }

    private Grupo crearGrupo(String sufijoCurso, String idInstructor,
            LocalDate fechaCreacion, LocalDate fechaFinalizacion) {
        String curso = "EstadoRegIT-" + sufijoCurso;
        Curso datosCurso = new Curso();
        datosCurso.setNombre(curso);
        datosCurso.setCategoriaCurso(CATEGORIA);
        datosCurso.setDescripcion("Curso de prueba para EstadoRegistroInstructorIT");
        datosCurso.setImagenId(imagenId);
        datosCurso.setDeporte(DEPORTE_SEED);
        datosCurso.setEstadoCurso(EstadoCurso.ACTIVO);
        datosCurso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
        cursoServicio.insertarCurso(datosCurso);

        Grupo nuevo = new Grupo();
        nuevo.setCategoria(CATEGORIA);
        nuevo.setCurso(curso);
        nuevo.setImagenGrupo(imagenId);
        nuevo.setIdInstructor(idInstructor);
        nuevo.setCupos(5);
        nuevo.setFechaCreacion(fechaCreacion);
        nuevo.setFechaFinalizacion(fechaFinalizacion);
        nuevo.setFechaInscripcionApertura(fechaCreacion);
        nuevo.setPeriodo(0);
        return grupoServicio.insertarGrupo(nuevo);
    }

    private void registrarClase(Grupo grupo, String idInstructor, LocalDate fecha) {
        Clase clase = new Clase();
        clase.setCategoria(grupo.getCategoria());
        clase.setCurso(grupo.getCurso());
        clase.setAnio(grupo.getAnio());
        clase.setIterable(grupo.getIterable());
        clase.setIdInstructor(idInstructor);
        clase.setFecha(fecha);
        clase.setHoras(2);
        clase.setMinutos(0);
        claseGateway.insertarClase(clase);
    }

    private String buildJwt(String rol, String perfId) {
        byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
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

    private String bearer(String rol, String perfId) {
        return "Bearer " + buildJwt(rol, perfId);
    }

    private List<Map<String, Object>> leerLista(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$");
    }

    private Map<String, Object> filaDe(List<Map<String, Object>> filas, String idInstructor) {
        for (Map<String, Object> fila : filas) {
            if (idInstructor.equals(fila.get("idInstructor"))) {
                return fila;
            }
        }
        return null;
    }

    // ── GET /instructores/estado-registro ─────────────────────────────────────

    @Test
    void obtenerEstadoRegistro_coordinador_retornaEstadosPorInstructorActivo() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v2/instructores/estado-registro")
                        .header("Authorization", bearer("Coordinador", COORD)))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> filas = leerLista(resultado);

        Map<String, Object> filaA = filaDe(filas, instructorA);
        Map<String, Object> filaB = filaDe(filas, instructorB);
        Map<String, Object> filaC = filaDe(filas, instructorC);
        Map<String, Object> filaD = filaDe(filas, instructorD);

        assertEquals("COMPLETO", filaA.get("estado"), "A tiene grupo activo con clase en el mes actual");
        assertEquals("PENDIENTE", filaB.get("estado"), "B tiene grupo activo sin clase en el mes actual");
        assertEquals("SIN_GRUPOS", filaC.get("estado"), "C no tiene grupos");
        assertEquals("SIN_GRUPOS", filaD.get("estado"), "el grupo de D cae entero en el mes anterior");
        assertEquals(hoy.toString(), filaA.get("hoy"), "la respuesta debe exponer 'hoy' del Clock");
    }

    @Test
    void obtenerEstadoRegistro_mesFueraDeRango_retorna400() throws Exception {
        mockMvc.perform(get("/api/v2/instructores/estado-registro")
                        .param("mes", "13")
                        .header("Authorization", bearer("Coordinador", COORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerEstadoRegistro_mesFuturo_retorna422() throws Exception {
        LocalDate futuro = hoy.plusMonths(1);
        mockMvc.perform(get("/api/v2/instructores/estado-registro")
                        .param("anio", String.valueOf(futuro.getYear()))
                        .param("mes", String.valueOf(futuro.getMonthValue()))
                        .header("Authorization", bearer("Coordinador", COORD)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void obtenerEstadoRegistro_instructorSinAutorizacion_retorna403() throws Exception {
        mockMvc.perform(get("/api/v2/instructores/estado-registro")
                        .header("Authorization", bearer("Instructor", instructorA)))
                .andExpect(status().isForbidden());
    }

    // ── GET /instructor/avisos-registro ───────────────────────────────────────

    @Test
    void obtenerAvisosRegistro_instructorVencido_retornaAvisoVencidoMesAnterior() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v2/instructor/avisos-registro")
                        .header("Authorization", bearer("Instructor", instructorD)))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> avisos = leerLista(resultado);
        assertEquals(1, avisos.size(), "D solo debe tener el aviso del mes anterior");

        Map<String, Object> aviso = avisos.get(0);
        assertEquals(primerDiaMesAnterior.getYear(), ((Number) aviso.get("anio")).intValue());
        assertEquals(primerDiaMesAnterior.getMonthValue(), ((Number) aviso.get("mes")).intValue());
        assertEquals("VENCIDO", aviso.get("estado"));
    }

    @Test
    void obtenerAvisosRegistro_instructorCompleto_retornaListaVacia() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v2/instructor/avisos-registro")
                        .header("Authorization", bearer("Instructor", instructorA)))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> avisos = leerLista(resultado);
        assertTrue(avisos.isEmpty(), "A tiene el mes actual COMPLETO y no tiene grupo en el mes anterior");
    }

    @Test
    void obtenerAvisosRegistro_instructorInexistente_retorna404() throws Exception {
        String inexistente = "estadoreg-noexiste-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(get("/api/v2/instructor/avisos-registro")
                        .header("Authorization", bearer("Instructor", inexistente)))
                .andExpect(status().isNotFound());
    }

    // ── GET /asistencia/ventana ───────────────────────────────────────────────

    @Test
    void obtenerVentanaRegistro_instructor_retornaFechaMinimaYFechaMaxima() throws Exception {
        mockMvc.perform(get("/api/v2/asistencia/ventana")
                        .header("Authorization", bearer("Instructor", instructorA)))
                .andExpect(status().isOk())
                .andExpect(jsonPathValue("$.fechaMinima", primerDiaMesActual.toString()))
                .andExpect(jsonPathValue("$.fechaMaxima", hoy.toString()))
                .andExpect(jsonPathValue("$.rol", "Instructor"));
    }

    @Test
    void obtenerVentanaRegistro_coordinador_retornaFechaMinimaNull() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v2/asistencia/ventana")
                        .header("Authorization", bearer("Coordinador", COORD)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> ventana = JsonPath.read(resultado.getResponse().getContentAsString(), "$");
        assertNull(ventana.get("fechaMinima"), "el Coordinador no tiene limite inferior");
        assertEquals(hoy.toString(), ventana.get("fechaMaxima"));
        assertEquals("Coordinador", ventana.get("rol"));
    }

    @Test
    void obtenerVentanaRegistro_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/v2/asistencia/ventana"))
                .andExpect(status().isUnauthorized());
    }

    private static org.springframework.test.web.servlet.ResultMatcher jsonPathValue(String path, Object valor) {
        return org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(path).value(valor);
    }
}
