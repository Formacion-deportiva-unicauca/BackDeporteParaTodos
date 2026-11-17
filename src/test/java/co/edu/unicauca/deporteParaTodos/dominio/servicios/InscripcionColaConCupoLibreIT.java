package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

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
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IImagenServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInscripcionServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
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
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionEnEspera;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;

/**
 * Cola de espera con cupo libre (test de integracion, BD MySQL real, por HTTP): si el grupo tiene alguien EN_ESPERA vigente,
 * quien llega NUEVO no entra directo aunque haya cupo: se une a la lista, al final. Sin cola, entra como INSCRITO.
 *
 * El estado "cupo libre con cola" se arma ampliando cupos con el gateway, que NO promueve (la promocion automatica la hace
 * GrupoServicio al ampliar). Cada escenario crea su propio grupo y sus propios alumnos.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class InscripcionColaConCupoLibreIT {

    private static final String CATEGORIA = "Deportes";
    private static final String CURSO = "InscripcionColaConCupoLibreIT-Curso";
    private static final String DEPORTE_SEED = "Natacion";
    private static final String INSTRUCTOR_SEED_ID = "2";

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
    private IGrupoGateway grupoGateway;

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
        imagenDto.setNombre("imagen-inscripcion-cola-it.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {7, 7, 7}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para InscripcionColaConCupoLibreIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        try {
            Curso curso = new Curso();
            curso.setNombre(CURSO);
            curso.setCategoriaCurso(CATEGORIA);
            curso.setDescripcion("Curso de prueba para InscripcionColaConCupoLibreIT");
            curso.setImagenId(imagenId);
            curso.setDeporte(DEPORTE_SEED);
            curso.setEstadoCurso(EstadoCurso.ACTIVO);
            curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
            cursoServicio.insertarCurso(curso);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior contra BD persistente; se reutiliza.
        }
    }

    // ── Escenarios ────────────────────────────────────────────────────────────

    @Test
    void cupoLibreConUnEnEspera_alumnoNuevoPorHttp_quedaEnEsperaDespuesDelQueYaEsperaba() throws Exception {
        int iterable = grupoConCupoLibreYUnEnEspera();
        String esperando = esperandoDe(iterable);
        String nuevo = nuevoAlumno();
        dormirParaFechaPosterior();

        mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(nuevo, iterable))
                .header("Authorization", "Bearer " + buildJwt("Alumno", nuevo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_ESPERA"));

        Inscripcion fila = inscripcionGateway.obtenerInscripcion(nuevo, CATEGORIA, CURSO, anio, iterable);
        assertEquals("EN_ESPERA", fila.getEstado());
        assertNull(fila.getFechaDesvinculacion());
        Inscripcion antes = inscripcionGateway.obtenerInscripcion(esperando, CATEGORIA, CURSO, anio, iterable);
        assertTrue(fila.getFechaInscripcion().after(antes.getFechaInscripcion()),
                "fechaInscripcion posterior: queda al FINAL de la cola");
        assertEquals(List.of(esperando, nuevo), alumnosEnCola(iterable), "orden de llegada");
        // El cupo libre sigue libre: nadie fue promovido ni se ocupo por la fuerza.
        assertEquals(1, servicio.obtenerDisponibilidad(CATEGORIA, CURSO, anio, iterable).getCuposDisponibles());
        assertEquals(2, servicio.obtenerDisponibilidad(CATEGORIA, CURSO, anio, iterable).getTamanoListaEspera());
    }

    @Test
    void sinCola_conCupoLibre_alumnoNuevoPorHttp_entraComoInscrito() throws Exception {
        int iterable = crearGrupo(2);
        inscribir(nuevoAlumno(), iterable);
        String nuevo = nuevoAlumno();

        mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(nuevo, iterable))
                .header("Authorization", "Bearer " + buildJwt("Alumno", nuevo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("INSCRITO"));

        assertEquals("INSCRITO", inscripcionGateway.obtenerInscripcion(nuevo, CATEGORIA, CURSO, anio, iterable).getEstado());
    }

    @Test
    void salioYVuelveConColaYCupoLibre_seTrataComoNuevo_alFinalDeLaCola() {
        int iterable = crearGrupo(1);
        inscribir(nuevoAlumno(), iterable);                  // ocupa el unico cupo
        String primeroEnCola = nuevoAlumno();
        String quienSaleYVuelve = nuevoAlumno();
        assertEquals("EN_ESPERA", inscribir(primeroEnCola, iterable).getEstado());
        dormirParaFechaPosterior();
        assertEquals("EN_ESPERA", inscribir(quienSaleYVuelve, iterable).getEstado());
        servicio.desvincularInscripcion(quienSaleYVuelve, CATEGORIA, CURSO, anio, iterable, "coord-it", "Coordinador");
        ampliarCupos(iterable, 2);                           // cupo libre con cola ([primeroEnCola])
        dormirParaFechaPosterior();

        assertEquals("EN_ESPERA", inscribir(quienSaleYVuelve, iterable).getEstado());

        assertEquals(List.of(primeroEnCola, quienSaleYVuelve), alumnosEnCola(iterable));
    }

    @Test
    void elStaffTambienVaAlFinalDeLaCola_yElCoordinadorPuedeSaltarlaConLaPromocionManual() {
        int iterable = grupoConCupoLibreYUnEnEspera();
        String esperando = esperandoDe(iterable);
        String nuevo = nuevoAlumno();
        dormirParaFechaPosterior();

        // POST /inscripcion hecho por el Coordinador: la misma regla (inscribir() recibe el rol solo para autorizar por grupo).
        assertEquals("EN_ESPERA", inscribir(nuevo, iterable).getEstado());
        assertEquals(List.of(esperando, nuevo), alumnosEnCola(iterable));

        // Decision explicita del Coordinador: promover a quien quiera, sin importar el orden.
        Inscripcion promovida = servicio.promoverManualmente(nuevo, CATEGORIA, CURSO, anio, iterable);
        assertEquals("INSCRITO", promovida.getEstado());
        assertEquals(List.of(esperando), alumnosEnCola(iterable));
    }

    @Test
    void llegadasSimultaneasConColaYCupoLibre_ningunaSeSaltaLaCola_ySinEstadosInconsistentes() throws Exception {
        int iterable = crearGrupo(1);
        inscribir(nuevoAlumno(), iterable);
        String esperando = nuevoAlumno();
        assertEquals("EN_ESPERA", inscribir(esperando, iterable).getEstado());
        ampliarCupos(iterable, 4);                           // 3 cupos libres y una cola de 1
        dormirParaFechaPosterior();                          // las llegadas son estrictamente posteriores a quien ya esperaba
        int llegadas = 6;
        List<String> nuevos = new ArrayList<>();
        for (int i = 0; i < llegadas; i++) {
            nuevos.add(nuevoAlumno());
        }

        ExecutorService pool = Executors.newFixedThreadPool(llegadas);
        try {
            CountDownLatch salida = new CountDownLatch(1);
            List<Future<String>> resultados = new ArrayList<>();
            for (String alumno : nuevos) {
                Callable<String> tarea = () -> {
                    salida.await();
                    return inscribir(alumno, iterable).getEstado();
                };
                resultados.add(pool.submit(tarea));
            }
            salida.countDown();
            for (Future<String> r : resultados) {
                assertEquals("EN_ESPERA", r.get(60, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(3, servicio.obtenerDisponibilidad(CATEGORIA, CURSO, anio, iterable).getCuposDisponibles(),
                "sigue habiendo un solo INSCRITO: nadie entro directo");
        List<String> cola = alumnosEnCola(iterable);
        assertEquals(llegadas + 1, cola.size());
        assertEquals(esperando, cola.get(0), "quien ya esperaba sigue primero");
        assertEquals(cola.size(), new java.util.HashSet<>(cola).size(), "sin filas duplicadas");
        for (String alumno : nuevos) {
            assertEquals("EN_ESPERA", inscripcionGateway.obtenerInscripcion(alumno, CATEGORIA, CURSO, anio, iterable).getEstado());
        }
    }

    // ── Armado ────────────────────────────────────────────────────────────────

    /** Grupo de 1 cupo: un INSCRITO, un EN_ESPERA, y luego cupos ampliados a 2 SIN promocion: cupo libre con cola de 1. */
    private int grupoConCupoLibreYUnEnEspera() {
        int iterable = crearGrupo(1);
        inscribir(nuevoAlumno(), iterable);
        String enEspera = nuevoAlumno();
        assertEquals("EN_ESPERA", inscribir(enEspera, iterable).getEstado());
        ampliarCupos(iterable, 2);
        return iterable;
    }

    private String esperandoDe(int iterable) {
        return alumnosEnCola(iterable).get(0);
    }

    private void ampliarCupos(int iterable, int cupos) {
        Grupo ampliar = new Grupo();
        ampliar.setCupos(cupos);
        ampliar.setIdInstructor(INSTRUCTOR_SEED_ID);
        grupoGateway.actualizarGrupo(CATEGORIA, CURSO, anio, iterable, ampliar);
    }

    private List<String> alumnosEnCola(int iterable) {
        return inscripcionGateway.listarEnEspera(CATEGORIA, CURSO, anio, iterable).stream()
                .map(InscripcionEnEspera::getAlumnoId).toList();
    }

    /** La fecha de inscripcion se guarda con precision de segundos: se espera para que "posterior" sea estricto. */
    private static void dormirParaFechaPosterior() {
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Ayudas ────────────────────────────────────────────────────────────────

    private Inscripcion inscribir(String alumnoId, int iterable) {
        return servicio.inscribir(
                new Inscripcion(alumnoId, CATEGORIA, CURSO, anio, iterable, null, null, null),
                "coord-it", "Coordinador");
    }

    /** Crea un grupo nuevo (iterable propio, asignado por insertarGrupo) y devuelve su iterable. */
    private int crearGrupo(int cupos) {
        Grupo grupo = new Grupo();
        grupo.setCategoria(CATEGORIA);
        grupo.setCurso(CURSO);
        grupo.setImagenGrupo(imagenId);
        grupo.setIdInstructor(INSTRUCTOR_SEED_ID);
        grupo.setCupos(cupos);
        grupo.setFechaCreacion(LocalDate.now(clock));
        grupo.setFechaIncripcionCierre(LocalDate.now(clock).plusYears(1));
        grupo.setFechaInscripcionApertura(LocalDate.now(clock));
        grupo.setPeriodo(1);
        return grupoServicio.insertarGrupo(grupo).getIterable();
    }

    private String nuevoAlumno() {
        String alumnoId = "alumno-it-cola-" + UUID.randomUUID().toString().substring(0, 8);
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
