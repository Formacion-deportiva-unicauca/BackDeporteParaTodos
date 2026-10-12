package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
 * GET /api/v2/grupo/instructor (test de integracion, BD MySQL real). SCRUM-78
 * (HE-13 HU-02): un Coordinador consulta los grupos de un instructor arbitrario
 * (idInstructor lo manda el cliente, a diferencia de misGrupos() que usa el JWT).
 *
 * Fixtures: instructor A con 2 grupos (uno con 2 horarios, 1 inscrito INSCRITO y
 * 1 EN_ESPERA; el otro eliminado logicamente y que NO debe aparecer); instructor B
 * con 1 grupo que NO debe aparecer en la lista de A; instructor eliminado -> 404.
 *
 * Identificadores UNICOS por corrida (sufijo aleatorio en cursos y perfiles) +
 * limpieza en @BeforeAll/@AfterAll (por prefijo, igual que InscripcionVentanaFechasIT)
 * para poder ejecutarse dos veces seguidas sin colisionar contra una BD persistente
 * (SCRUM-180). "hoy" se toma del Clock del contexto (America/Bogota), nunca de
 * LocalDate.now() sin zona.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class GrupoInstructorIT {

    private static final String CATEGORIA = "Deportes";
    private static final String PREFIJO_CURSO = "GrupoInstructorIT-%";
    private static final String PREFIJO_PERFIL = "grupoinstructorit-%";
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

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Clock clock;

    private Integer imagenId;
    private String instructorAId;
    private String instructorBId;
    private String instructorEliminadoId;
    private String cursoA1;
    private String cursoA2Eliminado;
    private String cursoB1;
    private Grupo grupoA1;

    @BeforeAll
    void crearFixtures() {
        limpiarRestosDeCorridasAnteriores();

        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-grupoinstructor-it.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {4, 4, 4}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        try {
            categoriaCursoServicio.obtenerCategoriaCursoPorId(CATEGORIA);
        } catch (NoExisteExcepcion e) {
            Categoria categoria = new Categoria();
            categoria.setTitulo(CATEGORIA);
            categoria.setDescripcion("Categoria de prueba para GrupoInstructorIT");
            categoria.setImagen(imagenId);
            categoria.setEliminado(0);
            categoriaCursoServicio.insertarCategoria(categoria);
        }

        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        cursoA1 = "GrupoInstructorIT-A1-" + sufijo;
        cursoA2Eliminado = "GrupoInstructorIT-A2-" + sufijo;
        cursoB1 = "GrupoInstructorIT-B1-" + sufijo;
        crearCurso(cursoA1);
        crearCurso(cursoA2Eliminado);
        crearCurso(cursoB1);

        instructorAId = crearInstructor("instr-a-" + sufijo);
        instructorBId = crearInstructor("instr-b-" + sufijo);
        instructorEliminadoId = crearInstructor("instr-elim-" + sufijo);
        instructorServicio.eliminarInstructor(instructorEliminadoId);

        // Grupo A1: instructor A, 2 horarios, 1 cupo (1 INSCRITO + 1 EN_ESPERA).
        Grupo nuevoA1 = new Grupo();
        nuevoA1.setCategoria(CATEGORIA);
        nuevoA1.setCurso(cursoA1);
        nuevoA1.setImagenGrupo(imagenId);
        nuevoA1.setIdInstructor(instructorAId);
        nuevoA1.setCupos(1);
        nuevoA1.setFechaCreacion(LocalDate.now(clock));
        nuevoA1.setFechaInscripcionApertura(LocalDate.now(clock));
        nuevoA1.setPeriodo(1);
        grupoA1 = grupoServicio.insertarGrupo(nuevoA1);
        insertarHorario(cursoA1, grupoA1.getAnio(), grupoA1.getIterable(), "LUNES", "08:00", "10:00");
        insertarHorario(cursoA1, grupoA1.getAnio(), grupoA1.getIterable(), "MIERCOLES", "08:00", "10:00");

        String alumno1 = "grupoinstructorit-alum-1-" + sufijo;
        String alumno2 = "grupoinstructorit-alum-2-" + sufijo;
        crearAlumnoSiNoExiste(alumno1);
        crearAlumnoSiNoExiste(alumno2);
        inscripcionServicio.inscribir(new Inscripcion(
                alumno1, CATEGORIA, cursoA1, grupoA1.getAnio(), grupoA1.getIterable(), null, null, null));
        inscripcionServicio.inscribir(new Inscripcion(
                alumno2, CATEGORIA, cursoA1, grupoA1.getAnio(), grupoA1.getIterable(), null, null, null));

        // Grupo A2: instructor A, se crea y se elimina logicamente -- no debe aparecer.
        Grupo nuevoA2 = new Grupo();
        nuevoA2.setCategoria(CATEGORIA);
        nuevoA2.setCurso(cursoA2Eliminado);
        nuevoA2.setImagenGrupo(imagenId);
        nuevoA2.setIdInstructor(instructorAId);
        nuevoA2.setCupos(5);
        nuevoA2.setFechaCreacion(LocalDate.now(clock));
        nuevoA2.setFechaInscripcionApertura(LocalDate.now(clock));
        nuevoA2.setPeriodo(1);
        Grupo grupoA2 = grupoServicio.insertarGrupo(nuevoA2);
        grupoServicio.eliminarGrupo(CATEGORIA, cursoA2Eliminado, grupoA2.getAnio(), grupoA2.getIterable());

        // Grupo B1: instructor B -- no debe aparecer en la lista de A.
        Grupo nuevoB1 = new Grupo();
        nuevoB1.setCategoria(CATEGORIA);
        nuevoB1.setCurso(cursoB1);
        nuevoB1.setImagenGrupo(imagenId);
        nuevoB1.setIdInstructor(instructorBId);
        nuevoB1.setCupos(5);
        nuevoB1.setFechaCreacion(LocalDate.now(clock));
        nuevoB1.setFechaInscripcionApertura(LocalDate.now(clock));
        nuevoB1.setPeriodo(1);
        grupoServicio.insertarGrupo(nuevoB1);
    }

    @AfterAll
    void limpiarAlTerminar() {
        limpiarRestosDeCorridasAnteriores();
    }

    // Orden de FK: inscripcion -> horario -> grupo -> curso -> instructor -> alumno
    // -> perfil. NO toca tbl_categoria_curso ("Deportes" es compartida con otras IT).
    private void limpiarRestosDeCorridasAnteriores() {
        jdbc.update("DELETE FROM tbl_inscripcion WHERE PERF_ID LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_horario WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_grupo WHERE CAT_TITULO = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_curso WHERE cat_titulo = ? AND CUR_NOMBRE LIKE ?", CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_instructor WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_alumno WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_perfil WHERE perf_id LIKE ?", PREFIJO_PERFIL);
    }

    private void crearCurso(String nombreCurso) {
        Curso curso = new Curso();
        curso.setNombre(nombreCurso);
        curso.setCategoriaCurso(CATEGORIA);
        curso.setDescripcion("Curso de prueba para GrupoInstructorIT");
        curso.setImagenId(imagenId);
        curso.setDeporte(DEPORTE_SEED);
        curso.setEstadoCurso(EstadoCurso.ACTIVO);
        curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
        cursoServicio.insertarCurso(curso);
    }

    // Retorna el id efectivamente usado (prefijo fijo + sufijo de esta corrida).
    private String crearInstructor(String sufijoId) {
        String id = "grupoinstructorit-" + sufijoId;
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

    private void insertarHorario(String curso, int anio, int iterable, String dia, String horaInicio, String horaFin) {
        Horario horario = new Horario();
        horario.setCategoria(CATEGORIA);
        horario.setCurso(curso);
        horario.setAnio(anio);
        horario.setIterable(iterable);
        horario.setDia(dia);
        horario.setHoraInicio(horaInicio);
        horario.setHoraFin(horaFin);
        horario.setEscenario("Cancha IT");
        horarioServicio.insertarHorario(horario);
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
    void gruposDeInstructor_http_coordinadorConsultaInstructorA_retornaSoloSusGruposActivos() throws Exception {
        mockMvc.perform(get("/api/v2/grupo/instructor")
                .param("idInstructor", instructorAId)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk())
                // Solo A1 -- ni A2 (eliminado) ni B1 (de otro instructor).
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[?(@.curso=='" + cursoA1 + "')]").exists())
                .andExpect(jsonPath("$[?(@.curso=='" + cursoA2Eliminado + "')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.curso=='" + cursoB1 + "')]").doesNotExist());
    }

    // Verificacion exacta de conteos/horarios a nivel de servicio (mismo bean que usa
    // el controller) -- igual patron que GrupoMisGruposIT, evita depender de sintaxis
    // de filtro JsonPath para extraer valores escalares.
    @Test
    void gruposDeInstructor_servicio_calculaConteosYHorariosCorrectamente() {
        List<InstructorGrupoResumen> resultado = grupoServicio.obtenerGruposDeInstructor(instructorAId);

        assertEquals(1, resultado.size());
        InstructorGrupoResumen filaA1 = resultado.get(0);
        assertEquals(cursoA1, filaA1.getCurso());
        assertEquals(1, filaA1.getInscritos(), "alumno1 ocupa el unico cupo");
        assertEquals(1, filaA1.getEnEspera(), "alumno2 en espera");
        assertEquals(2, filaA1.getHorarios().size(), "una sola fila por grupo, con los 2 horarios anidados");
    }

    @Test
    void gruposDeInstructor_http_instructorB_noVeLosGruposDeA() throws Exception {
        mockMvc.perform(get("/api/v2/grupo/instructor")
                .param("idInstructor", instructorBId)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].curso").value(cursoB1));
    }

    @Test
    void gruposDeInstructor_http_instructorEliminado_retorna404() throws Exception {
        mockMvc.perform(get("/api/v2/grupo/instructor")
                .param("idInstructor", instructorEliminadoId)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isNotFound());
    }

    @Test
    void gruposDeInstructor_http_instructorInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/api/v2/grupo/instructor")
                .param("idInstructor", "grupoinstructorit-no-existe")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isNotFound());
    }
}
