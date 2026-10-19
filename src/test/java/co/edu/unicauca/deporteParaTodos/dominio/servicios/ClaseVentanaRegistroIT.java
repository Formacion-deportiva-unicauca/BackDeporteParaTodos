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
 * SCRUM-80 (rediseno, bloque 2 de 3): POST /claseGrupo, DELETE /clase,
 * POST /atenciones y DELETE /asistencia respetando la VentanaRegistroAsistencia
 * y el calculo de duracion desde tbl_horario (test de integracion, BD MySQL real).
 *
 * Fixtures: 1 grupo CON una franja de horario para el dia de la semana de "hoy"
 * (prueba el calculo automatico de horas) y 1 grupo SIN ninguna franja (prueba
 * justificacion + duracion manual obligatorias). Instructor A es quien registra;
 * "coord-it" es el Coordinador que asigna prorrogas y que esta exento de plazo.
 *
 * Ejecutar este IT DOS veces seguidas debe dar el mismo resultado (limpieza en
 * @BeforeAll/@AfterAll + identificadores unicos por corrida, igual que
 * PlazoCargaAtencionesIT/ProrrogaRegistroAsistenciaIT). Una tercera corrida con
 * JAVA_TOOL_OPTIONS=-Duser.timezone=Pacific/Kiritimati prueba que el resultado
 * no cambia, porque "hoy" SIEMPRE sale del Clock inyectado (America/Bogota),
 * nunca de la zona del sistema.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class ClaseVentanaRegistroIT {

    private static final String CATEGORIA = "Deportes";
    private static final String PREFIJO_CURSO = "VentanaRegIT-%";
    private static final String PREFIJO_PERFIL = "ventanaregit-%";
    private static final String DEPORTE_SEED = "Natacion";
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
    private static final String COORD = "coord-it";

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
    private String instructorA;
    private String alumno1;
    private Grupo grupoConHorario;
    private Grupo grupoSinHorario;
    private Grupo grupoTodosLosDias;
    private LocalDate hoy;

    @BeforeAll
    void crearFixtures() {
        limpiarRestosDeCorridasAnteriores();

        hoy = LocalDate.now(clock);

        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-ventanaregit.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {9, 9, 9}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para ClaseVentanaRegistroIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        instructorA = crearInstructor("instr-a-" + sufijo);
        alumno1 = "ventanaregit-alum-1-" + sufijo;
        crearAlumnoSiNoExiste(alumno1);

        grupoConHorario = crearGrupo("CON-" + sufijo, instructorA, hoy);
        String diaDeHoy = CalculadoraDuracionClase.nombreDia(hoy.getDayOfWeek());
        horarioServicio.insertarHorario(new Horario(null, CATEGORIA, grupoConHorario.getCurso(),
                grupoConHorario.getAnio(), grupoConHorario.getIterable(), diaDeHoy, "14:00", "16:00", "Cancha", 0));

        grupoSinHorario = crearGrupo("SIN-" + sufijo, instructorA, hoy);

        // SCRUM-80 (correccion del cliente, 15 oct 2026): grupo con una franja en
        // CADA dia de la semana -- lo usan los tests de plazo/prorroga que
        // registran en fechas pasadas arbitrarias (mes anterior, etc.) y no
        // quieren que la regla de dia-en-horario (no relacionada con lo que esos
        // tests verifican) los bloquee por coincidencia de dia de la semana.
        grupoTodosLosDias = crearGrupo("TODOSDIAS-" + sufijo, instructorA, hoy);
        for (String dia : new String[] {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"}) {
            horarioServicio.insertarHorario(new Horario(null, CATEGORIA, grupoTodosLosDias.getCurso(),
                    grupoTodosLosDias.getAnio(), grupoTodosLosDias.getIterable(), dia, "06:00", "07:00", "Cancha", 0));
        }
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
        String id = "ventanaregit-" + sufijoId;
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
            // No deberia ocurrir (id con sufijo unico por corrida), pero se tolera.
        }
    }

    private Grupo crearGrupo(String sufijoCurso, String idInstructor, LocalDate fechaInscripcionApertura) {
        String curso = "VentanaRegIT-" + sufijoCurso;
        Curso datosCurso = new Curso();
        datosCurso.setNombre(curso);
        datosCurso.setCategoriaCurso(CATEGORIA);
        datosCurso.setDescripcion("Curso de prueba para ClaseVentanaRegistroIT");
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
        nuevo.setFechaCreacion(fechaInscripcionApertura);
        nuevo.setFechaInscripcionApertura(fechaInscripcionApertura);
        nuevo.setPeriodo(0);
        return grupoServicio.insertarGrupo(nuevo);
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

    private String payloadClase(Grupo grupo, LocalDate fecha, int horas, int minutos, String justificacion) {
        StringBuilder json = new StringBuilder("{")
                .append("\"idGrupoCategoria\":\"").append(grupo.getCategoria()).append("\",")
                .append("\"idGrupoCurso\":\"").append(grupo.getCurso()).append("\",")
                .append("\"idGrupoAnio\":").append(grupo.getAnio()).append(",")
                .append("\"idGrupoIterable\":").append(grupo.getIterable()).append(",")
                .append("\"idInstructor\":\"").append(instructorA).append("\",")
                .append("\"fecha\":\"").append(fecha).append("\",")
                .append("\"horas\":").append(horas).append(",")
                .append("\"minutos\":").append(minutos);
        if (justificacion != null) {
            json.append(",\"justificacion\":\"").append(justificacion).append("\"");
        }
        return json.append("}").toString();
    }

    private void asignarProrroga(LocalDate fecha, int dias) throws Exception {
        mockMvc.perform(put("/api/v2/instructores/prorroga-registro")
                .contentType("application/json")
                .content("{\"idsInstructores\":[\"" + instructorA + "\"],"
                        + "\"anio\":" + fecha.getYear() + ",\"mes\":" + fecha.getMonthValue() + ",\"dias\":" + dias + "}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isOk());
    }

    // ── POST /claseGrupo: calculo de duracion desde tbl_horario ────────────────

    @Test
    void postClase_conFranjaHoy_calculaHorasDesdeHorarioYLuegoReutilizaLaMisma() throws Exception {
        // Aislamiento de datos: atenciones_registrarDosVeces... tambien registra una
        // clase para (grupoConHorario, hoy) y el orden por defecto de JUnit 5 NO sigue
        // el orden del codigo fuente (asi salio en failsafe-reports: ese test corrio
        // ANTES que este). Sin este limpiado, el primer POST de aqui encontraria la
        // clase ya existente y ClaseRest.postClase responderia 200 en vez de 201.
        // Borrado LOGICO con el mismo criterio que eliminarClase: un DELETE fisico
        // violaria FK_ASISTENCIA_CLASE (ON DELETE RESTRICT) si ya hay asistencias.
        jdbc.update(
                "UPDATE tbl_clase SET META_ELIMINADO = 1 "
                        + "WHERE CAT_TITULO = ? AND CUR_NOMBRE = ? AND CLS_FECHA = ? AND META_ELIMINADO = 0",
                grupoConHorario.getCategoria(), grupoConHorario.getCurso(), hoy);

        // El cliente envia 9h/9min -- el servidor DEBE ignorarlos: hay franja 14:00-16:00.
        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoConHorario, hoy, 9, 9, null))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horas").value(2))
                .andExpect(jsonPath("$.minutos").value(0));

        // Segundo POST mismo grupo+fecha -- reutiliza la clase existente (200, no 201).
        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoConHorario, hoy, 0, 0, null))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horas").value(2));
    }

    @Test
    void postClase_fechaFutura_instructor_retorna422() throws Exception {
        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoConHorario, hoy.plusDays(5), 1, 0, null))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── POST /claseGrupo: dia-en-horario (correccion del cliente, 15 oct 2026) ──
    // El Instructor ya NO puede llegar a la rama de justificacion/duracion
    // manual -- si el grupo no tiene horario ese dia, se le bloquea con
    // DiaFueraDeHorarioExcepcion (GC-0024) ANTES de evaluar justificacion.

    @Test
    void postClase_instructorGrupoSinNingunHorario_retorna422Nuevo() throws Exception {
        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoSinHorario, hoy, 1, 0, null))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0024"));
    }

    @Test
    void postClase_instructorDiaSinFranjaEnGrupoConHorarioOtroDia_retorna422Nuevo() throws Exception {
        // grupoConHorario SI tiene franja, pero no en hoy.minusDays(1) (dia distinto).
        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoConHorario, hoy.minusDays(1), 1, 0, null))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0024"));
    }

    // ── POST /claseGrupo: sin franja ese dia -- justificacion + duracion manual
    // (Coordinador/Administrador -- comportamiento sin cambios, no sujetos a la
    // regla de dia-en-horario) ──────────────────────────────────────────────

    @Test
    void postClase_coordinadorSinFranjaSinJustificacion_retorna422() throws Exception {
        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoSinHorario, hoy, 1, 0, null))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0021"));
    }

    @Test
    void postClase_coordinadorSinFranjaDuracionManualInvalida_retorna422() throws Exception {
        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoSinHorario, hoy.minusDays(1), 0, 0,
                                "justificacion valida de mas de diez caracteres"))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0022"));
    }

    @Test
    void postClase_coordinadorSinFranjaConJustificacionYDuracionValidas_retorna201ConDuracionManual() throws Exception {
        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoSinHorario, hoy.minusDays(2), 2, 30,
                                "instalacion en mantenimiento, clase dictada en otro espacio"))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horas").value(2))
                .andExpect(jsonPath("$.minutos").value(30))
                .andExpect(jsonPath("$.justificacion").value("instalacion en mantenimiento, clase dictada en otro espacio"));
    }

    // ── POST /claseGrupo: ventana de registro (mes anterior, prorroga, exencion) ─

    @Test
    void postClase_instructorMesAnterior_bloqueadoLuegoPermitidoConProrroga() throws Exception {
        LocalDate fechaMesAnterior = hoy.minusMonths(1);
        // grupoTodosLosDias tiene franja TODOS los dias -- este test prueba
        // plazo/prorroga, no dia-en-horario, asi que no debe bloquearse por la
        // regla nueva sin importar en que dia de la semana caiga fechaMesAnterior.
        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoTodosLosDias, fechaMesAnterior, 1, 0, null))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isUnprocessableEntity());

        asignarProrroga(fechaMesAnterior, 5);

        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoTodosLosDias, fechaMesAnterior, 1, 0, null))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated());
    }

    @Test
    void postClase_coordinadorMesAnterior_noTieneLimiteDePlazo() throws Exception {
        LocalDate fechaMesAnterior = hoy.minusMonths(1).minusDays(1);

        mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoSinHorario, fechaMesAnterior, 1, 0,
                                "clase dictada por el coordinador, sin horario ese dia"))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isCreated());
    }

    // ── DELETE /clase: bloqueado fuera de plazo, luego habilitado con prorroga ──

    @Test
    void deleteClase_instructorMesAnterior_bloqueadoLuegoHabilitadoConProrroga() throws Exception {
        LocalDate fechaMesAnterior = hoy.minusMonths(2);
        String justificacion = "clase creada por el coordinador para probar el DELETE bloqueado";

        MvcResult creada = mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoSinHorario, fechaMesAnterior, 1, 0, justificacion))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isCreated())
                .andReturn();
        int codigo = JsonPath.read(creada.getResponse().getContentAsString(), "$.codigo");

        mockMvc.perform(delete("/api/v2/clase")
                        .param("id", String.valueOf(codigo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isUnprocessableEntity());

        asignarProrroga(fechaMesAnterior, 5);

        mockMvc.perform(delete("/api/v2/clase")
                        .param("id", String.valueOf(codigo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eliminado").value(1));
    }

    // ── POST /atenciones + DELETE /asistencia: idempotencia, reactivacion y ventana ─

    @Test
    void atenciones_registrarDosVecesActivoYLuegoEliminarYReactivar_esIdempotenteYReactiva() throws Exception {
        MvcResult creada = mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoConHorario, hoy, 0, 0, null))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andReturn();
        int codigo = JsonPath.read(creada.getResponse().getContentAsString(), "$.codigo");

        String cuerpoActivo = "[{\"idPerfil\":\"" + alumno1 + "\",\"idClase\":" + codigo + ",\"estaAtendido\":true}]";

        mockMvc.perform(post("/api/v2/atenciones")
                        .param("idClase", String.valueOf(codigo))
                        .contentType("application/json")
                        .content(cuerpoActivo)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());

        // Segunda vez (mismo alumno, misma clase, estaAtendido=true) -- idempotente, NO duplica fila.
        mockMvc.perform(post("/api/v2/atenciones")
                        .param("idClase", String.valueOf(codigo))
                        .contentType("application/json")
                        .content(cuerpoActivo)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());

        Long activas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_asistencia WHERE PERF_ID = ? AND CLS_CODIGO = ? AND META_ELIMINADO = 0",
                Long.class, alumno1, codigo);
        assertEquals(1L, activas, "registrar dos veces estaAtendido=true no debe duplicar la fila");

        // DELETE /asistencia -- eliminacion logica.
        mockMvc.perform(delete("/api/v2/asistencia")
                        .param("prmPerfId", alumno1)
                        .param("prmClsCodigo", String.valueOf(codigo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());

        // Registrar de nuevo -- debe REACTIVAR la fila existente (META_ELIMINADO=1 -> 0),
        // no quedar huerfana (hallazgo del PASO 0, corregido en este bloque).
        mockMvc.perform(post("/api/v2/atenciones")
                        .param("idClase", String.valueOf(codigo))
                        .contentType("application/json")
                        .content(cuerpoActivo)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());

        Long filasTotales = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_asistencia WHERE PERF_ID = ? AND CLS_CODIGO = ?",
                Long.class, alumno1, codigo);
        Long filasActivas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_asistencia WHERE PERF_ID = ? AND CLS_CODIGO = ? AND META_ELIMINADO = 0",
                Long.class, alumno1, codigo);
        assertEquals(1L, filasTotales, "la reactivacion no debe crear una segunda fila");
        assertEquals(1L, filasActivas, "la fila reactivada debe quedar con META_ELIMINADO=0");
    }

    @Test
    void atenciones_yAsistencia_claseVencida_bloqueaRegistroYBorradoHastaAsignarProrroga() throws Exception {
        LocalDate fechaMesAnterior = hoy.minusMonths(3);
        String justificacion = "clase creada por el coordinador para probar atenciones/asistencia vencidas";

        MvcResult creada = mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payloadClase(grupoSinHorario, fechaMesAnterior, 1, 0, justificacion))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isCreated())
                .andReturn();
        int codigo = JsonPath.read(creada.getResponse().getContentAsString(), "$.codigo");
        String cuerpo = "[{\"idPerfil\":\"" + alumno1 + "\",\"idClase\":" + codigo + ",\"estaAtendido\":true}]";

        // Instructor bloqueado para registrar (clase vencida, sin prorroga).
        mockMvc.perform(post("/api/v2/atenciones")
                        .param("idClase", String.valueOf(codigo))
                        .contentType("application/json")
                        .content(cuerpo)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isUnprocessableEntity());

        // Coordinador exento -- registra sin problema.
        mockMvc.perform(post("/api/v2/atenciones")
                        .param("idClase", String.valueOf(codigo))
                        .contentType("application/json")
                        .content(cuerpo)
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isOk());

        // Instructor bloqueado para eliminar la asistencia (misma clase vencida).
        mockMvc.perform(delete("/api/v2/asistencia")
                        .param("prmPerfId", alumno1)
                        .param("prmClsCodigo", String.valueOf(codigo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isUnprocessableEntity());

        asignarProrroga(fechaMesAnterior, 5);

        // Ahora si -- con prorroga vigente para ese mes, el Instructor puede eliminarla.
        mockMvc.perform(delete("/api/v2/asistencia")
                        .param("prmPerfId", alumno1)
                        .param("prmClsCodigo", String.valueOf(codigo))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());
    }
}
