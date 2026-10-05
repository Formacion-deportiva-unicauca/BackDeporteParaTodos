package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
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
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IHorarioServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IImagenServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInscripcionServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Categoria;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;

/**
 * GET /api/v2/inscripcion/misCursos (test de integracion, BD MySQL real).
 * Fixtures: un alumno con una inscripcion INSCRITO (grupo con 2 horarios e instructor
 * con nombre conocido) y otra EN_ESPERA (grupo sin cupo), una inscripcion ya
 * desvinculada que no debe aparecer, y la inscripcion de OTRO alumno en el mismo
 * grupo que tampoco debe aparecer en la respuesta de este alumno.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class InscripcionMisCursosIT {

    private static final String CATEGORIA = "Deportes";
    private static final String CURSO_INSCRITO = "MisCursosIT-Inscrito";
    private static final String CURSO_ESPERA = "MisCursosIT-Espera";
    private static final String CURSO_DESVINCULADO = "MisCursosIT-Desvinculado";
    private static final int ANIO = LocalDate.now().getYear();
    private static final String DEPORTE_SEED = "Natacion";
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";

    @Autowired
    private IInscripcionServicio inscripcionServicio;

    @Autowired
    private ICategoriaCursoServicio categoriaCursoServicio;

    @Autowired
    private ICursoServicio cursoServicio;

    @Autowired
    private IGrupoServicio grupoServicio;

    @Autowired
    private IHorarioServicio horarioServicio;

    @Autowired
    private IInstructorServicio instructorServicio;

    @Autowired
    private IImagenServicio imagenServicio;

    @Autowired
    private IPerfilGateway perfilGateway;

    @Autowired
    private MockMvc mockMvc;

    private Integer imagenId;
    private String alumnoId;
    private String instructorNombre;
    private int iterableInscrito;
    private int iterableEnEspera;

    @BeforeAll
    void crearFixtures() {
        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-miscursos-it.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {7, 7, 7}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para InscripcionMisCursosIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String instructorId = "instr-miscursos-" + sufijo;
        instructorNombre = "Prof. MisCursos " + sufijo;
        crearInstructor(instructorId, instructorNombre);

        crearCursoSiNoExiste(CURSO_INSCRITO);
        crearCursoSiNoExiste(CURSO_ESPERA);
        crearCursoSiNoExiste(CURSO_DESVINCULADO);

        // Grupo con cupos de sobra, 2 horarios y el instructor fixture -- debe
        // aparecer como una sola fila con los 2 horarios anidados.
        Grupo grupoInscrito = new Grupo();
        grupoInscrito.setCategoria(CATEGORIA);
        grupoInscrito.setCurso(CURSO_INSCRITO);
        grupoInscrito.setImagenGrupo(imagenId);
        grupoInscrito.setIdInstructor(instructorId);
        grupoInscrito.setCupos(5);
        grupoInscrito.setFechaCreacion(LocalDate.now());
        grupoInscrito.setFechaInscripcionApertura(LocalDate.now());
        grupoInscrito.setPeriodo(1);
        iterableInscrito = grupoServicio.insertarGrupo(grupoInscrito).getIterable();

        insertarHorario(CURSO_INSCRITO, iterableInscrito, "LUNES", "08:00", "10:00");
        insertarHorario(CURSO_INSCRITO, iterableInscrito, "MIERCOLES", "08:00", "10:00");

        // Grupo con un solo cupo, ya ocupado por otro alumno -- el alumno de este
        // test quedara EN_ESPERA al inscribirse.
        String alumnoOcupaCupo = "alumno-it-miscursos-ocupa-" + sufijo;
        crearAlumnoSiNoExiste(alumnoOcupaCupo);
        Grupo grupoEspera = new Grupo();
        grupoEspera.setCategoria(CATEGORIA);
        grupoEspera.setCurso(CURSO_ESPERA);
        grupoEspera.setImagenGrupo(imagenId);
        grupoEspera.setCupos(1);
        grupoEspera.setFechaCreacion(LocalDate.now());
        grupoEspera.setFechaInscripcionApertura(LocalDate.now());
        grupoEspera.setPeriodo(1);
        iterableEnEspera = grupoServicio.insertarGrupo(grupoEspera).getIterable();
        inscripcionServicio.inscribir(new Inscripcion(
                alumnoOcupaCupo, CATEGORIA, CURSO_ESPERA, ANIO, iterableEnEspera, null, null, null));

        // Grupo normal para la inscripcion que se desvincula (no debe aparecer).
        Grupo grupoDesvinculado = new Grupo();
        grupoDesvinculado.setCategoria(CATEGORIA);
        grupoDesvinculado.setCurso(CURSO_DESVINCULADO);
        grupoDesvinculado.setImagenGrupo(imagenId);
        grupoDesvinculado.setCupos(5);
        grupoDesvinculado.setFechaCreacion(LocalDate.now());
        grupoDesvinculado.setFechaInscripcionApertura(LocalDate.now());
        grupoDesvinculado.setPeriodo(1);
        int iterableDesvinculado = grupoServicio.insertarGrupo(grupoDesvinculado).getIterable();

        alumnoId = "alumno-it-miscursos-" + sufijo;
        crearAlumnoSiNoExiste(alumnoId);

        // Inscripcion de OTRO alumno en el mismo grupo CURSO_INSCRITO -- no debe
        // aparecer en la respuesta de alumnoId.
        String otroAlumno = "alumno-it-miscursos-otro-" + sufijo;
        crearAlumnoSiNoExiste(otroAlumno);
        inscripcionServicio.inscribir(new Inscripcion(
                otroAlumno, CATEGORIA, CURSO_INSCRITO, ANIO, iterableInscrito, null, null, null));

        inscripcionServicio.inscribir(new Inscripcion(
                alumnoId, CATEGORIA, CURSO_INSCRITO, ANIO, iterableInscrito, null, null, null));
        inscripcionServicio.inscribir(new Inscripcion(
                alumnoId, CATEGORIA, CURSO_ESPERA, ANIO, iterableEnEspera, null, null, null));
        inscripcionServicio.inscribir(new Inscripcion(
                alumnoId, CATEGORIA, CURSO_DESVINCULADO, ANIO, iterableDesvinculado, null, null, null));
        inscripcionServicio.desvincularInscripcion(alumnoId, CATEGORIA, CURSO_DESVINCULADO, ANIO, iterableDesvinculado);
    }

    private void insertarHorario(String curso, int iterable, String dia, String horaInicio, String horaFin) {
        Horario horario = new Horario();
        horario.setCategoria(CATEGORIA);
        horario.setCurso(curso);
        horario.setAnio(ANIO);
        horario.setIterable(iterable);
        horario.setDia(dia);
        horario.setHoraInicio(horaInicio);
        horario.setHoraFin(horaFin);
        horario.setEscenario("Cancha IT");
        horarioServicio.insertarHorario(horario);
    }

    private void crearInstructor(String id, String nombre) {
        PerfilDto perfilDto = new PerfilDto();
        perfilDto.setId(id);
        perfilDto.setNombre(nombre);
        perfilDto.setCorreo(id + "@it.unicauca.edu.co");
        perfilDto.setTipoId("CC");
        perfilDto.setSexo("M");
        // CKC_ALM_TIPO_ALUM solo permite Estudiante/Administrativo/Docente -- ver InstructorIT.
        perfilDto.setTipoAlumno("Docente");
        try {
            instructorServicio.registrarInstructor(perfilDto);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior contra BD persistente; se reutiliza.
        }
    }

    private void crearCursoSiNoExiste(String nombreCurso) {
        try {
            Curso curso = new Curso();
            curso.setNombre(nombreCurso);
            curso.setCategoriaCurso(CATEGORIA);
            curso.setDescripcion("Curso de prueba para InscripcionMisCursosIT");
            curso.setImagenId(imagenId);
            curso.setDeporte(DEPORTE_SEED);
            curso.setEstadoCurso(EstadoCurso.ACTIVO);
            curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
            cursoServicio.insertarCurso(curso);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior contra BD persistente; se reutiliza.
        }
    }

    private void crearAlumnoSiNoExiste(String id) {
        try {
            Perfil perfil = new Perfil();
            perfil.setId(id);
            perfil.setNombre("Alumno Prueba " + id);
            perfil.setCorreo(id + "@it.unicauca.edu.co");
            perfil.setTipoId("CC");
            perfil.setSexo("M");
            perfil.setTipoAlumno("Estudiante");
            perfilGateway.registrarAlumno(perfil);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior contra BD persistente; se reutiliza.
        }
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

    @Test
    void misCursos_http_retornaSoloInscripcionesActivasDelAlumno() throws Exception {
        mockMvc.perform(get("/api/v2/inscripcion/misCursos")
                .header("Authorization", "Bearer " + buildJwt("Alumno", alumnoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // orden: INSCRITO antes que EN_ESPERA
                .andExpect(jsonPath("$[0].curso").value(CURSO_INSCRITO))
                .andExpect(jsonPath("$[0].estado").value("INSCRITO"))
                .andExpect(jsonPath("$[0].grupoActivo").value(true))
                .andExpect(jsonPath("$[0].nombreInstructor").value(instructorNombre))
                .andExpect(jsonPath("$[0].horarios.length()").value(2))
                .andExpect(jsonPath("$[1].curso").value(CURSO_ESPERA))
                .andExpect(jsonPath("$[1].estado").value("EN_ESPERA"))
                .andExpect(jsonPath("$[1].nombreInstructor").isEmpty());
    }

    @Test
    void misCursos_servicio_excluyeDesvinculadaYDeOtroAlumno() {
        List<co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionResumen> resultado =
                inscripcionServicio.listarMisCursos(alumnoId);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().noneMatch(r -> CURSO_DESVINCULADO.equals(r.getCurso())),
                "la inscripcion desvinculada no debe aparecer");
        assertFalse(resultado.stream().anyMatch(r -> r.getEstado() == null));
    }
}
