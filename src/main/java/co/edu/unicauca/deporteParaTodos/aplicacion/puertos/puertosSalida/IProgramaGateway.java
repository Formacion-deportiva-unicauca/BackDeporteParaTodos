package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import java.util.List;
import java.util.Optional;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Programa;

public interface IProgramaGateway {

    public boolean existePrograma(String nombrePrograma);

    public List<Programa> obtenerProgramas();

    public List<Programa> obtenerProgramasPorFacultad(String nombreFacultad);

    public Optional<Programa> obtenerPrograma(String nombrePrograma);

    public Programa insertarPrograma(Programa datosPrograma);

    public Programa actualizarPrograma(Programa datosPrograma);

    public Programa eliminarPrograma(String nombrePrograma);

}
