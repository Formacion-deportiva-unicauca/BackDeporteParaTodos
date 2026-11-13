package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAutenticacionServicio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.logs.PeticionLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("api/v2")
@Validated
public class AutenticacionRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutenticacionRest.class);

    @Autowired
    private IAutenticacionServicio servicioAutenticacion;

    // Coordinador/Instructor/Administrador: cualquier correo (lo usa el flujo de inscribir
    // alumnos). Alumno: solo su propio correo (comparado contra el "sub" del dpt_token, que
    // es el correo -- ver TokenServicio). Cualquier otro rol lo rechaza @PreAuthorize con 403.
    @Operation(summary = "Autentica un perfil a partir del correo y retorna su rol dentro del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil autenticado"),
        @ApiResponse(responseCode = "403", description = "Un Alumno solo puede consultar su propio correo"),
        @ApiResponse(responseCode = "404", description = "Perfil no encontrado")
    })
    @PreAuthorize("hasAnyAuthority('Alumno','Coordinador','Instructor','Administrador')")
    @GetMapping("/login")
    public ResponseEntity<PerfilDto> login(
            @Parameter(description = "Correo del perfil que desea autenticarse")
            @RequestParam @NotBlank @Email String email,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt != null && "Alumno".equals(jwt.getClaimAsString("rol"))
                && !email.equalsIgnoreCase(jwt.getSubject())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        PeticionLogger.log(LOGGER, "GET", "/api/v2/login", "email=" + email);
        PerfilDto respuesta = servicioAutenticacion.login(email);
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Registra un nuevo perfil en el sistema y lo asocia como alumno")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Perfil registrado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
        @ApiResponse(responseCode = "409", description = "El perfil ya existe")
    })
    @PostMapping("/RegistroPerfilAlumno")
    public ResponseEntity<PerfilDto> registrarAlumno(@RequestBody @Valid PerfilDto dto) {
        PeticionLogger.log(LOGGER, "POST", "/api/v2/register", dto);
        PerfilDto respuesta = servicioAutenticacion.registrarAlumno(dto);
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }
}
