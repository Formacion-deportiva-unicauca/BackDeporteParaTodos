package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.INovedadClaseServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.NovedadClase;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.NovedadClaseDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.NovedadClaseRequestDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.logs.PeticionLogger;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.NovedadClaseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("api/v2")
@Validated
public class NovedadClaseRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NovedadClaseRest.class);

    @Autowired
    private INovedadClaseServicio servicioNovedad;

    @Operation(summary = "Registra una novedad de clase no realizada para un grupo y fecha determinada. "
            + "El motivo es obligatorio; si el motivo es 'OTRO', la descripci\u00f3n tambi\u00e9n lo es.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Novedad registrada correctamente"),
        @ApiResponse(responseCode = "400", description = "Solicitud inv\u00e1lida"),
        @ApiResponse(responseCode = "403", description = "No est\u00e1 autorizado para este grupo"),
        @ApiResponse(responseCode = "404", description = "Grupo no encontrado"),
        @ApiResponse(responseCode = "409", description = "Ya existe una novedad activa para este grupo y fecha"),
        @ApiResponse(responseCode = "422", description = "Fecha futura, fuera de ventana o motivo inv\u00e1lido")
    })
    @PreAuthorize("hasAnyAuthority('Instructor','Coordinador','Administrador')")
    @PostMapping("/novedades-clase")
    public ResponseEntity<NovedadClaseDto> registrarNovedad(
            @RequestBody @Valid NovedadClaseRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        String perfIdLlamador = jwt != null ? jwt.getClaimAsString("perf_id") : null;
        if (perfIdLlamador == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String rolLlamador = jwt.getClaimAsString("rol");
        PeticionLogger.log(LOGGER, "POST", "/api/v2/novedades-clase",
                "categoria=" + request.getCategoria() + ", curso=" + request.getCurso()
                + ", anio=" + request.getAnio() + ", iterable=" + request.getIterable()
                + ", fecha=" + request.getFecha() + ", motivo=" + request.getMotivo());

        NovedadClase datos = NovedadClaseMapper.fromDto(toDomainDto(request));
        NovedadClase resultado = servicioNovedad.registrarNovedad(datos, perfIdLlamador, rolLlamador);
        return new ResponseEntity<>(NovedadClaseMapper.toDto(resultado), HttpStatus.CREATED);
    }

    @Operation(summary = "Obtiene las novedades activas de un grupo")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado de novedades del grupo"),
        @ApiResponse(responseCode = "404", description = "No existen novedades para el grupo consultado")
    })
    @PreAuthorize("hasAnyAuthority('Instructor','Coordinador','Administrador')")
    @GetMapping("/novedades-clase")
    public ResponseEntity<List<NovedadClaseDto>> obtenerNovedadesGrupo(
            @Parameter(description = "Categor\u00eda del grupo") @RequestParam String categoria,
            @Parameter(description = "Curso del grupo") @RequestParam String curso,
            @Parameter(description = "A\u00f1o del grupo") @RequestParam Integer anio,
            @Parameter(description = "Iterable del grupo") @RequestParam Integer iterable) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/novedades-clase",
                "categoria=" + categoria + ", curso=" + curso + ", anio=" + anio + ", iterable=" + iterable);
        List<NovedadClaseDto> respuesta = servicioNovedad.obtenerNovedadesGrupo(categoria, curso, anio, iterable)
                .stream()
                .map(NovedadClaseMapper::toDto)
                .toList();
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Actualiza una novedad existente (motivo y/o descripci\u00f3n). "
            + "Conserva los datos originales de creaci\u00f3n y actualiza los de modificaci\u00f3n.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Novedad actualizada correctamente"),
        @ApiResponse(responseCode = "400", description = "Solicitud inv\u00e1lida"),
        @ApiResponse(responseCode = "403", description = "No est\u00e1 autorizado para modificar esta novedad"),
        @ApiResponse(responseCode = "404", description = "La novedad no existe"),
        @ApiResponse(responseCode = "422", description = "Motivo inv\u00e1lido o descripci\u00f3n vac\u00eda para OTRO")
    })
    @PreAuthorize("hasAnyAuthority('Instructor','Coordinador','Administrador')")
    @PutMapping("/novedades-clase/{codigo}")
    public ResponseEntity<NovedadClaseDto> actualizarNovedad(
            @Parameter(description = "C\u00f3digo de la novedad") @PathVariable @NotNull Long codigo,
            @RequestBody @Valid NovedadClaseRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        String perfIdLlamador = jwt != null ? jwt.getClaimAsString("perf_id") : null;
        if (perfIdLlamador == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String rolLlamador = jwt.getClaimAsString("rol");
        PeticionLogger.log(LOGGER, "PUT", "/api/v2/novedades-clase/" + codigo,
                "motivo=" + request.getMotivo());

        NovedadClase datos = NovedadClaseMapper.fromDto(toDomainDto(request));
        NovedadClase resultado = servicioNovedad.actualizarNovedad(codigo, datos, perfIdLlamador, rolLlamador);
        return new ResponseEntity<>(NovedadClaseMapper.toDto(resultado), HttpStatus.OK);
    }

    private NovedadClaseDto toDomainDto(NovedadClaseRequestDto request) {
        NovedadClaseDto dto = new NovedadClaseDto();
        dto.setCategoria(request.getCategoria());
        dto.setCurso(request.getCurso());
        dto.setAnio(request.getAnio());
        dto.setIterable(request.getIterable());
        dto.setFecha(request.getFecha());
        dto.setMotivo(request.getMotivo());
        dto.setDescripcion(request.getDescripcion());
        return dto;
    }
}
