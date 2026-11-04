package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fase 4: fila del tablero del Coordinador con la ocupacion y la ventana de inscripcion de un grupo. */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OcupacionGrupo {

    private String categoria;
    private String curso;
    private int anio;
    private int iterable;
    private int periodo;
    private PublicoCurso publico;
    private Integer cupos;
    private int inscritos;
    private int enEspera;
    private NivelOcupacion nivel;
    private EstadoVentanaInscripcion estadoInscripcion;
    private boolean cerradaManual;
    private LocalDate fechaApertura;
    private LocalDate fechaCierre;
}
