package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IReporteGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DatosInvalidosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ConsultaReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.FilaCrudaReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.OpcionesReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ReporteMensual;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ReporteMensual.Fila;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ReporteMensual.PorVinculo;

@ExtendWith(MockitoExtension.class)
class ReporteServicioTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Mock
    private IReporteGateway gateway;

    private ReporteServicio servicio;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(LocalDate.of(2026, 11, 19).atStartOfDay(BOGOTA).toInstant(), BOGOTA);
        servicio = new ReporteServicio(gateway, reloj);
        lenient().when(gateway.inscritos(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        lenient().when(gateway.atenciones(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
    }

    private static FilaCrudaReporte fila(String categoria, String curso, String vinculo, String sexo, long total, long sinInscripcion) {
        return new FilaCrudaReporte(categoria, curso, "Voleibol", "MIXTO", vinculo, sexo, total, sinInscripcion);
    }

    private ReporteMensual reporte() {
        return servicio.mensual(2026, 10, null, null, null, null, null, null, false);
    }

    // ── Normalizacion de modalidad ────────────────────────────────────────────

    @Test
    void normalizarModalidad_recortaYCapitaliza() {
        assertEquals("Seleccionado", ReporteServicio.normalizarModalidad("seleccionado "));
        assertEquals("Recreativo", ReporteServicio.normalizarModalidad("RECREATIVO"));
        assertEquals("Semillero deportivo", ReporteServicio.normalizarModalidad("  semillero    DEPORTIVO "));
        assertEquals("Sin modalidad", ReporteServicio.normalizarModalidad(null));
        assertEquals("Sin modalidad", ReporteServicio.normalizarModalidad("   "));
    }

    @Test
    void dosCategoriasQueSoloDifierenPorEspaciosOMayusculas_seUnenEnUnaFila() {
        when(gateway.atenciones(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                fila("seleccionado ", "Voleibol", "Estudiante", "M", 3, 0),
                fila("Seleccionado", "Voleibol", "Estudiante", "F", 2, 0)));

        List<Fila> filas = reporte().filas();

        assertEquals(1, filas.size());
        assertEquals("Seleccionado", filas.get(0).modalidad());
        assertEquals(3, filas.get(0).atencionesHombres());
        assertEquals(2, filas.get(0).atencionesMujeres());
    }

    // ── Conciliacion ──────────────────────────────────────────────────────────

    @Test
    void conciliacion_totalesSumaDeFilasYDistribucionPorVinculoIgualAlTotal() {
        when(gateway.inscritos(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                fila("Recreativo", "Natacion", "Estudiante", "M", 4, 0), fila("Recreativo", "Natacion", "Estudiante", "F", 6, 0),
                fila("Recreativo", "Natacion", "Docente", "F", 2, 0), fila("Semillero", "Futbol", "Contratista", "M", 1, 0)));
        when(gateway.atenciones(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                fila("Recreativo", "Natacion", "Estudiante", "M", 10, 1), fila("Recreativo", "Natacion", "Docente", "F", 5, 0),
                fila("Semillero", "Futbol", "Administrativo", "M", 3, 2), fila("Semillero", "Futbol", "Estudiante", "F", 7, 0)));
        when(gateway.personasDistintasInscritas(org.mockito.ArgumentMatchers.any())).thenReturn(11L);
        when(gateway.personasDistintasAtendidas(org.mockito.ArgumentMatchers.any())).thenReturn(9L);

        ReporteMensual r = reporte();

        assertEquals(2, r.filas().size());
        long inscritos = r.filas().stream().mapToLong(Fila::inscritosTotal).sum();
        long atenciones = r.filas().stream().mapToLong(Fila::atencionesTotal).sum();
        assertEquals(13, inscritos);
        assertEquals(25, atenciones);
        assertEquals(inscritos, r.totales().inscritosTotal());
        assertEquals(atenciones, r.totales().atencionesTotal());
        assertEquals(r.totales().inscritosHombres() + r.totales().inscritosMujeres(), r.totales().inscritosTotal());
        assertEquals(r.totales().atencionesHombres() + r.totales().atencionesMujeres(), r.totales().atencionesTotal());
        assertEquals(inscritos, r.porVinculo().stream().mapToLong(PorVinculo::inscritos).sum());
        assertEquals(atenciones, r.porVinculo().stream().mapToLong(PorVinculo::atenciones).sum());
        // Las personas distintas vienen de la consulta separada: no son la suma de nada.
        assertEquals(11, r.totales().personasDistintasInscritas());
        assertEquals(9, r.totales().personasDistintasAtendidas());
        // Orden estable: modalidad y luego curso.
        assertEquals("Recreativo", r.filas().get(0).modalidad());
        assertEquals("Semillero", r.filas().get(1).modalidad());
    }

    @Test
    void atencionesSinInscripcion_seCuentanEnLasFilasYSeInformanAparte() {
        when(gateway.atenciones(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                fila("Recreativo", "Natacion", "Estudiante", "M", 10, 1), fila("Semillero", "Futbol", "Estudiante", "F", 4, 3)));
        when(gateway.clasesSinConfirmar(org.mockito.ArgumentMatchers.any())).thenReturn(5L);

        ReporteMensual r = reporte();

        assertEquals(14, r.totales().atencionesTotal(), "las atenciones sin inscripcion TAMBIEN cuentan");
        assertEquals(4, r.calidad().atencionesSinInscripcion());
        assertEquals(5, r.calidad().clasesSinConfirmar());
    }

    @Test
    void mesSinDatos_devuelveCerosYLosCuatroVinculos_sinError() {
        ReporteMensual r = reporte();

        assertTrue(r.filas().isEmpty());
        assertEquals(0, r.totales().atencionesTotal());
        assertEquals(0, r.totales().inscritosTotal());
        assertEquals(0, r.calidad().atencionesSinInscripcion());
        assertEquals(List.of("Estudiante", "Docente", "Administrativo", "Contratista"),
                r.porVinculo().stream().map(PorVinculo::vinculo).toList());
        assertTrue(r.porVinculo().stream().allMatch(v -> v.inscritos() == 0 && v.atenciones() == 0));
    }

    @Test
    void ausentes_noSonAtenciones_porqueLaConsultaSoloTraePresentes() {
        // El gateway devuelve solo PRESENTE (el filtro ASI_ESTADO esta en el SQL, cubierto por la prueba de integracion);
        // aqui se confirma que el servicio no inventa atenciones donde no hay filas.
        when(gateway.inscritos(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(fila("Recreativo", "Natacion", "Estudiante", "M", 5, 0)));

        ReporteMensual r = reporte();

        assertEquals(5, r.totales().inscritosTotal());
        assertEquals(0, r.totales().atencionesTotal());
        assertEquals(0, r.filas().get(0).atencionesTotal());
    }

    // ── Vinculo y Funcionarios ────────────────────────────────────────────────

    @Test
    void filtroFuncionarios_seExpandeADocenteAdministrativoYContratista() {
        servicio.mensual(2026, 10, null, null, null, List.of("Funcionarios"), null, null, false);

        ArgumentCaptor<ConsultaReporte> captor = ArgumentCaptor.forClass(ConsultaReporte.class);
        verify(gateway).atenciones(captor.capture());
        assertEquals(List.of("Docente", "Administrativo", "Contratista"), captor.getValue().vinculos());
    }

    @Test
    void sinFiltroDeVinculo_seConsultanLosCuatro_yElOrdenPedidoNoImporta() {
        servicio.mensual(2026, 10, null, null, null, null, null, null, false);
        servicio.mensual(2026, 10, null, null, null, List.of("contratista", "ESTUDIANTE"), null, null, false);

        ArgumentCaptor<ConsultaReporte> captor = ArgumentCaptor.forClass(ConsultaReporte.class);
        verify(gateway, org.mockito.Mockito.times(2)).atenciones(captor.capture());
        assertEquals(List.of("Estudiante", "Docente", "Administrativo", "Contratista"), captor.getAllValues().get(0).vinculos());
        assertEquals(List.of("Estudiante", "Contratista"), captor.getAllValues().get(1).vinculos());
    }

    @Test
    void agruparFuncionarios_unePorVinculoPeroNoCambiaLosTotales() {
        when(gateway.atenciones(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                fila("Recreativo", "Natacion", "Estudiante", "M", 10, 0), fila("Recreativo", "Natacion", "Docente", "F", 5, 0),
                fila("Recreativo", "Natacion", "Administrativo", "M", 3, 0), fila("Recreativo", "Natacion", "Contratista", "M", 2, 0)));

        ReporteMensual agrupado = servicio.mensual(2026, 10, null, null, null, null, null, null, true);
        ReporteMensual normal = reporte();

        assertEquals(List.of("Estudiante", "Funcionarios"), agrupado.porVinculo().stream().map(PorVinculo::vinculo).toList());
        assertEquals(10, agrupado.porVinculo().get(1).atenciones());
        assertEquals(20, agrupado.porVinculo().stream().mapToLong(PorVinculo::atenciones).sum());
        assertEquals(normal.totales(), agrupado.totales());
    }

    // ── Filtros y validaciones ────────────────────────────────────────────────

    @Test
    void filtrosVacios_seTratanComoSinFiltro_yLaModalidadSeNormaliza() {
        servicio.mensual(2026, 10, "  ", " SEMILLERO ", " ", null, "", "", false);

        ArgumentCaptor<ConsultaReporte> captor = ArgumentCaptor.forClass(ConsultaReporte.class);
        verify(gateway).atenciones(captor.capture());
        ConsultaReporte c = captor.getValue();
        assertNull(c.disciplina());
        assertEquals("semillero", c.modalidad());
        assertNull(c.publico());
        assertNull(c.genero());
        assertNull(c.facultad());
    }

    @Test
    void elMesDefineElRango_yDiciembreCierraEnEneroDelAnioSiguiente() {
        servicio.mensual(2026, 12, null, null, null, null, null, null, false);

        ArgumentCaptor<ConsultaReporte> captor = ArgumentCaptor.forClass(ConsultaReporte.class);
        verify(gateway).atenciones(captor.capture());
        assertEquals(LocalDate.of(2026, 12, 1), captor.getValue().inicio());
        assertEquals(LocalDate.of(2027, 1, 1), captor.getValue().finExclusivo());
        assertEquals(LocalDate.of(2026, 11, 19), captor.getValue().hoy());
    }

    @Test
    void parametrosInvalidos_seRechazanConDatosInvalidos() {
        assertThrows(DatosInvalidosExcepcion.class, () -> servicio.mensual(2026, 0, null, null, null, null, null, null, false));
        assertThrows(DatosInvalidosExcepcion.class, () -> servicio.mensual(2026, 13, null, null, null, null, null, null, false));
        assertThrows(DatosInvalidosExcepcion.class, () -> servicio.mensual(1999, 5, null, null, null, null, null, null, false));
        assertThrows(DatosInvalidosExcepcion.class, () -> servicio.mensual(2026, 5, null, null, "TODOS", null, null, null, false));
        assertThrows(DatosInvalidosExcepcion.class, () -> servicio.mensual(2026, 5, null, null, null, null, "X", null, false));
        assertThrows(DatosInvalidosExcepcion.class, () -> servicio.mensual(2026, 5, null, null, null, List.of("Jefe"), null, null, false));
    }

    @Test
    void generoYPublico_seAceptanSinImportarLasMayusculas() {
        servicio.mensual(2026, 10, null, null, "mixto", null, "f", null, false);

        ArgumentCaptor<ConsultaReporte> captor = ArgumentCaptor.forClass(ConsultaReporte.class);
        verify(gateway).atenciones(captor.capture());
        assertEquals("MIXTO", captor.getValue().publico());
        assertEquals("F", captor.getValue().genero());
    }

    // ── Opciones ──────────────────────────────────────────────────────────────

    @Test
    void opciones_normalizaYDeduplicaModalidades_yDaElPrimerMesConDatos() {
        when(gateway.primeraFechaConDatos()).thenReturn(LocalDate.of(2026, 10, 5));
        when(gateway.ultimaFechaConDatos()).thenReturn(LocalDate.of(2026, 11, 12));
        when(gateway.modalidades()).thenReturn(List.of("seleccionado ", "Recreativo", "RECREATIVO", "Semillero"));
        when(gateway.disciplinas()).thenReturn(List.of("Futbol", "Voleibol"));
        when(gateway.facultades()).thenReturn(List.of("Ingenieria"));

        OpcionesReporte o = servicio.opciones();

        assertEquals("2026-10", o.primerMes());
        assertEquals("2026-11", o.ultimoMes());
        assertEquals(List.of("Recreativo", "Seleccionado", "Semillero"), o.modalidades());
        assertEquals(List.of("ESTUDIANTE", "FUNCIONARIO", "MIXTO"), o.publicos());
        assertEquals(List.of("Futbol", "Voleibol"), o.disciplinas());
    }

    @Test
    void opciones_sinDatos_primerMesNulo() {
        when(gateway.primeraFechaConDatos()).thenReturn(null);
        when(gateway.ultimaFechaConDatos()).thenReturn(null);

        OpcionesReporte o = servicio.opciones();

        assertNull(o.primerMes());
        assertNull(o.ultimoMes());
    }

    @Test
    void relojFijo_esElDeBogota() {
        assertEquals(Instant.parse("2026-11-19T05:00:00Z"), LocalDate.of(2026, 11, 19).atStartOfDay(BOGOTA).toInstant());
    }
}
