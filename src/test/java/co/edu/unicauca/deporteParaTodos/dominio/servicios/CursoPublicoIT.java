package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
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
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IImagenServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Categoria;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PublicoCurso;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;

/**
 * Publico del curso (test de integracion, BD MySQL real): se guarda y se actualiza, se valida al inscribir segun el tipo
 * de vinculo del alumno y el Alumno solo ve los cursos que le corresponden (su tipo + algun grupo abierto por fecha).
 *
 * Requiere la migracion 2026-11-03_publico_curso.sql (columna CUR_PUBLICO). Todo lo que crea lleva el prefijo
 * "CursoPubIT" y se borra al terminar.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class CursoPublicoIT {

    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
    private static final String PREFIJO_PERFIL = "cursopubit-%";
    private static final String PREFIJO_CATEGORIA = "CursoPubIT-Cat-%";
    private static final String DEPORTE_SEED = "Natacion";
    private static final String COORD = "cursopubit-coord";

    @Autowired private IGrupoServicio grupoServicio;
    @Autowired private ICategoriaCursoServicio categoriaCursoServicio;
    @Autowired private ICursoServicio cursoServicio;
    @Autowired private IInstructorServicio instructorServicio;
    @Autowired private IImagenServicio imagenServicio;
    @Autowired private IPerfilGateway perfilGateway;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Clock clock;

    private LocalDate hoy;
    private Integer imagenId;
    private String categoria;
    private String instructor;
    private String estudiante;
    private String docente;
    private String contratista;
    private Grupo grupoEstudiantes;
    private Grupo grupoFuncionarios;
    private Grupo grupoMixto;

    @BeforeAll
    void crearFixtures() {
        limpiar();
        hoy = LocalDate.now(clock);
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        categoria = "CursoPubIT-Cat-" + sufijo;

        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-cursopubit.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {3, 1, 4}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        Categoria datosCategoria = new Categoria();
        datosCategoria.setTitulo(categoria);
        datosCategoria.setDescripcion("Categoria de prueba para CursoPublicoIT");
        datosCategoria.setImagen(imagenId);
        datosCategoria.setEliminado(0);
        categoriaCursoServicio.insertarCategoria(datosCategoria);

        instructor = crearInstructor(sufijo);
        estudiante = crearAlumno("est-" + sufijo, "Estudiante");
        docente = crearAlumno("doc-" + sufijo, "Docente");
        contratista = crearAlumno("con-" + sufijo, "Contratista");

        grupoEstudiantes = crearCursoYGrupo("CursoPubIT-ParaEstudiantes", PublicoCurso.ESTUDIANTE, hoy.plusDays(10));
        grupoFuncionarios = crearCursoYGrupo("CursoPubIT-ParaFuncionarios", PublicoCurso.FUNCIONARIO, hoy.plusDays(10));
        grupoMixto = crearCursoYGrupo("CursoPubIT-ParaTodos", PublicoCurso.MIXTO, hoy.plusDays(10));
        // Curso para toda la comunidad cuyo unico grupo ya cerro por fecha: no debe verlo el Alumno.
        crearCursoYGrupo("CursoPubIT-Cerrado", PublicoCurso.MIXTO, hoy.minusDays(1));
    }

    @AfterAll
    void limpiarAlTerminar() {
        limpiar();
    }

    private void limpiar() {
        jdbc.update("DELETE FROM tbl_inscripcion WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_grupo WHERE CAT_TITULO LIKE ?", PREFIJO_CATEGORIA);
        jdbc.update("DELETE FROM tbl_curso WHERE cat_titulo LIKE ?", PREFIJO_CATEGORIA);
        jdbc.update("DELETE FROM tbl_categoria_curso WHERE CAT_TITULO LIKE ?", PREFIJO_CATEGORIA);
        jdbc.update("DELETE FROM tbl_instructor WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_alumno WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_perfil WHERE perf_id LIKE ?", PREFIJO_PERFIL);
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private String crearInstructor(String sufijo) {
        String id = "cursopubit-ins-" + sufijo;
        PerfilDto dto = new PerfilDto();
        dto.setId(id);
        dto.setNombre("Prof. Prueba " + sufijo);
        dto.setCorreo(id + "@it.unicauca.edu.co");
        dto.setTipoId("CC");
        dto.setSexo("M");
        dto.setTipoAlumno("Docente");
        instructorServicio.registrarInstructor(dto);
        return id;
    }

    private String crearAlumno(String sufijoId, String tipo) {
        String id = "cursopubit-" + sufijoId;
        Perfil perfil = new Perfil();
        perfil.setId(id);
        perfil.setNombre("Alumno Prueba " + sufijoId);
        perfil.setCorreo(id + "@it.unicauca.edu.co");
        perfil.setTipoId("CC");
        perfil.setSexo("M");
        perfil.setTipoAlumno(tipo);
        perfil.setDependencia("Contratista".equals(tipo) ? "Oficina de prueba" : null);
        perfilGateway.registrarAlumno(perfil);
        return id;
    }

    private Grupo crearCursoYGrupo(String nombreCurso, PublicoCurso publico, LocalDate cierreInscripcion) {
        Curso curso = new Curso();
        curso.setNombre(nombreCurso);
        curso.setCategoriaCurso(categoria);
        curso.setDescripcion("Curso de prueba para CursoPublicoIT");
        curso.setImagenId(imagenId);
        curso.setDeporte(DEPORTE_SEED);
        curso.setEstadoCurso(EstadoCurso.ACTIVO);
        curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
        curso.setPublico(publico);
        cursoServicio.insertarCurso(curso);

        Grupo nuevo = new Grupo();
        nuevo.setCategoria(categoria);
        nuevo.setCurso(nombreCurso);
        nuevo.setImagenGrupo(imagenId);
        nuevo.setIdInstructor(instructor);
        nuevo.setCupos(50);
        nuevo.setFechaCreacion(hoy.minusDays(30));
        nuevo.setFechaIncripcionCierre(java.time.LocalDate.now().plusYears(1)); // Fase 3: el cierre es obligatorio
        nuevo.setFechaInscripcionApertura(hoy.minusDays(20));
        nuevo.setFechaIncripcionCierre(cierreInscripcion);
        nuevo.setPeriodo(0);
        return grupoServicio.insertarGrupo(nuevo);
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

    private String cuerpoCurso(String nombre, String publico) {
        return "{\"nombre\":\"" + nombre + "\",\"deporte\":\"" + DEPORTE_SEED + "\",\"categoriaCurso\":\"" + categoria
                + "\",\"descripcion\":\"Curso creado por CursoPublicoIT\",\"idImagen\":" + imagenId
                + (publico == null ? "" : ",\"publico\":\"" + publico + "\"") + "}";
    }

    private String publicoGuardado(String nombre) {
        return jdbc.queryForObject("SELECT CUR_PUBLICO FROM tbl_curso WHERE CAT_TITULO = ? AND CUR_NOMBRE = ?",
                String.class, categoria, nombre);
    }

    private org.springframework.test.web.servlet.ResultActions inscribir(String alumnoId, Grupo g) throws Exception {
        String payload = "{\"alumnoId\":\"" + alumnoId + "\",\"categoria\":\"" + g.getCategoria()
                + "\",\"curso\":\"" + g.getCurso() + "\",\"anio\":" + g.getAnio()
                + ",\"iterable\":" + g.getIterable() + "}";
        return mockMvc.perform(post("/api/v2/inscripcion")
                .contentType("application/json")
                .content(payload)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)));
    }

    // ── guardar y actualizar el publico ───────────────────────────────────────

    @Test
    void crearCurso_conPublico_loGuardaYLoDevuelve() throws Exception {
        String nombre = "CursoPubIT-Creado-" + UUID.randomUUID().toString().substring(0, 6);

        mockMvc.perform(post("/api/v2/curso").contentType("application/json")
                        .content(cuerpoCurso(nombre, "FUNCIONARIO"))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publico").value("FUNCIONARIO"));

        assertEquals("FUNCIONARIO", publicoGuardado(nombre));
        mockMvc.perform(get("/api/v2/curso").param("prmCategoria", categoria).param("prmCurso", nombre)
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publico").value("FUNCIONARIO"));
    }

    @Test
    void crearCurso_sinPublico_quedaParaTodaLaComunidad() throws Exception {
        String nombre = "CursoPubIT-SinPublico-" + UUID.randomUUID().toString().substring(0, 6);

        mockMvc.perform(post("/api/v2/curso").contentType("application/json")
                        .content(cuerpoCurso(nombre, null))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publico").value("MIXTO"));

        assertEquals("MIXTO", publicoGuardado(nombre));
    }

    @Test
    void crearCurso_conPublicoDesconocido_retorna400() throws Exception {
        String nombre = "CursoPubIT-Invalido-" + UUID.randomUUID().toString().substring(0, 6);

        mockMvc.perform(post("/api/v2/curso").contentType("application/json")
                        .content(cuerpoCurso(nombre, "DOCENTES"))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarCurso_cambiaElPublico() throws Exception {
        String nombre = "CursoPubIT-Actualizar-" + UUID.randomUUID().toString().substring(0, 6);
        mockMvc.perform(post("/api/v2/curso").contentType("application/json")
                        .content(cuerpoCurso(nombre, "ESTUDIANTE"))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isCreated());
        assertEquals("ESTUDIANTE", publicoGuardado(nombre));

        mockMvc.perform(put("/api/v2/curso").param("categoria", categoria).param("curso", nombre)
                        .contentType("application/json")
                        .content(cuerpoCurso(nombre, "FUNCIONARIO"))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publico").value("FUNCIONARIO"));

        assertEquals("FUNCIONARIO", publicoGuardado(nombre));
    }

    // ── inscribir segun el tipo de vinculo ────────────────────────────────────

    @Test
    void inscribir_unEstudianteEnUnCursoParaFuncionarios_retorna422ConCodigoGC0030() throws Exception {
        inscribir(estudiante, grupoFuncionarios)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0030"));

        // Solo en ESTE curso: otros tests de la clase inscriben al mismo estudiante en el curso para estudiantes.
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM tbl_inscripcion WHERE perf_id = ? AND CUR_NOMBRE = ?",
                Integer.class, estudiante, grupoFuncionarios.getCurso()));
    }

    @Test
    void inscribir_unDocenteEnUnCursoParaEstudiantes_retorna422ConCodigoGC0030() throws Exception {
        inscribir(docente, grupoEstudiantes)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0030"));
    }

    @Test
    void inscribir_unDocenteEnUnCursoParaFuncionarios_seInscribe() throws Exception {
        inscribir(docente, grupoFuncionarios).andExpect(status().isCreated());
    }

    @Test
    void inscribir_unEstudianteEnUnCursoParaEstudiantes_seInscribe() throws Exception {
        inscribir(estudiante, grupoEstudiantes).andExpect(status().isCreated());
    }

    @Test
    void inscribir_unContratistaEnUnCursoParaTodaLaComunidad_seInscribe() throws Exception {
        inscribir(contratista, grupoMixto).andExpect(status().isCreated());
    }

    // ── lo que ve cada rol ────────────────────────────────────────────────────

    @Test
    void cursosPorCategoria_unEstudianteVeSoloLosSuyosConGrupoAbierto() throws Exception {
        mockMvc.perform(get("/api/v2/cursosbycategoria").param("prmCategoria", categoria)
                        .header("Authorization", "Bearer " + buildJwt("Alumno", estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", containsInAnyOrder("CursoPubIT-ParaEstudiantes", "CursoPubIT-ParaTodos")));
    }

    @Test
    void cursosPorCategoria_unDocenteVeFuncionariosYTodaLaComunidad() throws Exception {
        mockMvc.perform(get("/api/v2/cursosbycategoria").param("prmCategoria", categoria)
                        .header("Authorization", "Bearer " + buildJwt("Alumno", docente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", containsInAnyOrder("CursoPubIT-ParaFuncionarios", "CursoPubIT-ParaTodos")));
    }

    @Test
    void cursosPorCategoria_elCoordinadorVeTodosLosCursosIncluidoElCerrado() throws Exception {
        mockMvc.perform(get("/api/v2/cursosbycategoria").param("prmCategoria", categoria)
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isOk())
                // hasItems y no containsInAnyOrder: los demas tests de esta clase crean cursos en la misma categoria
                .andExpect(jsonPath("$[*].nombre", hasItems(
                        "CursoPubIT-ParaEstudiantes", "CursoPubIT-ParaFuncionarios", "CursoPubIT-ParaTodos", "CursoPubIT-Cerrado")));
    }
}
