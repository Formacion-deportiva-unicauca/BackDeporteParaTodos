package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IReporteServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.OpcionesReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ReporteMensual;
import co.edu.unicauca.deporteParaTodos.infraestructura.logs.PeticionLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/** Reportes de estadisticas: solo el Coordinador (cualquier otro rol recibe 403). Devuelven cifras agregadas, sin datos personales. */
@RestController
@RequestMapping("api/v2/reportes")
public class ReporteRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReporteRest.class);

    private final IReporteServicio servicio;

    public ReporteRest(IReporteServicio servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Reporte mensual: por curso, inscritos y atenciones (hombres y mujeres), totales, distribucion por vinculo y calidad de datos")
    @PreAuthorize("hasAuthority('Coordinador')")
    @GetMapping("mensual")
    public ResponseEntity<ReporteMensual> mensual(
            @Parameter(description = "Anio, por ejemplo 2026") @RequestParam("anio") int anio,
            @Parameter(description = "Mes, de 1 a 12") @RequestParam("mes") int mes,
            @RequestParam(required = false) String disciplina,
            @Parameter(description = "Modalidad (categoria del curso), sin importar mayusculas") @RequestParam(required = false) String modalidad,
            @Parameter(description = "ESTUDIANTE, FUNCIONARIO o MIXTO") @RequestParam(required = false) String publico,
            @Parameter(description = "Estudiante, Docente, Administrativo, Contratista o Funcionarios; repetible o separado por comas")
            @RequestParam(required = false) List<String> vinculo,
            @Parameter(description = "M o F") @RequestParam(required = false) String genero,
            @Parameter(description = "Facultad: quedan fuera administrativos y contratistas, que no tienen facultad registrada")
            @RequestParam(required = false) String facultad,
            @Parameter(description = "Agrupa Docente, Administrativo y Contratista como Funcionarios en la distribucion por vinculo")
            @RequestParam(defaultValue = "false") boolean agruparFuncionarios) {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/reportes/mensual", "anio=" + anio + ", mes=" + mes + ", disciplina=" + disciplina
                + ", modalidad=" + modalidad + ", publico=" + publico + ", vinculo=" + vinculo + ", genero=" + genero
                + ", facultad=" + facultad + ", agruparFuncionarios=" + agruparFuncionarios);
        return ResponseEntity.ok(servicio.mensual(anio, mes, disciplina, modalidad, publico, vinculo, genero, facultad, agruparFuncionarios));
    }

    @Operation(summary = "Valores de los filtros del reporte y primer y ultimo mes con datos")
    @PreAuthorize("hasAuthority('Coordinador')")
    @GetMapping("mensual/opciones")
    public ResponseEntity<OpcionesReporte> opciones() {
        PeticionLogger.log(LOGGER, "GET", "/api/v2/reportes/mensual/opciones", "");
        return ResponseEntity.ok(servicio.opciones());
    }
}
