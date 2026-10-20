package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import java.time.LocalDate;
import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;

public interface IClaseGateway {
    public boolean existeClase(int id);

    public boolean existeClaseEnFecha(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha);

    public Clase obtenerClaseEnFecha(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha);

    // SCRUM-80 (rediseno, bloque 2): lee la clase por su codigo -- se usa para
    // conocer su fecha y aplicar VentanaRegistroAsistencia desde
    // AsistenciaServicio/ClaseServicio.eliminarClase().
    public Clase obtenerClase(int id);

    public List<Clase> obtenerClasesGrupo(String categoria, String curso, Integer anio, Integer iterable);

    public Clase insertarClase(Clase datoClase);

    public Clase eliminarClase(int id);
}
