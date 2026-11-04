package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Facultad;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Programa;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.FacultadEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ProgramaEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IProgramaRepositorio;
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
class ProgramaGatewayTest {

    @Mock
    private IProgramaRepositorio repoPrograma;

    @InjectMocks
    private ProgramaGateway programaGateway;

    private static final String NOMBRE = "Ingeniería de Sistemas";
    private static final String NOMBRE_FAC = "Facultad de Ingeniería";

    private ProgramaEntidad entidadBase() {
        ProgramaEntidad e = new ProgramaEntidad();
        e.setPrg_nombre(NOMBRE);
        FacultadEntidad fe = new FacultadEntidad();
        fe.setNombre(NOMBRE_FAC);
        e.setFacultad(fe);
        return e;
    }

    private Programa modeloBase() {
        Programa p = new Programa();
        p.setPrg_nombre(NOMBRE);
        Facultad f = new Facultad();
        f.setNombre(NOMBRE_FAC);
        p.setFacultad(f);
        return p;
    }

    // --- existePrograma ---

    @Test
    void existePrograma_existe_retornaTrue() {
        when(repoPrograma.existsById(NOMBRE)).thenReturn(true);
        assertTrue(programaGateway.existePrograma(NOMBRE));
    }

    @Test
    void existePrograma_noExiste_retornaFalse() {
        when(repoPrograma.existsById(NOMBRE)).thenReturn(false);
        assertFalse(programaGateway.existePrograma(NOMBRE));
    }

    // --- obtenerProgramas ---

    @Test
    void obtenerProgramas_retornaListaMapeada() {
        when(repoPrograma.findAll()).thenReturn(List.of(entidadBase()));

        List<Programa> resultado = programaGateway.obtenerProgramas();

        assertEquals(1, resultado.size());
        assertEquals(NOMBRE, resultado.get(0).getPrg_nombre());
        assertEquals(NOMBRE_FAC, resultado.get(0).getFacultad().getNombre());
    }

    @Test
    void obtenerProgramas_listaVacia_retornaVacia() {
        when(repoPrograma.findAll()).thenReturn(List.of());
        assertTrue(programaGateway.obtenerProgramas().isEmpty());
    }

    // --- obtenerPrograma ---

    @Test
    void obtenerPrograma_existe_retornaOptionalConPrograma() {
        when(repoPrograma.findById(NOMBRE)).thenReturn(Optional.of(entidadBase()));

        Optional<Programa> resultado = programaGateway.obtenerPrograma(NOMBRE);

        assertTrue(resultado.isPresent());
        assertEquals(NOMBRE, resultado.get().getPrg_nombre());
    }

    @Test
    void obtenerPrograma_noExiste_retornaOptionalVacio() {
        when(repoPrograma.findById(NOMBRE)).thenReturn(Optional.empty());
        assertTrue(programaGateway.obtenerPrograma(NOMBRE).isEmpty());
    }

    // --- insertarPrograma ---

    @Test
    void insertarPrograma_exitoso_guardaYRetornaMapeado() {
        when(repoPrograma.save(any(ProgramaEntidad.class))).thenReturn(entidadBase());

        Programa resultado = programaGateway.insertarPrograma(modeloBase());

        assertNotNull(resultado);
        assertEquals(NOMBRE, resultado.getPrg_nombre());
        assertEquals(NOMBRE_FAC, resultado.getFacultad().getNombre());
        verify(repoPrograma).save(any(ProgramaEntidad.class));
    }

    // --- actualizarPrograma ---

    @Test
    void actualizarPrograma_lanzaUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class,
                () -> programaGateway.actualizarPrograma(modeloBase()));
    }

    // --- eliminarPrograma ---

    @Test
    void eliminarPrograma_existe_eliminaYRetorna() {
        when(repoPrograma.findById(NOMBRE)).thenReturn(Optional.of(entidadBase()));
        doNothing().when(repoPrograma).delete(any(ProgramaEntidad.class));

        Programa resultado = programaGateway.eliminarPrograma(NOMBRE);

        assertNotNull(resultado);
        assertEquals(NOMBRE, resultado.getPrg_nombre());
        verify(repoPrograma).delete(any(ProgramaEntidad.class));
    }

    @Test
    void eliminarPrograma_noExiste_lanzaNoExisteExcepcion() {
        when(repoPrograma.findById(NOMBRE)).thenReturn(Optional.empty());
        assertThrows(NoExisteExcepcion.class, () -> programaGateway.eliminarPrograma(NOMBRE));
    }

    // --- obtenerProgramasPorFacultad ---

    @Test
    void obtenerProgramasPorFacultad_mapeaLasEntidadesAlModelo() {
        when(repoPrograma.findPorFacultad(NOMBRE_FAC)).thenReturn(List.of(entidadBase()));

        List<Programa> resultado = programaGateway.obtenerProgramasPorFacultad(NOMBRE_FAC);

        assertEquals(1, resultado.size());
        assertEquals(NOMBRE, resultado.get(0).getPrg_nombre());
        assertEquals(NOMBRE_FAC, resultado.get(0).getFacultad().getNombre());
    }

    @Test
    void obtenerProgramasPorFacultad_sinProgramas_retornaListaVacia() {
        when(repoPrograma.findPorFacultad("Facultad de Artes")).thenReturn(List.of());

        assertTrue(programaGateway.obtenerProgramasPorFacultad("Facultad de Artes").isEmpty());
    }
}
