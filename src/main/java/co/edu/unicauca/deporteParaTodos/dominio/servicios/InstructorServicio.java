package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInstructorGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPlazoCargaAtencionesGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProrrogaRegistroAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoAvisoRegistro;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoRegistroMensual;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PeriodoAcademico;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PlazoCargaAtenciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.VentanaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.dominio.utils.LogSanitizer;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InstructorDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PlazoCargaDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ProrrogaDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AvisoRegistroDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.EstadoRegistroDto;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ErrorInternoException;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.MesFuturoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.PlazoCargaInvalidoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ProrrogaMesFuturoExcepcion;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.PerfilMapper;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;

@Service
public class InstructorServicio implements IInstructorServicio {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstructorServicio.class);

    @Autowired
    private IInstructorGateway instructorsGateway;

    @Autowired
    private IPerfilGateway perfilGateway;

    @Autowired
    private IPlazoCargaAtencionesGateway plazoCargaGateway;

    @Autowired
    private IProrrogaRegistroAsistenciaGateway prorrogaGateway;

    @Autowired
    private IGrupoGateway grupoGateway;

    @Autowired
    private IClaseGateway claseGateway;

    @Autowired
    private Clock clock;

    @Override
    public List<InstructorDto> obtenerInstructores() {
        List<Instructor> listInstructors = instructorsGateway.obtenerInstructores();
        if (listInstructors.isEmpty()) {
            throw new ListadoVacioExcepcion("No se encuentran instructores registrados");
        }
        List<InstructorDto> listaDtos = new ArrayList<>();
        listInstructors.forEach(modelo -> {
            InstructorDto dto = InstructorDto.fabricarDeModelo(modelo);
            listaDtos.add(dto);
        });
        return listaDtos;
    }

    @Override
    public Instructor insertarInstructor(Instructor datosInstructor) {
        return instructorsGateway.insertarInstructor(datosInstructor);
    }

    @Override
    public InstructorDto obtenerInstructor(String instructorId) {
        Instructor modelo = instructorsGateway.obtenerInstructor(instructorId).orElseThrow(
                () -> new NoExisteExcepcion("No existe el instructor con el identificador " + instructorId));
        return InstructorDto.fabricarDeModelo(modelo);
    }

    // SCRUM-79 (HE-14 HU-01): reutiliza perfilGateway.actualizarPerfil (SCRUM-182:
    // saveAndFlush + YaExisteElementoExcepcion en 409 por correo duplicado) -- no
    // se duplica ese mecanismo aqui. existeInstructorActivo() ya distingue en una
    // sola consulta "no existe", "existe pero eliminado" y "existe pero no es
    // instructor" (los 3 casos resultan en 404, sin tocar tbl_perfil).
    @Override
    public Instructor actualizarInstructor(String instructorId, Instructor datosInstructor, String idCoordinador) {
        if (!instructorsGateway.existeInstructorActivo(instructorId)) {
            throw new NoExisteExcepcion("No existe el instructor con el identificador " + instructorId);
        }
        Instructor actual = instructorsGateway.obtenerInstructor(instructorId)
                .orElseThrow(() -> new NoExisteExcepcion("No existe el instructor con el identificador " + instructorId));
        String correoActual = actual.getPerfil() != null ? actual.getPerfil().getCorreo() : null;
        String nombreNuevo = datosInstructor.getPerfil() != null ? datosInstructor.getPerfil().getNombre() : null;
        String correoNuevo = datosInstructor.getPerfil() != null ? datosInstructor.getPerfil().getCorreo() : null;

        Perfil datosPerfilActualizar = new Perfil();
        datosPerfilActualizar.setNombre(nombreNuevo);
        datosPerfilActualizar.setCorreo(correoNuevo);
        // HALLAZGO (verificacion SCRUM-79/139): el Perfil que RETORNA actualizarPerfil()
        // viene de un mapeo generico de ModelMapper (PerfilEntidad -> Perfil) que pierde
        // nombre/correo/tipoId/imagen -- confirmado con una sonda que mapea una entidad
        // real: solo "sexo" sobrevive el mapeo plano. La fila en BD SI queda correcta
        // (la escritura usa la entidad cargada, no el valor de retorno); el problema es
        // solo el objeto que ESTE metodo devolveria si confiara en el. Por eso se
        // IGNORA el valor de retorno y la respuesta se reconstruye abajo desde `actual`
        // (mapeo manual y confiable de InstructorGateway.mapearEntidadADominio).
        perfilGateway.actualizarPerfil(instructorId, datosPerfilActualizar);

        // Rastro de auditoria: solo si el correo realmente cambia (sin distinguir
        // mayusculas, con trim) -- y solo identificadores en el mensaje, nunca
        // datos personales (correo/nombre).
        // Seguridad (Sonar java:S5145, log forging): NO se usa `instructorId` crudo
        // (viene directo de la variable de ruta del PUT) -- se usa
        // actual.getInst_codigo(), tomado de la entidad YA CARGADA de BD
        // (repoInstructor.findById dentro de obtenerInstructor()), rompiendo el flujo
        // de dato tainted directo HTTP->log. Ademas, por si el propio valor
        // (sea el de BD o el claim del JWT) llegara a contener CR/LF, se sanitiza
        // antes de registrar para que un id/claim con saltos de linea no pueda
        // forjar lineas de log falsas.
        if (correoActual != null && correoNuevo != null
                && !correoActual.trim().equalsIgnoreCase(correoNuevo.trim())) {
            LOGGER.info("correo del instructor {} modificado por {}",
                    LogSanitizer.sanitizar(actual.getInst_codigo()), LogSanitizer.sanitizar(idCoordinador));
        }

        // Respuesta construida desde `actual` (preserva sexo/tipoId/imagen/id reales)
        // con nombre/correo sobreescritos por los valores nuevos -- si actualizarPerfil()
        // no lanzo, ya se sabe que esos dos valores quedaron persistidos tal cual.
        Perfil perfilResultado = actual.getPerfil() != null ? actual.getPerfil() : new Perfil();
        perfilResultado.setId(instructorId);
        perfilResultado.setNombre(nombreNuevo);
        perfilResultado.setCorreo(correoNuevo);

        Instructor resultado = new Instructor();
        resultado.setInst_codigo(instructorId);
        resultado.setPerfil(perfilResultado);
        resultado.setEliminado(actual.getEliminado());
        return resultado;
    }

    @Override
    public Instructor eliminarInstructor(String instructorId) {
        if (!instructorsGateway.existeInstructor(instructorId)) {
            throw new NoExisteExcepcion("No exoste el instructor con el identificador " + instructorId);
        }
        Instructor actual = instructorsGateway.obtenerInstructor(instructorId)
                .orElseThrow(() -> new NoExisteExcepcion("No exoste el instructor con el identificador " + instructorId));
        if (actual.getEliminado() != null && actual.getEliminado() == 1) {
            throw new YaExisteElementoExcepcion("El instructor ya se encuentra eliminado");
        }
        return instructorsGateway.eliminarInstructor(instructorId);
    }

    @Override
    @Transactional
    public InstructorDto registrarInstructor(PerfilDto perfilDto) {
        // Validar que el perfil no exista
        if (instructorsGateway.existeInstructor(perfilDto.getId())) {
            throw new YaExisteElementoExcepcion(
                    "El instructor con la identificación " + perfilDto.getId() + " ya se encuentra registrado");
        }

        // Convertir DTO a modelo de dominio
        Perfil perfil = PerfilMapper.fromDto(perfilDto);

        // Llamar al gateway para registrar el instructor atomicamente
        // (crea perfil + alumno + instructor en una transacción)
        Instructor instructorRegistrado = instructorsGateway.registrarInstructor(perfil, perfilDto.getTipoAlumno());

        if (instructorRegistrado == null) {
            throw new ErrorInternoException("Error al registrar el instructor");
        }

        // Convertir a DTO para respuesta
        InstructorDto respuesta = InstructorDto.fabricarDeModelo(instructorRegistrado);
        if (respuesta == null) {
            throw new ErrorInternoException("Error al convertir el instructor a DTO");
        }

        return respuesta;
    }

    // SCRUM-80 (HE-14 HU-02): un elemento por instructor ACTIVO, para el periodo
    // vigente (Clock inyectado). UNA consulta agrupada para los plazos y OTRA
    // para los conteos de clases-con-asistencia -- nunca una consulta por
    // instructor (evita N+1 con N instructores).
    @Override
    public List<PlazoCargaDto> obtenerPlazosCarga() {
        List<Instructor> activos = instructorsGateway.obtenerInstructores();
        if (activos == null || activos.isEmpty()) {
            return List.of();
        }
        LocalDate hoy = LocalDate.now(clock);
        PeriodoAcademico vigente = PeriodoAcademico.vigente(clock);
        Map<String, PlazoCargaAtenciones> plazosPorInstructor =
                plazoCargaGateway.obtenerPlazosPorPeriodo(vigente.getAnio(), vigente.getPeriodo());
        if (plazosPorInstructor == null) {
            plazosPorInstructor = Map.of();
        }
        Map<String, Integer> conteosPorInstructor =
                plazoCargaGateway.contarClasesConAsistenciaPorInstructor(vigente.getAnio(), vigente.getPeriodo());
        if (conteosPorInstructor == null) {
            conteosPorInstructor = Map.of();
        }

        List<PlazoCargaDto> resultado = new ArrayList<>();
        for (Instructor instructor : activos) {
            String idInstructor = instructor.getInst_codigo();
            PlazoCargaAtenciones plazo = plazosPorInstructor.get(idInstructor);
            if (plazo == null) {
                plazo = new PlazoCargaAtenciones();
                plazo.setIdInstructor(idInstructor);
                plazo.setAnio(vigente.getAnio());
                plazo.setPeriodo(vigente.getPeriodo());
            }
            int clasesConAsistencia = conteosPorInstructor.getOrDefault(idInstructor, 0);
            resultado.add(PlazoCargaDto.fabricarDeModelo(plazo, clasesConAsistencia, hoy));
        }
        return resultado;
    }

    // Upsert: crea o REEMPLAZA el plazo del periodo vigente -- el periodo lo
    // calcula el servidor con el Clock, el cliente solo manda fechaLimite.
    // idCoordinador llega como parametro explicito (claim del JWT en el
    // controller), igual que actualizarInstructor().
    @Override
    public PlazoCargaDto asignarPlazoCarga(String instructorId, LocalDate fechaLimite, String idCoordinador) {
        if (!instructorsGateway.existeInstructorActivo(instructorId)) {
            throw new NoExisteExcepcion("No existe el instructor con el identificador " + instructorId);
        }
        LocalDate hoy = LocalDate.now(clock);
        if (fechaLimite.isBefore(hoy)) {
            throw new PlazoCargaInvalidoExcepcion(
                    "La fecha limite " + fechaLimite + " no puede ser anterior a hoy (" + hoy + ")");
        }
        PeriodoAcademico vigente = PeriodoAcademico.vigente(clock);

        PlazoCargaAtenciones datos = new PlazoCargaAtenciones();
        datos.setIdInstructor(instructorId);
        datos.setAnio(vigente.getAnio());
        datos.setPeriodo(vigente.getPeriodo());
        datos.setFechaLimite(fechaLimite);
        datos.setIdCoordinador(idCoordinador);
        datos.setFechaAsignacion(LocalDateTime.now(clock));

        PlazoCargaAtenciones guardado = plazoCargaGateway.upsertPlazo(datos);

        // Rastro de auditoria: solo identificadores y la fecha -- nunca correos ni
        // nombres. Los 3 valores se sanitizan por politica (Sonar java:S5145, log
        // forging): instructorId/idCoordinador son Strings crudos del cliente;
        // fechaLimite es un LocalDate ya validado/parseado por Jackson (un valor
        // con CR/LF nunca llega a convertirse en LocalDate -- Jackson responde 400
        // antes), pero se sanea igual por consistencia con la politica.
        LOGGER.info("plazo de carga del instructor {} asignado hasta {} por {}",
                LogSanitizer.sanitizar(instructorId), LogSanitizer.sanitizar(String.valueOf(fechaLimite)),
                LogSanitizer.sanitizar(idCoordinador));

        int clasesConAsistencia = plazoCargaGateway
                .contarClasesConAsistenciaPorInstructor(vigente.getAnio(), vigente.getPeriodo())
                .getOrDefault(instructorId, 0);
        return PlazoCargaDto.fabricarDeModelo(guardado, clasesConAsistencia, hoy);
    }

    // SCRUM-80 (rediseno, bloque 1 de 3): prorroga de registro de asistencia
    // por instructor y mes. mes futuro (relativo al Clock) -> 422.
    private void validarMesNoFuturo(int anio, int mes) {
        LocalDate hoy = LocalDate.now(clock);
        if (anio > hoy.getYear() || (anio == hoy.getYear() && mes > hoy.getMonthValue())) {
            throw new ProrrogaMesFuturoExcepcion(
                    "No se puede asignar ni consultar una prorroga para " + anio + "-" + mes
                            + " porque es un mes futuro (hoy: " + hoy + ")");
        }
    }

    @Override
    public List<ProrrogaDto> obtenerProrrogasRegistro(int anio, int mes) {
        validarMesNoFuturo(anio, mes);
        List<ProrrogaDto> resultado = new ArrayList<>();
        prorrogaGateway.obtenerProrrogasPorMes(anio, mes)
                .values()
                .forEach(prorroga -> resultado.add(ProrrogaDto.fabricarDeModelo(prorroga)));
        return resultado;
    }

    // Atomica: primero valida TODOS los ids (deduplicados) antes de tocar
    // cualquier fila -- si alguno no es instructor activo, NoExisteExcepcion y
    // NINGUNA fila se guarda ni se elimina. dias=0 elimina (borrado fisico); el
    // limite de los demas se calcula con VentanaRegistroAsistencia (misma regla
    // de dominio que valida el registro, para que ambos lados coincidan).
    @Override
    public List<ProrrogaDto> asignarProrrogaRegistro(
            List<String> idsInstructores, int anio, int mes, int dias, String idCoordinador) {
        validarMesNoFuturo(anio, mes);

        List<String> idsUnicos = new ArrayList<>(new LinkedHashSet<>(idsInstructores));
        for (String idInstructor : idsUnicos) {
            if (!instructorsGateway.existeInstructorActivo(idInstructor)) {
                throw new NoExisteExcepcion("No existe el instructor con el identificador " + idInstructor);
            }
        }

        LocalDate hoy = LocalDate.now(clock);
        LocalDate finDeMes = VentanaRegistroAsistencia.finDeMes(anio, mes);
        LocalDateTime fechaAsignacion = LocalDateTime.now(clock);

        List<ProrrogaDto> resultado = new ArrayList<>();
        for (String idInstructor : idsUnicos) {
            if (dias == 0) {
                prorrogaGateway.eliminarProrroga(idInstructor, anio, mes);
                ProrrogaRegistroAsistencia eliminada = new ProrrogaRegistroAsistencia();
                eliminada.setIdInstructor(idInstructor);
                eliminada.setDias(0);
                resultado.add(ProrrogaDto.fabricarDeModelo(eliminada));
            } else {
                LocalDate limite = VentanaRegistroAsistencia.calcularLimiteProrroga(finDeMes, hoy, dias);
                ProrrogaRegistroAsistencia datos = new ProrrogaRegistroAsistencia();
                datos.setIdInstructor(idInstructor);
                datos.setAnio(anio);
                datos.setMes(mes);
                datos.setDias(dias);
                datos.setFechaLimite(limite);
                datos.setIdCoordinador(idCoordinador);
                datos.setFechaAsignacion(fechaAsignacion);
                ProrrogaRegistroAsistencia guardado = prorrogaGateway.upsertProrroga(datos);
                resultado.add(ProrrogaDto.fabricarDeModelo(guardado));
            }
            // Rastro de auditoria: solo identificadores (Sonar java:S5145, log forging).
            LOGGER.info("prorroga de registro de asistencia de {} para {}-{} asignada ({} dias) por {}",
                    LogSanitizer.sanitizar(idInstructor), anio, mes, dias, LogSanitizer.sanitizar(idCoordinador));
        }
        return resultado;
    }

    // SCRUM-80 (rediseno, bloque 3 de 4): estado del registro de asistencia de
    // cada instructor ACTIVO para un mes. anio/mes null -> mes actual del Clock.
    // Reutiliza los gateways existentes por instructor (N+1 aceptado, ver PASO 0).
    @Override
    public List<EstadoRegistroDto> obtenerEstadoRegistro(Integer anioParam, Integer mesParam) {
        LocalDate hoy = LocalDate.now(clock);
        int anio = anioParam != null ? anioParam : hoy.getYear();
        int mes = mesParam != null ? mesParam : hoy.getMonthValue();
        if (anio > hoy.getYear() || (anio == hoy.getYear() && mes > hoy.getMonthValue())) {
            throw new MesFuturoExcepcion(
                    "No se puede consultar el estado de registro de " + anio + "-" + mes
                            + " porque es un mes futuro (hoy: " + hoy + ")");
        }

        List<Instructor> instructoresActivos = instructorsGateway.obtenerInstructores();
        Map<String, ProrrogaRegistroAsistencia> prorrogasPorInstructor =
                prorrogaGateway.obtenerProrrogasPorMes(anio, mes);

        List<EstadoRegistroDto> resultado = new ArrayList<>();
        for (Instructor instructor : instructoresActivos) {
            String idInstructor = instructor.getInst_codigo();
            ConteoGrupos conteo = contarGruposDelMes(idInstructor, anio, mes);
            ProrrogaRegistroAsistencia prorroga = prorrogasPorInstructor.get(idInstructor);
            LocalDate fechaLimite = EstadoRegistroMensual.fechaLimiteEfectiva(
                    anio, mes, prorroga != null ? prorroga.getFechaLimite() : null);
            EstadoRegistroMensual estado = EstadoRegistroMensual.calcular(
                    conteo.activos, conteo.registrados, fechaLimite, hoy);
            resultado.add(EstadoRegistroDto.fabricarDeModelo(instructor, anio, mes, estado,
                    conteo.activos, conteo.registrados, fechaLimite, hoy));
        }
        return resultado;
    }

    // Avisos del MES ACTUAL y del ANTERIOR: solo cuando falta registrar algun
    // grupo activo y el plazo esta por vencer (<=3 dias) o ya vencio. El mes
    // anterior interesa tipicamente cuando hubo prorroga (o quedo vencido).
    @Override
    public List<AvisoRegistroDto> obtenerAvisosRegistro(String idInstructor) {
        if (idInstructor == null || idInstructor.isBlank()
                || !instructorsGateway.existeInstructorActivo(idInstructor)) {
            throw new NoExisteExcepcion("No existe el instructor con el identificador " + idInstructor);
        }
        LocalDate hoy = LocalDate.now(clock);
        LocalDate primerDiaMesActual = hoy.withDayOfMonth(1);

        List<AvisoRegistroDto> resultado = new ArrayList<>();
        for (LocalDate referencia : List.of(primerDiaMesActual, primerDiaMesActual.minusMonths(1))) {
            int anio = referencia.getYear();
            int mes = referencia.getMonthValue();
            ConteoGrupos conteo = contarGruposDelMes(idInstructor, anio, mes);
            ProrrogaRegistroAsistencia prorroga = prorrogaGateway.obtenerProrrogasPorMes(anio, mes)
                    .get(idInstructor);
            LocalDate fechaLimite = EstadoRegistroMensual.fechaLimiteEfectiva(
                    anio, mes, prorroga != null ? prorroga.getFechaLimite() : null);
            EstadoAvisoRegistro.calcular(conteo.activos, conteo.registrados, fechaLimite, hoy)
                    .ifPresent(estado -> resultado.add(
                            AvisoRegistroDto.fabricarDeModelo(anio, mes, estado, fechaLimite, hoy)));
        }
        return resultado;
    }

    private ConteoGrupos contarGruposDelMes(String idInstructor, int anio, int mes) {
        int activos = 0;
        int registrados = 0;
        for (Grupo grupo : grupoGateway.obtenerGruposInstructor(idInstructor)) {
            if (!EstadoRegistroMensual.grupoActivoEnMes(
                    grupo.getFechaCreacion(), grupo.getFechaFinalizacion(), anio, mes)) {
                continue;
            }
            activos++;
            if (tieneClaseEnMes(grupo, anio, mes)) {
                registrados++;
            }
        }
        return new ConteoGrupos(activos, registrados);
    }

    private boolean tieneClaseEnMes(Grupo grupo, int anio, int mes) {
        for (Clase clase : claseGateway.obtenerClasesGrupo(
                grupo.getCategoria(), grupo.getCurso(), grupo.getAnio(), grupo.getIterable())) {
            if (EstadoRegistroMensual.claseDentroDelMes(clase.getFecha(), anio, mes)) {
                return true;
            }
        }
        return false;
    }

    private static final class ConteoGrupos {
        private final int activos;
        private final int registrados;

        private ConteoGrupos(int activos, int registrados) {
            this.activos = activos;
            this.registrados = registrados;
        }
    }

}
