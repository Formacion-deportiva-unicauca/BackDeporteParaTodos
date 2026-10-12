package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInstructorGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InstructorDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ErrorInternoException;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.PerfilMapper;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;

@Service
public class InstructorServicio implements IInstructorServicio {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstructorServicio.class);

    @Autowired
    private IInstructorGateway instructorsGateway;

    @Autowired
    private IPerfilGateway perfilGateway;

    @Override
    public List<InstructorDto> obtenerInstructores() {
        List<Instructor> listInstructors = instructorsGateway.obtenerInstructores();
        if (listInstructors.isEmpty()) {
            throw new ListadoVacioExcepcion("No se encuentran instructores registrados");
        }
        List<InstructorDto> listaDtos = new ArrayList<>();
        listInstructors.forEach(modelo -> {
            InstructorDto dto = InstructorDto.fabricarDeModelo(modelo);
            listaDtos.add(dto);
        });
        return listaDtos;
    }

    @Override
    public Instructor insertarInstructor(Instructor datosInstructor) {
        return null;
    }

    @Override
    public InstructorDto obtenerInstructor(String instructorId) {
        Instructor modelo = instructorsGateway.obtenerInstructor(instructorId).orElseThrow(
                () -> new NoExisteExcepcion("No existe el instructor con el identificador " + instructorId));
        return InstructorDto.fabricarDeModelo(modelo);
    }

    // SCRUM-79 (HE-14 HU-01): reutiliza perfilGateway.actualizarPerfil (SCRUM-182:
    // saveAndFlush + YaExisteElementoExcepcion en 409 por correo duplicado) -- no
    // se duplica ese mecanismo aqui. existeInstructorActivo() ya distingue en una
    // sola consulta "no existe", "existe pero eliminado" y "existe pero no es
    // instructor" (los 3 casos resultan en 404, sin tocar tbl_perfil).
    @Override
    public Instructor actualizarInstructor(String instructorId, Instructor datosInstructor, String idCoordinador) {
        if (!instructorsGateway.existeInstructorActivo(instructorId)) {
            throw new NoExisteExcepcion("No existe el instructor con el identificador " + instructorId);
        }
        Instructor actual = instructorsGateway.obtenerInstructor(instructorId)
                .orElseThrow(() -> new NoExisteExcepcion("No existe el instructor con el identificador " + instructorId));
        String correoActual = actual.getPerfil() != null ? actual.getPerfil().getCorreo() : null;
        String nombreNuevo = datosInstructor.getPerfil().getNombre();
        String correoNuevo = datosInstructor.getPerfil().getCorreo();

        Perfil datosPerfilActualizar = new Perfil();
        datosPerfilActualizar.setNombre(nombreNuevo);
        datosPerfilActualizar.setCorreo(correoNuevo);
        // HALLAZGO (verificacion SCRUM-79/139): el Perfil que RETORNA actualizarPerfil()
        // viene de un mapeo generico de ModelMapper (PerfilEntidad -> Perfil) que pierde
        // nombre/correo/tipoId/imagen -- confirmado con una sonda que mapea una entidad
        // real: solo "sexo" sobrevive el mapeo plano. La fila en BD SI queda correcta
        // (la escritura usa la entidad cargada, no el valor de retorno); el problema es
        // solo el objeto que ESTE metodo devolveria si confiara en el. Por eso se
        // IGNORA el valor de retorno y la respuesta se reconstruye abajo desde `actual`
        // (mapeo manual y confiable de InstructorGateway.mapearEntidadADominio).
        perfilGateway.actualizarPerfil(instructorId, datosPerfilActualizar);

        // Rastro de auditoria: solo si el correo realmente cambia (sin distinguir
        // mayusculas, con trim) -- y solo identificadores en el mensaje, nunca
        // datos personales (correo/nombre).
        if (correoActual != null && correoNuevo != null
                && !correoActual.trim().equalsIgnoreCase(correoNuevo.trim())) {
            LOGGER.info("correo del instructor {} modificado por {}", instructorId, idCoordinador);
        }

        // Respuesta construida desde `actual` (preserva sexo/tipoId/imagen/id reales)
        // con nombre/correo sobreescritos por los valores nuevos -- si actualizarPerfil()
        // no lanzo, ya se sabe que esos dos valores quedaron persistidos tal cual.
        Perfil perfilResultado = actual.getPerfil() != null ? actual.getPerfil() : new Perfil();
        perfilResultado.setId(instructorId);
        perfilResultado.setNombre(nombreNuevo);
        perfilResultado.setCorreo(correoNuevo);

        Instructor resultado = new Instructor();
        resultado.setInst_codigo(instructorId);
        resultado.setPerfil(perfilResultado);
        resultado.setEliminado(actual.getEliminado());
        return resultado;
    }

    @Override
    public Instructor eliminarInstructor(String instructorId) {
        if (!instructorsGateway.existeInstructor(instructorId)) {
            throw new NoExisteExcepcion("No exoste el instructor con el identificador " + instructorId);
        }
        Instructor actual = instructorsGateway.obtenerInstructor(instructorId)
                .orElseThrow(() -> new NoExisteExcepcion("No exoste el instructor con el identificador " + instructorId));
        if (actual.getEliminado() != null && actual.getEliminado() == 1) {
            throw new YaExisteElementoExcepcion("El instructor ya se encuentra eliminado");
        }
        return instructorsGateway.eliminarInstructor(instructorId);
    }

    @Override
    @Transactional
    public InstructorDto registrarInstructor(PerfilDto perfilDto) {
        // Validar que el perfil no exista
        if (instructorsGateway.existeInstructor(perfilDto.getId())) {
            throw new YaExisteElementoExcepcion(
                    "El instructor con la identificación " + perfilDto.getId() + " ya se encuentra registrado");
        }

        // Convertir DTO a modelo de dominio
        Perfil perfil = PerfilMapper.fromDto(perfilDto);

        // Llamar al gateway para registrar el instructor atomicamente
        // (crea perfil + alumno + instructor en una transacción)
        Instructor instructorRegistrado = instructorsGateway.registrarInstructor(perfil, perfilDto.getTipoAlumno());

        if (instructorRegistrado == null) {
            throw new ErrorInternoException("Error al registrar el instructor");
        }

        // Convertir a DTO para respuesta
        InstructorDto respuesta = InstructorDto.fabricarDeModelo(instructorRegistrado);
        if (respuesta == null) {
            throw new ErrorInternoException("Error al convertir el instructor a DTO");
        }

        return respuesta;
    }

}
