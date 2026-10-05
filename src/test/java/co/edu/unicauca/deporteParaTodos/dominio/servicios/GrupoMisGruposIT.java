package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InstructorGrupoResumen;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;

/**
 * GET /api/v2/grupo/misGrupos (test de integracion, BD MySQL real).
 * Fixtures: instructor A con 2 grupos (uno con 2 horarios, 1 inscrito INSCRITO,
 * 1 EN_ESPERA y una inscripcion desvinculada que NO cuenta; el otro sin ninguna
 * inscripcion); instructor B con 1 grupo que NO debe aparecer en la respuesta de A;
 * y un grupo de A eliminado logicamente que tampoco debe aparecer.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class GrupoMisGruposIT {

    private static final String CATEGORIA = "Deportes";
    private static final String CURSO_A1 = "MisGruposIT-A1";
    private static final String CURSO_A2 = "MisGruposIT-A2";
    private static final String CURSO_A3_ELIMINADO = "MisGruposIT-A3";
    private static final String CURSO_B1 = "MisGruposIT-B1";
    private static final int ANIO = LocalDate.now().getYear();
    private static final String DEPORTE_SEED = "Natacion";
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";

    @Autowired
    private IGrupoServicio grupoServicio;

    @Autowired
    private IInscripcionServicio inscripcionServicio;

    @Autowired
    private ICategoriaCursoServicio categoriaCursoServicio;

    @Autowired
    private ICursoServicio cursoServicio;

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
    private String instructorAId;
    private int iterableA1;

    @BeforeAll
    void crearFixtures() {
        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-misgrupos-it.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {9, 9, 9}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para GrupoMisGruposIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        instructorAId = "instr-misgrupos-a-" + sufijo;
        String instructorBId = "instr-misgrupos-b-" + sufijo;
        crearInstructor(instructorAId, "Prof. A " + sufijo);
        crearInstructor(instructorBId, "Prof. B " + sufijo);

        crearCursoSiNoExiste(CURSO_A1);
        crearCursoSiNoExiste(CURSO_A2);
        crearCursoSiNoExiste(CURSO_A3_ELIMINADO);
        crearCursoSiNoExiste(CURSO_B1);

        // Grupo A1: cupos=1, 2 horarios, instructor A.
        Grupo grupoA1 = new Grupo();
        grupoA1.setCategoria(CATEGORIA);
        grupoA1.setCurso(CURSO_A1);
        grupoA1.setImagenGrupo(imagenId);
        grupoA1.setIdInstructor(instructorAId);
        grupoA1.setCupos(1);
        grupoA1.setFechaCreacion(LocalDate.now());
        grupoA1.setFechaInscripcionApertura(LocalDate.now());
        grupoA1.setPeriodo(1);
        iterableA1 = grupoServicio.insertarGrupo(grupoA1).getIterable();
        insertarHorario(CURSO_A1, iterableA1, "LUNES", "08:00", "10:00");
        insertarHorario(CURSO_A1, iterableA1, "MIERCOLES", "08:00", "10:00");

        // Grupo A2: sin inscripciones, sin horarios, instructor A.
        Grupo grupoA2 = new Grupo();
        grupoA2.setCategoria(CATEGORIA);
        grupoA2.setCurso(CURSO_A2);
        grupoA2.setImagenGrupo(imagenId);
        grupoA2.setIdInstructor(instructorAId);
        grupoA2.setCupos(5);
        grupoA2.setFechaCreacion(LocalDate.now());
        grupoA2.setFechaInscripcionApertura(LocalDate.now());
        grupoA2.setPeriodo(1);
        grupoServicio.insertarGrupo(grupoA2);

        // Grupo A3: se crea activo y luego se elimina logicamente -- no debe aparecer.
        Grupo grupoA3 = new Grupo();
        grupoA3.setCategoria(CATEGORIA);
        grupoA3.setCurso(CURSO_A3_ELIMINADO);
        grupoA3.setImagenGrupo(imagenId);
        grupoA3.setIdInstructor(instructorAId);
        grupoA3.setCupos(5);
        grupoA3.setFechaCreacion(LocalDate.now());
        grupoA3.setFechaInscripcionApertura(LocalDate.now());
        grupoA3.setPeriodo(1);
        int iterableA3 = grupoServicio.insertarGrupo(grupoA3).getIterable();
        try {
            grupoServicio.eliminarGrupo(CATEGORIA, CURSO_A3_ELIMINADO, ANIO, iterableA3);
        } catch (YaExisteElementoExcepcion e) {
            // Ya estaba eliminado de una corrida anterior; se reutiliza.
        }

        // Grupo B1: instructor B -- no debe aparecer en la respuesta de A.
        Grupo grupoB1 = new Grupo();
        grupoB1.setCategoria(CATEGORIA);
        grupoB1.setCurso(CURSO_B1);
        grupoB1.setImagenGrupo(imagenId);
        grupoB1.setIdInstructor(instructorBId);
        grupoB1.setCupos(5);
        grupoB1.setFechaCreacion(LocalDate.now());
        grupoB1.setFechaInscripcionApertura(LocalDate.now());
        grupoB1.setPeriodo(1);
        grupoServicio.insertarGrupo(grupoB1);

        // Conteos de A1: alumno1 ocupa el unico cupo (INSCRITO), alumno2 queda
        // EN_ESPERA, alumno3 se inscribe y luego se desvincula -- no debe contar.
        String alumno1 = "alumno-it-misgrupos-1-" + sufijo;
        String alumno2 = "alumno-it-misgrupos-2-" + sufijo;
        String alumno3 = "alumno-it-misgrupos-3-" + sufijo;
        crearAlumnoSiNoExiste(alumno1);
        crearAlumnoSiNoExiste(alumno2);
        crearAlumnoSiNoExiste(alumno3);
        inscripcionServicio.inscribir(new Inscripcion(alumno1, CATEGORIA, CURSO_A1, ANIO, iterableA1, null, null, null));
        inscripcionServicio.inscribir(new Inscripcion(alumno2, CATEGORIA, CURSO_A1, ANIO, iterableA1, null, null, null));
        inscripcionServicio.inscribir(new Inscripcion(alumno3, CATEGORIA, CURSO_A1, ANIO, iterableA1, null, null, null));
        inscripcionServicio.desvincularInscripcion(alumno3, CATEGORIA, CURSO_A1, ANIO, iterableA1);
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
        perfilDto.setTipoAlumno("Docente");
        try {
            instructorServicio.registrarInstructor(perfilDto);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior; se reutiliza.
        }
    }

    private void crearCursoSiNoExiste(String nombreCurso) {
        try {
            Curso curso = new Curso();
            curso.setNombre(nombreCurso);
            curso.setCategoriaCurso(CATEGORIA);
            curso.setDescripcion("Curso de prueba para GrupoMisGruposIT");
            curso.setImagenId(imagenId);
            curso.setDeporte(DEPORTE_SEED);
            curso.setEstadoCurso(EstadoCurso.ACTIVO);
            curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
            cursoServicio.insertarCurso(curso);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior; se reutiliza.
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
            // Ya existe de una corrida anterior; se reutiliza.
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
    void misGrupos_http_retornaSoloGruposActivosDelInstructor() throws Exception {
        mockMvc.perform(get("/api/v2/grupo/misGrupos")
                .header("Authorization", "Bearer " + buildJwt("Instructor", instructorAId)))
                .andExpect(status().isOk())
                // Solo A1 y A2 -- ni A3 (eliminado) ni B1 (de otro instructor).
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.curso=='" + CURSO_A1 + "')]").exists())
                .andExpect(jsonPath("$[?(@.curso=='" + CURSO_A2 + "')]").exists())
                .andExpect(jsonPath("$[?(@.curso=='" + CURSO_A3_ELIMINADO + "')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.curso=='" + CURSO_B1 + "')]").doesNotExist());
    }

    // Verificacion exacta de conteos/horarios a nivel de servicio (mismo bean que
    // usa el controller) -- evita depender de sintaxis de filtro JsonPath para
    // extraer valores escalares, que no esta probada en este proyecto mas alla de
    // exists()/doesNotExist() (ver InstructorIT).
    @Test
    void misGrupos_servicio_calculaConteosYHorariosCorrectamente() {
        List<InstructorGrupoResumen> resultado = grupoServicio.obtenerMisGrupos(instructorAId);

        assertEquals(2, resultado.size());

        InstructorGrupoResumen filaA1 = resultado.stream()
                .filter(r -> CURSO_A1.equals(r.getCurso())).findFirst().orElseThrow();
        assertEquals(1, filaA1.getInscritos(), "alumno1 ocupa el unico cupo");
        assertEquals(1, filaA1.getEnEspera(), "alumno2 en espera; alumno3 se desvinculo y no cuenta");
        assertEquals(2, filaA1.getHorarios().size(), "una sola fila por grupo, con los 2 horarios anidados");

        InstructorGrupoResumen filaA2 = resultado.stream()
                .filter(r -> CURSO_A2.equals(r.getCurso())).findFirst().orElseThrow();
        assertEquals(0, filaA2.getInscritos());
        assertEquals(0, filaA2.getEnEspera());
        assertTrue(filaA2.getHorarios().isEmpty());
    }

    // Confirma con datos reales que el PERF_ID guardado como instructor del grupo
    // coincide exactamente con el perf_id del JWT usado para autenticarse.
    @Test
    void misGrupos_elIdInstructorGuardadoCoincideConElPerfIdDelJwt() {
        Grupo grupoA1 = grupoServicio.obtenerGrupoPorId(CATEGORIA, CURSO_A1, ANIO, iterableA1);
        assertEquals(instructorAId, grupoA1.getIdInstructor());
    }
}
