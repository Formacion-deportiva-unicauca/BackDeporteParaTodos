package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;

@ExtendWith(MockitoExtension.class)
class PerfilServicioTest {

    @Mock
    private IPerfilGateway perfilGateway;

    @InjectMocks
    private PerfilServicio perfilServicio;

    private static final String ID = "perf1";

    // SonarCloud java:S2789: obtenerPerfil() devuelve Optional<Perfil>, que por
    // contrato de Optional NUNCA es null -- "perfilExistente == null" era codigo
    // muerto que nunca protegia nada. Antes de este fix, un perfil INEXISTENTE
    // (Optional.empty(), no null) pasaba de largo el guard y llegaba a
    // actualizarPerfil() en el gateway en vez de lanzar NoExisteExcepcion.
    @Test
    void actualizarPerfil_perfilInexistente_lanzaNoExisteExcepcionSinLlegarAlGateway() {
        when(perfilGateway.obtenerPerfil(ID)).thenReturn(Optional.empty());

        assertThrows(NoExisteExcepcion.class,
                () -> perfilServicio.actualizarPerfil(ID, new Perfil()));

        verify(perfilGateway, never()).actualizarPerfil(any(), any());
    }

    @Test
    void actualizarPerfil_perfilExistente_delegaAlGateway() {
        Perfil existente = new Perfil();
        existente.setId(ID);
        Perfil actualizado = new Perfil();
        actualizado.setId(ID);
        when(perfilGateway.obtenerPerfil(ID)).thenReturn(Optional.of(existente));
        when(perfilGateway.actualizarPerfil(eq(ID), any(Perfil.class))).thenReturn(actualizado);

        Perfil resultado = perfilServicio.actualizarPerfil(ID, new Perfil());

        assertNotNull(resultado);
        assertEquals(ID, resultado.getId());
        verify(perfilGateway).actualizarPerfil(eq(ID), any(Perfil.class));
    }
}
