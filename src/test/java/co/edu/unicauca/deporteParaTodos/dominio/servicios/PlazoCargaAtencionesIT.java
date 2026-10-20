package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
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

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAsistenciaServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICategoriaCursoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IClaseServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICursoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IGrupoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IImagenServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IHorarioServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Categoria;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.CalculadoraDuracionClase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PeriodoAcademico;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PlazoCargaDto;

/**
 * SCRUM-80 (HE-14 HU-02): GET /api/v2/instructores/plazos-carga y PUT
 * /api/v2/instructor/{id}/plazo-carga (test de integracion, BD MySQL real).
 *
 * Fixtures: instructor A con 2 grupos del periodo VIGENTE (uno con una clase
 * con asistencia activa -> cuenta; otro con una clase cuya UNICA asistencia
 * esta eliminada logicamente -> NO cuenta) + 1 grupo propio ELIMINADO
 * logicamente (con clase y asistencia activas -> NO cuenta) + 1 grupo propio
 * de OTRO periodo (clase y asistencia activas -> NO cuenta por periodo) ->
 * el conteo final esperado de A es exactamente 1 (GENERADO). Instructor X
 * (otro instructor, periodo vigente, 1 clase con asistencia activa) prueba
 * que esa clase NO se le atribuye a A (agrupacion correcta por instructor).
 * Instructor B sin plazo ni clases -> SIN_PLAZO. Instructor C con un plazo
 * VENCIDO insertado por JDBC (la API rechaza fechas pasadas) -> VENCIDO.
 * Instructor D recibe el plazo por la API (PUT) con fecha futura -> PENDIENTE;
 * un segundo PUT con otra fecha futura reemplaza la primera (upsert, no
 * duplica fila). Instructor eliminado logicamente -> no aparece en el listado.
 *
 * "hoy"/el periodo de cada grupo se calculan con el MISMO Clock inyectado
 * (America/Bogota) que usa el servicio bajo prueba -- nunca con
 * LocalDate.now() sin zona -- para que el periodo de los fixtures siempre
 * coincida con el periodo vigente real de la corrida, sin importar la zona
 * del sistema que ejecute la build.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class PlazoCargaAtencionesIT {

    private static final String CATEGORIA = "Deportes";
    private static final String PREFIJO_CURSO = "PlazoCargaIT-%";
    private static final String PREFIJO_PERFIL = "plazocargait-%";
    private static final String DEPORTE_SEED = "Natacion";
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";

    @Autowired
    private IGrupoServicio grupoServicio;

    @Autowired
    private ICategoriaCursoServicio categoriaCursoServicio;

    @Autowired
    private ICursoServicio cursoServicio;

    @Autowired
    private IClaseServicio claseServicio;

    @Autowired
    private IAsistenciaServicio asistenciaServicio;

    @Autowired
    private IInstructorServicio instructorServicio;

    @Autowired
    private IImagenServicio imagenServicio;

    @Autowired
    private IHorarioServicio horarioServicio;

    @Autowired
    private IPerfilGateway perfilGateway;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Clock clock;

    private Integer imagenId;
    private String instructorA;
    private String instructorX;
    private String instructorB;
    private String instructorC;
    private String instructorD;
    private String instructorEliminado;
    private int anioVigente;
    private int periodoVigente;

    @BeforeAll
    void crearFixtures() {
        limpiarRestosDeCorridasAnteriores();

        PeriodoAcademico vigente = PeriodoAcademico.vigente(clock);
        anioVigente = vigente.getAnio();
        periodoVigente = vigente.getPeriodo();
        LocalDate hoy = LocalDate.now(clock);
        // SCRUM-80 (rediseno, bloque 2): se usa minusMonths (no plusMonths) porque
        // la nueva regla de ClaseServicio.insertarClase rechaza cualquier fecha
        // futura para TODOS los roles -- una fecha pasada cruza el mismo limite
        // de periodo academico sin violar esa regla.
        LocalDate fechaOtroPeriodo = hoy.minusMonths(6);

        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-plazocarga-it.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {7, 7, 7}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para PlazoCargaAtencionesIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        instructorA = crearInstructor("instr-a-" + sufijo);
        instructorX = crearInstructor("instr-x-" + sufijo);
        instructorB = crearInstructor("instr-b-" + sufijo);
        instructorC = crearInstructor("instr-c-" + sufijo);
        instructorD = crearInstructor("instr-d-" + sufijo);
        instructorEliminado = crearInstructor("instr-elim-" + sufijo);
        instructorServicio.eliminarInstructor(instructorEliminado);

        String alumno1 = "plazocargait-alum-1-" + sufijo;
        String alumno2 = "plazocargait-alum-2-" + sufijo;
        String alumno3 = "plazocargait-alum-3-" + sufijo;
        String alumno4 = "plazocargait-alum-4-" + sufijo;
        crearAlumnoSiNoExiste(alumno1);
        crearAlumnoSiNoExiste(alumno2);
        crearAlumnoSiNoExiste(alumno3);
        crearAlumnoSiNoExiste(alumno4);

        // Grupo A1 (vigente): 1 clase con asistencia ACTIVA -> SI cuenta.
        Grupo grupoA1 = crearGrupo("A1-" + sufijo, instructorA, hoy);
        Clase claseA1 = crearClase(grupoA1, instructorA, hoy, "coord-it", "Coordinador");
        asistenciaServicio.InsertarAsistencia(new Asistencia(alumno1, claseA1.getCodigo(), 0));

        // Grupo A2 (vigente): 1 clase cuya UNICA asistencia esta ELIMINADA -> NO cuenta.
        // SCRUM-80 (rediseno, bloque 2): minusDays (no plusDays) -- ClaseServicio
        // ya rechaza cualquier fecha futura; una fecha pasada distinta de "hoy"
        // alcanza igual para que sea una fila de clase distinta a claseA1.
        Grupo grupoA2 = crearGrupo("A2-" + sufijo, instructorA, hoy);
        Clase claseA2 = crearClase(grupoA2, instructorA, hoy.minusDays(1), "coord-it", "Coordinador");
        asistenciaServicio.InsertarAsistencia(new Asistencia(alumno2, claseA2.getCodigo(), 0));
        asistenciaServicio.eliminarAsistencia(alumno2, claseA2.getCodigo().longValue(), "coord-it", "Coordinador");

        // Grupo A3 (vigente, pero el GRUPO queda eliminado logicamente): clase +
        // asistencia activas, pero el grupo contenedor esta eliminado -> NO cuenta.
        Grupo grupoA3 = crearGrupo("A3-" + sufijo, instructorA, hoy);
        Clase claseA3 = crearClase(grupoA3, instructorA, hoy.minusDays(2), "coord-it", "Coordinador");
        asistenciaServicio.InsertarAsistencia(new Asistencia(alumno3, claseA3.getCodigo(), 0));
        grupoServicio.eliminarGrupo(CATEGORIA, grupoA3.getCurso(), grupoA3.getAnio(), grupoA3.getIterable());

        // Grupo A4 de OTRO periodo (apertura -6 meses): clase + asistencia activas,
        // pero el periodo del grupo no es el vigente -> NO cuenta.
        Grupo grupoA4 = crearGrupo("A4-" + sufijo, instructorA, fechaOtroPeriodo);
        Clase claseA4 = crearClase(grupoA4, instructorA, fechaOtroPeriodo, "coord-it", "Coordinador");
        asistenciaServicio.InsertarAsistencia(new Asistencia(alumno4, claseA4.getCodigo(), 0));

        // Grupo del instructor X (vigente): 1 clase con asistencia ACTIVA -> prueba
        // que esta clase se le atribuye a X y NUNCA a A (agrupacion por instructor).
        Grupo grupoX1 = crearGrupo("X1-" + sufijo, instructorX, hoy);
        Clase claseX1 = crearClase(grupoX1, instructorX, hoy, "coord-it", "Coordinador");
        asistenciaServicio.InsertarAsistencia(new Asistencia(alumno1, claseX1.getCodigo(), 0));

        // Instructor C: plazo VENCIDO insertado por JDBC -- por la API no se puede
        // crear un plazo con fecha pasada (422).
        jdbc.update("""
                INSERT INTO tbl_plazo_carga_atenciones
                    (META_ELIMINADO, PERF_ID, PLZ_ANIO, PLZ_PERIODO, PLZ_FECHA_LIMITE, PLZ_COORDINADOR, PLZ_FECHA_ASIGNACION)
                VALUES (0, ?, ?, ?, ?, ?, ?)
                """,
                instructorC, anioVigente, periodoVigente, hoy.minusDays(10), "coord-it", LocalDateTime.now(clock).minusDays(10));
    }

    @AfterAll
    void limpiarAlTerminar() {
        limpiarRestosDeCorridasAnteriores();
    }

    // Orden de FK: asistencia -> clase -> grupo -> plazo_carga -> curso ->
    // instructor -> alumno -> perfil. NO toca tbl_categoria_curso (compartida).
    private void limpiarRestosDeCorridasAnteriores() {
        // Todas las asistencias de este IT usan un alumno con PREFIJO_PERFIL, asi
        // que este unico DELETE ya cubre las asistencias de todas las clases de
        // abajo (deben borrarse antes que tbl_clase por FK_ASISTENCIA_CLASE).
        jdbc.update("DELETE FROM tbl_asistencia WHERE PERF_ID LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_clase WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_grupo WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_plazo_carga_atenciones WHERE PERF_ID LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_curso WHERE cat_titulo = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_instructor WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_alumno WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_perfil WHERE perf_id LIKE ?", PREFIJO_PERFIL);
    }

    private String crearInstructor(String sufijoId) {
        String id = "plazocargait-" + sufijoId;
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
        String curso = "PlazoCargaIT-" + sufijoCurso;
        Curso datosCurso = new Curso();
        datosCurso.setNombre(curso);
        datosCurso.setCategoriaCurso(CATEGORIA);
        datosCurso.setDescripcion("Curso de prueba para PlazoCargaAtencionesIT");
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
        // El periodo lo deriva GrupoGateway.insertarGrupo() de fechaInscripcionApertura
        // (periodo <= 0 -> se calcula) -- no se fuerza aqui, se deja que la regla
        // real (la misma que PeriodoAcademico reutiliza) lo calcule.
        nuevo.setPeriodo(0);
        // Los grupos se crean con insertarGrupo y se usa el iterable que DEVUELVE.
        Grupo insertado = grupoServicio.insertarGrupo(nuevo);
        crearHorariosParaGrupo(insertado);
        return insertado;
    }

    private void crearHorariosParaGrupo(Grupo grupo) {
        for (DayOfWeek dia : DayOfWeek.values()) {
            Horario h = new Horario();
            h.setCategoria(grupo.getCategoria());
            h.setCurso(grupo.getCurso());
            h.setAnio(grupo.getAnio());
            h.setIterable(grupo.getIterable());
            h.setDia(CalculadoraDuracionClase.nombreDia(dia));
            h.setHoraInicio("08:00");
            h.setHoraFin("10:00");
            h.setEscenario("Cancha IT");
            h.setEliminado(0);
            horarioServicio.insertarHorario(h);
        }
    }

    private Clase crearClase(Grupo grupo, String idInstructor, LocalDate fecha,
            String perfIdLlamador, String rolLlamador) {
        Clase datos = new Clase();
        datos.setCategoria(grupo.getCategoria());
        datos.setCurso(grupo.getCurso());
        datos.setAnio(grupo.getAnio());
        datos.setIterable(grupo.getIterable());
        datos.setIdInstructor(idInstructor);
        datos.setFecha(fecha);
        datos.setHoras(1);
        datos.setMinutos(0);
        return claseServicio.insertarClase(datos, perfIdLlamador, rolLlamador);
    }

    private String buildJwt(String rol, String perfId) {
        byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(new OctetSequenceKey.Builder(key).build()));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(perfId + "@it.unicauca.edu.co")
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(3600))
                .claim("rol", rol)
                .claim("perf_id", perfId)
                .build();
        return new NimbusJwtEncoder(source)
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    void obtenerPlazosCarga_servicio_instructorA_cuentaSoloLaClaseValida() {
        List<PlazoCargaDto> resultado = instructorServicio.obtenerPlazosCarga();

        PlazoCargaDto dtoA = resultado.stream().filter(d -> d.getIdInstructor().equals(instructorA))
                .findFirst().orElseThrow(() -> new AssertionError("instructor A no aparece en el listado"));
        assertEquals(1, dtoA.getClasesConAsistencia(),
                "solo la clase A1 (vigente, grupo activo, asistencia activa) debe contar; "
                        + "A2 (asistencia eliminada), A3 (grupo eliminado) y A4 (otro periodo) no");
        assertEquals(EstadoReporte.GENERADO.name(), dtoA.getEstadoReporte());
    }

    @Test
    void obtenerPlazosCarga_servicio_instructorX_noAbsorbeLaClaseDeA() {
        List<PlazoCargaDto> resultado = instructorServicio.obtenerPlazosCarga();

        PlazoCargaDto dtoX = resultado.stream().filter(d -> d.getIdInstructor().equals(instructorX))
                .findFirst().orElseThrow(() -> new AssertionError("instructor X no aparece en el listado"));
        assertEquals(1, dtoX.getClasesConAsistencia(),
                "la clase de X debe contar para X, no para A (agrupacion correcta por g.PERF_ID)");
    }

    @Test
    void obtenerPlazosCarga_servicio_instructorB_sinPlazoNiClases_retornaSinPlazo() {
        List<PlazoCargaDto> resultado = instructorServicio.obtenerPlazosCarga();

        PlazoCargaDto dtoB = resultado.stream().filter(d -> d.getIdInstructor().equals(instructorB))
                .findFirst().orElseThrow(() -> new AssertionError("instructor B no aparece en el listado"));
        assertEquals(EstadoReporte.SIN_PLAZO.name(), dtoB.getEstadoReporte());
        assertEquals(0, dtoB.getClasesConAsistencia());
        assertFalse(dtoB.isPlazoVencido());
    }

    @Test
    void obtenerPlazosCarga_servicio_instructorC_plazoVencidoPorJdbc_retornaVencido() {
        List<PlazoCargaDto> resultado = instructorServicio.obtenerPlazosCarga();

        PlazoCargaDto dtoC = resultado.stream().filter(d -> d.getIdInstructor().equals(instructorC))
                .findFirst().orElseThrow(() -> new AssertionError("instructor C no aparece en el listado"));
        assertEquals(EstadoReporte.VENCIDO.name(), dtoC.getEstadoReporte());
        assertTrue(dtoC.isPlazoVencido());
        assertEquals(0, dtoC.getClasesConAsistencia());
    }

    @Test
    void obtenerPlazosCarga_servicio_instructorEliminado_noApareceEnElListado() {
        List<PlazoCargaDto> resultado = instructorServicio.obtenerPlazosCarga();

        assertTrue(resultado.stream().noneMatch(d -> d.getIdInstructor().equals(instructorEliminado)));
    }

    @Test
    void asignarPlazoCarga_servicio_instructorD_fechaFuturaYUnSegundoPutReemplazaLaPrimera() {
        LocalDate primeraFecha = LocalDate.now(clock).plusDays(5);
        LocalDate segundaFecha = LocalDate.now(clock).plusDays(20);

        PlazoCargaDto primero = instructorServicio.asignarPlazoCarga(instructorD, primeraFecha, "coord-it");
        assertEquals(primeraFecha.toString(), primero.getFechaLimite());
        assertEquals(EstadoReporte.PENDIENTE.name(), primero.getEstadoReporte());

        PlazoCargaDto segundo = instructorServicio.asignarPlazoCarga(instructorD, segundaFecha, "coord-it");
        assertEquals(segundaFecha.toString(), segundo.getFechaLimite());

        Long filas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_plazo_carga_atenciones WHERE PERF_ID = ? AND PLZ_ANIO = ? AND PLZ_PERIODO = ?",
                Long.class, instructorD, anioVigente, periodoVigente);
        assertEquals(1L, filas, "el segundo PUT debe REEMPLAZAR la fila existente, nunca duplicarla");
    }

    @Test
    void obtenerPlazosCarga_http_coordinador_retorna200ConLosEstadosEsperados() throws Exception {
        mockMvc.perform(get("/api/v2/instructores/plazos-carga")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idInstructor=='" + instructorA + "')].estadoReporte").value("GENERADO"))
                .andExpect(jsonPath("$[?(@.idInstructor=='" + instructorB + "')].estadoReporte").value("SIN_PLAZO"))
                .andExpect(jsonPath("$[?(@.idInstructor=='" + instructorC + "')].estadoReporte").value("VENCIDO"))
                .andExpect(jsonPath("$[?(@.idInstructor=='" + instructorEliminado + "')]").isEmpty());
    }

    @Test
    void asignarPlazoCarga_http_fechaPasada_retorna422() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v2/instructor/" + instructorB + "/plazo-carga")
                        .contentType("application/json")
                        .content("{\"fechaLimite\":\"2000-01-01\"}")
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isUnprocessableEntity());
    }
}
