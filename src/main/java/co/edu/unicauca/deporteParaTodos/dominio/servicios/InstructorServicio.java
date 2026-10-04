package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInstructorGateway;
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

    @Autowired
    private IInstructorGateway instructorsGateway;

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

    @Override
    public Instructor actualizarInstructor(String instructorId, Instructor datosInstructor) {
        if (!instructorsGateway.existeInstructor(instructorId)) {
            throw new NoExisteExcepcion("No exoste el instructor con el identificador " + instructorId);
        }
        return instructorsGateway.actualizarInstructor(instructorId, datosInstructor);
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
