package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProgramaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DatosInvalidosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Facultad;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Programa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramaServicioTest {

    @Mock
    private IProgramaGateway programaGateway;

    @InjectMocks
    private ProgramaServicio servicio;

    private static final String NOMBRE = "Ingeniería de Sistemas";

    private Programa programaBase() {
        Programa p = new Programa();
        p.setPrg_nombre(NOMBRE);
        Facultad f = new Facultad();
        f.setNombre("Facultad de Ingeniería");
        p.setFacultad(f);
        return p;
    }

    // --- obtenerProgramas ---

    @Test
    void obtenerProgramas_listaConElementos_retornaLista() {
        when(programaGateway.obtenerProgramas()).thenReturn(List.of(programaBase()));

        List<Programa> resultado = servicio.obtenerProgramas();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(NOMBRE, resultado.get(0).getPrg_nombre());
    }

    @Test
    void obtenerProgramas_listaVacia_lanzaListadoVacioExcepcion() {
        when(programaGateway.obtenerProgramas()).thenReturn(List.of());
        assertThrows(ListadoVacioExcepcion.class, () -> servicio.obtenerProgramas());
    }

    // --- obtenerPrograma ---

    @Test
    void obtenerPrograma_existe_retornaPrograma() {
        when(programaGateway.obtenerPrograma(NOMBRE)).thenReturn(Optional.of(programaBase()));

        Programa resultado = servicio.obtenerPrograma(NOMBRE);

        assertNotNull(resultado);
        assertEquals(NOMBRE, resultado.getPrg_nombre());
    }

    @Test
    void obtenerPrograma_noExiste_lanzaNoExisteExcepcion() {
        when(programaGateway.obtenerPrograma(NOMBRE)).thenReturn(Optional.empty());
        assertThrows(NoExisteExcepcion.class, () -> servicio.obtenerPrograma(NOMBRE));
    }

    // --- insertarPrograma ---

    @Test
    void insertarPrograma_noExiste_retornaPrograma() {
        when(programaGateway.existePrograma(NOMBRE)).thenReturn(false);
        when(programaGateway.insertarPrograma(any(Programa.class))).thenReturn(programaBase());

        Programa resultado = servicio.insertarPrograma(programaBase());

        assertNotNull(resultado);
        assertEquals(NOMBRE, resultado.getPrg_nombre());
        verify(programaGateway).insertarPrograma(any(Programa.class));
    }

    @Test
    void insertarPrograma_yaExiste_lanzaYaExisteElementoExcepcion() {
        when(programaGateway.existePrograma(NOMBRE)).thenReturn(true);
        assertThrows(YaExisteElementoExcepcion.class, () -> servicio.insertarPrograma(programaBase()));
    }

    // --- actualizarPrograma ---

    @Test
    void actualizarPrograma_lanzaUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class,
                () -> servicio.actualizarPrograma(programaBase()));
    }

    // --- eliminarPrograma ---

    @Test
    void eliminarPrograma_existe_retornaPrograma() {
        when(programaGateway.existePrograma(NOMBRE)).thenReturn(true);
        when(programaGateway.eliminarPrograma(NOMBRE)).thenReturn(programaBase());

        Programa resultado = servicio.eliminarPrograma(NOMBRE);

        assertNotNull(resultado);
        assertEquals(NOMBRE, resultado.getPrg_nombre());
        verify(programaGateway).eliminarPrograma(NOMBRE);
    }

    @Test
    void eliminarPrograma_noExiste_lanzaNoExisteExcepcion() {
        when(programaGateway.existePrograma(NOMBRE)).thenReturn(false);
        assertThrows(NoExisteExcepcion.class, () -> servicio.eliminarPrograma(NOMBRE));
    }

    // --- obtenerProgramasPorFacultad ---

    @Test
    void obtenerProgramasPorFacultad_conProgramas_retornaLista() {
        when(programaGateway.obtenerProgramasPorFacultad("Facultad de Ingeniería"))
                .thenReturn(List.of(programaBase()));

        List<Programa> resultado = servicio.obtenerProgramasPorFacultad("Facultad de Ingeniería");

        assertEquals(1, resultado.size());
        assertEquals(NOMBRE, resultado.get(0).getPrg_nombre());
    }

    @Test
    void obtenerProgramasPorFacultad_sinProgramas_lanzaListadoVacio() {
        when(programaGateway.obtenerProgramasPorFacultad("Facultad de Artes")).thenReturn(List.of());

        assertThrows(ListadoVacioExcepcion.class,
                () -> servicio.obtenerProgramasPorFacultad("Facultad de Artes"));
    }

    @Test
    void obtenerProgramasPorFacultad_facultadEnBlanco_lanzaDatosInvalidos() {
        assertThrows(DatosInvalidosExcepcion.class, () -> servicio.obtenerProgramasPorFacultad("  "));
        assertThrows(DatosInvalidosExcepcion.class, () -> servicio.obtenerProgramasPorFacultad(null));

        verify(programaGateway, never()).obtenerProgramasPorFacultad(any());
    }
}
