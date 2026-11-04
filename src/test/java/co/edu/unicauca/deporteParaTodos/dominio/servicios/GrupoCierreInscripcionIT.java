package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Categoria;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;

/**
 * Fase 3 (test de integracion, BD MySQL real): cierre manual de inscripciones por grupo y estado de inscripciones
 * del curso derivado de sus grupos.
 *  - PATCH /grupo/inscripciones cierra y reabre; solo el Coordinador.
 *  - Un grupo cerrado a mano rechaza POST /inscripcion aunque su ventana de fechas este abierta.
 *  - El estado de inscripciones del curso es ABIERTO / CERRADO / SIN_GRUPOS segun sus grupos.
 *
 * Requiere la migracion db/migraciones/2026-11-04_cierre_inscripciones_grupo.sql (columna GRP_INSCRIP_CERRADA_MANUAL).
 * No se ejecuta en el perfil skip-integration-tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class GrupoCierreInscripcionIT {

    private static final String CATEGORIA = "Deportes";
    private static final String PREFIJO_CURSO = "GrupoCierreInscripcionIT-%";
    private static final String CURSO_CON_GRUPO = "GrupoCierreInscripcionIT-ConGrupo";
    private static final String CURSO_SIN_GRUPOS = "GrupoCierreInscripcionIT-SinGrupos";
    private static final String DEPORTE_SEED = "Natacion";
    private static final String INSTRUCTOR_SEED_ID = "2";

    @Autowired private ICategoriaCursoServicio categoriaCursoServicio;
    @Autowired private ICursoServicio cursoServicio;
    @Autowired private IGrupoServicio grupoServicio;
    @Autowired private IImagenServicio imagenServicio;
    @Autowired private IPerfilGateway perfilGateway;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Clock clock;

    private Integer imagenId;
    private Grupo grupo;

    @BeforeAll
    void crearFixtures() {
        limpiar();

        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-grupo-cierre-inscripcion-it.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {7, 7, 7}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para GrupoCierreInscripcionIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        crearCurso(CURSO_CON_GRUPO);
        crearCurso(CURSO_SIN_GRUPOS);

        Grupo nuevo = new Grupo();
        nuevo.setCategoria(CATEGORIA);
        nuevo.setCurso(CURSO_CON_GRUPO);
        nuevo.setImagenGrupo(imagenId);
        nuevo.setIdInstructor(INSTRUCTOR_SEED_ID);
        nuevo.setCupos(5);
        nuevo.setFechaCreacion(LocalDate.now(clock));
        nuevo.setFechaIncripcionCierre(LocalDate.now(clock).plusDays(30));
        nuevo.setPeriodo(1);
        grupo = grupoServicio.insertarGrupo(nuevo);
    }

    @AfterAll
    void limpiarAlTerminar() {
        limpiar();
    }

    private void limpiar() {
        jdbc.update("DELETE FROM tbl_inscripcion WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_grupo WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_curso WHERE cat_titulo = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
    }

    private void crearCurso(String nombre) {
        Curso curso = new Curso();
        curso.setNombre(nombre);
        curso.setCategoriaCurso(CATEGORIA);
        curso.setDescripcion("Curso de prueba para GrupoCierreInscripcionIT");
        curso.setImagenId(imagenId);
        curso.setDeporte(DEPORTE_SEED);
        curso.setEstadoCurso(EstadoCurso.ACTIVO);
        curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
        cursoServicio.insertarCurso(curso);
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

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder cambiarCierre(boolean cerradas, String rol) {
        return patch("/api/v2/grupo/inscripciones")
                .param("categoria", CATEGORIA)
                .param("curso", CURSO_CON_GRUPO)
                .param("anio", String.valueOf(grupo.getAnio()))
                .param("iterable", String.valueOf(grupo.getIterable()))
                .param("cerradas", String.valueOf(cerradas))
                .header("Authorization", "Bearer " + buildJwt(rol, "coord-it"));
    }

    private String bodyInscripcion(String alumnoId) {
        return "{\"alumnoId\":\"" + alumnoId + "\",\"categoria\":\"" + CATEGORIA
                + "\",\"curso\":\"" + CURSO_CON_GRUPO + "\",\"anio\":" + grupo.getAnio()
                + ",\"iterable\":" + grupo.getIterable() + "}";
    }

    private EstadoInscripciones estadoDelCurso(String nombre) {
        return cursoServicio.obtenerCurso(CATEGORIA, nombre).getEstadoInscripciones();
    }

    @Test
    void cerrarYReabrirUnGrupo_cambiaElEstadoDelCursoYBloqueaLasInscripciones() throws Exception {
        String alumnoId = "alumno-it-cierre-manual-" + UUID.randomUUID().toString().substring(0, 8);
        crearAlumnoSiNoExiste(alumnoId);

        // Con el cierre a 30 dias el grupo esta abierto, y por tanto el curso tambien.
        assertEquals(EstadoInscripciones.ABIERTO, estadoDelCurso(CURSO_CON_GRUPO));

        // El Coordinador cierra el grupo a mano.
        mockMvc.perform(cambiarCierre(true, "Coordinador"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inscripcionesCerradasManual").value(true))
                .andExpect(jsonPath("$.inscripcionesAbiertas").value(false));
        assertEquals(EstadoInscripciones.CERRADO, estadoDelCurso(CURSO_CON_GRUPO));

        // Aunque la ventana de fechas siga abierta, nadie puede inscribirse.
        mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(alumnoId))
                .header("Authorization", "Bearer " + buildJwt("Alumno", alumnoId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("cerradas")));

        // Reabrir (la fecha de cierre no ha pasado) vuelve a permitirlo.
        mockMvc.perform(cambiarCierre(false, "Coordinador"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inscripcionesCerradasManual").value(false))
                .andExpect(jsonPath("$.inscripcionesAbiertas").value(true));
        assertEquals(EstadoInscripciones.ABIERTO, estadoDelCurso(CURSO_CON_GRUPO));

        mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(alumnoId))
                .header("Authorization", "Bearer " + buildJwt("Alumno", alumnoId)))
                .andExpect(status().isCreated());
    }

    @Test
    void soloElCoordinadorPuedeCambiarElCierreManual() throws Exception {
        mockMvc.perform(cambiarCierre(true, "Instructor")).andExpect(status().isForbidden());
        mockMvc.perform(cambiarCierre(true, "Alumno")).andExpect(status().isForbidden());
    }

    @Test
    void unCursoSinGrupos_tieneEstadoSIN_GRUPOS() {
        assertEquals(EstadoInscripciones.SIN_GRUPOS, estadoDelCurso(CURSO_SIN_GRUPOS));
    }

    @Test
    void unGrupoSinFechaDeCierre_sePideAlCrearlo() {
        Grupo sinCierre = new Grupo();
        sinCierre.setCategoria(CATEGORIA);
        sinCierre.setCurso(CURSO_CON_GRUPO);
        sinCierre.setImagenGrupo(imagenId);
        sinCierre.setIdInstructor(INSTRUCTOR_SEED_ID);
        sinCierre.setCupos(5);
        sinCierre.setFechaCreacion(LocalDate.now(clock));
        sinCierre.setPeriodo(1);

        org.junit.jupiter.api.Assertions.assertThrows(
                co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechasGrupoInvalidasExcepcion.class,
                () -> grupoServicio.insertarGrupo(sinCierre));
    }
}
