package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.InscripcionesCerradasExcepcion;
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
 * SCRUM-178 (test de integracion, BD MySQL real): un curso INACTIVO no debe aceptar
 * nuevas inscripciones en sus grupos, aunque el grupo mismo siga con cupos y fechas de
 * inscripcion abiertas. Reproducido primero contra la BD real (categoria "Deportes",
 * curso SCRUM178, grupo 2026-1) antes de tocar codigo: POST /inscripcion respondia 201.
 *
 * El @BeforeAll crea sus propios fixtures (curso propio por escenario, no SCRUM178) a
 * traves de los servicios reales de la app, idempotente para correr repetidas veces
 * contra una BD persistente en local -- mismo patron que InscripcionConcurrenciaIT/GrupoIT.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class InscripcionEstadoCursoIT {

    private static final String CATEGORIA = "Deportes";
    private static final String CURSO_INACTIVO = "InscripcionEstadoCursoIT-Inactivo";
    private static final String CURSO_ACTIVO = "InscripcionEstadoCursoIT-Activo";
    // GrupoGateway.insertarGrupo() ignora el anio pedido y crea el grupo en el anio ACTUAL.
    private static final int ANIO = LocalDate.now().getYear();
    private static final int ITERABLE = 1;
    private static final String DEPORTE_SEED = "Natacion";
    private static final String INSTRUCTOR_SEED_ID = "2";

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

    @Autowired
    private IPerfilGateway perfilGateway;

    @Autowired
    private MockMvc mockMvc;

    private Integer imagenId;

    // Grupo y alumno dedicados al escenario de promoverManualmente(), creados frescos
    // en cada corrida de JVM (@TestInstance PER_CLASS => una sola vez por suite) para
    // no depender de reactivar filas de inscripcion entre corridas contra una BD
    // persistente -- inscribir() trata una reinscripcion sobre una fila ya INSCRITO
    // como si fuera una inscripcion mas al contar cupos, lo que puede reclasificarla
    // en vez de dejarla igual. Un grupo/alumnos nuevos evitan ese caso por completo.
    private int iterablePromocion;
    private String alumnoEnEsperaPromocion;

    @BeforeAll
    void crearFixtures() {
        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-inscripcion-estado-curso-it.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {5, 5, 5}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para InscripcionEstadoCursoIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        crearCursoSiNoExiste(CURSO_INACTIVO);
        crearCursoSiNoExiste(CURSO_ACTIVO);
        // Se deja el curso ACTIVO explicitamente (idempotente en reruns donde ya quedo
        // INACTIVO por una corrida anterior) para poder preparar los fixtures de mas
        // abajo -- una vez inactivado, inscribir() (tras el fix) ya no lo permite.
        cursoServicio.cambiarEstadoCurso(CATEGORIA, CURSO_INACTIVO, EstadoCurso.ACTIVO);

        crearGrupoSiNoExiste(CURSO_INACTIVO, 5);
        crearGrupoSiNoExiste(CURSO_ACTIVO, 5);

        prepararFixturePromocion();

        // Ahora si, el escenario real de SCRUM-178: el curso pasa a INACTIVO con el
        // grupo de promocion ya poblado (1 inscrito, 1 en espera legitima con cupo real
        // disponible).
        cursoServicio.cambiarEstadoCurso(CATEGORIA, CURSO_INACTIVO, EstadoCurso.INACTIVO);
    }

    // Crea un grupo nuevo (iterable propio, asignado por insertarGrupo()) bajo
    // CURSO_INACTIVO con 1 alumno INSCRITO (ocupa el unico cupo) y 1 EN_ESPERA, luego
    // amplia los cupos de 1 a 2 sin desvincular a nadie (no dispara auto-promocion --
    // mismo mecanismo que InscripcionConcurrenciaIT), dejando 1 cupo real disponible.
    // Todo esto ocurre con el curso todavia ACTIVO; si el rechazo posterior en
    // promoverManualmente() fuera solo por falta de cupo (CuposAgotadosExcepcion), no
    // probaria nada sobre SCRUM-178 -- con un cupo realmente libre, cualquier rechazo
    // solo puede deberse a estadoCurso.
    private void prepararFixturePromocion() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String alumnoOcupaCupo = "alumno-it-estado-curso-ocupa-cupo-" + sufijo;
        alumnoEnEsperaPromocion = "alumno-it-estado-curso-en-espera-" + sufijo;
        crearAlumnoSiNoExiste(alumnoOcupaCupo);
        crearAlumnoSiNoExiste(alumnoEnEsperaPromocion);

        Grupo grupo = new Grupo();
        grupo.setCategoria(CATEGORIA);
        grupo.setCurso(CURSO_INACTIVO);
        grupo.setImagenGrupo(imagenId);
        grupo.setIdInstructor(INSTRUCTOR_SEED_ID);
        grupo.setCupos(1);
        grupo.setFechaCreacion(LocalDate.now());
        grupo.setFechaInscripcionApertura(LocalDate.now());
        grupo.setPeriodo(1);
        iterablePromocion = grupoServicio.insertarGrupo(grupo).getIterable();

        servicio.inscribir(new Inscripcion(
                alumnoOcupaCupo, CATEGORIA, CURSO_INACTIVO, ANIO, iterablePromocion, null, null, null));
        servicio.inscribir(new Inscripcion(
                alumnoEnEsperaPromocion, CATEGORIA, CURSO_INACTIVO, ANIO, iterablePromocion, null, null, null));

        Grupo grupoActual = grupoServicio.obtenerGrupoPorId(CATEGORIA, CURSO_INACTIVO, ANIO, iterablePromocion);
        grupoActual.setCupos(2);
        grupoServicio.actualizarGrupo(CATEGORIA, CURSO_INACTIVO, ANIO, iterablePromocion, grupoActual);
    }

    private void crearCursoSiNoExiste(String nombreCurso) {
        try {
            Curso curso = new Curso();
            curso.setNombre(nombreCurso);
            curso.setCategoriaCurso(CATEGORIA);
            curso.setDescripcion("Curso de prueba para InscripcionEstadoCursoIT");
            curso.setImagenId(imagenId);
            curso.setDeporte(DEPORTE_SEED);
            curso.setEstadoCurso(EstadoCurso.ACTIVO);
            curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
            cursoServicio.insertarCurso(curso);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior contra BD persistente; se reutiliza.
        }
    }

    private void crearGrupoSiNoExiste(String nombreCurso, int cupos) {
        try {
            Grupo existente = grupoServicio.obtenerGrupoPorId(CATEGORIA, nombreCurso, ANIO, ITERABLE);
            // Corridas anteriores de este mismo archivo pudieron dejar otros cupos
            // (p.ej. el fixture cambio de forma entre revisiones); se fuerza el valor
            // que el escenario necesita para que el test sea determinista en reruns.
            if (!Integer.valueOf(cupos).equals(existente.getCupos())) {
                existente.setCupos(cupos);
                grupoServicio.actualizarGrupo(CATEGORIA, nombreCurso, ANIO, ITERABLE, existente);
            }
        } catch (NoExisteExcepcion e) {
            Grupo grupo = new Grupo();
            grupo.setCategoria(CATEGORIA);
            grupo.setCurso(nombreCurso);
            grupo.setAnio(ANIO);
            grupo.setIterable(ITERABLE);
            grupo.setImagenGrupo(imagenId);
            grupo.setIdInstructor(INSTRUCTOR_SEED_ID);
            grupo.setCupos(cupos);
            grupo.setFechaCreacion(LocalDate.now());
            grupo.setFechaInscripcionApertura(LocalDate.now());
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

    private String bodyInscripcion(String alumnoId, String curso) {
        return "{\"alumnoId\":\"" + alumnoId + "\",\"categoria\":\"" + CATEGORIA
                + "\",\"curso\":\"" + curso + "\",\"anio\":" + ANIO + ",\"iterable\":" + ITERABLE + "}";
    }

    /**
     * SCRUM-178: reproduce contra MySQL real el hueco confirmado manualmente con el
     * curso SCRUM178 -- un grupo con cupos disponibles sigue aceptando POST /inscripcion
     * aunque su curso este INACTIVO.
     */
    @Test
    void postInscripcion_http_cursoInactivo_rechazaInscripcion() throws Exception {
        String alumnoId = "alumno-it-curso-inactivo-" + UUID.randomUUID().toString().substring(0, 8);
        crearAlumnoSiNoExiste(alumnoId);

        mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(alumnoId, CURSO_INACTIVO))
                .header("Authorization", "Bearer " + buildJwt("Alumno", alumnoId)))
                .andExpect(status().isUnprocessableEntity());
    }

    // Camino normal: mismo flujo (mismo endpoint, mismo tipo de grupo con cupos), pero
    // con el curso ACTIVO -- debe seguir aceptando la inscripcion sin cambios.
    @Test
    void postInscripcion_http_cursoActivo_siguePermitiendoInscripcion() throws Exception {
        String alumnoId = "alumno-it-curso-activo-" + UUID.randomUUID().toString().substring(0, 8);
        crearAlumnoSiNoExiste(alumnoId);

        mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(bodyInscripcion(alumnoId, CURSO_ACTIVO))
                .header("Authorization", "Bearer " + buildJwt("Alumno", alumnoId)))
                .andExpect(status().isCreated());
    }

    // Mismo patron de SCRUM-178 en promoverManualmente(): un Coordinador tampoco debe
    // poder mover a un alumno de la lista de espera a INSCRITO si el curso esta INACTIVO.
    // alumnoEnEsperaPromocion quedo legitimamente en espera (con un cupo real disponible)
    // en el @BeforeAll, mientras el curso todavia estaba ACTIVO -- confirma que el rechazo
    // es por estadoCurso y no porque el alumno no estuviera en la lista de espera o no
    // hubiera cupo.
    @Test
    void promoverManualmente_cursoInactivo_lanzaInscripcionesCerradasExcepcion() {
        assertThrows(InscripcionesCerradasExcepcion.class, () -> servicio.promoverManualmente(
                alumnoEnEsperaPromocion, CATEGORIA, CURSO_INACTIVO, ANIO, iterablePromocion));
    }
}
