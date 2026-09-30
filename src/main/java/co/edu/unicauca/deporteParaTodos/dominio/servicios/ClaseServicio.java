package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IClaseServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;

@Service
public class ClaseServicio implements IClaseServicio {

    @Autowired
    private IClaseGateway claseGateway;

    @Override
    public List<Clase> obtenerClasesGrupo(String categoria, String curso, Integer anio, Integer iterable) {
        List<Clase> listado = claseGateway.obtenerClasesGrupo(categoria, curso, anio, iterable);
        if (listado.isEmpty()) {
            throw new ListadoVacioExcepcion("No existen registros");
        }
        return listado;
    }

    @Override
    public boolean existeClaseEnFecha(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha) {
        return claseGateway.existeClaseEnFecha(categoria, curso, anio, iterable, fecha);
    }

    @Override
    public Clase insertarClase(Clase datoClase) {
        // El Instructor registra asistencia alumno por alumno, con un click por alumno
        // en momentos distintos durante la misma clase -- cada click repite el flujo
        // completo (crear clase + registrar atencion). Si ya existe una clase de hoy
        // para este grupo, se reutiliza en vez de rechazar o crear una segunda
        // (ver ClaseFechaDuplicadaIT).
        if (claseGateway.existeClaseEnFecha(datoClase.getCategoria(), datoClase.getCurso(),
                datoClase.getAnio(), datoClase.getIterable(), datoClase.getFecha())) {
            return claseGateway.obtenerClaseEnFecha(datoClase.getCategoria(), datoClase.getCurso(),
                    datoClase.getAnio(), datoClase.getIterable(), datoClase.getFecha());
        }
        return claseGateway.insertarClase(datoClase);
    }

    @Override
    public Clase eliminarClase(int id) {
        if (!claseGateway.existeClase(id)) {
            throw new NoExisteExcepcion();
        }
        return claseGateway.eliminarClase(id);
    }
}
