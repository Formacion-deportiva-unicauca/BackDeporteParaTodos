package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import java.time.LocalDate;
import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;

public interface IClaseServicio {
    public List<Clase> obtenerClasesGrupo(String categoria, String curso, Integer anio, Integer iterable);

    public boolean existeClaseEnFecha(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha);

    // SCRUM-80 (rediseno, bloque 2 de 3): perfIdLlamador/rolLlamador salen del
    // JWT en el controller (el dominio no conoce Spring ni el JWT) -- se usan
    // para aplicar VentanaRegistroAsistencia con el perf_id de QUIEN LLAMA
    // (no el instructor asignado al grupo) y para eximir a roles distintos de
    // Instructor del limite de plazo (pero no de la regla de fecha futura).
    public Clase insertarClase(Clase datoClase, String perfIdLlamador, String rolLlamador);

    public Clase eliminarClase(int id, String perfIdLlamador, String rolLlamador);
}
