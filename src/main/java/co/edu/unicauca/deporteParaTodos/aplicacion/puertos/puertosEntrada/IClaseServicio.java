package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import java.time.LocalDate;
import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;

public interface IClaseServicio {
    public List<Clase> obtenerClasesGrupo(String categoria, String curso, Integer anio, Integer iterable);

    public boolean existeClaseEnFecha(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha);

    public Clase insertarClase(Clase datoClase);

    public Clase eliminarClase(int id);
}
