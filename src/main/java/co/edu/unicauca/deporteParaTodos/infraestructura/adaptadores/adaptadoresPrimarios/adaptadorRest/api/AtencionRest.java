package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAsistenciaServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.AsistenciaMapper;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AtencionDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.logs.PeticionLogger;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("api/v2")
@Validated
public class AtencionRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtencionRest.class);

    private final IAsistenciaServicio servicioAsistencia;

    public AtencionRest(IAsistenciaServicio servicioAsistencia) {
        this.servicioAsistencia = servicioAsistencia;
    }

    @Operation(summary = "Obtiene las atenciones registradas para una clase especifica, retonan encapsulado en un Dto con el id de la clase, el id perfil del estudiante, todos son true pues solo retorna las asistencias no las no asistencias, en front para una clase comparan con la lista de alumnos del grupo, los que estan en esta lista son los que asistieron de ese grupo")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado de atenciones por clase"),
        @ApiResponse(responseCode = "404", description = "No existen atenciones para la clase consultada")
    })
    @PreAuthorize("hasAnyAuthority('Instructor','Coordinador')")
    @GetMapping("/atencionesporclase")
    public ResponseEntity<List<AtencionDto>> obtenerAtencionesPorClase(
            @Parameter(description = "Codigo de la clase a consultar")
            @RequestParam("claseid") @NotNull Integer claseId) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/atencionesporclase", "claseid=" + claseId);
        List<AtencionDto> respuesta = servicioAsistencia.obtenerAtencionesPorClase(claseId);
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Registra la asistencia de una lista de alumnos para una clase especifica, envie encapsulado en un dto con el id de la clase y el id del perfil estudiante, el valor booleano indica si asistio o no, enviese toda la lista de identificadores de alumnos de un grupo con el valor de true o false")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Asistencias procesadas correctamente"),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
        @ApiResponse(responseCode = "404", description = "Clase o alumno no encontrado"),
        @ApiResponse(responseCode = "422", description = "Fecha de la clase futura, o fuera del plazo de registro")
    })
    // SCRUM-80 (rediseno, bloque 2): 'Administrador' se agrega porque la regla
    // de negocio lo exime del limite de plazo igual que a 'Coordinador' --
    // ver nota equivalente en ClaseRest.
    @PreAuthorize("hasAnyAuthority('Instructor','Coordinador','Administrador')")
    @PostMapping("/atenciones")
    public ResponseEntity<Void> registrarAtencionesPorClase(
            @Parameter(description = "Codigo de la clase para las atenciones")
            @RequestParam("idClase") @NotNull Integer claseId,
            @RequestBody @Valid List<AtencionDto> atenciones,
            @AuthenticationPrincipal Jwt jwt) {
        String perfIdLlamador = jwt != null ? jwt.getClaimAsString("perf_id") : null;
        if (perfIdLlamador == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String rolLlamador = jwt.getClaimAsString("rol");
        PeticionLogger.log(LOGGER, "POST", "/api/v2/atenciones", "idClase=" + claseId + ", atenciones=" + atenciones);
        servicioAsistencia.registrarAtencionesPorClase(atenciones, claseId, perfIdLlamador, rolLlamador);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Eliminación lógica de una asistencia (marca META_ELIMINADO=1)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Asistencia eliminada correctamente"),
        @ApiResponse(responseCode = "404", description = "La asistencia no existe"),
        @ApiResponse(responseCode = "409", description = "La asistencia ya estaba eliminada"),
        @ApiResponse(responseCode = "422", description = "Fecha de la clase futura, o fuera del plazo de registro")
    })
    @PreAuthorize("hasAnyAuthority('Instructor','Coordinador','Administrador')")
    @DeleteMapping("/asistencia")
    public ResponseEntity<AtencionDto> eliminarAsistencia(
            @Parameter(description = "Identificador del perfil del alumno")
            @RequestParam String prmPerfId,
            @Parameter(description = "Código de la clase")
            @RequestParam Long prmClsCodigo,
            @AuthenticationPrincipal Jwt jwt) {
        String perfIdLlamador = jwt != null ? jwt.getClaimAsString("perf_id") : null;
        if (perfIdLlamador == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String rolLlamador = jwt.getClaimAsString("rol");
        PeticionLogger.log(LOGGER, "DELETE", "/api/v2/asistencia",
                "prmPerfId=" + prmPerfId + ", prmClsCodigo=" + prmClsCodigo);
        Asistencia resultado = servicioAsistencia.eliminarAsistencia(prmPerfId, prmClsCodigo, perfIdLlamador, rolLlamador);
        return new ResponseEntity<>(AsistenciaMapper.toDto(resultado), HttpStatus.OK);
    }
}
