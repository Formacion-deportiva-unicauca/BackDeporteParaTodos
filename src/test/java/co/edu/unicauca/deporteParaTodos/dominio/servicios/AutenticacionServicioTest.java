package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProgramaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DatosInvalidosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Facultad;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Programa;
import org.mockito.ArgumentCaptor;
import java.util.Optional;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacionServicioTest {

    @Mock
    private IPerfilGateway gatePerfil;

    @Mock
    private IProgramaGateway programaGateway;

    @InjectMocks
    private AutenticacionServicio autenticacionServicio;

    private Perfil perfilBase() {
        Perfil p = new Perfil();
        p.setId("12345678");
        p.setNombre("Juan García");
        p.setCorreo("juan@unicauca.edu.co");
        p.setTipoId("CC");
        p.setSexo("M");
        p.setRol("ALUMNO");
        p.setFacultad("Ingeniería");
        p.setTipoAlumno("Estudiante");
        return p;
    }

    private PerfilDto dtoBase() {
        PerfilDto d = new PerfilDto();
        d.setId("12345678");
        d.setNombre("Juan García");
        d.setCorreo("juan@unicauca.edu.co");
        d.setTipoId("CC");
        d.setSexo("M");
        d.setRole("ALUMNO");
        d.setFacultad("Facultad de Ingeniería Civil");
        d.setTipoAlumno("Estudiante");
        d.setAlumnoCodigo("104612");
        d.setPrograma("Ingeniería Civil");
        return d;
    }

    @Test
    void login_emailExistente_retornaPerfilDto() {
        when(gatePerfil.obtenerUsuario("juan@unicauca.edu.co")).thenReturn(perfilBase());

        PerfilDto resultado = autenticacionServicio.login("juan@unicauca.edu.co");

        assertNotNull(resultado);
        assertEquals("12345678", resultado.getId());
        assertEquals("ALUMNO",   resultado.getRole());
        verify(gatePerfil).obtenerUsuario("juan@unicauca.edu.co");
    }

    @Test
    void login_emailNoExistente_lanzaNoExisteExcepcion() {
        when(gatePerfil.obtenerUsuario(anyString())).thenReturn(null);

        assertThrows(NoExisteExcepcion.class,
                () -> autenticacionServicio.login("desconocido@externo.com"));
    }

    @Test
    void registrarAlumno_perfilNoExistente_retornaPerfilDto() {
        when(programaGateway.obtenerPrograma("Ingeniería Civil"))
                .thenReturn(Optional.of(new Programa("Ingeniería Civil", new Facultad("Facultad de Ingeniería Civil"))));
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        when(gatePerfil.registrarAlumno(any(Perfil.class))).thenReturn(perfilBase());

        PerfilDto resultado = autenticacionServicio.registrarAlumno(dtoBase());

        assertNotNull(resultado);
        assertEquals("12345678", resultado.getId());
        assertEquals("ALUMNO",   resultado.getRole());
        verify(gatePerfil).registrarAlumno(any(Perfil.class));
    }

    @Test
    void registrarAlumno_perfilYaExistente_lanzaYaExisteElementoExcepcion() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(true);

        assertThrows(YaExisteElementoExcepcion.class,
                () -> autenticacionServicio.registrarAlumno(dtoBase()));

        verify(gatePerfil, never()).registrarAlumno(any());
    }

    // ── Reglas del perfil según el tipo de vínculo ─────────────────────────────

    private static final String FACULTAD = "Facultad de Ingeniería Civil";
    private static final String PROGRAMA = "Ingeniería Civil";

    private void programaExiste() {
        when(programaGateway.obtenerPrograma(PROGRAMA))
                .thenReturn(Optional.of(new Programa(PROGRAMA, new Facultad(FACULTAD))));
    }

    private PerfilDto dtoDocente() {
        PerfilDto d = dtoBase();
        d.setTipoAlumno("Docente");
        d.setAlumnoCodigo(null);
        return d;
    }

    private PerfilDto dtoFuncionarioConDependencia(String tipo) {
        PerfilDto d = dtoBase();
        d.setTipoAlumno(tipo);
        d.setAlumnoCodigo(null);
        d.setFacultad(null);
        d.setPrograma(null);
        d.setDependencia("Oficina de Planeación");
        return d;
    }

    private Perfil perfilEnviadoAlGateway() {
        ArgumentCaptor<Perfil> captor = ArgumentCaptor.forClass(Perfil.class);
        verify(gatePerfil).registrarAlumno(captor.capture());
        return captor.getValue();
    }

    private void assertRechazado(PerfilDto dto) {
        assertThrows(DatosInvalidosExcepcion.class, () -> autenticacionServicio.registrarAlumno(dto));
        verify(gatePerfil, never()).registrarAlumno(any());
    }

    @Test
    void registrarAlumno_estudianteValido_enviaCodigoYProgramaSinDependencia() {
        programaExiste();
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        when(gatePerfil.registrarAlumno(any(Perfil.class))).thenReturn(perfilBase());
        PerfilDto dto = dtoBase();
        dto.setDependencia("Dato que no corresponde");

        autenticacionServicio.registrarAlumno(dto);

        Perfil enviado = perfilEnviadoAlGateway();
        assertEquals("104612", enviado.getAlumnoCodigo());
        assertEquals(PROGRAMA, enviado.getPrograma());
        assertNull(enviado.getDependencia());
    }

    @Test
    void registrarAlumno_estudianteSinCodigo_esRechazado() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        PerfilDto dto = dtoBase();
        dto.setAlumnoCodigo("  ");

        assertRechazado(dto);
    }

    @Test
    void registrarAlumno_estudianteSinPrograma_esRechazado() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        PerfilDto dto = dtoBase();
        dto.setPrograma(null);

        assertRechazado(dto);
    }

    @Test
    void registrarAlumno_estudianteSinFacultad_esRechazado() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        PerfilDto dto = dtoBase();
        dto.setFacultad("");

        assertRechazado(dto);
    }

    @Test
    void registrarAlumno_programaInexistente_esRechazado() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        when(programaGateway.obtenerPrograma(PROGRAMA)).thenReturn(Optional.empty());

        assertRechazado(dtoBase());
    }

    @Test
    void registrarAlumno_programaDeOtraFacultad_esRechazado() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        programaExiste();
        PerfilDto dto = dtoBase();
        dto.setFacultad("Facultad de Artes");

        assertRechazado(dto);
    }

    @Test
    void registrarAlumno_docenteValido_noPideCodigoYDescartaElQueVenga() {
        programaExiste();
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        when(gatePerfil.registrarAlumno(any(Perfil.class))).thenReturn(perfilBase());
        PerfilDto dto = dtoDocente();
        dto.setAlumnoCodigo("999");

        autenticacionServicio.registrarAlumno(dto);

        Perfil enviado = perfilEnviadoAlGateway();
        assertEquals("Docente", enviado.getTipoAlumno());
        assertNull(enviado.getAlumnoCodigo());
        assertEquals(PROGRAMA, enviado.getPrograma());
    }

    @Test
    void registrarAlumno_docenteSinPrograma_esRechazado() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        PerfilDto dto = dtoDocente();
        dto.setPrograma(null);

        assertRechazado(dto);
    }

    @Test
    void registrarAlumno_docenteSinFacultad_esRechazado() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        PerfilDto dto = dtoDocente();
        dto.setFacultad(null);

        assertRechazado(dto);
    }

    @Test
    void registrarAlumno_administrativoValido_soloDependenciaSinCodigoNiPrograma() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        when(gatePerfil.registrarAlumno(any(Perfil.class))).thenReturn(perfilBase());
        PerfilDto dto = dtoFuncionarioConDependencia("Administrativo");
        dto.setAlumnoCodigo("123");
        dto.setPrograma(PROGRAMA);

        autenticacionServicio.registrarAlumno(dto);

        Perfil enviado = perfilEnviadoAlGateway();
        assertEquals("Oficina de Planeación", enviado.getDependencia());
        assertNull(enviado.getAlumnoCodigo());
        assertNull(enviado.getPrograma());
        verify(programaGateway, never()).obtenerPrograma(anyString());
    }

    @Test
    void registrarAlumno_contratistaValido_soloDependencia() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        when(gatePerfil.registrarAlumno(any(Perfil.class))).thenReturn(perfilBase());

        autenticacionServicio.registrarAlumno(dtoFuncionarioConDependencia("Contratista"));

        Perfil enviado = perfilEnviadoAlGateway();
        assertEquals("Contratista", enviado.getTipoAlumno());
        assertEquals("Oficina de Planeación", enviado.getDependencia());
    }

    @Test
    void registrarAlumno_administrativoSinDependencia_esRechazado() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        PerfilDto dto = dtoFuncionarioConDependencia("Administrativo");
        dto.setDependencia(" ");

        assertRechazado(dto);
    }

    @Test
    void registrarAlumno_contratistaSinDependencia_esRechazado() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        PerfilDto dto = dtoFuncionarioConDependencia("Contratista");
        dto.setDependencia(null);

        assertRechazado(dto);
    }

    @Test
    void registrarAlumno_tipoDesconocido_esRechazado() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        PerfilDto dto = dtoBase();
        dto.setTipoAlumno("Regular");

        assertRechazado(dto);
    }

    @Test
    void registrarAlumno_tipoNulo_esRechazado() {
        when(gatePerfil.existePerfil(anyString())).thenReturn(false);
        PerfilDto dto = dtoBase();
        dto.setTipoAlumno(null);

        assertRechazado(dto);
    }
}
