package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import java.util.List;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.OpcionesReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ReporteMensual;

public interface IReporteServicio {

    /**
     * Reporte del mes. Filtros nulos o vacios = sin filtro. vinculos admite Estudiante, Docente, Administrativo, Contratista y
     * "Funcionarios" (= Docente + Administrativo + Contratista).
     */
    ReporteMensual mensual(int anio, int mes, String disciplina, String modalidad, String publico, List<String> vinculos,
            String genero, String facultad, boolean agruparFuncionarios);

    OpcionesReporte opciones();
}
