package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.util.List;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IProgramaServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProgramaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Programa;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DatosInvalidosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;

@Service
public class ProgramaServicio implements IProgramaServicio {

    private final IProgramaGateway programaGateway;

    public ProgramaServicio(IProgramaGateway programaGateway) {
        this.programaGateway = programaGateway;
    }

    @Override
    public List<Programa> obtenerProgramas() {
        List<Programa> lista = programaGateway.obtenerProgramas();
        if (lista.isEmpty()) {
            throw new ListadoVacioExcepcion("No hay programas registrados");
        }
        return lista;
    }

    @Override
    public List<Programa> obtenerProgramasPorFacultad(String nombreFacultad) {
        if (nombreFacultad == null || nombreFacultad.isBlank()) {
            throw new DatosInvalidosExcepcion("La facultad es obligatoria para listar sus programas");
        }
        List<Programa> lista = programaGateway.obtenerProgramasPorFacultad(nombreFacultad);
        if (lista.isEmpty()) {
            throw new ListadoVacioExcepcion("No hay programas registrados para la facultad " + nombreFacultad);
        }
        return lista;
    }

    @Override
    public Programa obtenerPrograma(String nombrePrograma) {
        return programaGateway.obtenerPrograma(nombrePrograma)
                .orElseThrow(() -> new NoExisteExcepcion("No existe el programa con el nombre " + nombrePrograma));
    }

    @Override
    public Programa insertarPrograma(Programa datosPrograma) {
        if (programaGateway.existePrograma(datosPrograma.getPrg_nombre())) {
            throw new YaExisteElementoExcepcion("El programa ya está registrado");
        }
        return programaGateway.insertarPrograma(datosPrograma);
    }

    @Override
    public Programa actualizarPrograma(Programa datosPrograma) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'actualizarPrograma'");
    }

    @Override
    public Programa eliminarPrograma(String nombrePrograma) {
        if (!programaGateway.existePrograma(nombrePrograma)) {
            throw new NoExisteExcepcion("No existe el programa con el nombre " + nombrePrograma);
        }
        return programaGateway.eliminarPrograma(nombrePrograma);
    }

}
