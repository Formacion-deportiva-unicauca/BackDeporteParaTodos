package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.NovedadClase;

public interface INovedadClaseGateway {

    NovedadClase insertarNovedad(NovedadClase datos);

    Optional<NovedadClase> obtenerNovedadPorId(Long codigo);

    List<NovedadClase> obtenerNovedadesPorGrupoFecha(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha);

    NovedadClase actualizarNovedad(NovedadClase datos);

    boolean existeNovedadActiva(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha);
}
