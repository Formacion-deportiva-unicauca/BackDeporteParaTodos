package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
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
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;

/**
 * Inscripcion duplicada (test de integracion, BD MySQL real): quien ya esta INSCRITO en un grupo no puede
 * volver a inscribirse en el. Antes, reenviar POST /inscripcion reactivaba la fila (refrescaba
 * fechaInscripcion) y, con el grupo lleno, pasaba al alumno a EN_ESPERA porque el propio alumno ocupaba un cupo.
 *
 * Lo mismo para quien esta EN_ESPERA: con cupo libre ya no se salta la cola (antes pasaba a INSCRITO).
 *
 * Cada escenario crea su propio grupo (iterable nuevo) y sus propios alumnos, asi que no depende del estado
 * que haya dejado una corrida anterior contra una BD persistente.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class InscripcionDuplicadaIT {

    private static final String CATEGORIA = "Deportes";
    private static final String CURSO = "InscripcionDuplicadaIT-Curso";
    private static final String DEPORTE_SEED = "Natacion";
    private static final String INSTRUCTOR_SEED_ID = "2";
    // "Ya estas inscrito en este grupo", con la tilde como escape para no depender de la codificacion del archivo.
    private static final String MENSAJE_DUPLICADO = "Ya estás inscrito en este grupo";
    private static final String MENSAJE_EN_ESPERA = "Ya estás en la lista de espera de este grupo";

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
        imagenDto.setNombre("imagen-inscripcion-duplicada-it.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {7, 7, 7}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para InscripcionDuplicadaIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        try {
            Curso curso = new Curso();
            curso.setNombre(CURSO);
            curso.setCategoriaCurso(CATEGORIA);
            curso.setDescripcion("Curso de prueba para InscripcionDuplicadaIT");
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
    void inscritoQueReenvia_recibe409YLaFilaQuedaIgual() throws Exception {
        int iterable = crearGrupo(5);
        String alumnoId = nuevoAlumno();
        inscribir(alumnoId, iterable);
        Inscripcion antes = inscripcionGateway.obtenerInscripcion(alumnoId, CATEGORIA, CURSO, anio, iterable);
        assertEquals("INSCRITO", antes.getEstado());

        MvcResult respuesta = mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(alumnoId, iterable))
                .header("Authorization", "Bearer " + buildJwt("Alumno", alumnoId)))
                .andExpect(status().isConflict())
                .andReturn();

        String cuerpo = respuesta.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(cuerpo.contains(MENSAJE_DUPLICADO), "el mensaje debe decir que ya esta inscrito: " + cuerpo);

        Inscripcion despues = inscripcionGateway.obtenerInscripcion(alumnoId, CATEGORIA, CURSO, anio, iterable);
        assertEquals("INSCRITO", despues.getEstado());
        assertEquals(antes.getFechaInscripcion(), despues.getFechaInscripcion(),
                "reenviar no debe refrescar fechaInscripcion");
        assertNull(despues.getFechaDesvinculacion());
    }

    @Test
    void inscritoConGrupoLleno_noPasaAEnEspera() {
        int iterable = crearGrupo(1);
        String alumnoId = nuevoAlumno();
        inscribir(alumnoId, iterable); // ocupa el unico cupo: el grupo queda lleno

        assertThrows(YaExisteElementoExcepcion.class, () -> inscribir(alumnoId, iterable));

        Inscripcion despues = inscripcionGateway.obtenerInscripcion(alumnoId, CATEGORIA, CURSO, anio, iterable);
        assertEquals("INSCRITO", despues.getEstado(), "con el grupo lleno no debe degradarse a EN_ESPERA");
        assertNull(despues.getFechaDesvinculacion());
    }

    @Test
    void sinTope_variasInscripciones_yReenviarSigueSiendoDuplicado() {
        String alumnoId = nuevoAlumno();
        int g1 = crearGrupo(5);
        int g2 = crearGrupo(5);
        int g3 = crearGrupo(5);
        int g4 = crearGrupo(5);
        inscribir(alumnoId, g1);
        inscribir(alumnoId, g2);
        inscribir(alumnoId, g3);
        // Sin tope de cursos (grupos sin horario, nada se cruza): una cuarta inscripcion es valida.
        assertEquals("INSCRITO", inscribir(alumnoId, g4).getEstado());

        // Reenviar a un grupo donde YA esta inscrito sigue dando el error de duplicado.
        assertThrows(YaExisteElementoExcepcion.class, () -> inscribir(alumnoId, g1));
    }

    @Test
    void cancelarYVolverAInscribirse_sigueFuncionando() {
        int iterable = crearGrupo(5);
        String alumnoId = nuevoAlumno();
        inscribir(alumnoId, iterable);

        servicio.desvincularInscripcion(alumnoId, CATEGORIA, CURSO, anio, iterable, "coord-it", "Coordinador");
        Inscripcion cancelada = inscripcionGateway.obtenerInscripcion(alumnoId, CATEGORIA, CURSO, anio, iterable);
        assertNotNull(cancelada.getFechaDesvinculacion());

        Inscripcion reinscrita = inscribir(alumnoId, iterable);

        assertEquals("INSCRITO", reinscrita.getEstado());
        Inscripcion despues = inscripcionGateway.obtenerInscripcion(alumnoId, CATEGORIA, CURSO, anio, iterable);
        assertEquals("INSCRITO", despues.getEstado());
        assertNull(despues.getFechaDesvinculacion());
        // Y, ya reinscrito, un nuevo envio vuelve a ser un duplicado.
        assertThrows(YaExisteElementoExcepcion.class, () -> inscribir(alumnoId, iterable));
    }

    @Test
    void enEsperaConCupoLibre_recibe409YLaFilaQuedaIgual() throws Exception {
        int iterable = crearGrupo(1);
        String primero = nuevoAlumno();
        String enEspera = nuevoAlumno();
        inscribir(primero, iterable);                    // ocupa el unico cupo
        assertEquals("EN_ESPERA", inscribir(enEspera, iterable).getEstado());
        // Queda un cupo libre SIN auto-promocion (el gateway no promueve; lo haria GrupoServicio al ampliar).
        Grupo ampliar = new Grupo();
        ampliar.setCupos(2);
        ampliar.setIdInstructor(INSTRUCTOR_SEED_ID);
        grupoGateway.actualizarGrupo(CATEGORIA, CURSO, anio, iterable, ampliar);
        Inscripcion antes = inscripcionGateway.obtenerInscripcion(enEspera, CATEGORIA, CURSO, anio, iterable);
        assertEquals("EN_ESPERA", antes.getEstado());

        MvcResult respuesta = mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(enEspera, iterable))
                .header("Authorization", "Bearer " + buildJwt("Alumno", enEspera)))
                .andExpect(status().isConflict())
                .andReturn();

        String cuerpo = respuesta.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(cuerpo.contains(MENSAJE_EN_ESPERA), "el mensaje debe decir que ya esta en la lista de espera: " + cuerpo);

        Inscripcion despues = inscripcionGateway.obtenerInscripcion(enEspera, CATEGORIA, CURSO, anio, iterable);
        assertEquals("EN_ESPERA", despues.getEstado(), "no debe pasar a INSCRITO saltandose la cola");
        assertEquals(antes.getFechaInscripcion(), despues.getFechaInscripcion(),
                "no debe cambiar su posicion en la lista");
        assertNull(despues.getFechaDesvinculacion());
    }

    @Test
    void salirDeLaListaYVolverAInscribirse_conCupoLibre_sigueFuncionando() {
        int iterable = crearGrupo(1);
        String primero = nuevoAlumno();
        String alumno = nuevoAlumno();
        inscribir(primero, iterable);
        inscribir(alumno, iterable); // EN_ESPERA
        servicio.desvincularInscripcion(alumno, CATEGORIA, CURSO, anio, iterable, "coord-it", "Coordinador");
        Grupo ampliar = new Grupo();
        ampliar.setCupos(2);
        ampliar.setIdInstructor(INSTRUCTOR_SEED_ID);
        grupoGateway.actualizarGrupo(CATEGORIA, CURSO, anio, iterable, ampliar);

        assertEquals("INSCRITO", inscribir(alumno, iterable).getEstado());
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
        String alumnoId = "alumno-it-duplicada-" + UUID.randomUUID().toString().substring(0, 8);
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
