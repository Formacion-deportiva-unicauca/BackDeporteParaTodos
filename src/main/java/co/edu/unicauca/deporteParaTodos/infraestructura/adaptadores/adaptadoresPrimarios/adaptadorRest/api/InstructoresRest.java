package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InstructorDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InstructorRequest;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PlazoCargaDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PlazoCargaRequest;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ProrrogaDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ProrrogaRequest;
import co.edu.unicauca.deporteParaTodos.dominio.utils.LogSanitizer;
import co.edu.unicauca.deporteParaTodos.infraestructura.logs.PeticionLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("api/v2")
@Validated
public class InstructoresRest {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstructoresRest.class);

    @Autowired
    private IInstructorServicio servicioInstructor;

    @Operation(summary = "Obtiene todos los instructores registrados en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de instructores"),
            @ApiResponse(responseCode = "404", description = "No existen instructores registrados")
    })
    @GetMapping("/instructores")
    public ResponseEntity<List<InstructorDto>> getMethodName() {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/instructores", "sin datos");
        List<InstructorDto> respuesta = servicioInstructor.obtenerInstructores();
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene un instructor por su identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Instructor encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe el instructor con ese identificador")
    })
    @GetMapping("/instructor")
    public ResponseEntity<InstructorDto> getInstructorById(
            @RequestParam(name = "idInstructor") @NotBlank String idInstructor) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/instructor", "idInstructor: " + idInstructor);
        InstructorDto respuesta = servicioInstructor.obtenerInstructor(idInstructor);
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Registra un nuevo instructor con su perfil de forma transparente (una llamada HTTP)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Instructor registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "El instructor ya existe")
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @PostMapping("/RegistroPerfilInstructor")
    public ResponseEntity<InstructorDto> registrarInstructor(@RequestBody @Valid PerfilDto perfilDto) {
        PeticionLogger.log(LOGGER, "POST", "/api/v2/RegistroPerfilInstructor",
                "id: " + perfilDto.getId() + ", nombre: " + perfilDto.getNombre());
        InstructorDto respuesta = servicioInstructor.registrarInstructor(perfilDto);
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualiza nombre y correo de un instructor (solo esos dos campos; cualquier "
            + "otro campo del cuerpo se ignora)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Instructor actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "El id no es un instructor activo (no existe, "
                    + "esta eliminado logicamente, o no es instructor)"),
            @ApiResponse(responseCode = "409", description = "El correo ya esta en uso por otro perfil")
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @PutMapping("/instructor/{id}")
    public ResponseEntity<InstructorDto> actualizarInstructor(
            @PathVariable String id,
            @RequestBody @Valid InstructorRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String idCoordinador = jwt != null ? jwt.getClaimAsString("perf_id") : null;
        if (idCoordinador == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Sonar java:S5145 (log forging): id es una variable de ruta cruda del
        // cliente -- se sanea CR/LF antes de interpolarla en el log (aparece dos
        // veces en esta linea: en "Direccion" y en "Datos entrantes").
        String idSaneado = LogSanitizer.sanitizar(id);
        PeticionLogger.log(LOGGER, "PUT", "/api/v2/instructor/" + idSaneado, "id: " + idSaneado);

        Perfil perfil = new Perfil();
        perfil.setNombre(request.getNombre().trim());
        perfil.setCorreo(request.getCorreo().trim());
        Instructor datosActualizar = new Instructor();
        datosActualizar.setPerfil(perfil);

        Instructor actualizado = servicioInstructor.actualizarInstructor(id, datosActualizar, idCoordinador);
        return new ResponseEntity<>(InstructorDto.fabricarDeModelo(actualizado), HttpStatus.OK);
    }

    @Operation(summary = "Borrado lógico de un instructor: marca meta_eliminado=1. Retorna 409 si ya estaba eliminado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Instructor eliminado lógicamente"),
            @ApiResponse(responseCode = "404", description = "El instructor no existe"),
            @ApiResponse(responseCode = "409", description = "El instructor ya estaba eliminado")
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @DeleteMapping("/instructor")
    public ResponseEntity<InstructorDto> deleteInstructor(
            @RequestParam(name = "instructorId") @NotBlank String instructorId) {
        PeticionLogger.log(LOGGER, "DELETE", "/api/v2/instructor", "instructorId: " + instructorId);
        Instructor eliminado = servicioInstructor.eliminarInstructor(instructorId);
        return new ResponseEntity<>(InstructorDto.fabricarDeModelo(eliminado), HttpStatus.OK);
    }

    // ── SCRUM-80 (HE-14 HU-02): plazo de carga de atenciones ─────────────────
    // FASE A, REEMPLAZADA por el rediseno (prorroga mensual, ver mas abajo).
    // Se mantienen SIN RETIRAR porque el frontend todavia los usa -- se
    // retiran cuando ese frontend migre a /instructores/prorroga-registro.

    @Deprecated
    @Operation(summary = "[DEPRECADO] Lista, para cada instructor activo, el plazo de carga de atenciones y el "
            + "estado del reporte del periodo academico vigente. Reemplazado por "
            + "GET /instructores/prorroga-registro (rediseno SCRUM-80, 15 oct 2026).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de plazos de carga")
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @GetMapping("/instructores/plazos-carga")
    public ResponseEntity<List<PlazoCargaDto>> obtenerPlazosCarga() {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/instructores/plazos-carga", "sin datos");
        List<PlazoCargaDto> respuesta = servicioInstructor.obtenerPlazosCarga();
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Deprecated
    @Operation(summary = "[DEPRECADO] Asigna o reemplaza (upsert) el plazo de carga de atenciones del periodo "
            + "academico vigente para un instructor. Reemplazado por "
            + "PUT /instructores/prorroga-registro (rediseno SCRUM-80, 15 oct 2026).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plazo asignado o reemplazado"),
            @ApiResponse(responseCode = "400", description = "fechaLimite ausente o con formato invalido"),
            @ApiResponse(responseCode = "404", description = "El id no es un instructor activo"),
            @ApiResponse(responseCode = "422", description = "La fecha limite es anterior a hoy")
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @PutMapping("/instructor/{id}/plazo-carga")
    public ResponseEntity<PlazoCargaDto> asignarPlazoCarga(
            @PathVariable String id,
            @RequestBody @Valid PlazoCargaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String idCoordinador = jwt != null ? jwt.getClaimAsString("perf_id") : null;
        if (idCoordinador == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Sonar java:S5145 (log forging): id es una variable de ruta cruda del
        // cliente -- se sanea CR/LF antes de interpolarla en el log. fechaLimite
        // tambien se sanea por politica (todo valor del cliente que se registra
        // pasa por LogSanitizer), aunque al ser un LocalDate ya validado/parseado
        // por Jackson no puede traer CR/LF en la practica -- ver nota en
        // InstructorServicio.asignarPlazoCarga().
        String idSaneado = LogSanitizer.sanitizar(id);
        String fechaLimiteSaneada = LogSanitizer.sanitizar(String.valueOf(request.getFechaLimite()));
        PeticionLogger.log(LOGGER, "PUT", "/api/v2/instructor/" + idSaneado + "/plazo-carga",
                "id: " + idSaneado + ", fechaLimite: " + fechaLimiteSaneada);
        PlazoCargaDto respuesta = servicioInstructor.asignarPlazoCarga(id, request.getFechaLimite(), idCoordinador);
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    // ── SCRUM-80 (rediseno, bloque 1 de 3): prorroga de registro de asistencia ──

    @Operation(summary = "Lista las prorrogas de registro de asistencia asignadas para un mes "
            + "(solo los instructores que SI tienen una asignada)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de prorrogas del mes"),
            @ApiResponse(responseCode = "400", description = "mes fuera de 1-12"),
            @ApiResponse(responseCode = "422", description = "El mes es futuro")
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @GetMapping("/instructores/prorroga-registro")
    public ResponseEntity<List<ProrrogaDto>> obtenerProrrogasRegistro(
            @RequestParam @NotNull Integer anio,
            @RequestParam @NotNull @Min(1) @Max(12) Integer mes) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/instructores/prorroga-registro",
                "anio=" + anio + ", mes=" + mes);
        List<ProrrogaDto> respuesta = servicioInstructor.obtenerProrrogasRegistro(anio, mes);
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Asigna o reemplaza (upsert) la prorroga de registro de asistencia de uno o "
            + "varios instructores para un mes. dias=0 elimina la prorroga existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Prorrogas procesadas (asignadas o eliminadas)"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (lista vacia, mes fuera de 1-12, dias fuera de 0-60)"),
            @ApiResponse(responseCode = "404", description = "Alguno de los ids no es un instructor activo (atomico: no se guarda nada)"),
            @ApiResponse(responseCode = "422", description = "El mes es futuro")
    })
    @PreAuthorize("hasAuthority('Coordinador')")
    @PutMapping("/instructores/prorroga-registro")
    public ResponseEntity<List<ProrrogaDto>> asignarProrrogaRegistro(
            @RequestBody @Valid ProrrogaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String idCoordinador = jwt != null ? jwt.getClaimAsString("perf_id") : null;
        if (idCoordinador == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Sonar java:S5145 (log forging): idsInstructores es una lista cruda del
        // cuerpo del cliente -- cada id se sanea antes de interpolarse en el log.
        String idsSaneados = request.getIdsInstructores().stream()
                .map(LogSanitizer::sanitizar)
                .collect(java.util.stream.Collectors.joining(","));
        PeticionLogger.log(LOGGER, "PUT", "/api/v2/instructores/prorroga-registro",
                "idsInstructores=[" + idsSaneados + "], anio=" + request.getAnio()
                        + ", mes=" + request.getMes() + ", dias=" + request.getDias());
        List<ProrrogaDto> respuesta = servicioInstructor.asignarProrrogaRegistro(
                request.getIdsInstructores(), request.getAnio(), request.getMes(), request.getDias(), idCoordinador);
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

}
