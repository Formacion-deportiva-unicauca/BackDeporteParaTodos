package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Base64;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICategoriaCursoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICursoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IGrupoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IImagenServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInscripcionServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Categoria;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Disponibilidad;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;

/**
 * Test de integracion (BD MySQL real) para control de cupos en inscripciones.
 *
 * El @BeforeAll crea sus propios fixtures (categoria/curso/grupo con 1 cupo) a
 * traves de los servicios reales de la app -- no via SQL directo -- para que
 * cualquier cambio futuro en el modelo de dominio o en las validaciones de
 * negocio haga fallar la creacion del fixture de forma explicita, en vez de
 * insertar silenciosamente una fila que la app misma nunca produciria.
 * La creacion es idempotente (verifica existencia antes de insertar) para
 * poder correr repetidas veces contra una BD persistente en local.
 */
// app.jwt.secret: valor de prueba para que el contexto cargue sin JWT_SECRET en el entorno de CI.
// El valor real en produccion siempre viene de la variable de entorno JWT_SECRET (nunca commiteado).
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class InscripcionConcurrenciaIT {

    private static final String CATEGORIA = "Deportes";
    private static final String CURSO = "Natacion";
    // GrupoGateway.insertarGrupo() ignora el anio pedido y crea el grupo en el anio ACTUAL.
    private static final int ANIO = LocalDate.now().getYear();
    private static final int ITERABLE = 1;
    // Referencias a filas ya insertadas por el seed data de DDL-MYSQL.sql -- existen
    // desde que se carga el esquema, no las crea este fixture.
    private static final String DEPORTE_SEED = "Natacion";
    private static final String INSTRUCTOR_SEED_ID = "2";

    private static final List<String> ALUMNOS_CONCURRENCIA =
            List.of("alumno-conc-1", "alumno-conc-2", "alumno-conc-3");
    private static final String ALUMNO_OCUPA_CUPO = "alumno-it-ocupa-cupo";
    private static final String ALUMNO_EN_ESPERA = "alumno-it-espera";

    // Grupo separado (mismo curso, otro iterable) para el escenario de
    // promoverManualmente(), asi no interfiere con los otros dos tests.
    private static final int ITERABLE_PROMOCION = 2;
    // Grupo propio para el test de fechaDesvinculacion (filas nuevas, sin interferir con los demas).
    private static final int ITERABLE_FECHA_DESVINCULACION = 3;
    private static final String ALUMNO_FILLER_PROMOCION = "alumno-it-filler-promocion";
    private static final String ALUMNO_CANDIDATO_1 = "alumno-it-candidato-1";
    private static final String ALUMNO_CANDIDATO_2 = "alumno-it-candidato-2";

    @Autowired
    private IInscripcionServicio servicio;

    @Autowired
    private ICategoriaCursoServicio categoriaCursoServicio;

    @Autowired
    private ICursoServicio cursoServicio;

    @Autowired
    private IGrupoServicio grupoServicio;

    @Autowired
    private IImagenServicio imagenServicio;

    // No hay una via a nivel de servicio para registrar un alumno nuevo:
    // IAlumnoServicio.insertAlumno() esta sin implementar (retorna null). Se usa el
    // gateway (registrarAlumno crea tbl_perfil + tbl_alumno atomicamente, es el mismo
    // codigo que usa el flujo real de registro) en vez de SQL directo.
    @Autowired
    private IPerfilGateway perfilGateway;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mockMvc;

    // Imagen de fixture creada en @BeforeAll; los tests que crean su propio grupo la reutilizan.
    private Integer imagenFixtureId;

    @BeforeAll
    void crearFixtures() {
        // Categoria/Curso/Grupo exigen (en el gateway, no solo en el DDL) una imagen
        // ya existente -- CAT_IMAGEN/CUR_IMAGEN/GRP_IMAGEN son nullable en la BD, pero
        // CategoriaGateway/CursoGateway/GrupoGateway llaman repoImagen.existsById(...)
        // igual, que lanza IllegalArgumentException si el id es null. Se crea una sola
        // imagen de prueba y se reutiliza en los tres.
        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-fixture-it.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4}));
        Integer imagenId = imagenServicio.insertarImagen(imagenDto).getId();
        imagenFixtureId = imagenId;

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para InscripcionConcurrenciaIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        try {
            Curso curso = new Curso();
            curso.setNombre(CURSO);
            curso.setCategoriaCurso(CATEGORIA);
            curso.setDescripcion("Curso de prueba para InscripcionConcurrenciaIT");
            curso.setImagenId(imagenId);
            curso.setDeporte(DEPORTE_SEED);
            curso.setEstadoCurso(EstadoCurso.ACTIVO);
            curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
            cursoServicio.insertarCurso(curso);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior contra BD persistente; se reutiliza.
        }

        crearGrupoSiNoExiste(ITERABLE, 1, imagenId);
        crearGrupoSiNoExiste(ITERABLE_PROMOCION, 1, imagenId);
        crearGrupoSiNoExiste(ITERABLE_FECHA_DESVINCULACION, 1, imagenId);

        // tbl_inscripcion.PERF_ID tiene FK hacia tbl_alumno -- cada alumno usado por
        // los tests debe existir antes de poder inscribirlo.
        for (String alumnoId : ALUMNOS_CONCURRENCIA) {
            crearAlumnoSiNoExiste(alumnoId);
        }
        crearAlumnoSiNoExiste(ALUMNO_OCUPA_CUPO);
        crearAlumnoSiNoExiste(ALUMNO_EN_ESPERA);
        crearAlumnoSiNoExiste(ALUMNO_FILLER_PROMOCION);
        crearAlumnoSiNoExiste(ALUMNO_CANDIDATO_1);
        crearAlumnoSiNoExiste(ALUMNO_CANDIDATO_2);
    }

    private void crearGrupoSiNoExiste(int iterable, int cupos, Integer imagenId) {
        try {
            grupoServicio.obtenerGrupoPorId(CATEGORIA, CURSO, ANIO, iterable);
        } catch (NoExisteExcepcion e) {
            Grupo grupo = new Grupo();
            grupo.setCategoria(CATEGORIA);
            grupo.setCurso(CURSO);
            grupo.setAnio(ANIO);
            grupo.setIterable(iterable);
            grupo.setImagenGrupo(imagenId);
            grupo.setIdInstructor(INSTRUCTOR_SEED_ID);
            grupo.setCupos(cupos);
            grupo.setFechaCreacion(LocalDate.now());
            grupo.setPeriodo(1);
            grupoServicio.insertarGrupo(grupo);
        }
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

    @Test
    void inscripcion_concurrente_soloUnaPermitida_cuandoHayUnCupo() throws Exception {
        // Preparar: 3 alumnos distintos intentan inscribirse al mismo grupo con 1 cupo
        String categoria = CATEGORIA;
        String curso = CURSO;
        int anio = ANIO;
        int iterable = ITERABLE;

        List<String> alumnosIds = ALUMNOS_CONCURRENCIA;
        int hilos = alumnosIds.size();

        CountDownLatch listo = new CountDownLatch(hilos);
        CountDownLatch inicio = new CountDownLatch(1);
        AtomicInteger exitosos = new AtomicInteger(0);
        AtomicInteger rechazados = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        List<Future<?>> futures = new ArrayList<>();

        for (String alumnoId : alumnosIds) {
            futures.add(executor.submit(() -> {
                listo.countDown();
                assertDoesNotThrow(() -> inicio.await());
                try {
                    // inscribir() nunca lanza excepcion cuando el grupo esta lleno --
                    // retorna estado EN_ESPERA. "Exitoso" significa realmente obtener
                    // el cupo (INSCRITO), no solo que la llamada no haya fallado.
                    Inscripcion resultado = servicio.inscribir(
                            new Inscripcion(alumnoId, categoria, curso, anio, iterable, null, null, null));
                    if ("INSCRITO".equals(resultado.getEstado())) {
                        exitosos.incrementAndGet();
                    } else {
                        rechazados.incrementAndGet();
                    }
                } catch (Exception e) {
                    rechazados.incrementAndGet();
                }
            }));
        }

        listo.await();
        inicio.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        // Con 1 cupo disponible, exactamente 1 debe tener exito y 2 deben ser rechazados
        assertTrue(exitosos.get() <= 1,
                "Con 1 cupo, a lo sumo 1 inscripcion debe ser exitosa, pero fueron: " + exitosos.get());
        assertTrue(rechazados.get() >= 2,
                "Con 1 cupo y 3 intentos concurrentes, al menos 2 deben ser rechazados");
    }

    /**
     * Regresion HALLAZGO 3-B: salir voluntariamente de la lista de espera no debe
     * promover a nadie (no se libero ningun cupo), por lo que los cupos disponibles
     * deben permanecer iguales antes y despues.
     *
     * El test ocupa el unico cupo del grupo el mismo (en vez de depender de que
     * inscripcion_concurrente_soloUnaPermitida_cuandoHayUnCupo haya corrido antes)
     * para no depender del orden de ejecucion entre tests -- JUnit no lo garantiza.
     */
    @Test
    void salirDeListaEspera_noSobreInscribeGrupo() {
        String categoria = CATEGORIA;
        String curso = CURSO;
        int anio = ANIO;
        int iterable = ITERABLE;
        String alumnoOcupaCupo = ALUMNO_OCUPA_CUPO;
        String alumnoEnEspera = ALUMNO_EN_ESPERA;

        // Ocupa el unico cupo del grupo (o queda EN_ESPERA si ya estaba ocupado por
        // una corrida local anterior -- de cualquier forma, el grupo queda sin cupos).
        servicio.inscribir(new Inscripcion(alumnoOcupaCupo, categoria, curso, anio, iterable, null, null, null));

        Inscripcion resultado = servicio.inscribir(
                new Inscripcion(alumnoEnEspera, categoria, curso, anio, iterable, null, null, null));
        assertEquals("EN_ESPERA", resultado.getEstado(),
                "Precondicion no cumplida: se esperaba que el grupo ya estuviera sin cupos");

        Disponibilidad antes = servicio.obtenerDisponibilidad(categoria, curso, anio, iterable);

        servicio.desvincularInscripcion(alumnoEnEspera, categoria, curso, anio, iterable);

        Disponibilidad despues = servicio.obtenerDisponibilidad(categoria, curso, anio, iterable);

        assertEquals(antes.getCuposDisponibles(), despues.getCuposDisponibles(),
                "Salir de la lista de espera no debe cambiar los cupos disponibles: nadie debio ser promovido");
        assertEquals(antes.getTamanoListaEspera() - 1, despues.getTamanoListaEspera(),
                "La lista de espera debe reducirse en 1 (el alumno que salio), sin promociones");
    }

    /**
     * Regresion del mismo bug de aislamiento REPEATABLE READ que afecta a
     * inscribir(): promoverManualmente() tambien hace una lectura no bloqueante
     * (existeEnEspera) antes de tomar el lock del grupo, asi que el conteo de
     * cupos posterior podia leer un snapshot obsoleto bajo concurrencia real.
     *
     * Escenario: un grupo con 1 cupo ya ocupado y 2 candidatos en espera. Un
     * Coordinador amplia los cupos de 1 a 2 (via actualizarGrupo, sin desvincular
     * a nadie -- por eso no se dispara la auto-promocion de HALLAZGO 3-B), dejando
     * 1 cupo genuinamente libre con 2 personas en cola. Dos Coordinadores
     * promueven a los 2 candidatos al mismo tiempo: solo 1 debe tener exito.
     */
    @Test
    void promoverManualmente_concurrente_soloUnaPermitida_cuandoHayUnCupoLiberado() throws Exception {
        servicio.inscribir(new Inscripcion(
                ALUMNO_FILLER_PROMOCION, CATEGORIA, CURSO, ANIO, ITERABLE_PROMOCION, null, null, null));
        servicio.inscribir(new Inscripcion(
                ALUMNO_CANDIDATO_1, CATEGORIA, CURSO, ANIO, ITERABLE_PROMOCION, null, null, null));
        servicio.inscribir(new Inscripcion(
                ALUMNO_CANDIDATO_2, CATEGORIA, CURSO, ANIO, ITERABLE_PROMOCION, null, null, null));

        Grupo grupoActual = grupoServicio.obtenerGrupoPorId(CATEGORIA, CURSO, ANIO, ITERABLE_PROMOCION);
        grupoActual.setCupos(2);
        grupoServicio.actualizarGrupo(CATEGORIA, CURSO, ANIO, ITERABLE_PROMOCION, grupoActual);

        List<String> candidatos = List.of(ALUMNO_CANDIDATO_1, ALUMNO_CANDIDATO_2);
        int hilos = candidatos.size();

        CountDownLatch listo = new CountDownLatch(hilos);
        CountDownLatch inicio = new CountDownLatch(1);
        AtomicInteger exitosos = new AtomicInteger(0);
        AtomicInteger rechazados = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        List<Future<?>> futures = new ArrayList<>();

        for (String candidato : candidatos) {
            futures.add(executor.submit(() -> {
                listo.countDown();
                assertDoesNotThrow(() -> inicio.await());
                try {
                    // promoverManualmente() si lanza CuposAgotadosExcepcion cuando no
                    // hay cupo -- a diferencia de inscribir(), no hace falta revisar
                    // el estado del resultado.
                    servicio.promoverManualmente(candidato, CATEGORIA, CURSO, ANIO, ITERABLE_PROMOCION);
                    exitosos.incrementAndGet();
                } catch (Exception e) {
                    rechazados.incrementAndGet();
                }
            }));
        }

        listo.await();
        inicio.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertTrue(exitosos.get() <= 1,
                "Con 1 cupo liberado, a lo sumo 1 promocion manual debe ser exitosa, pero fueron: " + exitosos.get());
        assertTrue(rechazados.get() >= 1,
                "Con 1 cupo liberado y 2 candidatos concurrentes, al menos 1 debe ser rechazado");
    }

    /**
     * POST /inscripcion copia fechaDesvinculacion del body sin validarla; si el servicio la
     * dejara pasar, se persistiria una inscripcion "activa" que las consultas de cupos y de
     * limite de cursos no cuentan (exigen fechaDesvinculacion IS NULL). El servicio debe
     * ignorarla al CREAR una fila nueva, en las dos ramas (INSCRITO y EN_ESPERA).
     *
     * Alumnos con sufijo unico => siempre filas nuevas (nunca reactivacion), tambien en
     * corridas repetidas. En una BD fresca el 1o cae en INSCRITO y el 2o en EN_ESPERA
     * (grupo de 1 cupo); en reruns ambos pueden ir a EN_ESPERA -- la asercion es la misma.
     */
    @Test
    void inscribir_filaNueva_ignoraFechaDesvinculacionDelClienteYPersisteNull() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String alumnoA = "alumno-it-fecha-a-" + sufijo;
        String alumnoB = "alumno-it-fecha-b-" + sufijo;
        crearAlumnoSiNoExiste(alumnoA);
        crearAlumnoSiNoExiste(alumnoB);
        Timestamp fechaForjada = Timestamp.valueOf(LocalDateTime.now().plusYears(1));

        servicio.inscribir(new Inscripcion(alumnoA, CATEGORIA, CURSO, ANIO,
                ITERABLE_FECHA_DESVINCULACION, null, fechaForjada, null));
        servicio.inscribir(new Inscripcion(alumnoB, CATEGORIA, CURSO, ANIO,
                ITERABLE_FECHA_DESVINCULACION, null, fechaForjada, null));

        for (String alumnoId : List.of(alumnoA, alumnoB)) {
            List<Timestamp> filas = jdbc.query(
                    "SELECT INSCR_FECHADESVINCULACION FROM tbl_inscripcion "
                            + "WHERE PERF_ID = ? AND CAT_TITULO = ? AND CUR_NOMBRE = ? "
                            + "AND GRP_ANIO = ? AND GRP_ITERABLE = ?",
                    (rs, i) -> rs.getTimestamp(1),
                    alumnoId, CATEGORIA, CURSO, ANIO, ITERABLE_FECHA_DESVINCULACION);
            assertEquals(1, filas.size(), "Debe existir exactamente 1 fila para " + alumnoId);
            assertNull(filas.get(0),
                    "La fechaDesvinculacion enviada por el cliente no debe persistirse (" + alumnoId + ")");
        }

        // Consecuencia del bug: con fecha forjada ninguna fila contaba para el cupo, asi que
        // ambos alumnos quedaban INSCRITO en un grupo de 1 cupo.
        Integer inscritosEnGrupo = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_inscripcion WHERE CAT_TITULO = ? AND CUR_NOMBRE = ? "
                        + "AND GRP_ANIO = ? AND GRP_ITERABLE = ? AND INSCR_ESTADO = 'INSCRITO'",
                Integer.class, CATEGORIA, CURSO, ANIO, ITERABLE_FECHA_DESVINCULACION);
        assertTrue(inscritosEnGrupo <= 1,
                "Un grupo de 1 cupo no puede tener mas de 1 INSCRITO, pero tiene " + inscritosEnGrupo);
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

    private String bodyInscripcion(String alumnoId, int iterable, String fechaDesvinculacionForjada) {
        return "{\"alumnoId\":\"" + alumnoId + "\",\"categoria\":\"" + CATEGORIA + "\",\"curso\":\"" + CURSO
                + "\",\"anio\":" + ANIO + ",\"iterable\":" + iterable
                + ",\"fechaDesvinculacion\":\"" + fechaDesvinculacionForjada + "\"}";
    }

    private List<Object[]> filaInscripcion(String alumnoId, int iterable) {
        return jdbc.query(
                "SELECT INSCR_ESTADO, INSCR_FECHADESVINCULACION FROM tbl_inscripcion "
                        + "WHERE PERF_ID = ? AND CAT_TITULO = ? AND CUR_NOMBRE = ? AND GRP_ANIO = ? AND GRP_ITERABLE = ?",
                (rs, i) -> new Object[] { rs.getString(1), rs.getTimestamp(2) },
                alumnoId, CATEGORIA, CURSO, ANIO, iterable);
    }

    /**
     * Extremo a extremo por HTTP real (Spring MVC + Spring Security con JWT firmado + JSON ->
     * InscripcionMapper.fromDto -> servicio -> gateway -> MySQL): POST /api/v2/inscripcion con
     * una fechaDesvinculacion NO nula en el body.
     *
     * Grupo nuevo por corrida (iterable aleatorio, 1 cupo) => estados deterministas:
     * el 1er alumno obtiene el unico cupo (INSCRITO); el 2o, con el grupo ya lleno, queda EN_ESPERA.
     * Ambos envian la fecha forjada.
     */
    @Test
    void postInscripcion_http_ignoraFechaDesvinculacionForjadaYRespetaElCupo() throws Exception {
        // insertarGrupo() asigna el iterable (count + 1): se usa el que devuelve, no uno elegido.
        Grupo nuevo = new Grupo();
        nuevo.setCategoria(CATEGORIA);
        nuevo.setCurso(CURSO);
        nuevo.setImagenGrupo(imagenFixtureId);
        nuevo.setIdInstructor(INSTRUCTOR_SEED_ID);
        nuevo.setCupos(1);
        nuevo.setFechaCreacion(LocalDate.now());
        nuevo.setPeriodo(1);
        int iterableNuevo = grupoServicio.insertarGrupo(nuevo).getIterable();
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String alumnoA = "alumno-it-http-a-" + sufijo;
        String alumnoB = "alumno-it-http-b-" + sufijo;
        crearAlumnoSiNoExiste(alumnoA);
        crearAlumnoSiNoExiste(alumnoB);
        String fechaForjada = "2099-01-01T00:00:00.000+00:00";

        // 1) Hay cupo real -> INSCRITO, y la fecha forjada no se persiste ni se devuelve.
        mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(alumnoA, iterableNuevo, fechaForjada))
                .header("Authorization", "Bearer " + buildJwt("Alumno", alumnoA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("INSCRITO"))
                .andExpect(jsonPath("$.fechaDesvinculacion").doesNotExist());

        // 2) Grupo ya lleno -> EN_ESPERA (no INSCRITO), tambien enviando fecha forjada.
        mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(alumnoB, iterableNuevo, fechaForjada))
                .header("Authorization", "Bearer " + buildJwt("Alumno", alumnoB)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_ESPERA"))
                .andExpect(jsonPath("$.fechaDesvinculacion").doesNotExist());

        // Lo que quedo realmente en la BD.
        List<Object[]> filaA = filaInscripcion(alumnoA, iterableNuevo);
        List<Object[]> filaB = filaInscripcion(alumnoB, iterableNuevo);
        assertEquals(1, filaA.size());
        assertEquals(1, filaB.size());
        assertEquals("INSCRITO", filaA.get(0)[0]);
        assertNull(filaA.get(0)[1], "fechaDesvinculacion de A debe ser NULL en BD");
        assertEquals("EN_ESPERA", filaB.get(0)[0]);
        assertNull(filaB.get(0)[1], "fechaDesvinculacion de B debe ser NULL en BD");

        // Y el cupo se contabiliza bien: 1 inscrito activo, 1 en espera.
        Disponibilidad d = servicio.obtenerDisponibilidad(CATEGORIA, CURSO, ANIO, iterableNuevo);
        assertEquals(0, d.getCuposDisponibles());
        assertEquals(1, d.getTamanoListaEspera());
    }
}
