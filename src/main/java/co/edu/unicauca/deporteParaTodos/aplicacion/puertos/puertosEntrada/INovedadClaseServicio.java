package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import java.time.LocalDate;
import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.NovedadClase;

public interface INovedadClaseServicio {

    NovedadClase registrarNovedad(NovedadClase datos, String perfIdLlamador, String rolLlamador);

    NovedadClase obtenerNovedad(Long codigo);

    List<NovedadClase> obtenerNovedadesGrupo(String categoria, String curso, Integer anio, Integer iterable);

    NovedadClase actualizarNovedad(Long codigo, NovedadClase datos, String perfIdLlamador, String rolLlamador);

    boolean existeNovedadGrupoFecha(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha);
}
