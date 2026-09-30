package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IClaseServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ClaseDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.logs.PeticionLogger;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.ClaseMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


@RestController
@RequestMapping("api/v2")
@Validated
public class ClaseRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaseRest.class);

    @Autowired
    private IClaseServicio servicioClase;

    @Operation(summary = "Obtiene las clases activas de un grupo especifico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado de clases del grupo"),
        @ApiResponse(responseCode = "404", description = "No existen clases para el grupo consultado")
    })
    @PreAuthorize("hasAnyAuthority('Instructor','Coordinador')")
    @GetMapping("/clasesGrupo")
    public ResponseEntity<List<ClaseDto>> getClasesGrupo(
            @Parameter(description = "Categoria del grupo")
            @RequestParam String categoria,
            @Parameter(description = "Curso del grupo")
            @RequestParam String curso,
            @Parameter(description = "Anio del grupo")
            @RequestParam Integer anio,
            @Parameter(description = "Iterable del grupo")
            @RequestParam Integer iterable) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/clasesGrupo",
                "categoria=" + categoria + ", curso=" + curso + ", anio=" + anio + ", iterable=" + iterable);
        List<ClaseDto> respuesta = servicioClase.obtenerClasesGrupo(categoria, curso, anio, iterable).stream()
                .map(ClaseMapper::toDto)
                .collect(Collectors.toList());
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Registra una nueva clase para un grupo, el codigo es generado por el sistema. "
            + "Si ya existe una clase para ese grupo en esa fecha, la reutiliza (200) en vez de crear otra.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Clase registrada correctamente"),
        @ApiResponse(responseCode = "200", description = "Ya existia una clase para ese grupo en esa fecha; se reutiliza")
    })
    @PreAuthorize("hasAnyAuthority('Instructor','Coordinador')")
    @PostMapping("/claseGrupo")
    public ResponseEntity<ClaseDto> postClase(@RequestBody @Valid ClaseDto entidad) {
        PeticionLogger.log(LOGGER, "POST", "/api/v2/claseGrupo", entidad);
        Clase modelo = ClaseMapper.fromDto(entidad);
        boolean yaExistiaClaseHoy = servicioClase.existeClaseEnFecha(
                modelo.getCategoria(), modelo.getCurso(), modelo.getAnio(), modelo.getIterable(), modelo.getFecha());
        Clase claseResultado = servicioClase.insertarClase(modelo);
        HttpStatus status = yaExistiaClaseHoy ? HttpStatus.OK : HttpStatus.CREATED;
        return new ResponseEntity<>(ClaseMapper.toDto(claseResultado), status);
    }

    @Operation(summary = "Marca una clase como eliminada")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Clase eliminada logicamente"),
        @ApiResponse(responseCode = "404", description = "La clase no existe")
    })
    @PreAuthorize("hasAnyAuthority('Instructor','Coordinador')")
    @DeleteMapping("/clase")
    public ResponseEntity<ClaseDto> deleteClase(
            @Parameter(description = "Codigo de la clase a eliminar")
            @RequestParam @NotNull Integer id) {
        PeticionLogger.log(LOGGER, "DELETE", "/api/v2/clase", "id=" + id);
        Clase claseEliminada = servicioClase.eliminarClase(id);
        return new ResponseEntity<>(ClaseMapper.toDto(claseEliminada), HttpStatus.OK);
    }
}
