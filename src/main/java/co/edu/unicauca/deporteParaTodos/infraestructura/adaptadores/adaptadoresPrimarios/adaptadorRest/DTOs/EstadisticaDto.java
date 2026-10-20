package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EstadisticaDto {
    ///titulo de la estadistica
    private String leyenda1;
    private String leyenda2;
    private String leyenda3;
    private String leyenda4;
    ///cantidad de clases
    private double clases;
    ///cantidad de horas
    private double horas;
    ///minutos excedentes de horas
    private double minutos;
    ///duracion total en minutos
    private double duracion;

    public static EstadisticaDto fromObjectCategorias(Object[] objects){
        EstadisticaDto estadistica = new EstadisticaDto();
        estadistica.setLeyenda1(objects[0].toString());
        estadistica.setClases(Double.parseDouble(objects[1].toString()));
        estadistica.setHoras(Double.parseDouble(objects[2].toString()));
        estadistica.setMinutos(Double.parseDouble(objects[3].toString()));
        estadistica.setDuracion(Double.parseDouble(objects[4].toString()));
        return estadistica;
    }

    public static EstadisticaDto fromObjectAlumno(Object[] objects){
        EstadisticaDto estadistica = new EstadisticaDto();
        estadistica.setLeyenda1(objects[0].toString());
        estadistica.setLeyenda2(objects[1].toString());
        estadistica.setClases(Double.parseDouble(objects[2].toString()));
        estadistica.setHoras(Double.parseDouble(objects[3].toString()));
        estadistica.setMinutos(Double.parseDouble(objects[4].toString()));
        estadistica.setDuracion(Double.parseDouble(objects[5].toString()));
        return estadistica;
    }

    public static EstadisticaDto fromObjectInstructor(Object[] objects){
        EstadisticaDto estadistica = new EstadisticaDto();
        estadistica.setLeyenda1(objects[0].toString());
        estadistica.setLeyenda2(objects[1].toString());
        estadistica.setClases(Double.parseDouble(objects[2].toString()));
        estadistica.setHoras(Double.parseDouble(objects[3].toString()));
        estadistica.setMinutos(Double.parseDouble(objects[4].toString()));
        estadistica.setDuracion(Double.parseDouble(objects[5].toString()));
        return estadistica;
    }

    public static EstadisticaDto fromObjectCursos(Object[] objects){
        EstadisticaDto estadistica = new EstadisticaDto();
        estadistica.setLeyenda1(objects[0].toString());
        estadistica.setLeyenda2(objects[1].toString());
        estadistica.setClases(Double.parseDouble(objects[2].toString()));
        estadistica.setHoras(Double.parseDouble(objects[3].toString()));
        estadistica.setMinutos(Double.parseDouble(objects[4].toString()));
        estadistica.setDuracion(Double.parseDouble(objects[5].toString()));
        return estadistica;
    }

    public static EstadisticaDto fromObjectGrupos(Object[] objects){
        EstadisticaDto estadistica = new EstadisticaDto();
        estadistica.setLeyenda1(objects[0].toString());
        estadistica.setLeyenda2(objects[1].toString());
        estadistica.setLeyenda3(objects[2].toString());
        estadistica.setLeyenda4(objects[3].toString());
        estadistica.setClases(Double.parseDouble(objects[4].toString()));
        estadistica.setHoras(Double.parseDouble(objects[5].toString()));
        estadistica.setMinutos(Double.parseDouble(objects[6].toString()));
        estadistica.setDuracion(Double.parseDouble(objects[7].toString()));
        return estadistica;
    }
}
