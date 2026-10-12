package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IImagenServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ImagenDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;

/**
 * PUT /api/v2/instructor/{id} (test de integracion, BD MySQL real). SCRUM-79
 * (HE-14 HU-01): edicion de nombre/correo de un instructor, reutilizando
 * PerfilGateway.actualizarPerfil (SCRUM-182: saveAndFlush + 409 por correo
 * duplicado) e IInstructorGateway.existeInstructorActivo (SCRUM-78) para el 404.
 *
 * Identificadores y correos UNICOS por corrida (sufijo aleatorio) + limpieza en
 * @BeforeAll/@AfterAll (mismo patron ya validado en GrupoInstructorIT/
 * InscripcionVentanaFechasIT) para poder ejecutarse dos veces seguidas sin
 * colisionar contra una BD persistente (SCRUM-180). No se inyecta Clock: este
 * flujo no construye ninguna fecha (el modelo Perfil no tiene campos de fecha),
 * asi que la regla "usa el Clock del contexto" no tiene nada a que aplicarse aqui.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class InstructorActualizarIT {

    private static final String PREFIJO_PERFIL = "instructoractualizarit-%";
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";

    @Autowired
    private IInstructorServicio instructorServicio;

    @Autowired
    private IPerfilGateway perfilGateway;

    @Autowired
    private IImagenServicio imagenServicio;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String instructorAId;
    private String instructorBId;
    private String instructorEliminadoId;
    private String soloAlumnoId;
    private String correoOriginalA;
    private String correoB;
    private Integer imagenIdA;

    @BeforeAll
    void crearFixtures() {
        limpiarRestosDeCorridasAnteriores();

        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        instructorAId = "instructoractualizarit-a-" + sufijo;
        instructorBId = "instructoractualizarit-b-" + sufijo;
        instructorEliminadoId = "instructoractualizarit-elim-" + sufijo;
        soloAlumnoId = "instructoractualizarit-alumno-" + sufijo;
        correoOriginalA = instructorAId + "@unicauca.edu.co";
        correoB = instructorBId + "@unicauca.edu.co";

        crearInstructor(instructorAId, "Prof. A Original", correoOriginalA);
        crearInstructor(instructorBId, "Prof. B", correoB);
        crearInstructor(instructorEliminadoId, "Prof. Eliminado", instructorEliminadoId + "@unicauca.edu.co");
        instructorServicio.eliminarInstructor(instructorEliminadoId);

        // Verificacion SCRUM-79/139: instructor A necesita sexo/tipoId/imagen NO
        // nulos para poder comprobar que PUT /instructor/{id} (solo nombre+correo)
        // los preserva. registrarInstructor()/PerfilDto no tienen campo imagen, asi
        // que se fija aparte con actualizarPerfil() -- sexo="M" y tipoId="CC" ya
        // quedaron fijados por crearInstructor().
        ImagenDto imagenDto = new ImagenDto();
        imagenDto.setNombre("imagen-instructoractualizarit.png");
        imagenDto.setTipoArchivo("image/png");
        imagenDto.setDatosBase64(Base64.getEncoder().encodeToString(new byte[] {8, 8, 8}));
        imagenIdA = imagenServicio.insertarImagen(imagenDto).getId();
        Perfil perfilConImagen = new Perfil();
        perfilConImagen.setNombre("Prof. A Original");
        perfilConImagen.setCorreo(correoOriginalA);
        perfilConImagen.setImagen(imagenIdA);
        perfilGateway.actualizarPerfil(instructorAId, perfilConImagen);

        Perfil soloAlumno = new Perfil();
        soloAlumno.setId(soloAlumnoId);
        soloAlumno.setNombre("Alumno Puro");
        soloAlumno.setCorreo(soloAlumnoId + "@unicauca.edu.co");
        soloAlumno.setTipoId("CC");
        soloAlumno.setSexo("M");
        soloAlumno.setTipoAlumno("Estudiante");
        perfilGateway.registrarAlumno(soloAlumno);
    }

    @AfterAll
    void limpiarAlTerminar() {
        limpiarRestosDeCorridasAnteriores();
    }

    // Orden de FK: instructor -> alumno -> perfil. NO toca ningun otro perfil/seed.
    private void limpiarRestosDeCorridasAnteriores() {
        jdbc.update("DELETE FROM tbl_instructor WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_alumno WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_perfil WHERE perf_id LIKE ?", PREFIJO_PERFIL);
    }

    private void crearInstructor(String id, String nombre, String correo) {
        PerfilDto dto = new PerfilDto();
        dto.setId(id);
        dto.setNombre(nombre);
        dto.setCorreo(correo);
        dto.setTipoId("CC");
        dto.setSexo("M");
        dto.setTipoAlumno("Docente");
        instructorServicio.registrarInstructor(dto);
    }

    private String buildJwt(String rol, String perfId) {
        byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(new OctetSequenceKey.Builder(key).build()));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(perfId + "@unicauca.edu.co")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("rol", rol)
                .claim("perf_id", perfId)
                .build();
        return new NimbusJwtEncoder(source)
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private String[] leerNombreYCorreo(String perfilId) {
        return jdbc.queryForObject(
                "SELECT PERF_NOMBRE, PERF_CORREO FROM tbl_perfil WHERE PERF_ID = ?",
                (rs, i) -> new String[] { rs.getString(1), rs.getString(2) },
                perfilId);
    }

    // [0]=PERF_SEXO [1]=PERF_TIPOID [2]=PERF_IMAGEN [3]=META_ELIMINADO
    private String[] leerSexoTipoIdImagenEliminado(String perfilId) {
        return jdbc.queryForObject(
                "SELECT PERF_SEXO, PERF_TIPOID, PERF_IMAGEN, META_ELIMINADO FROM tbl_perfil WHERE PERF_ID = ?",
                (rs, i) -> new String[] { rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4) },
                perfilId);
    }

    @Test
    void putInstructor_http_editaNombreYCorreo_persisteEnTblPerfilYVisibleEnGetInstructores() throws Exception {
        String nombreNuevo = "Prof. A Actualizado";
        String correoNuevo = instructorAId + "-nuevo@unicauca.edu.co";
        String body = "{\"nombre\":\"" + nombreNuevo + "\",\"correo\":\"" + correoNuevo + "\"}";

        mockMvc.perform(put("/api/v2/instructor/" + instructorAId)
                .contentType("application/json")
                .content(body)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(instructorAId))
                .andExpect(jsonPath("$.nombre").value(nombreNuevo))
                .andExpect(jsonPath("$.correo").value(correoNuevo));

        String[] filaBd = leerNombreYCorreo(instructorAId);
        assertEquals(nombreNuevo, filaBd[0]);
        assertEquals(correoNuevo, filaBd[1]);

        mockMvc.perform(get("/api/v2/instructores")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + instructorAId + "')].correo").value(correoNuevo));

        // Deja el correo de A como estaba para no interferir con otros tests de
        // esta misma clase que asumen correoOriginalA.
        mockMvc.perform(put("/api/v2/instructor/" + instructorAId)
                .contentType("application/json")
                .content("{\"nombre\":\"Prof. A Original\",\"correo\":\"" + correoOriginalA + "\"}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk());
    }

    // Verificacion SCRUM-79/139: PUT /instructor/{id} solo edita nombre/correo --
    // sexo, tipoId (PERF_TIPOID), imagen (PERF_IMAGEN) y META_ELIMINADO deben
    // quedar EXACTAMENTE iguales, tanto en BD (JDBC) como en el cuerpo del 200
    // (sexo, el unico de los 4 que expone InstructorDto). Ademas confirma, sobre
    // la respuesta REAL del servicio (sin mockear actualizarPerfil -- este es el
    // unico test de todo el proyecto que ejercita el PerfilGateway/ModelMapper
    // reales de punta a punta), que id/nombre/correo en el cuerpo son los NUEVOS
    // y sexo es el ORIGINAL (no null) -- justo lo que el hallazgo de ModelMapper
    // (sonda en PerfilGatewayTest) pondria en riesgo si InstructorServicio
    // todavia confiara en el valor de retorno de actualizarPerfil().
    @Test
    void putInstructor_http_editaNombreYCorreo_preservaSexoTipoIdImagenYMetaEliminado() throws Exception {
        String[] filaAntes = leerSexoTipoIdImagenEliminado(instructorAId);

        String nombreNuevo = "Prof. A Editado De Nuevo";
        String correoNuevo = instructorAId + "-otra-edicion@unicauca.edu.co";
        String body = "{\"nombre\":\"" + nombreNuevo + "\",\"correo\":\"" + correoNuevo + "\"}";

        mockMvc.perform(put("/api/v2/instructor/" + instructorAId)
                .contentType("application/json")
                .content(body)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(instructorAId))
                .andExpect(jsonPath("$.nombre").value(nombreNuevo))
                .andExpect(jsonPath("$.correo").value(correoNuevo))
                .andExpect(jsonPath("$.sexo").value(filaAntes[0]))
                .andExpect(jsonPath("$.sexo").isNotEmpty());

        String[] filaDespues = leerSexoTipoIdImagenEliminado(instructorAId);
        assertEquals(filaAntes[0], filaDespues[0], "PERF_SEXO no debe cambiar");
        assertEquals(filaAntes[1], filaDespues[1], "PERF_TIPOID no debe cambiar");
        assertEquals(filaAntes[2], filaDespues[2], "PERF_IMAGEN no debe cambiar");
        assertEquals(filaAntes[3], filaDespues[3], "META_ELIMINADO no debe cambiar");

        // Revertir correo para no interferir con los demas tests de esta clase.
        mockMvc.perform(put("/api/v2/instructor/" + instructorAId)
                .contentType("application/json")
                .content("{\"nombre\":\"Prof. A Original\",\"correo\":\"" + correoOriginalA + "\"}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk());
    }

    @Test
    void putInstructor_http_correoDeAAlDeB_retorna409YLaFilaDeAQuedaIntacta() throws Exception {
        String body = "{\"nombre\":\"Prof. A Intentando Robar Correo\",\"correo\":\"" + correoB + "\"}";

        mockMvc.perform(put("/api/v2/instructor/" + instructorAId)
                .contentType("application/json")
                .content(body)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("ya esta en uso por otro perfil")));

        String[] filaBd = leerNombreYCorreo(instructorAId);
        assertEquals("Prof. A Original", filaBd[0]);
        assertEquals(correoOriginalA, filaBd[1]);
    }

    @Test
    void putInstructor_http_perfilQueSoloEsAlumno_retorna404() throws Exception {
        mockMvc.perform(put("/api/v2/instructor/" + soloAlumnoId)
                .contentType("application/json")
                .content("{\"nombre\":\"Intento\",\"correo\":\"intento@unicauca.edu.co\"}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isNotFound());
    }

    @Test
    void putInstructor_http_instructorEliminado_retorna404() throws Exception {
        mockMvc.perform(put("/api/v2/instructor/" + instructorEliminadoId)
                .contentType("application/json")
                .content("{\"nombre\":\"Intento\",\"correo\":\"intento2@unicauca.edu.co\"}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isNotFound());
    }
}
