package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IHorarioServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IImagenServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInscripcionServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.CruceHorarioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Categoria;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.CruceHorarios;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;

/**
 * Cruce de horarios y concurrencia (test de integracion, BD MySQL real). Cada escenario crea sus propios grupos (iterable
 * nuevo) y alumnos, asi que no depende de lo que haya dejado una corrida anterior contra una BD persistente.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class InscripcionCruceIT {

    private static final String CATEGORIA = "Deportes";
    private static final String CURSO = "InscripcionCruceIT-Curso";
    private static final String DEPORTE_SEED = "Natacion";
    private static final String INSTRUCTOR_SEED_ID = "2";
    private static final String COORD = "coord-it";

    @Autowired
    private IInscripcionServicio servicio;
    @Autowired
    private IInscripcionGateway inscripcionGateway;
    @Autowired
    private ICategoriaCursoServicio categoriaCursoServicio;
    @Autowired
    private ICursoServicio cursoServicio;
    @Autowired
    private IGrupoServicio grupoServicio;
    @Autowired
    private IHorarioServicio horarioServicio;
    @Autowired
    private IImagenServicio imagenServicio;
    @Autowired
    private IPerfilGateway perfilGateway;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Clock clock;

    private int anio;
    private Integer imagenId;

    @BeforeAll
    void crearFixtures() {
        anio = LocalDate.now(clock).getYear();

        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-inscripcion-cruce-it.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {7, 7, 7}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para InscripcionCruceIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }
        try {
            Curso curso = new Curso();
            curso.setNombre(CURSO);
            curso.setCategoriaCurso(CATEGORIA);
            curso.setDescripcion("Curso de prueba para InscripcionCruceIT");
            curso.setImagenId(imagenId);
            curso.setDeporte(DEPORTE_SEED);
            curso.setEstadoCurso(EstadoCurso.ACTIVO);
            curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
            cursoServicio.insertarCurso(curso);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior contra BD persistente; se reutiliza.
        }
    }

    // ── Por HTTP ──────────────────────────────────────────────────────────────

    @Test
    void conCruce_http422ConElMensajeYLaFilaNoSeCrea() throws Exception {
        int g1 = crearGrupo(5, franja("MARTES", "10:00", "12:00"));
        int g2 = crearGrupo(5, franja("MARTES", "11:00", "13:00"));
        String alumno = nuevoAlumno();
        inscribir(alumno, g1);

        MvcResult respuesta = mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(alumno, g2))
                .header("Authorization", "Bearer " + buildJwt("Alumno", alumno)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        String cuerpo = respuesta.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String esperado = "Cruce de horario, Se cruza con " + CURSO + ", grupo " + CruceHorarios.letraDeIterable(g1)
                + ", martes 10:00-12:00";
        assertTrue(cuerpo.contains(esperado), "mensaje esperado: " + esperado + " -- recibido: " + cuerpo);
        assertTrue(cuerpo.contains("GC-0031"), cuerpo);
        assertThrows(NoExisteExcepcion.class, () -> inscripcionGateway.obtenerInscripcion(alumno, CATEGORIA, CURSO, anio, g2));
    }

    @Test
    void conCruce_tambienSeRechazaUnirseALaListaDeEspera() {
        int g1 = crearGrupo(5, franja("MARTES", "10:00", "12:00"));
        int llenoConCruce = crearGrupo(1, franja("MARTES", "10:30", "11:30"));
        inscribir(nuevoAlumno(), llenoConCruce);   // lo llena
        String alumno = nuevoAlumno();
        inscribir(alumno, g1);

        assertThrows(CruceHorarioExcepcion.class, () -> inscribir(alumno, llenoConCruce));
        assertThrows(NoExisteExcepcion.class, () -> inscripcionGateway.obtenerInscripcion(alumno, CATEGORIA, CURSO, anio, llenoConCruce));
    }

    @Test
    void sinCruce_cualquierCantidadDeInscripcionesEsValida_yElBordeExactoNoEsCruce() {
        String alumno = nuevoAlumno();
        int base = crearGrupo(5, franja("MARTES", "10:00", "12:00"));
        inscribir(alumno, base);
        // Borde exacto: empieza justo cuando termina el otro.
        assertEquals("INSCRITO", inscribir(alumno, crearGrupo(5, franja("MARTES", "12:00", "14:00"))).getEstado());
        // Otros dias, y un grupo sin horario: sin tope de cursos, todas son validas.
        assertEquals("INSCRITO", inscribir(alumno, crearGrupo(5, franja("LUNES", "10:00", "12:00"))).getEstado());
        assertEquals("INSCRITO", inscribir(alumno, crearGrupo(5, franja("JUEVES", "10:00", "12:00"))).getEstado());
        assertEquals("INSCRITO", inscribir(alumno, crearGrupo(5, franja("VIERNES", "10:00", "12:00"))).getEstado());
        assertEquals("INSCRITO", inscribir(alumno, crearGrupo(5)).getEstado());
    }

    @Test
    void horariosConDatosSucios_nuncaDan500_seIgnoran() {
        String alumno = nuevoAlumno();
        int sucio = crearGrupo(5, franja("MARTES", "xx", "doce"), franja("FUNDAY", "10:00", "12:00"),
                franja("MARTES", "12:00", "10:00"), franja("MARTES", "8:00:00", "9:00"));
        inscribir(alumno, sucio);

        int limpio = crearGrupo(5, franja("MARTES", "10:00", "12:00"));
        assertEquals("INSCRITO", inscribir(alumno, limpio).getEstado());
    }

    @Test
    void periodoDistinto_noSeComparan() {
        String alumno = nuevoAlumno();
        inscribir(alumno, crearGrupoConPeriodo(5, 1, franja("MARTES", "10:00", "12:00")));
        assertEquals("INSCRITO", inscribir(alumno, crearGrupoConPeriodo(5, 2, franja("MARTES", "10:00", "12:00"))).getEstado());
    }

    @Test
    void enEsperaNoCuentaParaElCruce_yDesvinculadaTampoco() {
        String alumno = nuevoAlumno();
        int lleno = crearGrupo(1, franja("MARTES", "10:00", "12:00"));
        inscribir(nuevoAlumno(), lleno);
        assertEquals("EN_ESPERA", inscribir(alumno, lleno).getEstado());
        // Estar EN_ESPERA en un grupo no impide inscribirse en otro que se cruza.
        int g2 = crearGrupo(5, franja("MARTES", "11:00", "13:00"));
        assertEquals("INSCRITO", inscribir(alumno, g2).getEstado());

        // Tras desvincularse del grupo, su franja ya no cuenta.
        servicio.desvincularInscripcion(alumno, CATEGORIA, CURSO, anio, g2, COORD, "Coordinador");
        int g3 = crearGrupo(5, franja("MARTES", "11:30", "12:30"));
        assertEquals("INSCRITO", inscribir(alumno, g3).getEstado());
    }

    // ── Promociones ───────────────────────────────────────────────────────────

    /** Grupo P (1 cupo, martes 10-12) con X inscrito y la cola [s1 con cruce despues, s2 libre]. */
    private Escenario colaConCruce() {
        Escenario e = new Escenario();
        e.p = crearGrupo(1, franja("MARTES", "10:00", "12:00"));
        e.otro = crearGrupo(5, franja("MARTES", "11:00", "12:00"));
        e.x = nuevoAlumno();
        e.s1 = nuevoAlumno();
        e.s2 = nuevoAlumno();
        inscribir(e.x, e.p);
        // La cola se escribe con fechas explicitas (el orden de la lista es por fecha de inscripcion).
        esperar(e.s1, e.p, "2026-01-01 10:00:00");
        esperar(e.s2, e.p, "2026-01-01 10:00:01");
        // s1 se inscribe despues en otro grupo que se cruza con P: estar EN_ESPERA en P no lo impedia.
        assertEquals("INSCRITO", inscribir(e.s1, e.otro).getEstado());
        return e;
    }

    private static class Escenario {
        int p;
        int otro;
        String x;
        String s1;
        String s2;
    }

    @Test
    void promocionAutomatica_saltaAlAlumnoConCruce_yPromueveAlSiguiente() {
        Escenario e = colaConCruce();

        servicio.desvincularInscripcion(e.x, CATEGORIA, CURSO, anio, e.p, COORD, "Coordinador");

        assertEquals("EN_ESPERA", estado(e.s1, e.p), "s1 tendria un cruce: se queda en espera");
        assertEquals("INSCRITO", estado(e.s2, e.p), "se promueve al siguiente de la cola");
    }

    @Test
    void promocionAutomatica_todosLosDeLaColaConCruce_nadieEsPromovido_yElCupoQuedaLibre() {
        Escenario e = colaConCruce();
        // s2 tambien queda con cruce: se inscribe en otro grupo que se cruza con P.
        inscribir(e.s2, crearGrupo(5, franja("MARTES", "10:00", "11:00")));

        servicio.desvincularInscripcion(e.x, CATEGORIA, CURSO, anio, e.p, COORD, "Coordinador");

        assertEquals("EN_ESPERA", estado(e.s1, e.p));
        assertEquals("EN_ESPERA", estado(e.s2, e.p));
        assertEquals(0, inscripcionGateway.contarInscripcionesActivasGrupo(CATEGORIA, CURSO, anio, e.p));
    }

    @Test
    void ampliacionDeCupos_saltaAlAlumnoConCruce() {
        Escenario e = colaConCruce();

        ampliarCupos(e.p, 2);

        assertEquals("EN_ESPERA", estado(e.s1, e.p));
        assertEquals("INSCRITO", estado(e.s2, e.p));
    }

    @Test
    void promocionManual_conCruce_seRechazaYLaFilaQuedaIgual() {
        Escenario e = colaConCruce();
        ampliarCuposSinPromover(e.p, 2);
        Inscripcion antes = inscripcionGateway.obtenerInscripcion(e.s1, CATEGORIA, CURSO, anio, e.p);

        assertThrows(CruceHorarioExcepcion.class, () -> servicio.promoverManualmente(e.s1, CATEGORIA, CURSO, anio, e.p));

        Inscripcion despues = inscripcionGateway.obtenerInscripcion(e.s1, CATEGORIA, CURSO, anio, e.p);
        assertEquals("EN_ESPERA", despues.getEstado());
        assertEquals(antes.getFechaInscripcion(), despues.getFechaInscripcion());
        // Quien no tiene cruce se promueve sin problema.
        assertEquals("INSCRITO", servicio.promoverManualmente(e.s2, CATEGORIA, CURSO, anio, e.p).getEstado());
    }

    // ── Concurrencia ──────────────────────────────────────────────────────────

    @Test
    void dosInscripcionesDelMismoAlumnoEnGruposQueSeCruzan_ganaSoloUna() throws Exception {
        int g1 = crearGrupo(500, franja("MARTES", "10:00", "12:00"));
        int g2 = crearGrupo(500, franja("MARTES", "11:00", "13:00"));
        for (int ronda = 0; ronda < 20; ronda++) {
            String alumno = nuevoAlumno();
            List<Object> resultados = enParalelo(() -> inscribir(alumno, g1), () -> inscribir(alumno, g2));

            long exitos = resultados.stream().filter(r -> r instanceof Inscripcion).count();
            long cruces = resultados.stream().filter(r -> r instanceof CruceHorarioExcepcion).count();
            assertEquals(1, exitos, "ronda " + ronda + ": debe ganar exactamente una: " + resultados);
            assertEquals(1, cruces, "ronda " + ronda + ": la otra debe recibir el cruce: " + resultados);
            long activos = (esInscrito(alumno, g1) ? 1 : 0) + (esInscrito(alumno, g2) ? 1 : 0);
            assertEquals(1, activos, "ronda " + ronda + ": el alumno nunca queda inscrito en los dos");
        }
    }

    @Test
    void alumnoInexistente_elLockNoFalla_yEsLaFKLaQueRechazaComoAntesDeEstePR() {
        int g = crearGrupo(5, franja("MARTES", "10:00", "12:00"));   // con horario: SI se intenta bloquear al alumno
        String fantasma = "alumno-inexistente-" + UUID.randomUUID().toString().substring(0, 8);

        // Sin fila de alumno no hay nada que bloquear (bloquearAlumno devuelve false) ni inscripciones que cruzar: sigue el
        // camino de siempre y es la FK de tbl_inscripcion la que rechaza el insert. Es el comportamiento que ya existia
        // (esa excepcion no tiene manejador propio: llega como 500); el cruce NO lo cambia ni lo enmascara.
        Throwable error = assertThrows(Throwable.class, () -> inscribir(fantasma, g));

        assertTrue(error instanceof org.springframework.dao.DataIntegrityViolationException, String.valueOf(error));
        assertFalse(error instanceof CruceHorarioExcepcion);
        assertThrows(NoExisteExcepcion.class, () -> inscripcionGateway.obtenerInscripcion(fantasma, CATEGORIA, CURSO, anio, g));
    }

    @Test
    void desvincularEInscribirSobreElMismoGrupo_nadieQuedaEnEsperaConCupoLibre() throws Exception {
        for (int ronda = 0; ronda < 12; ronda++) {
            int g = crearGrupo(1);
            String a = nuevoAlumno();
            String c = nuevoAlumno();
            inscribir(a, g);   // ocupa el unico cupo

            List<Object> resultados = enParalelo(
                    () -> servicio.desvincularInscripcion(a, CATEGORIA, CURSO, anio, g, COORD, "Coordinador"),
                    () -> inscribir(c, g));

            sinErrores(resultados, ronda);
            // Ocurra en el orden que ocurra, el cupo liberado tiene que acabar en c: o entra directo (si a ya salio)
            // o queda en espera y la promocion de la desvinculacion lo sube. Nunca "en espera con cupo libre".
            assertEquals("INSCRITO", estado(c, g), "ronda " + ronda);
            assertEquals(1, inscripcionGateway.contarInscripcionesActivasGrupo(CATEGORIA, CURSO, anio, g));
        }
    }

    @Test
    void dosAmpliacionesSimultaneasConAlumnosEnComun_sinDeadlock() throws Exception {
        for (int ronda = 0; ronda < 15; ronda++) {
            int g1 = crearGrupo(1, franja("LUNES", "10:00", "12:00"));
            int g2 = crearGrupo(1, franja("MARTES", "10:00", "12:00"));
            inscribir(nuevoAlumno(), g1);
            inscribir(nuevoAlumno(), g2);
            // Mismos tres alumnos en las dos colas, en ORDEN OPUESTO (el orden de la cola es el de los locks si no se ordenan).
            String s1 = nuevoAlumno();
            String s2 = nuevoAlumno();
            String s3 = nuevoAlumno();
            esperar(s1, g1, "2026-01-01 10:00:01");
            esperar(s2, g1, "2026-01-01 10:00:02");
            esperar(s3, g1, "2026-01-01 10:00:03");
            esperar(s3, g2, "2026-01-01 10:00:01");
            esperar(s2, g2, "2026-01-01 10:00:02");
            esperar(s1, g2, "2026-01-01 10:00:03");

            List<Object> resultados = enParalelo(() -> ampliarCupos(g1, 4), () -> ampliarCupos(g2, 4));

            sinErrores(resultados, ronda);
            for (String s : List.of(s1, s2, s3)) {
                assertEquals("INSCRITO", estado(s, g1), "ronda " + ronda);
                assertEquals("INSCRITO", estado(s, g2), "ronda " + ronda);
            }
        }
    }

    // ── Ayudas ────────────────────────────────────────────────────────────────

    private Inscripcion inscribir(String alumnoId, int iterable) {
        return servicio.inscribir(new Inscripcion(alumnoId, CATEGORIA, CURSO, anio, iterable, null, null, null),
                COORD, "Coordinador");
    }

    private String estado(String alumnoId, int iterable) {
        return inscripcionGateway.obtenerInscripcion(alumnoId, CATEGORIA, CURSO, anio, iterable).getEstado();
    }

    private boolean esInscrito(String alumnoId, int iterable) {
        try {
            Inscripcion i = inscripcionGateway.obtenerInscripcion(alumnoId, CATEGORIA, CURSO, anio, iterable);
            return "INSCRITO".equals(i.getEstado()) && i.getFechaDesvinculacion() == null;
        } catch (NoExisteExcepcion e) {
            return false;
        }
    }

    /** Escribe una fila EN_ESPERA con fecha explicita (el orden de la cola es por fecha de inscripcion). */
    private void esperar(String alumnoId, int iterable, String fecha) {
        inscripcionGateway.guardarInscripcion(new Inscripcion(alumnoId, CATEGORIA, CURSO, anio, iterable,
                Timestamp.valueOf(fecha), null, "EN_ESPERA"));
    }

    private Grupo ampliarCupos(int iterable, int cupos) {
        Grupo ampliar = new Grupo();
        ampliar.setCupos(cupos);
        ampliar.setIdInstructor(INSTRUCTOR_SEED_ID);
        return grupoServicio.actualizarGrupo(CATEGORIA, CURSO, anio, iterable, ampliar);
    }

    /** Sube los cupos SIN la auto-promocion de GrupoServicio (queda cupo libre con gente esperando). */
    private void ampliarCuposSinPromover(int iterable, int cupos) {
        Grupo ampliar = new Grupo();
        ampliar.setCupos(cupos);
        ampliar.setIdInstructor(INSTRUCTOR_SEED_ID);
        grupoGateway().actualizarGrupo(CATEGORIA, CURSO, anio, iterable, ampliar);
    }

    @Autowired
    private co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway grupoGateway;

    private co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway grupoGateway() {
        return grupoGateway;
    }

    private static void sinErrores(List<Object> resultados, int ronda) {
        for (Object r : resultados) {
            assertFalse(r instanceof Throwable, "ronda " + ronda + ": " + r);
        }
    }

    /** Lanza las dos tareas a la vez (compuerta comun) y devuelve lo que cada una devolvio o la excepcion que lanzo. */
    private List<Object> enParalelo(Callable<Object> a, Callable<Object> b) throws Exception {
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Object>> futuros = new ArrayList<>();
            for (Callable<Object> tarea : List.of(a, b)) {
                futuros.add(pool.submit(() -> {
                    listos.countDown();
                    salida.await();
                    try {
                        return tarea.call();
                    } catch (Throwable t) {
                        return t;
                    }
                }));
            }
            listos.await(10, TimeUnit.SECONDS);
            salida.countDown();
            List<Object> resultados = new ArrayList<>();
            for (Future<Object> f : futuros) {
                resultados.add(f.get(60, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    private Horario franja(String dia, String ini, String fin) {
        Horario h = new Horario();
        h.setDia(dia);
        h.setHoraInicio(ini);
        h.setHoraFin(fin);
        h.setEscenario("Escenario IT");
        return h;
    }

    private int crearGrupo(int cupos, Horario... horarios) {
        return crearGrupoConPeriodo(cupos, 1, horarios);
    }

    /** Crea un grupo nuevo (iterable propio, asignado por insertarGrupo) con sus horarios y devuelve su iterable. */
    private int crearGrupoConPeriodo(int cupos, int periodo, Horario... horarios) {
        Grupo grupo = new Grupo();
        grupo.setCategoria(CATEGORIA);
        grupo.setCurso(CURSO);
        grupo.setImagenGrupo(imagenId);
        grupo.setIdInstructor(INSTRUCTOR_SEED_ID);
        grupo.setCupos(cupos);
        grupo.setFechaCreacion(LocalDate.now(clock));
        grupo.setFechaIncripcionCierre(LocalDate.now(clock).plusYears(1));
        grupo.setFechaInscripcionApertura(LocalDate.now(clock));
        grupo.setPeriodo(periodo);
        int iterable = grupoServicio.insertarGrupo(grupo).getIterable();
        for (Horario h : horarios) {
            Horario copia = new Horario();
            copia.setCategoria(CATEGORIA);
            copia.setCurso(CURSO);
            copia.setAnio(anio);
            copia.setIterable(iterable);
            copia.setDia(h.getDia());
            copia.setHoraInicio(h.getHoraInicio());
            copia.setHoraFin(h.getHoraFin());
            copia.setEscenario(h.getEscenario());
            horarioServicio.insertarHorario(copia);
        }
        return iterable;
    }

    private String nuevoAlumno() {
        String alumnoId = "alumno-it-cruce-" + UUID.randomUUID().toString().substring(0, 8);
        Perfil perfil = new Perfil();
        perfil.setId(alumnoId);
        perfil.setNombre("Alumno Prueba " + alumnoId);
        perfil.setCorreo(alumnoId + "@it.unicauca.edu.co");
        perfil.setTipoId("CC");
        perfil.setSexo("M");
        perfil.setTipoAlumno("Estudiante");
        perfilGateway.registrarAlumno(perfil);
        return alumnoId;
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

    private String bodyInscripcion(String alumnoId, int iterable) {
        return "{\"alumnoId\":\"" + alumnoId + "\",\"categoria\":\"" + CATEGORIA
                + "\",\"curso\":\"" + CURSO + "\",\"anio\":" + anio + ",\"iterable\":" + iterable + "}";
    }
}
