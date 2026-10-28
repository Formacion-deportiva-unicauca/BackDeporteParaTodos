package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Faltas acumuladas - Etapas 1 y 3 (test de integracion, BD MySQL real): las 5
 * consultas nativas de IAsistenciaRepositorio no filtraban META_ELIMINADO ni
 * en tbl_clase ni en tbl_asistencia (confirmado por lectura directa antes de
 * este cambio) -- una clase o una fila de asistencia borrada logicamente
 * seguia sumando en /estadisticas/*. Ademas (etapa 3), estadisticasAlumno es
 * la UNICA de las 5 que representa asistencias efectivas (las otras 4 cuentan
 * CLASES dictadas, no asistencias de alumnos) -- esa es la que debe excluir
 * tambien las filas ASI_ESTADO='AUSENTE' activas. Esta prueba fija ambos
 * comportamientos con datos reales: 3 clases activas + 1 clase eliminada + 1
 * fila de asistencia eliminada (en una clase activa) + 1 fila AUSENTE activa
 * (insertada por JDBC, sin endpoint real que la cree todavia), y verifica que
 * ni la clase eliminada ni la asistencia eliminada ni la ausencia se cuenten
 * en ninguna de las 5
 * consultas.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class EstadisticasMetaEliminadoIT {

    private static final String PREFIJO_CURSO = "EstadMetaElimIT-%";
    private static final String PREFIJO_CATEGORIA = "EstadMetaElimIT-Cat-%";
    private static final String PREFIJO_PERFIL = "estadmetaelimit-%";
    private static final String DEPORTE_SEED = "Natacion";
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
    private static final String COORD = "estadmetaelimit-coord";

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
    private String instructorA;
    private String alumno;
    private Grupo grupo;
    private LocalDate hoy;
    // Categoria PROPIA de esta corrida (no se reusa "Deportes" como en otros IT):
    // estadisticasCategorias() agrega por CATEGORIA completa, sin alcance de
    // curso/grupo -- compartir una categoria con otros IT haria que sus clases
    // tambien se sumaran aqui, rompiendo el conteo exacto que esta prueba verifica.
    private String categoria;

    @BeforeAll
    void crearFixtures() throws Exception {
        limpiarRestosDeCorridasAnteriores();
        hoy = LocalDate.now(clock);
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        categoria = "EstadMetaElimIT-Cat-" + sufijo;

        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-estadmetaelimit.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {3, 1, 4}));
        imagenId = imagenServicio.insertarImagen(imagenDto).getId();

        Categoria datosCategoria = new Categoria();
        datosCategoria.setTitulo(categoria);
        datosCategoria.setDescripcion("Categoria de prueba para EstadisticasMetaEliminadoIT");
        datosCategoria.setImagen(imagenId);
        datosCategoria.setEliminado(0);
        categoriaCursoServicio.insertarCategoria(datosCategoria);
        instructorA = crearInstructor("a-" + sufijo);
        alumno = crearAlumno("alm-" + sufijo);

        String curso = "EstadMetaElimIT-" + sufijo;
        Curso datosCurso = new Curso();
        datosCurso.setNombre(curso);
        datosCurso.setCategoriaCurso(categoria);
        datosCurso.setDescripcion("Curso de prueba para EstadisticasMetaEliminadoIT");
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
        grupo = grupoServicio.insertarGrupo(nuevo);
        for (String dia : new String[] {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"}) {
            horarioServicio.insertarHorario(new Horario(null, grupo.getCategoria(), grupo.getCurso(),
                    grupo.getAnio(), grupo.getIterable(), dia, "00:00", "23:59", "Cancha", 0));
        }

        // Clase 1: activa, con asistencia activa -- caso base (cuenta en todo).
        int clase1 = crearClase();
        marcarPresente(clase1);

        // Clase 2: tiene asistencia, pero la CLASE se elimina logicamente despues --
        // prueba cl.meta_eliminado=0 en las 5 consultas (categorias/cursos/grupos/
        // instructor via conteo de clases, alumno via el JOIN con tbl_clase).
        int clase2 = crearClase();
        marcarPresente(clase2);
        mockMvc.perform(delete("/api/v2/clase")
                        .param("id", String.valueOf(clase2))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());

        // Clase 3: queda ACTIVA, pero su fila de asistencia se elimina logicamente
        // despues -- prueba asis.meta_eliminado=0 especificamente (la clase en si
        // SI debe seguir contando en categorias/cursos/grupos/instructor).
        int clase3 = crearClase();
        marcarPresente(clase3);
        mockMvc.perform(delete("/api/v2/asistencia")
                        .param("prmPerfId", alumno)
                        .param("prmClsCodigo", String.valueOf(clase3))
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());

        // Clase 4 (etapa 3): activa, con una fila de asistencia ACTIVA (no
        // eliminada) pero en estado AUSENTE -- insertada directamente por JDBC
        // porque ningun endpoint actual crea filas AUSENTE todavia (eso llega
        // en la etapa 4; el filtro ASI_ESTADO='PRESENTE' de estadisticasAlumno
        // debe funcionar igual sin importar como se creo la fila). Prueba que
        // una ausencia activa no eliminada NO cuenta como asistencia efectiva.
        int clase4 = crearClase();
        jdbc.update("INSERT INTO tbl_asistencia (META_ELIMINADO, PERF_ID, CLS_CODIGO, ASI_ESTADO) VALUES (0, ?, ?, 'AUSENTE')",
                alumno, clase4);
    }

    @AfterAll
    void limpiarAlTerminar() {
        limpiarRestosDeCorridasAnteriores();
    }

    private void limpiarRestosDeCorridasAnteriores() {
        jdbc.update("DELETE FROM tbl_asistencia WHERE PERF_ID LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_clase WHERE CAT_TITULO LIKE ? AND CUR_NOMBRE LIKE ?", PREFIJO_CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_inscripcion WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_horario WHERE CAT_TITULO LIKE ? AND CUR_NOMBRE LIKE ?", PREFIJO_CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_grupo WHERE CAT_TITULO LIKE ? AND CUR_NOMBRE LIKE ?", PREFIJO_CATEGORIA, PREFIJO_CURSO);
        jdbc.update("DELETE FROM tbl_curso WHERE cat_titulo LIKE ? AND CUR_NOMBRE LIKE ?", PREFIJO_CATEGORIA, PREFIJO_CURSO);
        // Categoria propia de esta corrida (no "Deportes"): se limpia tambien, a
        // diferencia de otros IT que SI dejan "Deportes" para reutilizarla.
        jdbc.update("DELETE FROM tbl_categoria_curso WHERE CAT_TITULO LIKE ?", PREFIJO_CATEGORIA);
        jdbc.update("DELETE FROM tbl_instructor WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_alumno WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_perfil WHERE perf_id LIKE ?", PREFIJO_PERFIL);
    }

    private String crearInstructor(String sufijoId) {
        String id = "estadmetaelimit-" + sufijoId;
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
        String id = "estadmetaelimit-" + sufijoId;
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

    private int crearClase() throws Exception {
        MvcResult creada = mockMvc.perform(post("/api/v2/claseGrupo")
                        .contentType("application/json")
                        .content(construirPayloadClaseUnica())
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(creada.getResponse().getContentAsString(), "$.codigo");
    }

    private int contadorClases = 0;

    private String construirPayloadClaseUnica() {
        // Las 3 clases de esta prueba deben quedar en filas DISTINTAS (no
        // reusarse): se separan por fecha, retrocediendo un dia por cada nueva
        // clase (el grupo tiene horario los 7 dias, asi que cualquier fecha es
        // valida para el Instructor).
        LocalDate fecha = hoy.minusDays(contadorClases++);
        return "{\"idGrupoCategoria\":\"" + grupo.getCategoria() + "\",\"idGrupoCurso\":\"" + grupo.getCurso()
                + "\",\"idGrupoAnio\":" + grupo.getAnio() + ",\"idGrupoIterable\":" + grupo.getIterable()
                + ",\"idInstructor\":\"" + instructorA + "\",\"fecha\":\"" + fecha
                + "\",\"horas\":0,\"minutos\":0}";
    }

    private void marcarPresente(int claseId) throws Exception {
        String cuerpo = "[{\"idPerfil\":\"" + alumno + "\",\"idClase\":" + claseId + ",\"estaAtendido\":true}]";
        mockMvc.perform(post("/api/v2/atenciones")
                        .param("idClase", String.valueOf(claseId))
                        .contentType("application/json")
                        .content(cuerpo)
                        .header("Authorization", "Bearer " + buildJwt("Instructor", instructorA)))
                .andExpect(status().isOk());
    }

    // ── Etapa 1: las 4 consultas que cuentan CLASES deben excluir la eliminada ──

    @Test
    void estadisticasCategorias_excluyeClaseEliminada() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v2/estadisticas/categorias")
                        .param("inicio", hoy.minusDays(5).toString())
                        .param("fin", hoy.toString())
                        .param("categoria", categoria)
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].leyenda1").value(categoria))
                .andReturn();
        double clases = JsonPath.read(resultado.getResponse().getContentAsString(), "$[0].clases");
        assertEquals(3.0, clases, "clase1, clase3 y clase4 estan activas; solo clase2 fue eliminada");
    }

    @Test
    void estadisticasCursos_excluyeClaseEliminada() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v2/estadisticas/cursos")
                        .param("inicio", hoy.minusDays(5).toString())
                        .param("fin", hoy.toString())
                        .param("categoria", grupo.getCategoria())
                        .param("curso", grupo.getCurso())
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isOk())
                .andReturn();
        double clases = JsonPath.read(resultado.getResponse().getContentAsString(), "$[0].clases");
        assertEquals(3.0, clases);
    }

    @Test
    void estadisticasGrupos_excluyeClaseEliminada() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v2/estadisticas/grupos")
                        .param("inicio", hoy.minusDays(5).toString())
                        .param("fin", hoy.toString())
                        .param("categoria", grupo.getCategoria())
                        .param("curso", grupo.getCurso())
                        .param("anio", String.valueOf(grupo.getAnio()))
                        .param("iterable", String.valueOf(grupo.getIterable()))
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isOk())
                .andReturn();
        double clases = JsonPath.read(resultado.getResponse().getContentAsString(), "$[0].clases");
        assertEquals(3.0, clases);
    }

    @Test
    void estadisticasInstructor_excluyeClaseEliminada() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v2/estadisticas/instructores")
                        .param("inicio", hoy.minusDays(5).toString())
                        .param("fin", hoy.toString())
                        .param("instructor", instructorA)
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isOk())
                .andReturn();
        double clases = JsonPath.read(resultado.getResponse().getContentAsString(), "$[0].clases");
        assertEquals(3.0, clases);
    }

    // ── Etapa 3: estadisticasAlumno tambien excluye AUSENTE activa (ademas de
    // asistencia eliminada y asistencia de clase eliminada, de la etapa 1) ──

    @Test
    void estadisticasAlumno_excluyeAsistenciaEliminadaClaseEliminadaYAusente() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v2/estadisticas/alumnos")
                        .param("inicio", hoy.minusDays(5).toString())
                        .param("fin", hoy.toString())
                        .param("alumno", alumno)
                        .header("Authorization", "Bearer " + buildJwt("Coordinador", COORD)))
                .andExpect(status().isOk())
                .andReturn();
        double clases = JsonPath.read(resultado.getResponse().getContentAsString(), "$[0].clases");
        assertEquals(1.0, clases,
                "solo la asistencia PRESENTE de clase1 cuenta: clase2 esta eliminada, la asistencia de "
                        + "clase3 esta eliminada, y la de clase4 esta AUSENTE");
    }
}
