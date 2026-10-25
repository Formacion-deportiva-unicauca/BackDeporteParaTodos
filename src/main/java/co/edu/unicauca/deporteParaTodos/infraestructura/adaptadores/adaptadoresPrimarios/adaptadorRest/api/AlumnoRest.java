package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAlumnoServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AlumnoDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AlumnoRequest;
import co.edu.unicauca.deporteParaTodos.infraestructura.logs.PeticionLogger;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("api/v2")
@Validated
public class AlumnoRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlumnoRest.class);

    @Autowired
    private IAlumnoServicio servicioAlumno;

    @Operation(summary = "Obtiene los alumnos inscritos en un grupo especifico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado de alumnos del grupo"),
        @ApiResponse(responseCode = "404", description = "No existen alumnos para el grupo consultado")
    })
    @PreAuthorize("hasAnyAuthority('Instructor','Coordinador')")
    @GetMapping("/alumnosGrupo")
    public ResponseEntity<List<AlumnoDto>> obtenerAlumnosGrupo(
            @Parameter(description = "Categoria del grupo")
            @RequestParam @NotBlank String categoria,
            @Parameter(description = "Curso del grupo")
            @RequestParam @NotBlank String curso,
            @Parameter(description = "Anio del grupo")
            @RequestParam @NotNull Integer anio,
            @Parameter(description = "Iterable del grupo")
            @RequestParam @NotNull Integer iterable) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/alumnosGrupo",
                "categoria=" + categoria + ", curso=" + curso + ", anio=" + anio + ", iterable=" + iterable);
        List<AlumnoDto> respuesta = servicioAlumno.obtenerAlumnosGrupo(categoria, curso, anio, iterable);
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Actualiza nombre, correo y tipo de un alumno")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alumno actualizado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "No existe el alumno con el identificador indicado")
    })
    @PreAuthorize("hasAnyAuthority('Coordinador', 'Instructor')")
    @PutMapping("/alumnos/{id}")
    public ResponseEntity<AlumnoDto> actualizarAlumno(
            @Parameter(description = "Cédula del alumno")
            @PathVariable String id,
            @Valid @RequestBody AlumnoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String perfIdLlamador = jwt != null ? jwt.getClaimAsString("perf_id") : null;
        if (perfIdLlamador == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String rolLlamador = jwt.getClaimAsString("rol");
        PeticionLogger.log(LOGGER, "PUT", "/api/v2/alumnos/" + id, "id=" + id);

        // Construir el modelo de dominio desde el DTO de entrada
        Perfil perfil = new Perfil();
        perfil.setNombre(request.getNombre());
        perfil.setCorreo(request.getCorreo());

        Alumno datosActualizar = new Alumno();
        datosActualizar.setTipoAlumno(request.getTipoAlumno());
        datosActualizar.setPerfil(perfil);

        Alumno alumnoActualizado = servicioAlumno.actualizarAlumno(id, datosActualizar, perfIdLlamador, rolLlamador);
        AlumnoDto respuesta = AlumnoDto.fabricarDeModelo(alumnoActualizado);
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Elimina lógicamente un alumno (META_ELIMINADO=1)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alumno eliminado lógicamente"),
        @ApiResponse(responseCode = "404", description = "No existe el alumno con el identificador indicado")
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @DeleteMapping("/alumnos/{id}")
    public ResponseEntity<AlumnoDto> eliminarAlumno(
            @Parameter(description = "Cédula del alumno")
            @PathVariable String id) {
        PeticionLogger.log(LOGGER, "DELETE", "/api/v2/alumnos/" + id, "id=" + id);
        Alumno alumnoEliminado = servicioAlumno.eliminarAlumno(id);
        AlumnoDto respuesta = AlumnoDto.fabricarDeModelo(alumnoEliminado);
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }
}
