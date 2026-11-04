package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAutenticacionServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProgramaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Programa;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DatosInvalidosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.PerfilMapper;

@Service
public class AutenticacionServicio implements IAutenticacionServicio{

    private static final String ESTUDIANTE = "Estudiante";
    private static final String DOCENTE = "Docente";
    private static final String ADMINISTRATIVO = "Administrativo";
    private static final String CONTRATISTA = "Contratista";

    @Autowired
    private IPerfilGateway gatePerfil;

    @Autowired
    private IProgramaGateway programaGateway;

    @Override
    public PerfilDto login(String email) {
        Perfil perfil = gatePerfil.obtenerUsuario(email);
        if(perfil==null){
            throw new NoExisteExcepcion();
        }
        return PerfilMapper.toDto(perfil);
    }

    @Override
    public PerfilDto registrarAlumno(PerfilDto datosPerfil) {
        if (gatePerfil.existePerfil(datosPerfil.getId())) {
            throw new YaExisteElementoExcepcion("El perfil con la identificacion ya se encuentra registrado");
        }
        Perfil perfil = PerfilMapper.fromDto(datosPerfil);
        aplicarReglasPorTipo(perfil);
        Perfil perfilRegistrado = gatePerfil.registrarAlumno(perfil);
        return PerfilMapper.toDto(perfilRegistrado);
    }

    

    // Datos que pide cada tipo de vinculo (el formulario de Funcionarios no pide codigo a docentes,
    // administrativos ni contratistas). Lo que no corresponde al tipo se descarta para no guardar datos ajenos.
    //   Estudiante:                codigo + facultad + programa
    //   Docente:                   facultad + programa
    //   Administrativo/Contratista: dependencia
    private void aplicarReglasPorTipo(Perfil perfil) {
        String tipo = perfil.getTipoAlumno();
        if (tipo == null) {
            throw rechazo("El tipo de vinculo es obligatorio");
        }
        switch (tipo) {
            case ESTUDIANTE -> {
                perfil.setAlumnoCodigo(exigir(perfil.getAlumnoCodigo(), "El codigo institucional es obligatorio para estudiantes"));
                validarFacultadYPrograma(perfil);
                perfil.setDependencia(null);
            }
            case DOCENTE -> {
                validarFacultadYPrograma(perfil);
                perfil.setAlumnoCodigo(null);
                perfil.setDependencia(null);
            }
            case ADMINISTRATIVO, CONTRATISTA -> {
                perfil.setDependencia(exigir(perfil.getDependencia(), "La dependencia es obligatoria para administrativos y contratistas"));
                perfil.setAlumnoCodigo(null);
                perfil.setPrograma(null);
                perfil.setFacultad(null);
            }
            default -> throw rechazo("El tipo de vinculo debe ser Estudiante, Docente, Administrativo o Contratista");
        }
    }

    private void validarFacultadYPrograma(Perfil perfil) {
        String facultad = exigir(perfil.getFacultad(), "La facultad es obligatoria");
        String nombrePrograma = exigir(perfil.getPrograma(), "El programa academico es obligatorio");
        Programa programa = programaGateway.obtenerPrograma(nombrePrograma)
                .orElseThrow(() -> rechazo("El programa " + nombrePrograma + " no existe"));
        if (programa.getFacultad() == null || !facultad.equals(programa.getFacultad().getNombre())) {
            throw rechazo("El programa " + nombrePrograma + " no pertenece a la facultad " + facultad);
        }
        perfil.setFacultad(facultad);
        perfil.setPrograma(nombrePrograma);
    }

    private static String exigir(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw rechazo(mensaje);
        }
        return valor.trim();
    }

    private static DatosInvalidosExcepcion rechazo(String mensaje) {
        return new DatosInvalidosExcepcion(mensaje);
    }

    //El logOut es implementado por google internamente, no es necesario hacerlo aqui
    
}
