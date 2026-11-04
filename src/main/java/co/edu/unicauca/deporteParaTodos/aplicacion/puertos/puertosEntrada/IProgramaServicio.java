package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Programa;

public interface IProgramaServicio {

    public List<Programa> obtenerProgramas();

    public List<Programa> obtenerProgramasPorFacultad(String nombreFacultad);

    public Programa obtenerPrograma(String nombrePrograma);

    public Programa insertarPrograma(Programa datosPrograma);

    public Programa actualizarPrograma(Programa datosPrograma);

    public Programa eliminarPrograma(String nombrePrograma);

}
