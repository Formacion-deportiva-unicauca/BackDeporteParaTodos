package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IHorarioServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IImagenServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.CalculadoraDuracionClase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Categoria;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;

/**
 * Autorizacion por grupo para el rol Instructor (test de integracion, BD MySQL
 * real, Spring Security real via MockMvc + JWT firmado). Cubre los 7 puntos de
 * A/B/C del ticket: el Instructor solo actua sobre el grupo donde
 * grupo.idInstructor == claim "perf_id" del token; Coordinador/Administrador
 * quedan exentos; el id nunca viene del cliente.
 *
 * Fixtures: 1 grupo CON horario (evita interferencia de la regla de
 * dia-en-horario, ya probada en ClaseVentanaRegistroIT) a cargo de
 * instructorA; instructorB es un instructor cualquiera SIN ese grupo a cargo
 * (el "ajeno" de cada caso). Cada metodo de prueba crea sus propios datos
 * (alumno/clase/inscripcion) para ser independiente y repetible, sin depender
 * del orden de ejecucion.
 *
 * Ejecutar este IT DOS veces seguidas debe dar el mismo resultado (limpieza en
 * @BeforeAll/@AfterAll + identificadores unicos por corrida). Una tercera
 * corrida con JAVA_TOOL_OPTIONS=-Duser.timezone=Pacific/Kiritimati prueba que
 * el resultado no cambia, porque "hoy" siempre sale del Clock inyectado
 * (America/Bogota).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class AutorizacionGrupoIT {

    private static final String CATEGORIA = "Deportes";
    private static final String PREFIJO_CURSO = "AutorizGrupoIT-%";
    private static final String PREFIJO_PERFIL = "autorizgrupoit-%";
    private static final String DEPORTE_SEED = "Natacion";
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
    private static final String COORD = "autorizgrupoit-coord";
    private static final String ADMIN = "autorizgrupoit-admin";

    @Autowired private IGrupoServicio grupoServicio;
    @Autowired private ICategoriaCursoServicio categoriaCursoServicio;
    @Autowired private ICursoServicio cursoServicio;
    @Autowired private IHorarioServicio horarioServicio;
    @Autowired private IInstructorServicio instructorServicio;
    @Autowired private IImagenServicio imagenServicio;
    @Autowired private IPerfilGateway perfilGateway;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Clock clock;

    private Integer imagenId;
    private String instructorA; // titular del grupo
    private String instructorB; // instructor cualquiera, SIN el grupo a cargo
    private Grupo grupo;
    private LocalDate hoy;
    private String diaDeHoy;

    @BeforeAll
    void crearFixtures() {
        limpiarRestosDeCorridasAnteriores();

        hoy = LocalDate.now(clock);
        diaDeHoy = CalculadoraDuracionClase.nombreDia(hoy.getDayOfWeek());

        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-autorizgrupoit.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {3, 1, 4}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para AutorizacionGrupoIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        instructorA = crearInstructor("a-" + sufijo);
        instructorB = crearInstructor("b-" + sufijo);

        grupo = crearGrupo("G-" + sufijo, instructorA, hoy);
        horarioServicio.insertarHorario(new Horario(null, CATEGORIA, grupo.getCurso(),
                grupo.getAnio(), grupo.getIterable(), diaDeHoy, "14:00", "16:00", "Cancha", 0));
    }

    @AfterAll
    void limpiarAlTerminar() {
        limpiarRestosDeCorridasAnteriores();
    }

    // Orden de FK: asistencia -> clase -> inscripcion -> horario -> grupo -> curso -> instructor/alumno -> perfil.
    private void limpiarRestosDeCorridasAnteriores() {
        jdbc.update("DELETE FROM tbl_asistencia WHERE PERF_ID LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_clase WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_inscripcion WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_horario WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_grupo WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_curso WHERE cat_titulo = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_instructor WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_alumno WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_perfil WHERE perf_id LIKE ?", PREFIJO_PERFIL);
    }

    private String crearInstructor(String sufijoId) {
        String id = "autorizgrupoit-" + sufijoId;
        PerfilDto dto = new PerfilDto();
        dto.setId(id);
        dto.setNombre("Prof. Prueba " + sufijoId);
        dto.setCorreo(id + "@it.unicauca.edu.co");
        dto.setTipoId("CC");
        dto.setSexo("M");
        dto.setTipoAlumno("Docente");
        instructorServicio.registrarInstructor(dto);
        return id;
    }

    private String crearAlumno(String sufijoId) {
        String id = "autorizgrupoit-alum-" + sufijoId + "-" + UUID.randomUUID().toString().substring(0, 6);
        try {
            Perfil perfil = new Perfil();
            perfil.setId(id);
            perfil.setNombre("Alumno Prueba " + sufijoId);
            perfil.setCorreo(id + "@it.unicauca.edu.co");
            perfil.setTipoId("CC");
            perfil.setSexo("M");
            perfil.setTipoAlumno("Estudiante");
            perfilGateway.registrarAlumno(perfil);
        } catch (YaExisteElementoExcepcion e) {
            // No deberia ocurrir (id con sufijo unico), pero se tolera.
        }
        return id;
    }

    private Grupo crearGrupo(String sufijoCurso, String idInstructor, LocalDate fechaInscripcionApertura) {
        String curso = "AutorizGrupoIT-" + sufijoCurso;
        Curso datosCurso = new Curso();
        datosCurso.setNombre(curso);
        datosCurso.setCategoriaCurso(CATEGORIA);
        datosCurso.setDescripcion("Curso de prueba para AutorizacionGrupoIT");
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
        // Alto a proposito: esta clase comparte un unico grupo entre ~15 metodos de
        // prueba (PER_CLASS, orden no garantizado) que se van inscribiendo -- un
        // valor bajo haria que algunas inscripciones cayeran en EN_ESPERA segun el
        // orden real de ejecucion, lo que rompe "repetible sin depender del orden".
        nuevo.setCupos(999);
        nuevo.setFechaCreacion(fechaInscripcionApertura);
        nuevo.setFechaInscripcionApertura(fechaInscripcionApertura);
        nuevo.setPeriodo(0);
        return grupoServicio.insertarGrupo(nuevo);
    }

    // Grupo dedicado para los escenarios de clase/asistencia: cada uno crea el
    // suyo (en vez de reusar el `grupo` compartido) para poder usar siempre
    // fecha=hoy sin chocar con clases de otras pruebas ni con el dia-de-semana
    // de un horario ajeno -- el horario cubre TODOS los dias, asi que la fecha
    // usada nunca dispara DiaFueraDeHorarioExcepcion.
    private Grupo crearGrupoClases(String sufijo) {
        Grupo g = crearGrupo("clases-" + sufijo, instructorA, hoy);
        for (String dia : new String[] {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"}) {
            horarioServicio.insertarHorario(new Horario(null, g.getCategoria(), g.getCurso(),
                    g.getAnio(), g.getIterable(), dia, "00:00", "23:59", "Cancha", 0));
        }
        return g;
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

    private String payloadInscripcion(String alumnoId, Grupo g) {
        return "{\"alumnoId\":\"" + alumnoId + "\",\"categoria\":\"" + g.getCategoria()
                + "\",\"curso\":\"" + g.getCurso() + "\",\"anio\":" + g.getAnio()
                + ",\"iterable\":" + g.getIterable() + "}";
    }

    private String payloadClase(Grupo g, LocalDate fecha, String idInstructorEnBody) {
        return "{\"idGrupoCategoria\":\"" + g.getCategoria() + "\",\"idGrupoCurso\":\"" + g.getCurso()
                + "\",\"idGrupoAnio\":" + g.getAnio() + ",\"idGrupoIterable\":" + g.getIterable()
                + ",\"idInstructor\":\"" + idInstructorEnBody + "\",\"fecha\":\"" + fecha
                + "\",\"horas\":0,\"minutos\":0}";
    }

    private long contarInscripcionesActivas(String alumnoId, Grupo g) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_inscripcion WHERE perf_id = ? AND CAT_TITULO = ? AND CUR_NOMBRE = ? "
                        + "AND GRP_ANIO = ? AND GRP_ITERABLE = ? AND INSCR_ESTADO = 'INSCRITO' "
                        + "AND inscr_fechadesvinculacion IS NULL",
                Long.class, alumnoId, g.getCategoria(), g.getCurso(), g.getAnio(), g.getIterable());
        return n == null ? 0 : n;
    }

    private String obtenerNombrePerfil(String perfId) {
        return jdbc.queryForObject("SELECT perf_nombre FROM tbl_perfil WHERE perf_id = ?", String.class, perfId);
    }

    private String obtenerCorreoPerfil(String perfId) {
        return jdbc.queryForObject("SELECT perf_correo FROM tbl_perfil WHERE perf_id = ?", String.class, perfId);
    }

    // ── 1. POST /inscripcion ────────────────────────────────────────────────

    @Test
    void inscripcion_instructorAjeno_retorna403YNoCreaLaInscripcion() throws Exception {
        String alumno = crearAlumno("insc-ajeno");

        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType("application/json")
                        .content(payloadInscripcion(alumno, grupo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorB)))
                .andExpect(status().isForbidden());

        assertEquals(0, contarInscripcionesActivas(alumno, grupo),
                "tras el 403 no debe haber quedado ninguna inscripcion creada");
    }

    @Test
    void inscripcion_instructorTitular_retorna201() throws Exception {
        String alumno = crearAlumno("insc-titular");

        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType("application/json")
                        .content(payloadInscripcion(alumno, grupo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated());

        assertEquals(1, contarInscripcionesActivas(alumno, grupo));
    }

    @Test
    void inscripcion_coordinadorYAdministrador_noEstanSujetosALaRegla() throws Exception {
        String alumnoCoord = crearAlumno("insc-coord");
        String alumnoAdmin = crearAlumno("insc-admin");

        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType("application/json")
                        .content(payloadInscripcion(alumnoCoord, grupo))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType("application/json")
                        .content(payloadInscripcion(alumnoAdmin, grupo))
                        .header("Authorization", "Bearer " + buildJwt("Administrador", ADMIN)))
                .andExpect(status().isCreated());
    }

    // ── 2. PUT /desvincularInscripcion ──────────────────────────────────────

    @Test
    void desvincular_instructorAjeno_retorna403YNoDesvincula() throws Exception {
        String alumno = crearAlumno("desv-ajeno");
        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType("application/json")
                        .content(payloadInscripcion(alumno, grupo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v2/desvincularInscripcion")
                        .contentType("application/json")
                        .content(payloadInscripcion(alumno, grupo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorB)))
                .andExpect(status().isForbidden());

        assertEquals(1, contarInscripcionesActivas(alumno, grupo),
                "tras el 403 la inscripcion debe seguir activa, sin desvincular");
    }

    @Test
    void desvincular_instructorTitular_retorna200() throws Exception {
        String alumno = crearAlumno("desv-titular");
        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType("application/json")
                        .content(payloadInscripcion(alumno, grupo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v2/desvincularInscripcion")
                        .contentType("application/json")
                        .content(payloadInscripcion(alumno, grupo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());

        assertEquals(0, contarInscripcionesActivas(alumno, grupo));
    }

    // ── 3. PUT /alumnos/{id} ─────────────────────────────────────────────────

    private String payloadAlumno(String nombre, String correo) {
        return "{\"nombre\":\"" + nombre + "\",\"correo\":\"" + correo + "\",\"tipoAlumno\":\"Estudiante\"}";
    }

    @Test
    void actualizarAlumno_instructorAjeno_retorna403YNoModificaNada() throws Exception {
        String alumno = crearAlumno("alm-ajeno");
        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType("application/json")
                        .content(payloadInscripcion(alumno, grupo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated());
        String nombreOriginal = obtenerNombrePerfil(alumno);

        mockMvc.perform(put("/api/v2/alumnos/" + alumno)
                        .contentType("application/json")
                        .content(payloadAlumno("Nombre Cambiado", alumno + "@it.unicauca.edu.co"))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorB)))
                .andExpect(status().isForbidden());

        assertEquals(nombreOriginal, obtenerNombrePerfil(alumno), "el nombre no debe haber cambiado tras el 403");
    }

    @Test
    void actualizarAlumno_sinInscripcionActivaEnGrupoDelInstructor_retorna403() throws Exception {
        // Alumno existe pero NUNCA se inscribio en ningun grupo de instructorA.
        String alumnoSinInscripcion = crearAlumno("alm-sin-insc");

        mockMvc.perform(put("/api/v2/alumnos/" + alumnoSinInscripcion)
                        .contentType("application/json")
                        .content(payloadAlumno("Nombre Nuevo", alumnoSinInscripcion + "@it.unicauca.edu.co"))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarAlumno_instructorTitular_actualizaNombreYTipo() throws Exception {
        String alumno = crearAlumno("alm-titular");
        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType("application/json")
                        .content(payloadInscripcion(alumno, grupo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v2/alumnos/" + alumno)
                        .contentType("application/json")
                        .content(payloadAlumno("Nombre Actualizado", alumno + "@it.unicauca.edu.co"))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre Actualizado"));

        assertEquals("Nombre Actualizado", obtenerNombrePerfil(alumno));
    }

    // Decision documentada: el Instructor no puede cambiar el correo (afecta el
    // login del alumno) -- 422, y el correo no debe cambiar en BD.
    @Test
    void actualizarAlumno_instructorTitularIntentaCambiarCorreo_retorna422YNoCambiaElCorreo() throws Exception {
        String alumno = crearAlumno("alm-correo");
        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType("application/json")
                        .content(payloadInscripcion(alumno, grupo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated());
        String correoOriginal = obtenerCorreoPerfil(alumno);

        mockMvc.perform(put("/api/v2/alumnos/" + alumno)
                        .contentType("application/json")
                        .content(payloadAlumno("Nombre Actualizado", "otro-correo-" + alumno + "@it.unicauca.edu.co"))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isUnprocessableEntity());

        assertEquals(correoOriginal, obtenerCorreoPerfil(alumno));
    }

    @Test
    void actualizarAlumno_coordinador_puedeCambiarCorreoDeCualquierAlumno() throws Exception {
        String alumno = crearAlumno("alm-coord");

        mockMvc.perform(put("/api/v2/alumnos/" + alumno)
                        .contentType("application/json")
                        .content(payloadAlumno("Nombre Coord", "nuevo-correo-" + alumno + "@it.unicauca.edu.co"))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isOk());

        assertEquals("nuevo-correo-" + alumno + "@it.unicauca.edu.co", obtenerCorreoPerfil(alumno));
    }

    // ── 4 y 7. POST /claseGrupo y DELETE /clase ─────────────────────────────

    @Test
    void claseGrupo_instructorAjeno_retorna403YNoCreaLaClase() throws Exception {
        Grupo grupoClases = crearGrupoClases("ajeno");
        long antes = contarClases(grupoClases, hoy);

        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoClases, hoy, instructorA))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorB)))
                .andExpect(status().isForbidden());

        assertEquals(antes, contarClases(grupoClases, hoy), "no debe haberse creado ninguna clase tras el 403");
    }

    private long contarClases(Grupo g, LocalDate fecha) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_clase WHERE CAT_TITULO = ? AND CUR_NOMBRE = ? AND GRP_ANIO = ? "
                        + "AND GRP_ITERABLE = ? AND CLS_FECHA = ?",
                Long.class, g.getCategoria(), g.getCurso(), g.getAnio(), g.getIterable(), fecha);
        return n == null ? 0 : n;
    }

    @Test
    void claseGrupo_instructorTitular_retorna201_elIdDelBodyNoSeUsaParaAutorizar() throws Exception {
        Grupo grupoClases = crearGrupoClases("titular");
        // instructorA (titular) manda en el body el id de instructorB -- el id real
        // para autorizar sale del JWT (perf_id), nunca del campo idInstructor del body.
        MvcResult creada = mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoClases, hoy, instructorB))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated())
                .andReturn();

        int codigo = JsonPath.read(creada.getResponse().getContentAsString(), "$.codigo");
        assertEquals(1, contarClases(grupoClases, hoy));

        // DELETE /clase: el mismo codigo, probado por instructor ajeno -> 403, sin borrar.
        mockMvc.perform(delete("/api/v2/clase")
                        .param("id", String.valueOf(codigo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorB)))
                .andExpect(status().isForbidden());
        Integer eliminado = jdbc.queryForObject(
                "SELECT META_ELIMINADO FROM tbl_clase WHERE CLS_CODIGO = ?", Integer.class, codigo);
        assertEquals(0, eliminado, "la clase no debe quedar eliminada tras el 403");

        // El titular si puede eliminarla.
        mockMvc.perform(delete("/api/v2/clase")
                        .param("id", String.valueOf(codigo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());
    }

    @Test
    void claseGrupo_coordinadorYAdministrador_noEstanSujetosALaRegla() throws Exception {
        Grupo grupoCoord = crearGrupoClases("coord");
        Grupo grupoAdmin = crearGrupoClases("admin");

        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoCoord, hoy, instructorA)
                                .replace("\"horas\":0,\"minutos\":0",
                                        "\"horas\":1,\"minutos\":0,\"justificacion\":"
                                                + "\"clase dictada por el coordinador en grupo ajeno\""))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoAdmin, hoy, instructorA)
                                .replace("\"horas\":0,\"minutos\":0",
                                        "\"horas\":1,\"minutos\":0,\"justificacion\":"
                                                + "\"clase dictada por el administrador en grupo ajeno\""))
                        .header("Authorization", "Bearer " + buildJwt("Administrador", ADMIN)))
                .andExpect(status().isCreated());
    }

    // ── 5 y 6. POST /atenciones y DELETE /asistencia ────────────────────────

    private int crearClaseComoTitular(Grupo g, LocalDate fecha) throws Exception {
        MvcResult creada = mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(g, fecha, instructorA))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(creada.getResponse().getContentAsString(), "$.codigo");
    }

    private long contarAsistenciasActivas(String alumnoId, int claseId) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_asistencia WHERE PERF_ID = ? AND CLS_CODIGO = ? AND META_ELIMINADO = 0",
                Long.class, alumnoId, claseId);
        return n == null ? 0 : n;
    }

    @Test
    void atenciones_instructorAjeno_retorna403YNoRegistraNada() throws Exception {
        Grupo grupoClases = crearGrupoClases("aten-ajeno");
        int claseId = crearClaseComoTitular(grupoClases, hoy);
        String alumno = crearAlumno("aten-ajeno");
        String cuerpo = "[{\"idPerfil\":\"" + alumno + "\",\"idClase\":" + claseId + ",\"estaAtendido\":true}]";

        mockMvc.perform(post("/api/v2/atenciones")
                        .param("idClase", String.valueOf(claseId))
                        .contentType("application/json")
                        .content(cuerpo)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorB)))
                .andExpect(status().isForbidden());

        assertEquals(0, contarAsistenciasActivas(alumno, claseId));
    }

    @Test
    void atenciones_instructorTitular_retorna200_yDeleteAsistencia_ajenoBloqueadoTitularPermitido() throws Exception {
        Grupo grupoClases = crearGrupoClases("aten-titular");
        int claseId = crearClaseComoTitular(grupoClases, hoy);
        String alumno = crearAlumno("aten-titular");
        String cuerpo = "[{\"idPerfil\":\"" + alumno + "\",\"idClase\":" + claseId + ",\"estaAtendido\":true}]";

        mockMvc.perform(post("/api/v2/atenciones")
                        .param("idClase", String.valueOf(claseId))
                        .contentType("application/json")
                        .content(cuerpo)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());
        assertEquals(1, contarAsistenciasActivas(alumno, claseId));

        // DELETE /asistencia -- instructor ajeno bloqueado, sin cambiar nada.
        mockMvc.perform(delete("/api/v2/asistencia")
                        .param("prmPerfId", alumno)
                        .param("prmClsCodigo", String.valueOf(claseId))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorB)))
                .andExpect(status().isForbidden());
        assertEquals(1, contarAsistenciasActivas(alumno, claseId), "tras el 403 la asistencia sigue activa");

        // El titular si puede eliminarla.
        mockMvc.perform(delete("/api/v2/asistencia")
                        .param("prmPerfId", alumno)
                        .param("prmClsCodigo", String.valueOf(claseId))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());
        assertEquals(0, contarAsistenciasActivas(alumno, claseId));
    }

    // ── GET de lectura: el Instructor SI puede leer grupos ajenos ───────────

    @Test
    void getAlumnosGrupoYClasesGrupo_instructorAjeno_sigueLeyendoSinBloqueo() throws Exception {
        // Grupo dedicado, con su propio alumno inscrito y su propia clase, para que
        // ambos GET tengan garantizado algo que devolver (200) sin depender de que
        // otras pruebas ya hayan poblado el grupo compartido antes que esta.
        Grupo grupoLectura = crearGrupoClases("lectura");
        String alumno = crearAlumno("get-ajeno");
        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType("application/json")
                        .content(payloadInscripcion(alumno, grupoLectura))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated());
        crearClaseComoTitular(grupoLectura, hoy);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v2/alumnosGrupo")
                        .param("categoria", grupoLectura.getCategoria())
                        .param("curso", grupoLectura.getCurso())
                        .param("anio", String.valueOf(grupoLectura.getAnio()))
                        .param("iterable", String.valueOf(grupoLectura.getIterable()))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorB)))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v2/clasesGrupo")
                        .param("categoria", grupoLectura.getCategoria())
                        .param("curso", grupoLectura.getCurso())
                        .param("anio", String.valueOf(grupoLectura.getAnio()))
                        .param("iterable", String.valueOf(grupoLectura.getIterable()))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorB)))
                .andExpect(status().isOk());
    }

    // ── Sin token ────────────────────────────────────────────────────────────

    @Test
    void endpointsProtegidos_sinToken_retornan401() throws Exception {
        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupo, hoy, instructorA)))
                .andExpect(status().isUnauthorized());
    }
}
