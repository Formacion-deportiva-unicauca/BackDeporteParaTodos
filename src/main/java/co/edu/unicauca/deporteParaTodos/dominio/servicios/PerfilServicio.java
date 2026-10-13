package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IPerfilServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.InsercionFallidaExepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;

@Service
public class PerfilServicio implements IPerfilServicio {

    @Autowired
    private IPerfilGateway perfilGateway;

    @Override
    public List<Perfil> obtenerPerfiles() {
        List<Perfil> listaPerfiles = perfilGateway.obtenerPerfiles();
        if (listaPerfiles.isEmpty()) {
            throw new ListadoVacioExcepcion("No hay perfiles registrados");
        }
        return listaPerfiles;
    }

    @Override
    public Perfil insertarPerfil(Perfil perfil) {
        Perfil perfilInsertado = perfilGateway.insertarPerfil(perfil);
        if (perfilInsertado == null) {
            throw new InsercionFallidaExepcion("No se pudo realizar la insersion");
        }
        return perfilInsertado;
    }

    @Override
    public Perfil obtenerPerfil(String perfilId) {
        return perfilGateway.obtenerPerfil(perfilId)
                .orElseThrow(() -> new NoExisteExcepcion("No existe el perfil con el identificador " + perfilId));
    }

    @Override
    public Perfil actualizarPerfil(String perfilId, Perfil datosPerfil) {
       // SonarCloud java:S2789: Optional NUNCA es null por contrato -- el guard
       // "== null" era codigo muerto que nunca protegia nada; un perfil
       // inexistente (Optional.empty()) pasaba de largo hasta el gateway.
       Optional<Perfil> perfilExistente = perfilGateway.obtenerPerfil(perfilId);
       if (perfilExistente.isEmpty()) {
        throw new NoExisteExcepcion("No existe el perfil con el identificador " + perfilId);
       }
       return perfilGateway.actualizarPerfil(perfilId, datosPerfil);
    }

    @Override
    public Perfil eliminarPerfil(String perfilId) {
       if(!perfilGateway.existePerfil(perfilId)){
        throw new NoExisteExcepcion("No existe el perfil con el identificador " + perfilId);
       }
       return perfilGateway.eliminarPerfil(perfilId);
    }

}
