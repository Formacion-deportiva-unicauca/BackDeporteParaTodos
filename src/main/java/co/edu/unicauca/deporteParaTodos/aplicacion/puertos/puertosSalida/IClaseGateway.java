package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import java.time.LocalDate;
import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;

public interface IClaseGateway {
    public boolean existeClase(int id);

    public boolean existeClaseEnFecha(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha);

    public Clase obtenerClaseEnFecha(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha);

    public List<Clase> obtenerClasesGrupo(String categoria, String curso, Integer anio, Integer iterable);

    public Clase insertarClase(Clase datoClase);

    public Clase eliminarClase(int id);
}
