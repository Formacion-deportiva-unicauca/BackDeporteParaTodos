package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInscripcionServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Disponibilidad;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionEnEspera;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionResumen;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.DisponibilidadDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InscripcionDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InscripcionEnEsperaDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InscripcionResumenDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.logs.PeticionLogger;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.InscripcionMapper;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v2")
@Validated
public class InscripcionRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InscripcionRest.class);

    private final IInscripcionServicio servicio;

    public InscripcionRest(IInscripcionServicio servicio) {
        this.servicio = servicio;
    }

    @PreAuthorize("hasAnyAuthority('Alumno', 'Coordinador')")
    @PostMapping("/inscripcion")
    public ResponseEntity<InscripcionDto> inscribir(
            @RequestBody InscripcionDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt != null && "Alumno".equals(jwt.getClaimAsString("rol"))
                && !dto.getAlumnoId().equals(jwt.getClaimAsString("perf_id"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        PeticionLogger.log(LOGGER, "POST", "/api/v2/inscripcion", dto);
        Inscripcion inscripcion = InscripcionMapper.fromDto(dto);
        Inscripcion resultado = servicio.inscribir(inscripcion);
        return new ResponseEntity<>(InscripcionMapper.toDto(resultado), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyAuthority('Alumno', 'Coordinador')")
    @GetMapping("/validarInscripcion")
    public ResponseEntity<Boolean> validarInscripcion(
            @RequestParam String alumnoId,
            @RequestParam String categoria,
            @RequestParam String curso,
            @RequestParam int anio,
            @RequestParam int iterable,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt != null && "Alumno".equals(jwt.getClaimAsString("rol"))
                && !alumnoId.equals(jwt.getClaimAsString("perf_id"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        PeticionLogger.log(LOGGER, "GET", "/api/v2/validarInscripcion", alumnoId);
        boolean resultado = servicio.validarInscripcion(alumnoId, categoria, curso, anio, iterable);
        return new ResponseEntity<>(resultado, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyAuthority('Alumno', 'Coordinador')")
    @PutMapping("/desvincularInscripcion")
    public ResponseEntity<InscripcionDto> desvincularInscripcion(
            @RequestBody InscripcionDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt != null && "Alumno".equals(jwt.getClaimAsString("rol"))
                && !dto.getAlumnoId().equals(jwt.getClaimAsString("perf_id"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        PeticionLogger.log(LOGGER, "PUT", "/api/v2/desvincularInscripcion", dto);
        Inscripcion resultado = servicio.desvincularInscripcion(
                dto.getAlumnoId(), dto.getCategoria(), dto.getCurso(), dto.getAnio(), dto.getIterable());
        return new ResponseEntity<>(InscripcionMapper.toDto(resultado), HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('Coordinador')")
    @PatchMapping("/inscripcion/promover")
    public ResponseEntity<InscripcionDto> promoverInscripcion(
            @RequestParam String prmPerfId,
            @RequestParam String prmCategoria,
            @RequestParam String prmCurso,
            @RequestParam int prmAnio,
            @RequestParam int prmIterable) {
        PeticionLogger.log(LOGGER, "PATCH", "/api/v2/inscripcion/promover", prmPerfId);
        Inscripcion resultado = servicio.promoverManualmente(prmPerfId, prmCategoria, prmCurso, prmAnio, prmIterable);
        return new ResponseEntity<>(InscripcionMapper.toDto(resultado), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyAuthority('Alumno', 'Coordinador')")
    @GetMapping("/inscripcion/disponibilidad")
    public ResponseEntity<DisponibilidadDto> obtenerDisponibilidad(
            @RequestParam String prmCategoria,
            @RequestParam String prmCurso,
            @RequestParam int prmAnio,
            @RequestParam int prmIterable) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/inscripcion/disponibilidad", prmCategoria);
        Disponibilidad disponibilidad = servicio.obtenerDisponibilidad(prmCategoria, prmCurso, prmAnio, prmIterable);
        return new ResponseEntity<>(DisponibilidadDto.fabricarDeModelo(disponibilidad), HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('Coordinador')")
    @GetMapping("/inscripcion/listaEspera")
    public ResponseEntity<List<InscripcionEnEsperaDto>> listarEnEspera(
            @RequestParam String prmCategoria,
            @RequestParam String prmCurso,
            @RequestParam int prmAnio,
            @RequestParam int prmIterable) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/inscripcion/listaEspera", prmCategoria);
        List<InscripcionEnEspera> lista = servicio.listarEnEspera(prmCategoria, prmCurso, prmAnio, prmIterable);
        List<InscripcionEnEsperaDto> dtos = lista.stream()
                .map(InscripcionEnEsperaDto::fabricarDeModelo)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('Alumno')")
    @GetMapping("/inscripcion/misCursos")
    public ResponseEntity<List<InscripcionResumenDto>> misCursos(@AuthenticationPrincipal Jwt jwt) {
        String alumnoId = jwt != null ? jwt.getClaimAsString("perf_id") : null;
        if (alumnoId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        PeticionLogger.log(LOGGER, "GET", "/api/v2/inscripcion/misCursos", alumnoId);
        List<InscripcionResumen> lista = servicio.listarMisCursos(alumnoId);
        List<InscripcionResumenDto> dtos = lista.stream()
                .map(InscripcionResumenDto::fabricarDeModelo)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }
}
