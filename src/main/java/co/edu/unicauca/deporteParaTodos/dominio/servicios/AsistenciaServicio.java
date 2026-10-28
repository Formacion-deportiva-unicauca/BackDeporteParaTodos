package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAsistenciaServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAlumnoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IHorarioGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProrrogaRegistroAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.CalculadoraDuracionClase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ResultadoConfirmacionAsistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.VentanaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AtencionDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ItemConfirmacionAsistenciaDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.VentanaRegistroDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.AsistenciaMapper;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.AlumnoFueraDeGrupoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ConfirmacionSinAsistentesExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ErrorInternoException;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;

@Service
public class AsistenciaServicio implements IAsistenciaServicio {

    private final IAsistenciaGateway asistenciaGateway;
    private final IAlumnoGateway alumnoGateway;
    private final IClaseGateway claseGateway;
    private final IInscripcionGateway inscripcionGateway;
    private final IProrrogaRegistroAsistenciaGateway prorrogaGateway;
    private final IHorarioGateway horarioGateway;
    private final AutorizacionGrupoServicio autorizacionGrupo;
    private final Clock clock;

    public AsistenciaServicio(IAsistenciaGateway asistenciaGateway, IAlumnoGateway alumnoGateway,
            IClaseGateway claseGateway, IInscripcionGateway inscripcionGateway,
            IProrrogaRegistroAsistenciaGateway prorrogaGateway,
            IHorarioGateway horarioGateway, AutorizacionGrupoServicio autorizacionGrupo, Clock clock) {
        this.asistenciaGateway = asistenciaGateway;
        this.alumnoGateway = alumnoGateway;
        this.claseGateway = claseGateway;
        this.inscripcionGateway = inscripcionGateway;
        this.prorrogaGateway = prorrogaGateway;
        this.horarioGateway = horarioGateway;
        this.autorizacionGrupo = autorizacionGrupo;
        this.clock = clock;
    }

    @Override
    public List<Asistencia> obtenerAsistencias() {
        List<Asistencia> asistencias = asistenciaGateway.obtenerAsistencias();
        if (asistencias == null || asistencias.isEmpty()) {
            throw new ListadoVacioExcepcion("No hay registro de asistencias");
        }
        return asistencias;
    }

    @Override
    public List<AtencionDto> obtenerAtencionesPorClase(Integer claseId) {
        List<Asistencia> asistencias = asistenciaGateway.obtenerAtencionesPorClase(claseId);
        if (asistencias == null || asistencias.isEmpty()) {
            throw new ListadoVacioExcepcion("No hay atenciones registradas para la clase consultada");
        }
        List<AtencionDto> listaDtos = new ArrayList<>();
        asistencias.forEach(modelo -> listaDtos.add(AsistenciaMapper.toDto(modelo)));
        return listaDtos;
    }

    // SCRUM-80 (rediseno, bloque 2): misma regla que ClaseServicio -- optimiza
    // para no consultar la prorroga cuando la fecha ya es futura o el rol
    // esta exento (quien decide/lanza sigue siendo VentanaRegistroAsistencia).
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

    private Clase validarVentanaRegistro(Integer claseId, String perfIdLlamador, String rolLlamador) {
        Clase clase = claseGateway.obtenerClase(claseId);
        autorizacionGrupo.verificarPropietario(clase.getCategoria(), clase.getCurso(),
                clase.getAnio(), clase.getIterable(), perfIdLlamador, rolLlamador);

        boolean esExentoDePlazo = !"Instructor".equals(rolLlamador);
        LocalDate hoy = LocalDate.now(clock);
        LocalDate limiteProrroga = limiteProrrogaSiAplica(esExentoDePlazo, perfIdLlamador, clase.getFecha(), hoy);
        VentanaRegistroAsistencia.validarRegistro(clase.getFecha(), hoy, esExentoDePlazo, limiteProrroga);
        return clase;
    }

    // SCRUM-80 (correccion del cliente, 15 oct 2026) / Faltas acumuladas (etapa
    // 4 de 5): propietario + ventana/plazo + (solo Instructor) dia-en-horario,
    // en ese orden. Compartido entre registrarAtencionesPorClase() y
    // confirmarAsistencias() -- antes cada uno repetia este bloque por
    // separado. NO es el mismo metodo que validarVentanaRegistro() (usado por
    // eliminarAsistencia()): ese no exige dia-en-horario -- una clase fuera de
    // horario creada por un Coordinador no exime al Instructor de registrar
    // asistencia, pero esa regla nunca se aplico a los deletes individuales.
    private Clase validarYAutorizarRegistro(Integer claseId, String perfIdLlamador, String rolLlamador) {
        if (claseId == null || !claseGateway.existeClase(claseId)) {
            throw new NoExisteExcepcion("No existe la clase con codigo " + claseId);
        }
        Clase clase = claseGateway.obtenerClase(claseId);
        autorizacionGrupo.verificarPropietario(clase.getCategoria(), clase.getCurso(),
                clase.getAnio(), clase.getIterable(), perfIdLlamador, rolLlamador);

        boolean esInstructor = "Instructor".equals(rolLlamador);
        boolean esExentoDePlazo = !esInstructor;
        LocalDate hoy = LocalDate.now(clock);
        LocalDate limiteProrroga = limiteProrrogaSiAplica(esExentoDePlazo, perfIdLlamador, clase.getFecha(), hoy);
        VentanaRegistroAsistencia.validarRegistro(clase.getFecha(), hoy, esExentoDePlazo, limiteProrroga);
        if (esInstructor) {
            List<Horario> franjasDelGrupo = horarioGateway.listarHorariosPorGrupo(
                    clase.getCategoria(), clase.getCurso(), clase.getAnio(), clase.getIterable());
            CalculadoraDuracionClase.validarDiaConHorario(franjasDelGrupo, clase.getFecha());
        }
        return clase;
    }

    // Faltas acumuladas (etapa 4 de 5): elegible para AUSENTE = el alumno tenia
    // una inscripcion en este grupo cuya fecha de inscripcion (parte fecha) es
    // <= la fecha de la clase. PRESENTE nunca pasa por aqui -- se registra
    // siempre, elegible o no (el Instructor lo marco presente).
    private boolean esElegibleParaAusencia(String alumnoId, Clase clase) {
        boolean tieneInscripcion = inscripcionGateway.existeInscripcion(
                alumnoId, clase.getCategoria(), clase.getCurso(), clase.getAnio(), clase.getIterable());
        if (!tieneInscripcion) {
            return false;
        }
        Inscripcion inscripcion = inscripcionGateway.obtenerInscripcion(
                alumnoId, clase.getCategoria(), clase.getCurso(), clase.getAnio(), clase.getIterable());
        if (inscripcion.getFechaInscripcion() == null) {
            return false;
        }
        LocalDate fechaInscripcion = inscripcion.getFechaInscripcion().toLocalDateTime().toLocalDate();
        return !fechaInscripcion.isAfter(clase.getFecha());
    }

    // Faltas acumuladas (etapa 4 de 5): si es elegible, upsert a AUSENTE
    // (META_ELIMINADO=0). Si no, nunca se crea una fila nueva; si ya tenia
    // una fila activa, se marca META_ELIMINADO=1 (regla 3 del ticket).
    // Compartido por confirmarAsistencias() y por los dos endpoints viejos
    // cuando la clase YA esta confirmada (regla 8).
    private Asistencia marcarAusenteSegunElegibilidad(String alumnoId, Integer claseId, Clase clase) {
        if (esElegibleParaAusencia(alumnoId, clase)) {
            return asistenciaGateway.marcarAusente(alumnoId, claseId);
        }
        return eliminarAsistenciaActivaSiExiste(alumnoId, claseId);
    }

    // Devuelve el resultado de la escritura (o el estado ya existente si no
    // habia nada que eliminar) -- evita que los llamantes tengan que releer
    // con obtenerAsistencia() despues de escribir.
    private Asistencia eliminarAsistenciaActivaSiExiste(String alumnoId, Integer claseId) {
        Optional<Asistencia> existente = asistenciaGateway.obtenerAsistencia(alumnoId, claseId);
        if (existente.isPresent() && !Integer.valueOf(1).equals(existente.get().getEliminado())) {
            return asistenciaGateway.eliminarAsistencia(alumnoId, claseId.longValue());
        }
        return existente.orElse(null);
    }

    @Transactional
    @Override
    public void registrarAtencionesPorClase(List<AtencionDto> atenciones, Integer claseId,
            String perfIdLlamador, String rolLlamador) {
        Clase clase = validarYAutorizarRegistro(claseId, perfIdLlamador, rolLlamador);
        boolean confirmada = clase.getAsistenciaConfirmadaEn() != null;

        if (atenciones == null || atenciones.isEmpty()) {
            return;
        }
        for (AtencionDto dto : atenciones) {
            if (dto == null || dto.getIdPerfil() == null || dto.getEstaAtendido() == null) {
                continue;
            }
            String alumnoId = dto.getIdPerfil();
            if (alumnoId == null || alumnoId.isBlank()) {
                continue;
            }
            if (!alumnoGateway.existeAlumno(alumnoId)) {
                throw new NoExisteExcepcion("No existe el perfil de alumno con id " + alumnoId);
            }
            if (dto.getIdClase() != null && !dto.getIdClase().equals(claseId)) {
                throw new ErrorInternoException("La clase indicada en el DTO no coincide con la claseId proporcionada");
            }

            // Faltas acumuladas (etapa 4 de 5, regla 8): con la clase YA
            // confirmada, "desmarcar" deja de ser un borrado fisico -- pasa a
            // ser una ausencia (sujeta a la misma regla de elegibilidad), y
            // "marcar" siempre fuerza PRESENTE (incluso si antes quedo AUSENTE
            // por una confirmacion previa). Sin clase confirmada: igual que
            // siempre, sin cambios.
            if (confirmada) {
                if (Boolean.TRUE.equals(dto.getEstaAtendido())) {
                    asistenciaGateway.marcarPresente(alumnoId, claseId);
                } else {
                    marcarAusenteSegunElegibilidad(alumnoId, claseId, clase);
                }
                continue;
            }

            // SonarCloud/PASO 0 (caracterizacion): existeAsistencia() no distingue
            // META_ELIMINADO -- se usa obtenerAsistencia() para decidir entre
            // insertar (no existe ninguna fila), reactivar (existe con eliminado=1)
            // o no hacer nada (ya existe activa -- idempotente), y lo mismo en
            // sentido inverso para eliminar.
            Optional<Asistencia> existente = asistenciaGateway.obtenerAsistencia(alumnoId, claseId);
            if (Boolean.TRUE.equals(dto.getEstaAtendido())) {
                if (existente.isEmpty()) {
                    asistenciaGateway.InsertarAsistencia(new Asistencia(alumnoId, claseId, 0));
                } else if (Integer.valueOf(1).equals(existente.get().getEliminado())) {
                    asistenciaGateway.reactivarAsistencia(alumnoId, claseId);
                }
                // else: ya existe activa -- idempotente, no se hace nada.
            } else {
                if (existente.isPresent() && !Integer.valueOf(1).equals(existente.get().getEliminado())) {
                    asistenciaGateway.eliminarAsitencia(alumnoId, claseId);
                }
            }
        }
    }

    // Faltas acumuladas (etapa 4 de 5): PUT /clase/{codigo}/asistencia.
    // UNA transaccion -- reutiliza validarYAutorizarRegistro() (propietario +
    // ventana/plazo + dia-en-horario solo Instructor) sin duplicarla.
    @Transactional
    @Override
    public ResultadoConfirmacionAsistencia confirmarAsistencias(Integer claseId,
            List<ItemConfirmacionAsistenciaDto> asistencias, boolean confirmaTodosAusentes,
            String perfIdLlamador, String rolLlamador) {
        Clase clase = validarYAutorizarRegistro(claseId, perfIdLlamador, rolLlamador);
        if (Integer.valueOf(1).equals(clase.getEliminado())) {
            throw new NoExisteExcepcion("La clase con codigo " + claseId + " esta eliminada");
        }

        boolean alguienPresente = asistencias != null && asistencias.stream()
                .anyMatch(item -> item != null && Boolean.TRUE.equals(item.getEstaAtendido()));
        if (!alguienPresente && !confirmaTodosAusentes) {
            throw new ConfirmacionSinAsistentesExcepcion(
                    "Debe marcar al menos un alumno presente, o confirmar que todos estan ausentes");
        }

        // Universo U = alumnos con inscripcion activa en el grupo de la clase
        // (misma condicion que buscarAlumnosGrupoRaw).
        List<Alumno> universo = alumnoGateway.obtenerAlumnosGrupo(
                clase.getCategoria(), clase.getCurso(), clase.getAnio(), clase.getIterable());
        Set<String> idsUniverso = new HashSet<>();
        for (Alumno alumno : universo) {
            idsUniverso.add(alumno.getPerfil().getId());
        }

        // Un idPerfil fuera de U se rechaza SIN cambios -- se valida todo el
        // cuerpo antes de escribir cualquier fila (ver @Transactional arriba).
        Map<String, Boolean> estaAtendidoPorAlumno = new HashMap<>();
        if (asistencias != null) {
            for (ItemConfirmacionAsistenciaDto item : asistencias) {
                if (item == null || item.getIdPerfil() == null) {
                    continue;
                }
                if (!idsUniverso.contains(item.getIdPerfil())) {
                    throw new AlumnoFueraDeGrupoExcepcion(
                            "El alumno " + item.getIdPerfil() + " no tiene una inscripcion activa en el grupo de esta clase");
                }
                estaAtendidoPorAlumno.put(item.getIdPerfil(), Boolean.TRUE.equals(item.getEstaAtendido()));
            }
        }

        int presentes = 0;
        int ausentes = 0;
        for (String alumnoId : idsUniverso) {
            if (Boolean.TRUE.equals(estaAtendidoPorAlumno.get(alumnoId))) {
                asistenciaGateway.marcarPresente(alumnoId, claseId);
                presentes++;
                continue;
            }
            if (esElegibleParaAusencia(alumnoId, clase)) {
                asistenciaGateway.marcarAusente(alumnoId, claseId);
                ausentes++;
            } else {
                eliminarAsistenciaActivaSiExiste(alumnoId, claseId);
            }
        }

        LocalDateTime confirmadaEn = LocalDateTime.now(clock);
        claseGateway.confirmarAsistencia(claseId, confirmadaEn, perfIdLlamador);
        return new ResultadoConfirmacionAsistencia(claseId, confirmadaEn, presentes, ausentes);
    }

    @Override
    public Asistencia obtenerAsistencia(String perfId, int clsId) {
        return asistenciaGateway.obtenerAsistencia(perfId, clsId).orElseThrow(() -> new NoExisteExcepcion());
    }

    @Override
    public Asistencia InsertarAsistencia(Asistencia datosAsistencia) {
        if (asistenciaGateway.existeAsistencia(datosAsistencia.getIdPerfil(), datosAsistencia.getClsCodigo())) {
            throw new YaExisteElementoExcepcion("Ya existe el elemento");
        }
        return asistenciaGateway.InsertarAsistencia(datosAsistencia);
    }

    @Override
    public Asistencia eliminarAsitencia(String perfId, int clsId) {
        if (asistenciaGateway.existeAsistencia(perfId, clsId)) {
            return asistenciaGateway.eliminarAsitencia(perfId, clsId);
        }
        throw new NoExisteExcepcion();
    }

    @Override
    public Asistencia eliminarAsistencia(String perfId, Long clsCodigo, String perfIdLlamador, String rolLlamador) {
        if (!asistenciaGateway.existeAsistencia(perfId, clsCodigo.intValue())) {
            throw new NoExisteExcepcion("La asistencia no existe");
        }
        Asistencia asistencia = asistenciaGateway.obtenerAsistencia(perfId, clsCodigo.intValue())
                .orElseThrow(() -> new NoExisteExcepcion("La asistencia no existe"));
        if (Integer.valueOf(1).equals(asistencia.getEliminado())) {
            throw new YaExisteElementoExcepcion("La asistencia ya se encuentra eliminada");
        }
        Clase clase = validarVentanaRegistro(clsCodigo.intValue(), perfIdLlamador, rolLlamador);
        // Faltas acumuladas (etapa 4 de 5, regla 8): con la clase YA confirmada,
        // DELETE /asistencia deja la fila AUSENTE (misma regla de elegibilidad),
        // no eliminada logicamente. Sin confirmar: comportamiento actual intacto.
        if (clase.getAsistenciaConfirmadaEn() != null) {
            return marcarAusenteSegunElegibilidad(perfId, clsCodigo.intValue(), clase);
        }
        return asistenciaGateway.eliminarAsistencia(perfId, clsCodigo);
    }

    // SCRUM-80 (rediseno, bloque 3 de 4): ventana de registro de asistencia de
    // "hoy" hacia atras. Instructor: 1o del mes actual (o del mes anterior si
    // tiene prorroga todavia vigente). Coordinador/Administrador: sin limite
    // inferior (fechaMinima null). fechaMaxima siempre "hoy" (nadie registra
    // fechas futuras -- ver VentanaRegistroAsistencia.validarRegistro).
    @Override
    public VentanaRegistroDto obtenerVentanaRegistro(String perfIdLlamador, String rolLlamador) {
        LocalDate hoy = LocalDate.now(clock);
        if (!"Instructor".equals(rolLlamador)) {
            return VentanaRegistroDto.fabricarDeModelo(null, hoy, rolLlamador);
        }
        LocalDate mesAnterior = hoy.minusMonths(1);
        Map<String, ProrrogaRegistroAsistencia> prorrogas = prorrogaGateway.obtenerProrrogasPorMes(mesAnterior.getYear(), mesAnterior.getMonthValue());
        ProrrogaRegistroAsistencia prorroga = prorrogas != null ? prorrogas.get(perfIdLlamador) : null;
        LocalDate limiteProrrogaMesAnterior = prorroga != null ? prorroga.getFechaLimite() : null;
        LocalDate fechaMinima = VentanaRegistroAsistencia.calcularFechaMinima(hoy, limiteProrrogaMesAnterior);
        return VentanaRegistroDto.fabricarDeModelo(fechaMinima, hoy, rolLlamador);
    }
}
