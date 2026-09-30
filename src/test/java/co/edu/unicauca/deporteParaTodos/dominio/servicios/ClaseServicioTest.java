package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaseServicioTest {

    @Mock
    private IClaseGateway claseGateway;

    @InjectMocks
    private ClaseServicio claseServicio;

    private static final String  CATEGORIA = "Recreativo";
    private static final String  CURSO     = "Natacion";
    private static final Integer ANIO      = 2025;
    private static final Integer ITERABLE  = 1;

    private Clase claseBase() {
        Clase c = new Clase();
        c.setCodigo(1);
        c.setCategoria(CATEGORIA);
        c.setCurso(CURSO);
        c.setAnio(ANIO);
        c.setIterable(ITERABLE);
        c.setIdInstructor("INS001");
        c.setFecha(LocalDate.of(2025, 1, 15));
        c.setHoras(1);
        c.setMinutos(30);
        c.setEliminado(0);
        return c;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerClasesGrupo
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerClasesGrupo_exitoso_retornaListado() {
        when(claseGateway.obtenerClasesGrupo(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(List.of(claseBase(), claseBase()));

        List<Clase> resultado = claseServicio.obtenerClasesGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);

        assertEquals(2, resultado.size());
        verify(claseGateway).obtenerClasesGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);
    }

    @Test
    void obtenerClasesGrupo_vacio_lanzaListadoVacioExcepcion() {
        when(claseGateway.obtenerClasesGrupo(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(Collections.emptyList());

        assertThrows(ListadoVacioExcepcion.class,
                () -> claseServicio.obtenerClasesGrupo(CATEGORIA, CURSO, ANIO, ITERABLE));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // insertarClase
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void insertarClase_exitoso_delegaAlGateway() {
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        when(claseGateway.insertarClase(any(Clase.class))).thenReturn(claseBase());

        Clase resultado = claseServicio.insertarClase(claseBase());

        assertNotNull(resultado);
        assertEquals(CATEGORIA, resultado.getCategoria());
        verify(claseGateway).insertarClase(any(Clase.class));
    }

    // El Instructor registra asistencia alumno por alumno (un click por alumno, en
    // momentos distintos). Si ya existe una clase de hoy para el grupo, insertarClase()
    // debe REUTILIZARLA -- devolver la existente, no lanzar excepcion ni crear una
    // segunda fila (ver ClaseFechaDuplicadaIT para la prueba real contra MySQL).
    @Test
    void insertarClase_yaExisteClaseEnFecha_retornaClaseExistenteSinCrearOtra() {
        Clase datos = claseBase();
        Clase existente = claseBase();
        existente.setCodigo(99); // codigo distinto: confirma que es la fila YA existente, no una nueva
        when(claseGateway.existeClaseEnFecha(
                datos.getCategoria(), datos.getCurso(), datos.getAnio(), datos.getIterable(), datos.getFecha()))
                .thenReturn(true);
        when(claseGateway.obtenerClaseEnFecha(
                datos.getCategoria(), datos.getCurso(), datos.getAnio(), datos.getIterable(), datos.getFecha()))
                .thenReturn(existente);

        Clase resultado = claseServicio.insertarClase(datos);

        assertEquals(99, resultado.getCodigo());
        verify(claseGateway, never()).insertarClase(any(Clase.class));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // existeClaseEnFecha
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void existeClaseEnFecha_delegaAlGateway() {
        LocalDate fecha = LocalDate.of(2026, 1, 1);
        when(claseGateway.existeClaseEnFecha(CATEGORIA, CURSO, ANIO, ITERABLE, fecha)).thenReturn(true);

        boolean resultado = claseServicio.existeClaseEnFecha(CATEGORIA, CURSO, ANIO, ITERABLE, fecha);

        assertTrue(resultado);
        verify(claseGateway).existeClaseEnFecha(CATEGORIA, CURSO, ANIO, ITERABLE, fecha);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // eliminarClase
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void eliminarClase_exitoso_delegaAlGateway() {
        when(claseGateway.existeClase(1)).thenReturn(true);
        when(claseGateway.eliminarClase(1)).thenReturn(claseBase());

        Clase resultado = claseServicio.eliminarClase(1);

        assertNotNull(resultado);
        verify(claseGateway).eliminarClase(1);
    }

    @Test
    void eliminarClase_noExiste_lanzaNoExisteExcepcion() {
        when(claseGateway.existeClase(99)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class, () -> claseServicio.eliminarClase(99));

        verify(claseGateway, never()).eliminarClase(anyInt());
    }
}
