package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
 * Faltas acumuladas - Etapa 5 (test de integracion, BD MySQL real):
 * GET /api/v2/alumnosGrupo con el campo "faltas" nuevo. Combina llamadas
 * reales a la API (para los casos que la confirmacion de asistencia ya
 * produce por si misma) con un par de inserciones directas por JDBC para los
 * dos casos "defensivos" de la consulta agregada que la app, tal como esta
 * hoy, nunca llega a producir por si sola (una fila AUSENTE en una clase SIN
 * confirmar no existe en ningun flujo real; ver AsistenciaServicio -- AUSENTE
 * solo se crea dentro de confirmarAsistencias(), que siempre fija
 * CLS_ASISTENCIA_CONFIRMADA_EN en la misma transaccion). Se deja explicito en
 * cada prueba cual es el origen del dato.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class FaltasAlumnosGrupoIT {

    private static final String PREFIJO_CURSO = "FaltasGrupoIT-%";
    private static final String PREFIJO_CATEGORIA = "FaltasGrupoIT-Cat-%";
    private static final String PREFIJO_PERFIL = "faltasgrupoit-%";
    private static final String DEPORTE_SEED = "Natacion";
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
    private static final String COORD = "faltasgrupoit-coord";

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
    private LocalDate hoy;

    @BeforeAll
    void crearFixtures() {
        limpiar();
        hoy = LocalDate.now(clock);
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        categoria = "FaltasGrupoIT-Cat-" + sufijo;

        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-faltasgrupoit.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {3, 1, 4}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        Categoria datosCategoria = new Categoria();
        datosCategoria.setTitulo(categoria);
        datosCategoria.setDescripcion("Categoria de prueba para FaltasAlumnosGrupoIT");
        datosCategoria.setImagen(imagenId);
        datosCategoria.setEliminado(0);
        categoriaCursoServicio.insertarCategoria(datosCategoria);

        instructorA = crearInstructor("a-" + sufijo);
    }

    @AfterAll
    void limpiarAlTerminar() {
        limpiar();
    }

    private void limpiar() {
        jdbc.update("DELETE FROM tbl_asistencia WHERE PERF_ID LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_novedad_clase WHERE CAT_TITULO LIKE ? AND CUR_NOMBRE LIKE ?", PREFIJO_CATEGORIA, PREFIJO_CURSO);
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
        String id = "faltasgrupoit-" + sufijoId;
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
        String id = "faltasgrupoit-alm-" + sufijoId;
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

    private Grupo crearGrupo(String sufijo) {
        String curso = "FaltasGrupoIT-" + sufijo;
        Curso datosCurso = new Curso();
        datosCurso.setNombre(curso);
        datosCurso.setCategoriaCurso(categoria);
        datosCurso.setDescripcion("Curso de prueba para FaltasAlumnosGrupoIT");
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
        nuevo.setFechaCreacion(hoy.minusDays(60));
        nuevo.setFechaInscripcionApertura(hoy.minusDays(60));
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

    private void inscribir(String alumnoId, Grupo g) throws Exception {
        String payload = "{\"alumnoId\":\"" + alumnoId + "\",\"categoria\":\"" + g.getCategoria()
                + "\",\"curso\":\"" + g.getCurso() + "\",\"anio\":" + g.getAnio()
                + ",\"iterable\":" + g.getIterable() + "}";
        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType("application/json")
                        .content(payload)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated());
    }

    // inscribir() usa la API real, que fija INSCR_FECHAINSCRIPCION="ahora".
    // Estas pruebas crean clases con fecha pasada (hoy.minusDays(N)) para
    // simular faltas ya ocurridas -- sin retrasar la inscripcion por JDBC,
    // esElegibleParaAusencia() las descartaria todas (correctamente: un
    // alumno no puede faltar a una clase anterior a su inscripcion). Mismo
    // patron de insercion directa que el resto de este archivo.
    private void backdatarInscripcion(String alumnoId, Grupo g, LocalDate fecha) {
        int filasActualizadas = jdbc.update("UPDATE tbl_inscripcion SET INSCR_FECHAINSCRIPCION = ? "
                        + "WHERE PERF_ID = ? AND CAT_TITULO = ? AND CUR_NOMBRE = ? AND GRP_ANIO = ? AND GRP_ITERABLE = ?",
                java.sql.Timestamp.valueOf(fecha.atStartOfDay()), alumnoId, g.getCategoria(), g.getCurso(),
                g.getAnio(), g.getIterable());

        if (filasActualizadas != 1) {
            throw new AssertionError(
                    "Se esperaba actualizar exactamente una inscripcion para el alumno "
                            + alumnoId + ", pero se actualizaron " + filasActualizadas);
        }
    }

    private int crearClase(Grupo grupo, LocalDate fecha) throws Exception {
        String payload = "{\"idGrupoCategoria\":\"" + grupo.getCategoria() + "\",\"idGrupoCurso\":\"" + grupo.getCurso()
                + "\",\"idGrupoAnio\":" + grupo.getAnio() + ",\"idGrupoIterable\":" + grupo.getIterable()
                + ",\"idInstructor\":\"" + instructorA + "\",\"fecha\":\"" + fecha
                + "\",\"horas\":0,\"minutos\":0}";
        MvcResult creada = mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(payload)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(creada.getResponse().getContentAsString(), "$.codigo");
    }

    // Confirma la clase marcando AUSENTE a "alumnoAusente" (no viene en el
    // cuerpo) -- genera exactamente 1 falta elegible para ese alumno.
    private void confirmarConAusente(int claseId, String alumnoPresenteDistinto) throws Exception {
        String cuerpo = alumnoPresenteDistinto == null
                ? "{\"asistencias\":[],\"confirmaTodosAusentes\":true}"
                : "{\"asistencias\":[{\"idPerfil\":\"" + alumnoPresenteDistinto + "\",\"estaAtendido\":true}],"
                        + "\"confirmaTodosAusentes\":false}";
        mockMvc.perform(put("/api/v2/clase/{codigo}/asistencia", claseId)
                        .contentType("application/json")
                        .content(cuerpo)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());
    }

    private Integer leerFaltas(Grupo grupo, String alumnoId, String rol, String perfId) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v2/alumnosGrupo")
                        .param("categoria", grupo.getCategoria())
                        .param("curso", grupo.getCurso())
                        .param("anio", String.valueOf(grupo.getAnio()))
                        .param("iterable", String.valueOf(grupo.getIterable()))
                        .header("Authorization", "Bearer " + buildJwt(rol, perfId)))
                .andExpect(status().isOk())
                .andReturn();
        String json = resultado.getResponse().getContentAsString();
        java.util.List<java.util.Map<String, Object>> lista = JsonPath.read(json, "$");
        for (java.util.Map<String, Object> item : lista) {
            if (alumnoId.equals(item.get("id"))) {
                return (Integer) item.get("faltas");
            }
        }
        throw new AssertionError("El alumno " + alumnoId + " no aparecio en la respuesta de /alumnosGrupo");
    }

    @Test
    void alumnosGrupo_dosFaltasConfirmadasElegibles_sumanDos() throws Exception {
        Grupo grupo = crearGrupo("basico");
        String alumno = crearAlumno("basico-1");
        inscribir(alumno, grupo);
        backdatarInscripcion(alumno, grupo, hoy.minusDays(10));

        int clase1 = crearClase(grupo, hoy.minusDays(2));
        confirmarConAusente(clase1, null);
        int clase2 = crearClase(grupo, hoy.minusDays(1));
        confirmarConAusente(clase2, null);

        assertEquals(2, leerFaltas(grupo, alumno, "Coordinador", COORD));
    }

    @Test
    void alumnosGrupo_claseEliminada_noCuentaLaFalta() throws Exception {
        Grupo grupo = crearGrupo("claseelim");
        String alumno = crearAlumno("claseelim-1");
        inscribir(alumno, grupo);
        backdatarInscripcion(alumno, grupo, hoy.minusDays(10));

        int claseActiva = crearClase(grupo, hoy.minusDays(3));
        confirmarConAusente(claseActiva, null);
        int claseAEliminar = crearClase(grupo, hoy.minusDays(2));
        confirmarConAusente(claseAEliminar, null);
        mockMvc.perform(delete("/api/v2/clase")
                        .param("id", String.valueOf(claseAEliminar))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());

        assertEquals(1, leerFaltas(grupo, alumno, "Coordinador", COORD));
    }

    @Test
    void alumnosGrupo_novedadActivaEseDia_excluyeLaFalta() throws Exception {
        Grupo grupo = crearGrupo("novedad");
        String alumno = crearAlumno("novedad-1");
        inscribir(alumno, grupo);
        backdatarInscripcion(alumno, grupo, hoy.minusDays(10));

        LocalDate fechaConNovedad = hoy.minusDays(2);
        int clase1 = crearClase(grupo, fechaConNovedad);
        confirmarConAusente(clase1, null);
        int clase2 = crearClase(grupo, hoy.minusDays(1));
        confirmarConAusente(clase2, null);

        String novedad = "{\"categoria\":\"" + grupo.getCategoria() + "\",\"curso\":\"" + grupo.getCurso()
                + "\",\"anio\":" + grupo.getAnio() + ",\"iterable\":" + grupo.getIterable()
                + ",\"fecha\":\"" + fechaConNovedad + "\",\"motivo\":\"CLIMA\"}";
        mockMvc.perform(post("/api/v2/novedades-clase")
                        .contentType("application/json")
                        .content(novedad)
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isCreated());

        // Solo cuenta la falta de clase2 -- la de clase1 (fechaConNovedad) se excluye.
        assertEquals(1, leerFaltas(grupo, alumno, "Coordinador", COORD));
    }

    @Test
    void alumnosGrupo_filaEliminadaLogicamente_noCuenta() throws Exception {
        Grupo grupo = crearGrupo("filaelim");
        String alumno = crearAlumno("filaelim-1");
        inscribir(alumno, grupo);
        backdatarInscripcion(alumno, grupo, hoy.minusDays(10));

        int clase1 = crearClase(grupo, hoy.minusDays(2));
        confirmarConAusente(clase1, null);
        int clase2 = crearClase(grupo, hoy.minusDays(1));
        confirmarConAusente(clase2, null);
        // Elimina logicamente la fila AUSENTE de clase2 directo por JDBC: ningun
        // flujo real de la app deja una fila AUSENTE con META_ELIMINADO=1 hoy
        // (ver javadoc de la clase) -- se fuerza el dato para probar el filtro.
        jdbc.update("UPDATE tbl_asistencia SET META_ELIMINADO = 1 WHERE PERF_ID = ? AND CLS_CODIGO = ?",
                alumno, clase2);

        assertEquals(1, leerFaltas(grupo, alumno, "Coordinador", COORD));
    }

    @Test
    void alumnosGrupo_claseSinConfirmar_noCuenta() throws Exception {
        Grupo grupo = crearGrupo("pendiente");
        String alumno = crearAlumno("pendiente-1");
        inscribir(alumno, grupo);
        backdatarInscripcion(alumno, grupo, hoy.minusDays(10));

        int clase1 = crearClase(grupo, hoy.minusDays(2));
        confirmarConAusente(clase1, null);
        int clasePendiente = crearClase(grupo, hoy.minusDays(1));
        // Fila AUSENTE insertada directo por JDBC en una clase SIN confirmar --
        // ningun endpoint real crea una fila AUSENTE fuera de confirmarAsistencias(),
        // que siempre fija CLS_ASISTENCIA_CONFIRMADA_EN; se fuerza el dato para
        // probar que la consulta exige confirmacion, no solo el estado de la fila.
        jdbc.update("INSERT INTO tbl_asistencia (META_ELIMINADO, PERF_ID, CLS_CODIGO, ASI_ESTADO) VALUES (0, ?, ?, 'AUSENTE')",
                alumno, clasePendiente);

        assertEquals(1, leerFaltas(grupo, alumno, "Coordinador", COORD));
    }

    @Test
    void alumnosGrupo_alumnoReinscrito_conservaFaltasAnteriores() throws Exception {
        Grupo grupo = crearGrupo("reinscrito");
        String alumno = crearAlumno("reinscrito-1");
        inscribir(alumno, grupo);
        backdatarInscripcion(alumno, grupo, hoy.minusDays(10));
        int clase1 = crearClase(grupo, hoy.minusDays(5));
        confirmarConAusente(clase1, null);

        // Se desvincula y se vuelve a inscribir -- las faltas ya generadas deben
        // seguir contando (la consulta agregada no filtra por inscripcion actual).
        String payloadDesvincular = "{\"alumnoId\":\"" + alumno + "\",\"categoria\":\"" + grupo.getCategoria()
                + "\",\"curso\":\"" + grupo.getCurso() + "\",\"anio\":" + grupo.getAnio()
                + ",\"iterable\":" + grupo.getIterable() + "}";
        mockMvc.perform(put("/api/v2/desvincularInscripcion")
                        .contentType("application/json")
                        .content(payloadDesvincular)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());
        inscribir(alumno, grupo);

        assertEquals(1, leerFaltas(grupo, alumno, "Coordinador", COORD));
    }

    @Test
    void alumnosGrupo_instructorYCoordinador_recibenElMismoTotal() throws Exception {
        Grupo grupo = crearGrupo("mismototal");
        String alumno = crearAlumno("mismototal-1");
        inscribir(alumno, grupo);
        backdatarInscripcion(alumno, grupo, hoy.minusDays(10));
        int clase1 = crearClase(grupo, hoy.minusDays(1));
        confirmarConAusente(clase1, null);

        Integer paraInstructor = leerFaltas(grupo, alumno, "Instructor", instructorA);
        Integer paraCoordinador = leerFaltas(grupo, alumno, "Coordinador", COORD);

        assertEquals(paraInstructor, paraCoordinador);
        assertEquals(1, paraInstructor);
    }
}
