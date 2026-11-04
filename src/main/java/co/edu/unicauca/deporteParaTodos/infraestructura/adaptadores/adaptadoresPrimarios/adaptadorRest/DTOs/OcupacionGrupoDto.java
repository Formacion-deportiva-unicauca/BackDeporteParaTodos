package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import java.time.LocalDate;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoVentanaInscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.NivelOcupacion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.OcupacionGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PublicoCurso;
import lombok.Getter;
import lombok.Setter;

// Fase 4: fila del tablero de ocupacion del Coordinador (solo lectura).
@Getter
@Setter
public class OcupacionGrupoDto {

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

    public static OcupacionGrupoDto fabricarDeModelo(OcupacionGrupo o) {
        OcupacionGrupoDto dto = new OcupacionGrupoDto();
        dto.categoria = o.getCategoria();
        dto.curso = o.getCurso();
        dto.anio = o.getAnio();
        dto.iterable = o.getIterable();
        dto.periodo = o.getPeriodo();
        dto.publico = o.getPublico();
        dto.cupos = o.getCupos();
        dto.inscritos = o.getInscritos();
        dto.enEspera = o.getEnEspera();
        dto.nivel = o.getNivel();
        dto.estadoInscripcion = o.getEstadoInscripcion();
        dto.cerradaManual = o.isCerradaManual();
        dto.fechaApertura = o.getFechaApertura();
        dto.fechaCierre = o.getFechaCierre();
        return dto;
    }
}
