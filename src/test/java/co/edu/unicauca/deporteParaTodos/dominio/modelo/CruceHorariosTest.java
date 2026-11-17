package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.CruceHorarios.Cruce;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.CruceHorarios.Franja;

/** Regla pura de cruce: intervalos [inicio, fin), mismo dia, datos sucios ignorados, orden determinista y letra del grupo. */
class CruceHorariosTest {

    private static Grupo destino() {
        Grupo g = new Grupo();
        g.setCategoria("Deportes");
        g.setCurso("Natación");
        g.setAnio(2026);
        g.setIterable(1);
        g.setPeriodo(1);
        return g;
    }

    private static HorarioInscrito inscrito(String curso, int iterable, String dia, String ini, String fin) {
        return new HorarioInscrito("Deportes", curso, 2026, iterable, 1, 1, dia, ini, fin);
    }

    private static Optional<Cruce> cruce(String dia, String ini, String fin, HorarioInscrito... inscritos) {
        List<Franja> destinoFranjas = CruceHorarios.normalizar(dia, ini, fin).map(List::of).orElse(List.of());
        return CruceHorarios.primerCruce(destino(), destinoFranjas, List.of(inscritos));
    }

    // ── Semantica del cruce ───────────────────────────────────────────────────

    @Test
    void mismoDiaYSolapado_esCruce() {
        assertTrue(cruce("MARTES", "10:00", "12:00", inscrito("Yoga", 2, "MARTES", "11:00", "13:00")).isPresent());
        assertTrue(cruce("MARTES", "11:00", "13:00", inscrito("Yoga", 2, "MARTES", "10:00", "12:00")).isPresent());
    }

    @Test
    void bordeExacto_unoTerminaCuandoEmpiezaElOtro_noEsCruce() {
        assertFalse(cruce("MARTES", "10:00", "12:00", inscrito("Yoga", 2, "MARTES", "12:00", "14:00")).isPresent());
        assertFalse(cruce("MARTES", "12:00", "14:00", inscrito("Yoga", 2, "MARTES", "10:00", "12:00")).isPresent());
    }

    @Test
    void unaFranjaDentroDeLaOtra_esCruce() {
        assertTrue(cruce("MARTES", "08:00", "18:00", inscrito("Yoga", 2, "MARTES", "10:00", "11:00")).isPresent());
        assertTrue(cruce("MARTES", "10:00", "11:00", inscrito("Yoga", 2, "MARTES", "08:00", "18:00")).isPresent());
    }

    @Test
    void mismaFranjaExacta_esCruce() {
        assertTrue(cruce("MARTES", "10:00", "12:00", inscrito("Yoga", 2, "MARTES", "10:00", "12:00")).isPresent());
    }

    @Test
    void diasDistintos_noEsCruce() {
        assertFalse(cruce("MARTES", "10:00", "12:00", inscrito("Yoga", 2, "MIERCOLES", "10:00", "12:00")).isPresent());
    }

    @Test
    void elPropioGrupoDestino_noSeCompararConsigoMismo() {
        // Al reactivar la fila del mismo grupo (misma categoria, curso, anio e iterable) sus franjas no cuentan.
        assertFalse(cruce("MARTES", "10:00", "12:00",
                inscrito("Natación", 1, "MARTES", "10:00", "12:00")).isPresent());
        // Otro iterable del mismo curso SI cuenta.
        assertTrue(cruce("MARTES", "10:00", "12:00",
                inscrito("Natación", 2, "MARTES", "10:00", "12:00")).isPresent());
    }

    // ── Normalizacion y datos invalidos (fail-open, sin excepciones) ──────────

    @Test
    void normalizaDiaConTildesMinusculasYEspacios_yHoraSinCeroInicial() {
        assertTrue(cruce("MARTES", "10:00", "12:00", inscrito("Yoga", 2, " martes ", "9:30", "10:30")).isPresent());
        assertTrue(cruce("MIERCOLES", "10:00", "12:00", inscrito("Yoga", 2, "Miércoles", "08:00", "11:00")).isPresent());
        assertTrue(CruceHorarios.normalizar("sábado", "8:00", "9:00").isPresent());
    }

    @Test
    void franjasInvalidas_seIgnoran_sinExcepcion() {
        String[][] basura = {
                {"MARTES", "", "12:00"}, {"MARTES", "10:00", ""}, {"MARTES", null, "12:00"}, {"MARTES", "10:00", null},
                {"MARTES", "diez", "doce"}, {"MARTES", "25:00", "26:00"}, {"MARTES", "10:60", "11:00"},
                {"MARTES", "10:00:00", "12:00:00"}, {"MARTES", "12:00", "10:00"}, {"MARTES", "10:00", "10:00"},
                {"FUNDAY", "10:00", "12:00"}, {null, "10:00", "12:00"}, {"", "10:00", "12:00"},
        };
        for (String[] f : basura) {
            assertFalse(CruceHorarios.normalizar(f[0], f[1], f[2]).isPresent(), String.join("|", String.valueOf(f[0]), f[1] + "", f[2] + ""));
            // Como franja de un grupo donde ya esta inscrito: no bloquea.
            assertFalse(cruce("MARTES", "10:00", "12:00", inscrito("Yoga", 2, f[0], f[1], f[2])).isPresent());
        }
    }

    @Test
    void sinFranjasValidasEnElDestino_nuncaHayCruce() {
        assertFalse(CruceHorarios.primerCruce(destino(), List.of(), List.of(inscrito("Yoga", 2, "MARTES", "10:00", "12:00"))).isPresent());
        assertFalse(CruceHorarios.primerCruce(destino(), List.of(new Franja(1, 600, 720)), null).isPresent());
    }

    @Test
    void franjasValidasDelDestino_descartaLasInvalidas() {
        Horario bueno = horario("MARTES", "10:00", "12:00");
        Horario malo = horario("MARTES", "xx", "12:00");
        assertEquals(1, CruceHorarios.franjasValidas(List.of(bueno, malo)).size());
        assertEquals(0, CruceHorarios.franjasValidas(null).size());
    }

    private static Horario horario(String dia, String ini, String fin) {
        Horario h = new Horario();
        h.setDia(dia);
        h.setHoraInicio(ini);
        h.setHoraFin(fin);
        return h;
    }

    // ── Orden determinista y mensaje ──────────────────────────────────────────

    @Test
    void variosCruces_seInformaElPrimeroEnOrdenDeterminista_sinImportarElOrdenDeEntrada() {
        HorarioInscrito zumba = inscrito("Zumba", 1, "MARTES", "10:30", "11:30");
        HorarioInscrito yogaB = inscrito("Yoga", 2, "MARTES", "11:00", "12:00");
        HorarioInscrito yogaA = inscrito("Yoga", 1, "MARTES", "11:30", "12:30");
        HorarioInscrito yogaAlunes = inscrito("Yoga", 1, "LUNES", "10:00", "12:00");

        Optional<Cruce> a = CruceHorarios.primerCruce(destino(), List.of(new Franja(0, 600, 720), new Franja(1, 600, 720)),
                List.of(zumba, yogaB, yogaA, yogaAlunes));
        Optional<Cruce> b = CruceHorarios.primerCruce(destino(), List.of(new Franja(1, 600, 720), new Franja(0, 600, 720)),
                List.of(yogaAlunes, yogaA, yogaB, zumba));

        // categoria, luego curso (Yoga < Zumba), luego grupo (1 < 2), luego dia (LUNES < MARTES)
        assertEquals("Se cruza con Yoga, grupo A, lunes 10:00-12:00", a.get().detalle());
        assertEquals(a, b);
    }

    @Test
    void mensajeExacto_nombraCursoGrupoDiaYFranja() {
        Cruce c = cruce("MARTES", "10:00", "12:00", inscrito("Natación", 2, "MARTES", "10:00", "12:00")).get();
        assertEquals("Se cruza con Natación, grupo B, martes 10:00-12:00", c.detalle());
        assertEquals("miércoles 08:05-09:00", cruce("MIERCOLES", "08:00", "09:00",
                inscrito("Yoga", 1, "MIERCOLES", "8:05", "9:00")).get().franjaTexto());
    }

    // ── Letra del grupo: misma regla que iterable-letra.util.ts del frontend ──

    @Test
    void letraDeIterable_igualQueElFrontend() {
        // frontend: if (!n || n < 1) return '?'; return String.fromCharCode(64 + n);
        assertEquals("?", CruceHorarios.letraDeIterable(0));
        assertEquals("?", CruceHorarios.letraDeIterable(-1));
        assertEquals("?", CruceHorarios.letraDeIterable(Integer.MIN_VALUE));
        assertEquals("A", CruceHorarios.letraDeIterable(1));
        assertEquals("B", CruceHorarios.letraDeIterable(2));
        assertEquals("Y", CruceHorarios.letraDeIterable(25));
        assertEquals("Z", CruceHorarios.letraDeIterable(26));
        assertEquals("[", CruceHorarios.letraDeIterable(27));   // fromCharCode(91)
        assertEquals("a", CruceHorarios.letraDeIterable(33));   // fromCharCode(97)
        assertEquals("~", CruceHorarios.letraDeIterable(62));   // fromCharCode(126)
    }

    // ── Interruptor: la variable de entorno equivalente ───────────────────────

    @Test
    void laVariableDeEntornoEquivalenteResuelveLaPropiedadDelInterruptor() {
        SystemEnvironmentPropertySource entorno = new SystemEnvironmentPropertySource("entorno",
                java.util.Map.of("INSCRIPCIONES_VALIDAR_CRUCE_HORARIOS", "false"));
        Object valor = entorno.getProperty("inscripciones.validar-cruce-horarios");
        assertNotNull(valor);
        assertEquals("false", valor);
    }
}
