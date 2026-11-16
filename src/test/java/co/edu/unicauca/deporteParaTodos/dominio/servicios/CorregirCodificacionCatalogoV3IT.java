package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;

/**
 * Migracion V3 (db/flyway/V3__corregir_codificacion_catalogo.sql, BD MySQL real).
 *
 * Reproduce el problema de produccion (doble codificacion: UTF-8 leido como
 * Latin-1 y guardado otra vez como UTF-8) partiendo de nombres correctos y
 * reinterpretando sus bytes, inserta filas con esos nombres danados (facultad,
 * programa y la relacion alumno-programa que depende de PRG_NOMBRE), ejecuta
 * el archivo V3 real (no una copia de su logica) y comprueba, por bytes, que
 * queda corregido, que las llaves/referencias siguen consistentes, que 'fiet'
 * se borra, y que una segunda ejecucion no cambia nada.
 *
 * La V3 real ya se aplico (via Flyway, antes de que arranque esta suite) sobre
 * el catalogo sin danar de V2/local -- por eso esta clase no depende de "la
 * base solo hasta V2": fabrica su propia corrupcion sobre el esquema ya migrado y
 * verifica que la MISMA migracion lo corrige igual sobre datos nuevos.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class CorregirCodificacionCatalogoV3IT {

    private static final String RUTA_V3 = "db/flyway/V3__corregir_codificacion_catalogo.sql";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private IPerfilGateway perfilGateway;

    // Codigos Unicode (hex, texto 100% ASCII en este .java) de las dos vocales
    // con tilde que usan los nombres de prueba, para no depender en absoluto de
    // la codificacion con la que se lea o compile este archivo.
    private static final char VOCAL_I_TILDE = (char) 0xED;
    private static final char VOCAL_O_TILDE = (char) 0xF3;

    private String facultadCorrecta;
    private String programaCorrecto;
    private String facultadDanada;
    private String programaDanado;
    private String alumnoId;

    @BeforeAll
    void prepararDatosDanados() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        facultadCorrecta = "Facultad de Ingenier" + VOCAL_I_TILDE + "a de Prueba V3IT " + sufijo;
        programaCorrecto = "Especializaci" + VOCAL_O_TILDE + "n de Prueba V3IT " + sufijo;
        alumnoId = "v3it-" + sufijo;

        facultadDanada = simularDobleCodificacion(facultadCorrecta);
        programaDanado = simularDobleCodificacion(programaCorrecto);

        // 'fiet' puede ya no existir: la V3 real (aplicada por Flyway antes de que
        // arranque esta suite) ya la pudo haber borrado si ningun programa la
        // referenciaba. Se recrea aqui para probar que esta ejecucion tambien la borra.
        jdbc.update("INSERT IGNORE INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, 'fiet')");

        jdbc.update("INSERT INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, ?)", facultadDanada);
        jdbc.update("INSERT INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, ?, ?)",
                programaDanado, facultadDanada);

        Perfil perfil = new Perfil();
        perfil.setId(alumnoId);
        perfil.setNombre("Alumno Prueba V3IT " + sufijo);
        perfil.setCorreo(alumnoId + "@it.unicauca.edu.co");
        perfil.setTipoId("CC");
        perfil.setSexo("M");
        perfil.setTipoAlumno("Estudiante");
        try {
            perfilGateway.registrarAlumno(perfil);
        } catch (YaExisteElementoExcepcion e) {
            // No deberia pasar (id generado con UUID), pero si se reintenta la
            // suite sobre el mismo id, se reutiliza igual que en otros IT.
        }

        jdbc.update("INSERT INTO tbl_intermedia_alumno_programa (META_ELIMINADO, PERF_ID, PRG_NOMBRE) VALUES (0, ?, ?)",
                alumnoId, programaDanado);
    }

    // Toma un nombre correcto, obtiene sus bytes UTF-8 y los reinterpreta como
    // Latin-1 -- exactamente la causa real documentada en produccion. Al guardar
    // este resultado en una columna utf8mb4, el driver JDBC lo vuelve a codificar
    // en UTF-8, reproduciendo el mismo patron de bytes (incluido 0xC3 0x83) que
    // esta migracion corrige.
    private static String simularDobleCodificacion(String correcto) {
        byte[] bytesUtf8 = correcto.getBytes(StandardCharsets.UTF_8);
        return new String(bytesUtf8, StandardCharsets.ISO_8859_1);
    }

    private void ejecutarV3() throws Exception {
        try (Connection con = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(con, new FileSystemResource(RUTA_V3));
        }
    }

    private String hexDeFacultad(String nombre) {
        return jdbc.query("SELECT HEX(FAC_NOMBRE) FROM tbl_facultad WHERE FAC_NOMBRE = ?",
                        (rs, n) -> rs.getString(1), nombre)
                .stream().findFirst().orElse(null);
    }

    private String hexDePrograma(String nombre) {
        return jdbc.query("SELECT HEX(PRG_NOMBRE) FROM tbl_programa WHERE PRG_NOMBRE = ?",
                        (rs, n) -> rs.getString(1), nombre)
                .stream().findFirst().orElse(null);
    }

    private static boolean contieneSecuenciaDanada(String hex) {
        return hex != null && hex.contains("C383");
    }

    @Test
    void v3_corrigeBytesDanados_mantieneReferencias_yEsIdempotente() throws Exception {
        // Precondicion: el helper de simulacion realmente produjo el patron danado
        // (si esto falla, el problema esta en el test, no en la migracion).
        assertTrue(contieneSecuenciaDanada(hexDeFacultad(facultadDanada)),
                "la facultad de prueba deberia quedar mal codificada antes de correr V3");
        assertTrue(contieneSecuenciaDanada(hexDePrograma(programaDanado)),
                "el programa de prueba deberia quedar mal codificado antes de correr V3");

        ejecutarV3();

        // Nombres corregidos -- comprobado por bytes (HEX), no por impresion.
        String hexFacultad = hexDeFacultad(facultadCorrecta);
        assertNotNull(hexFacultad, "la facultad deberia existir ya con el nombre corregido");
        assertFalse(contieneSecuenciaDanada(hexFacultad));

        String hexPrograma = hexDePrograma(programaCorrecto);
        assertNotNull(hexPrograma, "el programa deberia existir ya con el nombre corregido");
        assertFalse(contieneSecuenciaDanada(hexPrograma));

        // La referencia tbl_programa.FAC_NOMBRE tambien quedo corregida.
        assertEquals(facultadCorrecta, jdbc.queryForObject(
                "SELECT FAC_NOMBRE FROM tbl_programa WHERE PRG_NOMBRE = ?", String.class, programaCorrecto));

        // La relacion alumno-programa sigue intacta y apunta al nombre corregido.
        assertEquals(programaCorrecto, jdbc.queryForObject(
                "SELECT PRG_NOMBRE FROM tbl_intermedia_alumno_programa WHERE PERF_ID = ?",
                String.class, alumnoId));

        // Cero huerfanos: ninguna fila de la intermedia referencia un programa
        // que no exista (chequeo global, no solo de los datos de este test).
        Integer huerfanos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_intermedia_alumno_programa i "
                        + "LEFT JOIN tbl_programa p ON i.PRG_NOMBRE = p.PRG_NOMBRE "
                        + "WHERE p.PRG_NOMBRE IS NULL",
                Integer.class);
        assertEquals(0, huerfanos);

        // 'fiet' se borro (ningun programa la referenciaba).
        assertEquals(0, contarFiet());

        // Segunda ejecucion: no debe cambiar nada.
        ejecutarV3();

        assertEquals(facultadCorrecta, jdbc.queryForObject(
                "SELECT FAC_NOMBRE FROM tbl_facultad WHERE FAC_NOMBRE = ?", String.class, facultadCorrecta));
        assertEquals(facultadCorrecta, jdbc.queryForObject(
                "SELECT FAC_NOMBRE FROM tbl_programa WHERE PRG_NOMBRE = ?", String.class, programaCorrecto));
        assertEquals(programaCorrecto, jdbc.queryForObject(
                "SELECT PRG_NOMBRE FROM tbl_intermedia_alumno_programa WHERE PERF_ID = ?",
                String.class, alumnoId));
        assertEquals(0, contarFiet());
    }

    private int contarFiet() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_facultad WHERE FAC_NOMBRE = 'fiet'", Integer.class);
    }

    @AfterAll
    void limpiar() {
        jdbc.update("DELETE FROM tbl_intermedia_alumno_programa WHERE PERF_ID = ?", alumnoId);
        jdbc.update("DELETE FROM tbl_alumno WHERE PERF_ID = ?", alumnoId);
        jdbc.update("DELETE FROM tbl_perfil WHERE PERF_ID = ?", alumnoId);
        jdbc.update("DELETE FROM tbl_programa WHERE PRG_NOMBRE = ?", programaCorrecto);
        jdbc.update("DELETE FROM tbl_facultad WHERE FAC_NOMBRE = ?", facultadCorrecta);
    }
}
