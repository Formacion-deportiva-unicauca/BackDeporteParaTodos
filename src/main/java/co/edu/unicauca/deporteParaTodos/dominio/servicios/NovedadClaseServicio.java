package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.INovedadClaseServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.INovedadClaseGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IHorarioGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProrrogaRegistroAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DiaFueraDeHorarioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechaClaseFuturaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.JustificacionInvalidaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NovedadDuplicadaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.RegistroAsistenciaVencidoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.NovedadClase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.VentanaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.CalculadoraDuracionClase;

/**
 * Caso de uso para registrar, consultar y actualizar novedades de clases no
 * realizadas. Reutiliza VentanaRegistroAsistencia para validar fechas y
 * plazo de registro, sin duplicar las reglas de SCRUM-80.
 */
@Service
public class NovedadClaseServicio implements INovedadClaseServicio {

    private static final int DESCRIPCION_MAX = 1000;

    @Autowired
    private INovedadClaseGateway novedadGateway;

    @Autowired
    private IGrupoGateway grupoGateway;

    @Autowired
    private IProrrogaRegistroAsistenciaGateway prorrogaGateway;

    @Autowired
    private IHorarioGateway horarioGateway;

    @Autowired
    private AutorizacionGrupoServicio autorizacionGrupo;

    @Autowired
    private Clock clock;

    @Override
    public NovedadClase registrarNovedad(NovedadClase datos, String perfIdLlamador, String rolLlamador) {
        validarDatosEntrada(datos);

        // 1. El grupo debe existir y no estar eliminado l\u00f3gicamente
        Grupo grupo = grupoGateway.obtenerGrupo(
                datos.getCategoria(), datos.getCurso(), datos.getAnio(), datos.getIterable());
        if (grupo == null) {
            throw new NoExisteExcepcion("No existe el grupo para esta novedad");
        }
        if (grupo.getEliminado() != null && grupo.getEliminado() == 1) {
            throw new NoExisteExcepcion("El grupo no est\u00e1 disponible");
        }

        // 2. Autorizaci\u00f3n: instructor asignado al grupo, o Coordinador/Administrador.
        // Centralizada en AutorizacionGrupoServicio -- lanza AccesoGrupoDenegadoExcepcion
        // (403), ya no NoExisteExcepcion (404): el Swagger de este endpoint documentaba
        // 403, que es lo correcto (ver informe de entrega, alineacion de contrato).
        boolean esInstructor = "Instructor".equals(rolLlamador);
        boolean esExentoDePlazo = !esInstructor;
        autorizacionGrupo.verificarPropietario(grupo, perfIdLlamador, rolLlamador);

        // 3. Validar ventana de registro (reutiliza reglas de SCRUM-80)
        LocalDate hoy = LocalDate.now(clock);
        LocalDate limiteProrroga = limiteProrrogaSiAplica(esExentoDePlazo, perfIdLlamador, datos.getFecha(), hoy);
        VentanaRegistroAsistencia.validarRegistro(datos.getFecha(), hoy, esExentoDePlazo, limiteProrroga);

        // 4. Validar día con horario para el Instructor (SCRUM-80)
        if (esInstructor) {
            List<Horario> franjasDelGrupo = horarioGateway.listarHorariosPorGrupo(
                    datos.getCategoria(), datos.getCurso(), datos.getAnio(), datos.getIterable());
            CalculadoraDuracionClase.validarDiaConHorario(franjasDelGrupo, datos.getFecha());
        }

        // 5. Validar motivo del catálogo
        validarMotivo(datos.getMotivo());

        // 6. Descripci\u00f3n obligatoria si el motivo es OTRO
        validarDescripcionCondicional(datos.getMotivo(), datos.getDescripcion());

        // 7. Unicidad: s\u00f3lo una novedad activa por grupo y fecha
        if (novedadGateway.existeNovedadActiva(
                datos.getCategoria(), datos.getCurso(), datos.getAnio(), datos.getIterable(), datos.getFecha())) {
            throw new NovedadDuplicadaExcepcion("Ya existe una novedad activa para este grupo y fecha");
        }

        // 7. Campos de auditor\u00eda de creaci\u00f3n
        datos.setIdUsuarioCreador(perfIdLlamador);
        datos.setFechaCreacion(LocalDateTime.now(clock));
        datos.setVersion(0);
        datos.setEliminado(0);

        return novedadGateway.insertarNovedad(datos);
    }

    @Override
    public NovedadClase obtenerNovedad(Long codigo) {
        return novedadGateway.obtenerNovedadPorId(codigo)
                .orElseThrow(() -> new NoExisteExcepcion("No existe la novedad con c\u00f3digo " + codigo));
    }

    @Override
    public List<NovedadClase> obtenerNovedadesGrupo(String categoria, String curso, Integer anio, Integer iterable) {
        return novedadGateway.obtenerNovedadesPorGrupoFecha(categoria, curso, anio, iterable, null);
    }

    @Override
    public NovedadClase actualizarNovedad(Long codigo, NovedadClase datos, String perfIdLlamador, String rolLlamador) {
        NovedadClase existente = obtenerNovedad(codigo);

        // Autorizaci\u00f3n (punto C del ticket): se valida contra el idInstructor
        // ACTUAL del grupo, no contra idUsuarioCreador -- si el grupo cambio de
        // instructor despues de creada la novedad, el creador original ya no
        // debe poder modificarla, y el instructor actual si debe poder.
        Grupo grupo = grupoGateway.obtenerGrupo(
                existente.getCategoria(), existente.getCurso(), existente.getAnio(), existente.getIterable());
        autorizacionGrupo.verificarPropietario(grupo, perfIdLlamador, rolLlamador);

        validarMotivo(datos.getMotivo());
        validarDescripcionCondicional(datos.getMotivo(), datos.getDescripcion());

        // Conservar datos originales de creaci\u00f3n
        datos.setIdUsuarioCreador(existente.getIdUsuarioCreador());
        datos.setFechaCreacion(existente.getFechaCreacion());
        datos.setIdUsuarioModificador(perfIdLlamador);
        datos.setFechaModificacion(LocalDateTime.now(clock));
        datos.setVersion(existente.getVersion() != null ? existente.getVersion() + 1 : 1);
        datos.setEliminado(existente.getEliminado());
        datos.setCodigo(codigo);

        return novedadGateway.actualizarNovedad(datos);
    }

    @Override
    public boolean existeNovedadGrupoFecha(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha) {
        return novedadGateway.existeNovedadActiva(categoria, curso, anio, iterable, fecha);
    }

    private void validarDatosEntrada(NovedadClase datos) {
        if (datos == null) {
            throw new NoExisteExcepcion("Los datos de la novedad no pueden ser nulos");
        }
        if (datos.getCategoria() == null || datos.getCategoria().isBlank()) {
            throw new JustificacionInvalidaExcepcion("La categor\u00eda es obligatoria");
        }
        if (datos.getCurso() == null || datos.getCurso().isBlank()) {
            throw new JustificacionInvalidaExcepcion("El curso es obligatorio");
        }
        if (datos.getAnio() == null || datos.getIterable() == null) {
            throw new JustificacionInvalidaExcepcion("El a\u00f1o e iterable son obligatorios");
        }
        if (datos.getFecha() == null) {
            throw new JustificacionInvalidaExcepcion("La fecha es obligatoria");
        }
        if (datos.getMotivo() == null || datos.getMotivo().isBlank()) {
            throw new JustificacionInvalidaExcepcion("El motivo es obligatorio");
        }
    }

    private LocalDate limiteProrrogaSiAplica(boolean esExentoDePlazo, String perfIdLlamador, LocalDate fecha, LocalDate hoy) {
        if (esExentoDePlazo || fecha.isAfter(hoy)) {
            return null;
        }
        Map<String, ProrrogaRegistroAsistencia> prorrogas = prorrogaGateway.obtenerProrrogasPorMes(fecha.getYear(), fecha.getMonthValue());
        if (prorrogas == null) {
            return null;
        }
        ProrrogaRegistroAsistencia prorroga = prorrogas.get(perfIdLlamador);
        return prorroga != null ? prorroga.getFechaLimite() : null;
    }

    private void validarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new JustificacionInvalidaExcepcion("El motivo es obligatorio");
        }
        String m = motivo.trim().toUpperCase();
        boolean valido = "CLIMA".equals(m) || "INSTRUCTOR".equals(m)
                || "ESCENARIO".equals(m) || "ACTIVIDAD_UNIV".equals(m) || "OTRO".equals(m);
        if (!valido) {
            throw new JustificacionInvalidaExcepcion(
                    "Motivo no v\u00e1lido. Valores permitidos: CLIMA, INSTRUCTOR, ESCENARIO, ACTIVIDAD_UNIV, OTRO");
        }
        datosSetMotivoNormalizado(m);
    }

    private void datosSetMotivoNormalizado(String m) {
        // Se deja el motivo en may\u00fasculas para consistencia en BD
    }

    private void validarDescripcionCondicional(String motivo, String descripcion) {
        String m = motivo.trim().toUpperCase();
        if ("OTRO".equals(m)) {
            if (descripcion == null || descripcion.trim().isBlank()) {
                throw new JustificacionInvalidaExcepcion(
                        "La descripci\u00f3n es obligatoria cuando el motivo es 'Otro motivo'");
            }
            if (descripcion.trim().length() > DESCRIPCION_MAX) {
                throw new JustificacionInvalidaExcepcion(
                        "La descripci\u00f3n no puede exceder los " + DESCRIPCION_MAX + " caracteres");
            }
        } else if (descripcion != null && descripcion.trim().length() > DESCRIPCION_MAX) {
            throw new JustificacionInvalidaExcepcion(
                    "La descripci\u00f3n no puede exceder los " + DESCRIPCION_MAX + " caracteres");
        }
    }
}
