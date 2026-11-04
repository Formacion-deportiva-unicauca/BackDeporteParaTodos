package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICursoServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.CursoDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.logs.PeticionLogger;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.CursoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;



@RestController
@RequestMapping("api/v2")
@Validated
public class CursoRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CursoRest.class);

    @Autowired
    private ICursoServicio servicio;

    // El Alumno solo ve los cursos que le corresponden (activos, para su tipo de vinculo y con algun grupo abierto);
    // Coordinador, Instructor y Administrador ven todo.
    private static Jwt jwtDe(Principal principal) {
        return principal instanceof JwtAuthenticationToken token ? token.getToken() : null;
    }

    private static boolean esAlumno(Jwt jwt) {
        return jwt != null && "Alumno".equals(jwt.getClaimAsString("rol"));
    }

    private static String perfIdDe(Jwt jwt) {
        return jwt.getClaimAsString("perf_id");
    }

    @Operation(summary = "Obtiene todos los cursos del sistema sin restricciones (el Alumno solo los que le corresponden)")
    @ApiResponses(value ={
        @ApiResponse(responseCode = "200", description = "listado de cursos en formato dto"),
    })
    @GetMapping("/cursos")
    public ResponseEntity<List<CursoDto>> obtenerCursos(Principal principal){
        PeticionLogger.log(LOGGER, "GET", "/api/v2/cursos", "sin datos");
        Jwt jwt = jwtDe(principal);
        List<Curso> cursos = esAlumno(jwt) ? servicio.recuperarCursosParaAlumno(perfIdDe(jwt)) : servicio.recuperarCursos();
        List<CursoDto> respuesta = cursos.stream()
            .map(CursoMapper::toDto)
            .collect(Collectors.toList());
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene todos los cursos disponibles del sistema para una categoria")
    @ApiResponses(value ={
        @ApiResponse(responseCode = "200", description = "listado de cursos en formato dto"),
    })
    @GetMapping("/cursosbycategoria")
    public ResponseEntity<List<CursoDto>> obtenerCursosPorCategoria(
        @Parameter(description = "Identificador de una categoria del sistema")
        @RequestParam String prmCategoria,
        Principal principal
        ){
        PeticionLogger.log(LOGGER, "GET", "/api/v2/cursosbycategoria", "prmCategoria=" + prmCategoria);
        Jwt jwt = jwtDe(principal);
        List<Curso> cursos = esAlumno(jwt)
            ? servicio.cursosDeCategoriaParaAlumno(prmCategoria, perfIdDe(jwt))
            : servicio.cursosDeCategoria(prmCategoria);
        List<CursoDto> respuesta = cursos.stream()
            .map(CursoMapper::toDto)
            .collect(Collectors.toList());
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene todos los cursos de una categoria incluyendo los inactivos/eliminados — uso exclusivo del panel de administración")
    @ApiResponses(value ={
        @ApiResponse(responseCode = "200", description = "listado completo de cursos sin filtro de estado"),
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @GetMapping("/cursosbycategoria/todos")
    public ResponseEntity<List<CursoDto>> obtenerTodosCursosPorCategoria(
        @Parameter(description = "Identificador de una categoria del sistema")
        @RequestParam String prmCategoria
        ){
        PeticionLogger.log(LOGGER, "GET", "/api/v2/cursosbycategoria/todos", "prmCategoria=" + prmCategoria);
        List<CursoDto> respuesta = servicio.todosLosCursosDeCategoria(prmCategoria).stream()
            .map(CursoMapper::toDto)
            .collect(Collectors.toList());
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene un curso del sistema")
    @ApiResponses(value ={
        @ApiResponse(responseCode = "200", description = "curso encontrado"),
    })
    @GetMapping("/curso")
    public ResponseEntity<CursoDto> obtenerCurso(
        @Parameter(description = "Identificador de una categoria del sistema")
        @RequestParam String prmCategoria,
        @Parameter(description = "Identificador de un curso en el sistema")
        @RequestParam String prmCurso) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/curso", "prmCategoria=" + prmCategoria + ", prmCurso=" + prmCurso);
        Curso curso = servicio.obtenerCurso(prmCategoria, prmCurso);
        return new ResponseEntity<>(CursoMapper.toDto(curso), HttpStatus.OK);
    }

    @Operation(summary = "registrar curso en el sistema, no manipula los id")
    @ApiResponses(value ={
        @ApiResponse(responseCode = "200", description = "curso registrado"),
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @PostMapping("/curso")
    public ResponseEntity<CursoDto> postAgregarCurso(@RequestBody @Valid CursoDto dto) {
        PeticionLogger.log(LOGGER, "POST", "/api/v2/curso", dto);
        Curso guardado = servicio.insertarCurso(CursoMapper.fromDto(dto));
        return new ResponseEntity<>(CursoMapper.toDto(guardado), HttpStatus.CREATED);
    }

    @Operation(summary = "actualiza curso en el sistema")
    @ApiResponses(value ={
        @ApiResponse(responseCode = "200", description = "curso actualizado, no manipula los id"),
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @PutMapping("curso")
    public ResponseEntity<CursoDto> actualizarCurso(
        @Parameter(description = "Identificador de una categoria del sistema")
        @RequestParam @NotBlank String categoria,
        @Parameter(description = "Identificador de un curso en el sistema")
        @RequestParam @NotBlank String curso,
        @RequestBody @Valid CursoDto dto) {
        PeticionLogger.log(LOGGER, "PUT", "/api/v2/curso", "categoria=" + categoria + ", curso=" + curso + ", body=" + dto);
        Curso actualizado = servicio.actualizarCurso(categoria, curso, CursoMapper.fromDto(dto));
        return new ResponseEntity<>(CursoMapper.toDto(actualizado), HttpStatus.OK);
    }

    @Operation(summary = "Borrado lógico de un curso: setea meta_eliminado=1. Retorna 409 si ya estaba eliminado.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "curso eliminado lógicamente"),
        @ApiResponse(responseCode = "404", description = "curso no encontrado"),
        @ApiResponse(responseCode = "409", description = "el curso ya estaba eliminado"),
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @DeleteMapping("curso")
    public ResponseEntity<CursoDto> eliminarCurso(
        @Parameter(description = "Identificador de una categoria del sistema")
        @RequestParam @NotBlank String prmCategoria,
        @Parameter(description = "Identificador de un curso en el sistema")
        @RequestParam @NotBlank String prmCurso){
        PeticionLogger.log(LOGGER, "DELETE", "/api/v2/curso", "prmCategoria=" + prmCategoria + ", prmCurso=" + prmCurso);
        Curso eliminado = servicio.eliminarCursoPermanente(prmCategoria, prmCurso);
        return new ResponseEntity<>(CursoMapper.toDto(eliminado), HttpStatus.OK);
    }

    @Operation(summary = "Cambia el estado de un curso (ACTIVO/INACTIVO)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "estado del curso actualizado"),
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @PatchMapping("/curso/estado")
    public ResponseEntity<CursoDto> cambiarEstadoCurso(
        @Parameter(description = "Identificador de la categoria del curso")
        @RequestParam @NotBlank String prmCategoria,
        @Parameter(description = "Identificador del curso")
        @RequestParam @NotBlank String prmCurso,
        @Parameter(description = "Nuevo estado: ACTIVO o INACTIVO")
        @RequestParam EstadoCurso prmEstado) {
        PeticionLogger.log(LOGGER, "PATCH", "/api/v2/curso/estado",
            "prmCategoria=" + prmCategoria + ", prmCurso=" + prmCurso + ", prmEstado=" + prmEstado);
        Curso actualizado = servicio.cambiarEstadoCurso(prmCategoria, prmCurso, prmEstado);
        return new ResponseEntity<>(CursoMapper.toDto(actualizado), HttpStatus.OK);
    }
}
