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

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IProgramaServicio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ProgramaDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.logs.PeticionLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("api/v2")
@Validated
public class ProgramaRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgramaRest.class);

    @Autowired
    private IProgramaServicio servicioPrograma;

    @Operation(summary = "Obtiene los programas academicos de una facultad. Es publico: lo usa el formulario de registro, antes de que exista el dpt_token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado de programas de la facultad (si no hay, cuerpo con codigo GC-0002)"),
        @ApiResponse(responseCode = "400", description = "Falta el parametro facultad")
    })
    @GetMapping("/programas")
    public ResponseEntity<List<ProgramaDto>> obtenerProgramasPorFacultad(
            @Parameter(description = "Nombre de la facultad")
            @RequestParam String facultad) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/programas", "facultad=" + facultad);
        List<ProgramaDto> respuesta = servicioPrograma.obtenerProgramasPorFacultad(facultad).stream()
                .map(ProgramaDto::fabricarDeModelo)
                .collect(Collectors.toList());
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }
}
