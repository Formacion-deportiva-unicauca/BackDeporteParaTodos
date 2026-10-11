package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.hamcrest.Matchers;
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

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICategoriaCursoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICursoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IGrupoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IImagenServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInscripcionServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Categoria;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;

/**
 * SCRUM-185 (test de integracion, BD MySQL real): la ventana de inscripcion del grupo
 * (fechaInscripcionApertura/fechaIncripcionCierre) debe respetarse en POST /inscripcion,
 * no solo en el catalogo GET /gruposInscripcion. "hoy" se calcula con el Clock inyectable
 * de America/Bogota -- esta IT no lo sobreescribe, usa la fecha real del sistema.
 *
 * RE-EJECUTABLE (bug encontrado 2026-10-10): esta IT usaba fixtures idempotentes
 * ("si no existe, crear") con fechas relativas a "hoy". El grupo de auto-promocion
 * intentaba "reabrir" su ventana con setFechaIncripcionCierre(null) + actualizarGrupo,
 * pero GrupoGateway.actualizarGrupo trata null como "conservar el valor actual", no
 * como "borrar" -- el cierre de la corrida anterior (p.ej. ayer) persistia, y al dia
 * siguiente inscribir() lo rechazaba como ventana cerrada. Ahora @BeforeAll limpia
 * SIEMPRE los restos de corridas anteriores (por prefijo de nombre de curso) antes de
 * crear los fixtures desde cero, y @AfterAll hace la misma limpieza al terminar.
 *
 * NOTA: esta clase requiere una BD MySQL real y credenciales configuradas (ver
 * application.properties); no se ejecuta en el perfil skip-integration-tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class InscripcionVentanaFechasIT {

    private static final String CATEGORIA = "Deportes";
    // Prefijo exclusivo de esta clase (confirmado por grep en todo src/test: ninguna
    // otra IT usa "InscripcionVentanaFechasIT-*") -- limita la limpieza a SOLO los
    // cursos/grupos/inscripciones de esta IT, nunca a "Deportes" ni a otros cursos.
    private static final String PREFIJO_CURSO = "InscripcionVentanaFechasIT-%";
    private static final String CURSO_CIERRE_AYER = "InscripcionVentanaFechasIT-CierreAyer";
    private static final String CURSO_CIERRE_HOY = "InscripcionVentanaFechasIT-CierreHoy";
    private static final String CURSO_APERTURA_NULL = "InscripcionVentanaFechasIT-AperturaNull";
    private static final String CURSO_AUTO_PROMOCION = "InscripcionVentanaFechasIT-AutoPromocion";
    private static final String DEPORTE_SEED = "Natacion";
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
    private IPerfilGateway perfilGateway;

    @Autowired
    private IInscripcionGateway inscripcionGateway;

    @Autowired
    private IInscripcionServicio servicio;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    // "hoy" se calcula con el MISMO Clock (America/Bogota) que usa la app para la
    // regla EstadoVentanaInscripcion -- antes usaba LocalDate.now() (zona del
    // sistema/runner, UTC en CI), que entre 19:00 y 24:00 de Bogota ya es "manana"
    // y hacia que "cierre ayer" significara en realidad "cierre hoy" para la regla.
    @Autowired
    private Clock clock;

    private Integer imagenId;

    // SCRUM-177: insertarGrupo() calcula anio/iterable en el servidor e ignora lo que
    // el caller haya puesto -- se guarda el Grupo tal como lo DEVUELVE el servicio,
    // nunca se asume un numero fijo.
    private Grupo grupoCierreAyer;
    private Grupo grupoCierreHoy;
    private Grupo grupoAperturaNull;
    private Grupo grupoAutoPromocion;

    private String alumnoOcupaCupoAutoPromocion;
    private String alumnoEnEsperaAutoPromocion;

    @BeforeAll
    void crearFixtures() {
        limpiarRestosDeCorridasAnteriores();

        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-inscripcion-ventana-fechas-it.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {5, 5, 5}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para InscripcionVentanaFechasIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        LocalDate hoy = LocalDate.now(clock);

        grupoCierreAyer = crearGrupoConVentana(CURSO_CIERRE_AYER, null, hoy.minusDays(1));
        grupoCierreHoy = crearGrupoConVentana(CURSO_CIERRE_HOY, null, hoy);
        grupoAperturaNull = crearGrupoConVentana(CURSO_APERTURA_NULL, null, null);
        grupoAutoPromocion = prepararFixtureAutoPromocion();
    }

    @AfterAll
    void limpiarAlTerminar() {
        limpiarRestosDeCorridasAnteriores();
    }

    // Limpieza por prefijo de nombre de curso, en orden de FK: inscripcion -> grupo ->
    // curso. NO toca tbl_categoria_curso ("Deportes" es compartida con otras IT) ni
    // ningun otro curso/seed. Idempotente: no falla si no hay nada que borrar.
    private void limpiarRestosDeCorridasAnteriores() {
        jdbc.update("DELETE FROM tbl_inscripcion WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?",
                CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_grupo WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?",
                CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_curso WHERE cat_titulo = ? AND CUR_NOMBRE LIKE ?",
                CATEGORIA, PREFIJO_CURSO);
    }

    // Siempre inserta un grupo NUEVO: limpiarRestosDeCorridasAnteriores() ya garantizo
    // que no existe uno previo con este nombre de curso, asi que no hace falta la
    // dualidad "si no existe, crear" de antes (esa era la fuente del bug de re-ejecucion).
    private Grupo crearGrupoConVentana(String nombreCurso, LocalDate apertura, LocalDate cierre) {
        crearCurso(nombreCurso);
        Grupo grupo = new Grupo();
        grupo.setCategoria(CATEGORIA);
        grupo.setCurso(nombreCurso);
        grupo.setImagenGrupo(imagenId);
        grupo.setIdInstructor(INSTRUCTOR_SEED_ID);
        grupo.setCupos(5);
        grupo.setFechaCreacion(LocalDate.now(clock));
        grupo.setFechaInscripcionApertura(apertura);
        grupo.setFechaIncripcionCierre(cierre);
        grupo.setPeriodo(1);
        return grupoServicio.insertarGrupo(grupo);
    }

    private void crearCurso(String nombreCurso) {
        Curso curso = new Curso();
        curso.setNombre(nombreCurso);
        curso.setCategoriaCurso(CATEGORIA);
        curso.setDescripcion("Curso de prueba para InscripcionVentanaFechasIT");
        curso.setImagenId(imagenId);
        curso.setDeporte(DEPORTE_SEED);
        curso.setEstadoCurso(EstadoCurso.ACTIVO);
        curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
        cursoServicio.insertarCurso(curso);
    }

    // Paso 1: grupo con 1 cupo y ventana ABIERTA (null/null, recien creado) -- inscribir()
    // (que ahora rechaza si la ventana ya esta cerrada) acepta a los dos alumnos.
    // Paso 2: ya con 1 INSCRITO y 1 EN_ESPERA, se cierra la ventana (cierre = ayer, un
    // valor NO nulo, asi que actualizarGrupo si lo persiste -- a diferencia del intento
    // de "reabrir" con null que causaba el bug de re-ejecucion).
    // El escenario real que SCRUM-185 debe dejar intacto: desvincularInscripcion()
    // no debe mirar estas fechas para decidir si promueve al primero en espera.
    private Grupo prepararFixtureAutoPromocion() {
        Grupo grupoNuevo = crearGrupoConVentana(CURSO_AUTO_PROMOCION, null, null);
        grupoNuevo.setCupos(1);
        grupoServicio.actualizarGrupo(
                CATEGORIA, CURSO_AUTO_PROMOCION, grupoNuevo.getAnio(), grupoNuevo.getIterable(), grupoNuevo);

        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        alumnoOcupaCupoAutoPromocion = "alumno-it-autopromocion-ocupa-" + sufijo;
        alumnoEnEsperaAutoPromocion = "alumno-it-autopromocion-espera-" + sufijo;
        crearAlumnoSiNoExiste(alumnoOcupaCupoAutoPromocion);
        crearAlumnoSiNoExiste(alumnoEnEsperaAutoPromocion);

        servicio.inscribir(new Inscripcion(alumnoOcupaCupoAutoPromocion, CATEGORIA, CURSO_AUTO_PROMOCION,
                grupoNuevo.getAnio(), grupoNuevo.getIterable(), null, null, null));
        servicio.inscribir(new Inscripcion(alumnoEnEsperaAutoPromocion, CATEGORIA, CURSO_AUTO_PROMOCION,
                grupoNuevo.getAnio(), grupoNuevo.getIterable(), null, null, null));

        Grupo grupoCerrado = grupoServicio.obtenerGrupoPorId(
                CATEGORIA, CURSO_AUTO_PROMOCION, grupoNuevo.getAnio(), grupoNuevo.getIterable());
        grupoCerrado.setFechaIncripcionCierre(LocalDate.now(clock).minusDays(1));
        grupoServicio.actualizarGrupo(
                CATEGORIA, CURSO_AUTO_PROMOCION, grupoNuevo.getAnio(), grupoNuevo.getIterable(), grupoCerrado);
        return grupoCerrado;
    }

    private void crearAlumnoSiNoExiste(String alumnoId) {
        try {
            Perfil perfil = new Perfil();
            perfil.setId(alumnoId);
            perfil.setNombre("Alumno Prueba " + alumnoId);
            perfil.setCorreo(alumnoId + "@it.unicauca.edu.co");
            perfil.setTipoId("CC");
            perfil.setSexo("M");
            perfil.setTipoAlumno("Estudiante");
            perfilGateway.registrarAlumno(perfil);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior contra BD persistente; se reutiliza.
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

    private String bodyInscripcion(String alumnoId, Grupo grupo) {
        return "{\"alumnoId\":\"" + alumnoId + "\",\"categoria\":\"" + CATEGORIA
                + "\",\"curso\":\"" + grupo.getCurso() + "\",\"anio\":" + grupo.getAnio()
                + ",\"iterable\":" + grupo.getIterable() + "}";
    }

    @Test
    void postInscripcion_http_grupoConCierreAyer_rechaza422YNoCreaFila() throws Exception {
        String alumnoId = "alumno-it-ventana-cierre-ayer-" + UUID.randomUUID().toString().substring(0, 8);
        crearAlumnoSiNoExiste(alumnoId);

        mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(alumnoId, grupoCierreAyer))
                .header("Authorization", "Bearer " + buildJwt("Alumno", alumnoId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("cerraron el")));

        assertFalse(inscripcionGateway.existeInscripcion(
                        alumnoId, CATEGORIA, grupoCierreAyer.getCurso(), grupoCierreAyer.getAnio(), grupoCierreAyer.getIterable()),
                "no debe haberse creado ninguna fila en tbl_inscripcion cuando la ventana rechaza la inscripcion");
    }

    // Dia de cierre INCLUSIVO: hoy == cierre todavia debe permitir inscribirse.
    @Test
    void postInscripcion_http_grupoConCierreHoy_sigueAceptando201() throws Exception {
        String alumnoId = "alumno-it-ventana-cierre-hoy-" + UUID.randomUUID().toString().substring(0, 8);
        crearAlumnoSiNoExiste(alumnoId);

        mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(alumnoId, grupoCierreHoy))
                .header("Authorization", "Bearer " + buildJwt("Alumno", alumnoId)))
                .andExpect(status().isCreated());
    }

    // apertura null = abierto desde siempre.
    @Test
    void postInscripcion_http_grupoConAperturaNull_aceptando201() throws Exception {
        String alumnoId = "alumno-it-ventana-apertura-null-" + UUID.randomUUID().toString().substring(0, 8);
        crearAlumnoSiNoExiste(alumnoId);

        mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(alumnoId, grupoAperturaNull))
                .header("Authorization", "Bearer " + buildJwt("Alumno", alumnoId)))
                .andExpect(status().isCreated());
    }

    // El catalogo GET /gruposInscripcion debe usar la misma regla: un grupo con
    // apertura null (abierto desde siempre) debe aparecer en la lista.
    @Test
    void getGruposInscripcion_http_grupoConAperturaNull_apareceEnElCatalogo() throws Exception {
        mockMvc.perform(get("/api/v2/gruposInscripcion")
                .header("Authorization", "Bearer " + buildJwt("Alumno", "alumno-it-catalogo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.curso == '" + CURSO_APERTURA_NULL + "')]").isNotEmpty());
    }

    // Blindaje explicito: la promocion AUTOMATICA (desvincularInscripcion ->
    // promoverPrimeroEnEspera) no debe depender de la ventana de fechas del grupo.
    // El grupo del fixture ya tiene fechaIncripcionCierre = ayer (ventana CERRADA)
    // cuando se desvincula al alumno INSCRITO -- el primero en espera debe pasar a
    // INSCRITO igual, porque desvincularInscripcion() nunca consulta esas fechas.
    @Test
    void desvincularInscripcion_http_grupoConVentanaCerrada_promueveAutomaticamenteAlPrimeroEnEspera() throws Exception {
        mockMvc.perform(put("/api/v2/desvincularInscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(alumnoOcupaCupoAutoPromocion, grupoAutoPromocion))
                .header("Authorization", "Bearer " + buildJwt("Alumno", alumnoOcupaCupoAutoPromocion)))
                .andExpect(status().isOk());

        Inscripcion promovido = inscripcionGateway.obtenerInscripcion(alumnoEnEsperaAutoPromocion, CATEGORIA,
                grupoAutoPromocion.getCurso(), grupoAutoPromocion.getAnio(), grupoAutoPromocion.getIterable());
        assertEquals("INSCRITO", promovido.getEstado());
        assertFalse(inscripcionGateway.existeEnEspera(alumnoEnEsperaAutoPromocion, CATEGORIA,
                grupoAutoPromocion.getCurso(), grupoAutoPromocion.getAnio(), grupoAutoPromocion.getIterable()));
    }
}
