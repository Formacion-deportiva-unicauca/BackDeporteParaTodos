package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
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
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Categoria;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;

/**
 * Faltas acumuladas - Etapa 4 (test de integracion, BD MySQL real):
 * PUT /api/v2/clase/{codigo}/asistencia. Cubre los escenarios de mayor riesgo
 * del contrato -- registro normal, instructor ajeno, alumno fuera del grupo,
 * SIN_ASISTENTES_SIN_CONFIRMAR, y los dos endpoints viejos (POST /atenciones,
 * DELETE /asistencia) una vez que la clase ya quedo confirmada -- sin repetir
 * exhaustivamente cada escenario que ya tiene cobertura unitaria equivalente
 * en AsistenciaServicioTest (idempotencia, reconfirmacion, elegibilidad por
 * fecha de inscripcion, ventana/plazo/horario).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class ConfirmacionAsistenciaIT {

    private static final String PREFIJO_CURSO = "ConfirmAsistIT-%";
    private static final String PREFIJO_CATEGORIA = "ConfirmAsistIT-Cat-%";
    private static final String PREFIJO_PERFIL = "confirmasistit-%";
    private static final String DEPORTE_SEED = "Natacion";
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
    private static final String COORD = "confirmasistit-coord";

    @Autowired private IGrupoServicio grupoServicio;
    @Autowired private ICategoriaCursoServicio categoriaCursoServicio;
    @Autowired private ICursoServicio cursoServicio;
    @Autowired private IHorarioServicio horarioServicio;
    @Autowired private IInstructorServicio instructorServicio;
    @Autowired private IImagenServicio imagenServicio;
    @Autowired private IPerfilGateway perfilGateway;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Clock clock;

    private Integer imagenId;
    private String categoria;
    private String instructorA;
    private String instructorB;
    private LocalDate hoy;

    @BeforeAll
    void crearFixtures() {
        limpiar();
        hoy = LocalDate.now(clock);
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        categoria = "ConfirmAsistIT-Cat-" + sufijo;

        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-confirmasistit.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {3, 1, 4}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        Categoria datosCategoria = new Categoria();
        datosCategoria.setTitulo(categoria);
        datosCategoria.setDescripcion("Categoria de prueba para ConfirmacionAsistenciaIT");
        datosCategoria.setImagen(imagenId);
        datosCategoria.setEliminado(0);
        categoriaCursoServicio.insertarCategoria(datosCategoria);

        instructorA = crearInstructor("a-" + sufijo);
        instructorB = crearInstructor("b-" + sufijo);
    }

    @AfterAll
    void limpiarAlTerminar() {
        limpiar();
    }

    private void limpiar() {
        jdbc.update("DELETE FROM tbl_asistencia WHERE PERF_ID LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_clase WHERE CAT_TITULO LIKE ? AND CUR_NOMBRE LIKE ?", PREFIJO_CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_inscripcion WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_horario WHERE CAT_TITULO LIKE ? AND CUR_NOMBRE LIKE ?", PREFIJO_CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_grupo WHERE CAT_TITULO LIKE ? AND CUR_NOMBRE LIKE ?", PREFIJO_CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_curso WHERE cat_titulo LIKE ? AND CUR_NOMBRE LIKE ?", PREFIJO_CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_categoria_curso WHERE CAT_TITULO LIKE ?", PREFIJO_CATEGORIA);
        jdbc.update("DELETE FROM tbl_instructor WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_alumno WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_perfil WHERE perf_id LIKE ?", PREFIJO_PERFIL);
    }

    private String crearInstructor(String sufijoId) {
        String id = "confirmasistit-" + sufijoId;
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

    private String crearAlumno(String sufijoId) {
        String id = "confirmasistit-alm-" + sufijoId;
        try {
            Perfil perfil = new Perfil();
            perfil.setId(id);
            perfil.setNombre("Alumno Prueba " + sufijoId);
            perfil.setCorreo(id + "@it.unicauca.edu.co");
            perfil.setTipoId("CC");
            perfil.setSexo("M");
            perfil.setTipoAlumno("Estudiante");
            perfilGateway.registrarAlumno(perfil);
        } catch (YaExisteElementoExcepcion e) {
            // No deberia ocurrir (id con sufijo unico), pero se tolera.
        }
        return id;
    }

    // Grupo + horario (todos los dias) + curso dedicados por prueba, para que
    // cada escenario sea independiente y repetible sin importar el orden.
    private Grupo crearGrupo(String sufijo) {
        String curso = "ConfirmAsistIT-" + sufijo;
        Curso datosCurso = new Curso();
        datosCurso.setNombre(curso);
        datosCurso.setCategoriaCurso(categoria);
        datosCurso.setDescripcion("Curso de prueba para ConfirmacionAsistenciaIT");
        datosCurso.setImagenId(imagenId);
        datosCurso.setDeporte(DEPORTE_SEED);
        datosCurso.setEstadoCurso(EstadoCurso.ACTIVO);
        datosCurso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
        cursoServicio.insertarCurso(datosCurso);

        Grupo nuevo = new Grupo();
        nuevo.setCategoria(categoria);
        nuevo.setCurso(curso);
        nuevo.setImagenGrupo(imagenId);
        nuevo.setIdInstructor(instructorA);
        nuevo.setCupos(50);
        nuevo.setFechaCreacion(hoy);
        nuevo.setFechaInscripcionApertura(hoy);
        nuevo.setPeriodo(0);
        Grupo grupo = grupoServicio.insertarGrupo(nuevo);
        for (String dia : new String[] {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"}) {
            horarioServicio.insertarHorario(new Horario(null, grupo.getCategoria(), grupo.getCurso(),
                    grupo.getAnio(), grupo.getIterable(), dia, "00:00", "23:59", "Cancha", 0));
        }
        return grupo;
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

    private String payloadInscripcion(String alumnoId, Grupo g) {
        return "{\"alumnoId\":\"" + alumnoId + "\",\"categoria\":\"" + g.getCategoria()
                + "\",\"curso\":\"" + g.getCurso() + "\",\"anio\":" + g.getAnio()
                + ",\"iterable\":" + g.getIterable() + "}";
    }

    private void inscribir(String alumnoId, Grupo g) throws Exception {
        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType("application/json")
                        .content(payloadInscripcion(alumnoId, g))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated());
    }

    private Integer contarAusenteActivo(String alumnoId, int claseId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_asistencia WHERE PERF_ID = ? AND CLS_CODIGO = ? "
                        + "AND ASI_ESTADO = 'AUSENTE' AND META_ELIMINADO = 0",
                Integer.class, alumnoId, claseId);
    }

    private String confirmarPayload(String alumnoPresente, boolean confirmaTodosAusentes) {
        String asistencias = alumnoPresente == null ? "[]"
                : "[{\"idPerfil\":\"" + alumnoPresente + "\",\"estaAtendido\":true}]";
        return "{\"asistencias\":" + asistencias + ",\"confirmaTodosAusentes\":" + confirmaTodosAusentes + "}";
    }

    @Test
    void confirmarAsistencias_instructorTitular_registroNormal_marcaPresenteYAusente() throws Exception {
        Grupo grupo = crearGrupo("normal");
        String alum1 = crearAlumno("normal-1");
        String alum2 = crearAlumno("normal-2");
        inscribir(alum1, grupo);
        inscribir(alum2, grupo);
        int claseId = crearClase(grupo);

        MvcResult resultado = mockMvc.perform(put("/api/v2/clase/{codigo}/asistencia", claseId)
                        .contentType("application/json")
                        .content(confirmarPayload(alum1, false))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoClase").value(claseId))
                .andExpect(jsonPath("$.presentes").value(1))
                .andExpect(jsonPath("$.ausentes").value(1))
                .andReturn();

        assertNotNull(JsonPath.read(resultado.getResponse().getContentAsString(), "$.confirmadaEn"));
        assertEquals(1, (int) contarAusenteActivo(alum2, claseId));
        LocalDate confirmadaEnBd = jdbc.queryForObject(
                "SELECT CAST(CLS_ASISTENCIA_CONFIRMADA_EN AS DATE) FROM tbl_clase WHERE CLS_CODIGO = ?",
                LocalDate.class, claseId);
        assertNotNull(confirmadaEnBd);
    }

    private int crearClase(Grupo grupo) throws Exception {
        String payload = "{\"idGrupoCategoria\":\"" + grupo.getCategoria() + "\",\"idGrupoCurso\":\"" + grupo.getCurso()
                + "\",\"idGrupoAnio\":" + grupo.getAnio() + ",\"idGrupoIterable\":" + grupo.getIterable()
                + ",\"idInstructor\":\"" + instructorA + "\",\"fecha\":\"" + hoy
                + "\",\"horas\":0,\"minutos\":0}";
        MvcResult creada = mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payload)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(creada.getResponse().getContentAsString(), "$.codigo");
    }

    @Test
    void confirmarAsistencias_instructorAjeno_retorna403YNoConfirmaLaClase() throws Exception {
        Grupo grupo = crearGrupo("ajeno");
        String alum1 = crearAlumno("ajeno-1");
        inscribir(alum1, grupo);
        int claseId = crearClase(grupo);

        mockMvc.perform(put("/api/v2/clase/{codigo}/asistencia", claseId)
                        .contentType("application/json")
                        .content(confirmarPayload(alum1, false))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorB)))
                .andExpect(status().isForbidden());

        Object confirmadaEn = jdbc.queryForObject(
                "SELECT CLS_ASISTENCIA_CONFIRMADA_EN FROM tbl_clase WHERE CLS_CODIGO = ?", Object.class, claseId);
        assertNull(confirmadaEn, "tras el 403 la clase no debe quedar confirmada");
    }

    @Test
    void confirmarAsistencias_idPerfilFueraDeGrupo_retorna422YNoCambiaNada() throws Exception {
        Grupo grupo = crearGrupo("fuera");
        String alum1 = crearAlumno("fuera-1");
        String ajeno = crearAlumno("fuera-ajeno"); // nunca se inscribe en este grupo
        inscribir(alum1, grupo);
        int claseId = crearClase(grupo);

        String cuerpo = "{\"asistencias\":[{\"idPerfil\":\"" + alum1 + "\",\"estaAtendido\":true},"
                + "{\"idPerfil\":\"" + ajeno + "\",\"estaAtendido\":true}],\"confirmaTodosAusentes\":false}";

        mockMvc.perform(put("/api/v2/clase/{codigo}/asistencia", claseId)
                        .contentType("application/json")
                        .content(cuerpo)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0027"));

        Long filas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_asistencia WHERE CLS_CODIGO = ?", Long.class, claseId);
        assertEquals(0L, filas, "fallo parcial: no debe quedar ninguna fila, ni la de alum1 que si era valido");
    }

    @Test
    void confirmarAsistencias_sinAsistentesSinBandera_retorna422() throws Exception {
        Grupo grupo = crearGrupo("sinbandera");
        String alum1 = crearAlumno("sinbandera-1");
        inscribir(alum1, grupo);
        int claseId = crearClase(grupo);

        mockMvc.perform(put("/api/v2/clase/{codigo}/asistencia", claseId)
                        .contentType("application/json")
                        .content(confirmarPayload(null, false))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0028"));
    }

    // Regla 8 del ticket: con la clase YA confirmada, los endpoints viejos
    // cambian de significado -- "marcar" siempre fuerza PRESENTE (incluso si
    // quedo AUSENTE por la confirmacion), y "desmarcar"/DELETE deja AUSENTE
    // (no un borrado) en vez del comportamiento fisico/logico de antes.
    @Test
    void endpointsViejos_claseConfirmada_marcarFuerzaPresenteYDeleteMarcaAusente() throws Exception {
        Grupo grupo = crearGrupo("viejos");
        String alum1 = crearAlumno("viejos-1");
        String alum2 = crearAlumno("viejos-2");
        inscribir(alum1, grupo);
        inscribir(alum2, grupo);
        int claseId = crearClase(grupo);

        // Confirma con alum1 presente -> alum2 queda AUSENTE.
        mockMvc.perform(put("/api/v2/clase/{codigo}/asistencia", claseId)
                        .contentType("application/json")
                        .content(confirmarPayload(alum1, false))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());
        assertEquals(1, (int) contarAusenteActivo(alum2, claseId));

        // POST /atenciones marcando a alum2 presente -- debe forzar PRESENTE.
        String cuerpoAtencion = "[{\"idPerfil\":\"" + alum2 + "\",\"idClase\":" + claseId + ",\"estaAtendido\":true}]";
        mockMvc.perform(post("/api/v2/atenciones")
                        .param("idClase", String.valueOf(claseId))
                        .contentType("application/json")
                        .content(cuerpoAtencion)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());
        assertEquals(0, (int) contarAusenteActivo(alum2, claseId), "alum2 ya no debe seguir AUSENTE");

        // DELETE /asistencia sobre alum1 (PRESENTE) -- debe dejarlo AUSENTE, no eliminado.
        mockMvc.perform(delete("/api/v2/asistencia")
                        .param("prmPerfId", alum1)
                        .param("prmClsCodigo", String.valueOf(claseId))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());
        assertEquals(1, (int) contarAusenteActivo(alum1, claseId), "DELETE en clase confirmada debe dejar AUSENTE, no eliminar");
    }

    @Test
    void confirmarAsistencias_coordinador_noEstaSujetoAHorarioNiPlazo() throws Exception {
        Grupo grupo = crearGrupo("coord");
        String alum1 = crearAlumno("coord-1");
        inscribir(alum1, grupo);
        int claseId = crearClase(grupo);

        mockMvc.perform(put("/api/v2/clase/{codigo}/asistencia", claseId)
                        .contentType("application/json")
                        .content(confirmarPayload(alum1, false))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isOk());
    }

    @Test
    void confirmarAsistencias_sinToken_retorna401() throws Exception {
        mockMvc.perform(put("/api/v2/clase/{codigo}/asistencia", 1)
                        .contentType("application/json")
                        .content(confirmarPayload(null, true)))
                .andExpect(status().isUnauthorized());
    }
}
