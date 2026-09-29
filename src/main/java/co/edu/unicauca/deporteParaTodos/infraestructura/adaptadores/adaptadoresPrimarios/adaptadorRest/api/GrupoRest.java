package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IGrupoServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.GrupoDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.logs.PeticionLogger;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.GrupoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("api/v2")
@Validated
public class GrupoRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrupoRest.class);

    @Autowired
    private IGrupoServicio servicio;

    @Operation(summary = "Obtiene todos los grupos del sistema sin discriminar su estado eliminado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Grupos recuperados"),
    })
    @GetMapping("/grupos")
    public ResponseEntity<List<GrupoDto>> obtenerCursos() {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/grupos", "sin datos");
        List<GrupoDto> dtos = servicio.obtenerTodosGrupos().stream()
                .map(GrupoMapper::toDto)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene todos los grupos disponibles del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Grupos recuperados"),
    })
    @GetMapping("gruposNoEliminados")
    public ResponseEntity<List<GrupoDto>> obtenerCursosNoeliminados() {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/gruposNoEliminados", "sin datos");
        List<GrupoDto> dtos = servicio.obtenerGruposDisponibles().stream()
                .map(GrupoMapper::toDto)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene todos los grupos disponibles del sistema para un curso")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Grupos recuperados"),
    })
    @GetMapping("gruposCurso")
    public ResponseEntity<List<GrupoDto>> obtenerGruposDe(@RequestParam String prmCategoria, @RequestParam String prmCurso) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/gruposCurso", "prmCategoria=" + prmCategoria + ", prmCurso=" + prmCurso);
        List<GrupoDto> dtos = servicio.obtenerGruposDeCurso(prmCategoria, prmCurso).stream()
                .map(GrupoMapper::toDto)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene todos los grupos del sistema disponibles a la inscripcion")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Grupos recuperados"),
    })
    @GetMapping("/gruposInscripcion")
    public ResponseEntity<List<GrupoDto>> obtenerGruposInscripcion() {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/gruposInscripcion", "sin datos");
        List<GrupoDto> dtos = servicio.obtenerGruposInscripcionDisponible().stream()
                .map(GrupoMapper::toDto)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene todos los grupos del sistema asociados a un instructor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Grupos recuperados"),
    })
    @PreAuthorize("hasAnyAuthority('Instructor','Coordinador')")
    @GetMapping("/gruposInstructor")
    public ResponseEntity<List<GrupoDto>> obtnerGruposInstructor(@RequestParam String idInstructor) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/gruposInstructor", "idInstructor=" + idInstructor);
        List<GrupoDto> dtos = servicio.obtenerGruposInstructor(idInstructor).stream()
                .map(GrupoMapper::toDto)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @Operation(summary = "Inserta un registro en el sistema, los valores de anio e iterable son calculados internamete por el servidor, reportes de error por json malformados pueden ser causados por fechas no formateadas adecuadamente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Grupo insertado"),
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @PostMapping("/grupo")
    public ResponseEntity<GrupoDto> postGrupo(@RequestBody @Valid GrupoDto dto) {
        PeticionLogger.log(LOGGER, "POST", "/api/v2/grupo", dto);
        Grupo grupo = GrupoMapper.fromDto(dto);
        Grupo guardado = servicio.insertarGrupo(grupo);
        return new ResponseEntity<>(GrupoMapper.toDto(guardado), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualiza un grupo existente. idInstructor null quita el instructor "
            + "asignado; imagenGrupo no enviada (null) conserva la imagen actual del grupo.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Grupo actualizado"),
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @PutMapping("/grupo")
    public ResponseEntity<GrupoDto> putGrupo(
            @RequestParam String categoria, @RequestParam String curso,
            @RequestParam Integer anio, @RequestParam Integer iterable,
            @RequestBody GrupoDto dto) {
        PeticionLogger.log(LOGGER, "PUT", "/api/v2/grupo",
                "categoria=" + categoria + ", curso=" + curso + ", anio=" + anio + ", iterable=" + iterable);
        Grupo datos = GrupoMapper.fromDto(dto);
        Grupo actualizado = servicio.actualizarGrupo(categoria, curso, anio, iterable, datos);
        return new ResponseEntity<>(GrupoMapper.toDto(actualizado), HttpStatus.OK);
    }

    @Operation(summary = "obtiene un grupo del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "grupo encontrado"),
    })
    @GetMapping("/grupo")
    public ResponseEntity<GrupoDto> obtenerGrupo(@RequestParam String categoria, @RequestParam String curso, @RequestParam Integer anio, @RequestParam Integer iterable) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/grupo", "categoria=" + categoria + ", curso=" + curso + ", anio=" + anio + ", iterable=" + iterable);
        Grupo grupo = servicio.obtenerGrupo(categoria, curso, anio, iterable);
        return new ResponseEntity<>(GrupoMapper.toDto(grupo), HttpStatus.OK);
    }
}
