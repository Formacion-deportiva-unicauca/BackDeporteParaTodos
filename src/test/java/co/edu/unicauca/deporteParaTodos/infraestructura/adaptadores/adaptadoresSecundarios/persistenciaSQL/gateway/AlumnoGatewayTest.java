package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IAlumnoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IPerfilRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlumnoGatewayTest {

    @Mock
    private IAlumnoRepositorio repoAlumno;

    @Mock
    private IPerfilRepositorio repoPerfil;

    @Mock
    private ModelMapper mapper;

    @InjectMocks
    private AlumnoGateway alumnoGateway;

    // columnas: [0]=eliminadoestado [1]=id [2]=codigo [3]=tipo
    //           [4]=nombre [5]=correo [6]=sexo [7]=tipoid [8]=imagen
    private Object[] filaAlumnoActivo() {
        return new Object[]{ 0, "12345678", "2025-CIEN-001", "Estudiante",
                "Juan Test", "juan@unicauca.edu.co", "M", "CC", 1 };
    }

    // List.of(Object[]) se resuelve como List<Object> por desempaque de varargs;
    // este helper garantiza List<Object[]> sin ambigüedad.
    private static List<Object[]> unaFila(Object[] fila) {
        List<Object[]> lista = new ArrayList<>();
        lista.add(fila);
        return lista;
    }

    @Test
    void obtenerAlumnosGrupo_llamaRepoConEliminadoCero() {
        when(repoAlumno.buscarAlumnosGrupoRaw("Recreativo", "Natacion", 2025, 1, 0))
                .thenReturn(unaFila(filaAlumnoActivo()));

        alumnoGateway.obtenerAlumnosGrupo("Recreativo", "Natacion", 2025, 1);

        // La query nativa filtra eliminado=0 Y inscr_estado='INSCRITO'; solo alumnos activos e inscritos llegan al gateway.
        verify(repoAlumno).buscarAlumnosGrupoRaw("Recreativo", "Natacion", 2025, 1, 0);
        verify(repoAlumno, never()).buscarAlumnosGrupoRaw(any(), any(), any(), any(), eq(1));
    }

    @Test
    void obtenerAlumnosGrupo_soloAlumnosInscritos_alumnoEnEsperaExcluidoPorFiltroSQL() {
        // La query nativa incluye AND ins.inscr_estado = 'INSCRITO'.
        // Si el único alumno del grupo está EN_ESPERA, el SQL lo filtra y el repo retorna vacío.
        when(repoAlumno.buscarAlumnosGrupoRaw("Recreativo", "Natacion", 2025, 1, 0))
                .thenReturn(Collections.emptyList());

        List<Alumno> resultado = alumnoGateway.obtenerAlumnosGrupo("Recreativo", "Natacion", 2025, 1);

        assertTrue(resultado.isEmpty(),
                "Un alumno EN_ESPERA no debe aparecer en la lista de alumnos del grupo");
    }

    @Test
    void obtenerAlumnosGrupo_mapeaRegistroAAlumnoCorrectamente() {
        when(repoAlumno.buscarAlumnosGrupoRaw("Recreativo", "Natacion", 2025, 1, 0))
                .thenReturn(unaFila(filaAlumnoActivo()));

        List<Alumno> resultado = alumnoGateway.obtenerAlumnosGrupo("Recreativo", "Natacion", 2025, 1);

        assertEquals(1, resultado.size());
        Alumno alumno = resultado.get(0);
        assertEquals("12345678", alumno.getPerfil().getId());
        assertEquals("2025-CIEN-001", alumno.getAlm_codigo());
        assertEquals("Estudiante", alumno.getTipoAlumno());
        assertEquals("Juan Test", alumno.getPerfil().getNombre());
        assertEquals("juan@unicauca.edu.co", alumno.getPerfil().getCorreo());
    }

    @Test
    void obtenerAlumnosGrupo_sinResultados_retornaListaVacia() {
        when(repoAlumno.buscarAlumnosGrupoRaw(any(), any(), any(), any(), eq(0)))
                .thenReturn(Collections.emptyList());

        List<Alumno> resultado = alumnoGateway.obtenerAlumnosGrupo("Recreativo", "Natacion", 2025, 1);

        assertTrue(resultado.isEmpty());
    }

    // HALLAZGO (verificacion SCRUM-79/139, solo documentado -- NO corregido aqui a
    // peticion explicita): actualizarAlumno() construye la respuesta "desde memoria"
    // (comentario en el propio metodo) reusando el Perfil de ENTRADA que PUT
    // /alumnos/{id} arma con SOLO nombre/correo -- sexo/tipoid/imagen NUNCA se
    // setean ahi, asi que llegan null. Tampoco se setean eliminadoestado/alm_codigo
    // en el Alumno de salida. El 200 de PUT /alumnos/{id} pierde esos 5 campos en
    // el cuerpo de la respuesta, aunque la fila en BD (tbl_perfil/tbl_alumno) queda
    // intacta -- mismo tipo de bug que se encontro en InstructorServicio.actualizarInstructor,
    // pero por un mecanismo distinto (aqui no es ModelMapper, es que el objeto de
    // entrada nunca cargo esos campos).
    @Test
    void actualizarAlumno_soloEnviaNombreYCorreo_laRespuestaPierdeSexoTipoidImagenCodigoYEliminadoestado() {
        Perfil perfilDeEntradaSoloNombreYCorreo = new Perfil();
        perfilDeEntradaSoloNombreYCorreo.setNombre("Juan Actualizado");
        perfilDeEntradaSoloNombreYCorreo.setCorreo("juan.nuevo@unicauca.edu.co");
        // sexo/tipoId/imagen quedan null -- asi los construye AlumnoRest.actualizarAlumno().
        Alumno datosAlumno = new Alumno();
        datosAlumno.setTipoAlumno("Estudiante");
        datosAlumno.setPerfil(perfilDeEntradaSoloNombreYCorreo);

        Alumno resultado = alumnoGateway.actualizarAlumno("12345678", datosAlumno);

        assertEquals("Juan Actualizado", resultado.getPerfil().getNombre(), "nombre si se refleja (es lo que se edito)");
        assertEquals("juan.nuevo@unicauca.edu.co", resultado.getPerfil().getCorreo(), "correo si se refleja (es lo que se edito)");
        assertNull(resultado.getPerfil().getSexo(), "HALLAZGO: sexo real en BD se preserva, pero la RESPUESTA lo pierde");
        assertNull(resultado.getPerfil().getTipoId(), "HALLAZGO: tipoId real en BD se preserva, pero la RESPUESTA lo pierde");
        assertNull(resultado.getPerfil().getImagen(), "HALLAZGO: imagen real en BD se preserva, pero la RESPUESTA lo pierde");
        assertNull(resultado.getAlm_codigo(), "HALLAZGO: alm_codigo real en BD se preserva, pero la RESPUESTA lo pierde");
        assertNull(resultado.getEliminadoestado(), "HALLAZGO: eliminadoestado real en BD se preserva, pero la RESPUESTA lo pierde");
    }
}
