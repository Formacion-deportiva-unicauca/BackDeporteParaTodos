package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DependenciaFallida;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import org.springframework.dao.DataIntegrityViolationException;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.AlumnoEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.PerfilEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IAlumnoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.ICoordinadorRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IImagenRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IInstructorRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IPerfilRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerfilGatewayTest {

    @Mock
    private IPerfilRepositorio repoPerfil;

    @Mock
    private ICoordinadorRepositorio repoCoordinador;

    @Mock
    private IInstructorRepositorio repoInstructor;

    @Mock
    private IAlumnoRepositorio repoAlumno;

    @Mock
    private IImagenRepositorio repoImagen;

    @Mock
    private ModelMapper mapper;

    @InjectMocks
    private PerfilGateway perfilGateway;

    private static final String  ID       = "12345678";
    private static final String  NOMBRE   = "Juan Test";
    private static final String  CORREO   = "juan@unicauca.edu.co";
    private static final Integer IMAGEN   = 1;
    private static final String  TIPO_ID  = "CC";
    private static final String  SEXO     = "M";
    private static final String  TIPO_ALM = "Regular";
    private static final String  ALM_COD  = "2025-CIEN-001";

    private Perfil perfilBase() {
        Perfil p = new Perfil();
        p.setId(ID);
        p.setNombre(NOMBRE);
        p.setCorreo(CORREO);
        p.setImagen(IMAGEN);
        p.setTipoId(TIPO_ID);
        p.setSexo(SEXO);
        p.setTipoAlumno(TIPO_ALM);
        p.setAlumnoCodigo(ALM_COD);
        return p;
    }

    private PerfilEntidad entidadBase() {
        PerfilEntidad e = new PerfilEntidad();
        e.setPerf_id(ID);
        e.setPerf_nombre(NOMBRE);
        e.setPerfcorreo(CORREO);
        e.setPerf_imagen(IMAGEN);
        e.setPerf_tipo(TIPO_ID);
        e.setPerf_Sexo(SEXO);
        e.setEliminado(0);
        return e;
    }

    // ── insertarPerfil ──────────────────────────────────────────────────────

    @Test
    void insertarPerfil_perfilNoExistente_retornaPerfil() {
        when(repoPerfil.existsById(ID)).thenReturn(false);
        when(repoImagen.existsById(IMAGEN)).thenReturn(true);
        when(repoPerfil.save(any(PerfilEntidad.class))).thenReturn(entidadBase());

        Perfil resultado = perfilGateway.insertarPerfil(perfilBase());

        assertNotNull(resultado);
        assertEquals(ID,     resultado.getId());
        assertEquals(CORREO, resultado.getCorreo());
        verify(repoPerfil).save(any(PerfilEntidad.class));
    }

    @Test
    void insertarPerfil_perfilYaExistente_lanzaYaExisteElementoExcepcion() {
        when(repoPerfil.existsById(ID)).thenReturn(true);

        assertThrows(YaExisteElementoExcepcion.class,
                () -> perfilGateway.insertarPerfil(perfilBase()));

        verify(repoPerfil, never()).save(any());
    }

    @Test
    void insertarPerfil_imagenNoExistente_lanzaDependenciaFallida() {
        when(repoPerfil.existsById(ID)).thenReturn(false);
        when(repoImagen.existsById(IMAGEN)).thenReturn(false);

        assertThrows(DependenciaFallida.class,
                () -> perfilGateway.insertarPerfil(perfilBase()));

        verify(repoPerfil, never()).save(any());
    }

    // ── obtenerUsuario ──────────────────────────────────────────────────────

    @Test
    void obtenerUsuario_emailNoExistente_retornaNull() {
        when(repoPerfil.findByPerfcorreo(anyString())).thenReturn(Optional.empty());

        Perfil resultado = perfilGateway.obtenerUsuario("desconocido@externo.com");

        assertNull(resultado);
    }

    @Test
    void obtenerUsuario_emailExistente_esCoordinador_retornaPerfilConRolAdministrador() {
        when(repoPerfil.findByPerfcorreo(CORREO)).thenReturn(Optional.of(entidadBase()));
        when(repoCoordinador.existsById(ID)).thenReturn(true);

        Perfil resultado = perfilGateway.obtenerUsuario(CORREO);

        assertNotNull(resultado);
        assertEquals(ID,              resultado.getId());
        assertEquals(CORREO,          resultado.getCorreo());
        assertEquals("Coordinador", resultado.getRol());
    }

    @Test
    void obtenerUsuario_emailExistente_esInstructor_retornaPerfilConRolInstructor() {
        when(repoPerfil.findByPerfcorreo(CORREO)).thenReturn(Optional.of(entidadBase()));
        when(repoCoordinador.existsById(ID)).thenReturn(false);
        when(repoInstructor.existsById(ID)).thenReturn(true);

        Perfil resultado = perfilGateway.obtenerUsuario(CORREO);

        assertNotNull(resultado);
        assertEquals("Instructor", resultado.getRol());
    }

    @Test
    void obtenerUsuario_emailExistente_esAlumno_retornaPerfilConRolAlumno() {
        AlumnoEntidad alumno = new AlumnoEntidad();
        alumno.setIdPerfil(ID);
        alumno.setTipoAlumno(TIPO_ALM);

        when(repoPerfil.findByPerfcorreo(CORREO)).thenReturn(Optional.of(entidadBase()));
        when(repoCoordinador.existsById(ID)).thenReturn(false);
        when(repoInstructor.existsById(ID)).thenReturn(false);
        when(repoAlumno.existsById(ID)).thenReturn(true);
        when(repoAlumno.findById(ID)).thenReturn(Optional.of(alumno));
        when(repoAlumno.obtenerFacultadPorPerfilId(ID)).thenReturn("Ingeniería");

        Perfil resultado = perfilGateway.obtenerUsuario(CORREO);

        assertNotNull(resultado);
        assertEquals("Alumno",     resultado.getRol());
        assertEquals(TIPO_ALM,     resultado.getTipoAlumno());
        assertEquals("Ingeniería", resultado.getFacultad());
    }

    @Test
    void obtenerUsuario_emailExistente_sinRol_retornaNull() {
        when(repoPerfil.findByPerfcorreo(CORREO)).thenReturn(Optional.of(entidadBase()));
        when(repoCoordinador.existsById(ID)).thenReturn(false);
        when(repoInstructor.existsById(ID)).thenReturn(false);
        when(repoAlumno.existsById(ID)).thenReturn(false);

        Perfil resultado = perfilGateway.obtenerUsuario(CORREO);

        assertNull(resultado);
    }

    // ── registrarAlumno ─────────────────────────────────────────────────────

    @Test
    void registrarAlumno_perfilYaExistente_lanzaYaExisteElementoExcepcion() {
        when(repoPerfil.existsById(ID)).thenReturn(true);

        assertThrows(YaExisteElementoExcepcion.class,
                () -> perfilGateway.registrarAlumno(perfilBase()));

        verify(repoPerfil, never()).save(any());
    }

    @Test
    void registrarAlumno_correoDuplicado_lanzaYaExisteElementoExcepcion() {
        // Simula: PERF_ID nuevo (pasa la guarda de app), pero PERF_CORREO ya existe
        // → repoPerfil.save() lanza DataIntegrityViolationException por UQ_PERFIL_CORREO
        when(repoPerfil.existsById(ID)).thenReturn(false);
        when(repoPerfil.save(any(PerfilEntidad.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key UQ_PERFIL_CORREO"));

        assertThrows(YaExisteElementoExcepcion.class,
                () -> perfilGateway.registrarAlumno(perfilBase()));

        verify(repoAlumno, never()).save(any());
    }

    @Test
    void registrarAlumno_conAlumnoCodigo_persisteEnAlmCodigo() {
        Perfil perfil = new Perfil();
        perfil.setId(ID);
        perfil.setNombre(NOMBRE);
        perfil.setCorreo(CORREO);
        perfil.setTipoId(TIPO_ID);
        perfil.setSexo(SEXO);
        perfil.setTipoAlumno(TIPO_ALM);
        perfil.setAlumnoCodigo(ALM_COD);

        when(repoPerfil.existsById(ID)).thenReturn(false);
        when(repoPerfil.save(any(PerfilEntidad.class))).thenReturn(new PerfilEntidad());
        when(repoAlumno.save(any(AlumnoEntidad.class))).thenAnswer(inv -> inv.getArgument(0));

        perfilGateway.registrarAlumno(perfil);

        ArgumentCaptor<AlumnoEntidad> captor = ArgumentCaptor.forClass(AlumnoEntidad.class);
        verify(repoAlumno).save(captor.capture());
        assertEquals(ALM_COD, captor.getValue().getAlm_codigo());
    }

    // ── actualizarPerfil (SCRUM-182) ────────────────────────────────────────

    // La violacion de UQ_PERFIL_CORREO solo se dispara en el flush -- por eso el
    // gateway usa saveAndFlush() (no save()) dentro del try/catch. Un save() mockeado
    // "normal" no distinguiria este matiz, asi que el mock se arma sobre
    // saveAndFlush() especificamente.
    @Test
    void actualizarPerfil_correoYaUsadoPorOtroPerfil_lanzaYaExisteElementoExcepcion() {
        when(repoPerfil.existsById(ID)).thenReturn(true);
        when(repoPerfil.findById(ID)).thenReturn(Optional.of(entidadBase()));
        when(repoPerfil.saveAndFlush(any(PerfilEntidad.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key UQ_PERFIL_CORREO"));

        Perfil datosActualizar = new Perfil();
        datosActualizar.setNombre(NOMBRE);
        datosActualizar.setCorreo("correo-de-otro-perfil@unicauca.edu.co");

        assertThrows(YaExisteElementoExcepcion.class,
                () -> perfilGateway.actualizarPerfil(ID, datosActualizar));
    }

    @Test
    void actualizarPerfil_correoSinCambios_actualizaCorrectamente() {
        when(repoPerfil.existsById(ID)).thenReturn(true);
        when(repoPerfil.findById(ID)).thenReturn(Optional.of(entidadBase()));
        when(repoPerfil.saveAndFlush(any(PerfilEntidad.class))).thenReturn(entidadBase());
        when(mapper.map(any(PerfilEntidad.class), eq(Perfil.class))).thenReturn(perfilBase());

        Perfil datosActualizar = new Perfil();
        datosActualizar.setNombre(NOMBRE);
        datosActualizar.setCorreo(CORREO); // el mismo correo que ya tenia -- no debe fallar

        Perfil resultado = perfilGateway.actualizarPerfil(ID, datosActualizar);

        assertNotNull(resultado);
        assertEquals(CORREO, resultado.getCorreo());
    }
}
