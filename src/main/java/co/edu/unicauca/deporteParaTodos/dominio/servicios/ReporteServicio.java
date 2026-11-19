package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IReporteServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IReporteGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DatosInvalidosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ConsultaReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.FilaCrudaReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.OpcionesReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ReporteMensual;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ReporteMensual.Calidad;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ReporteMensual.Fila;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ReporteMensual.Filtros;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ReporteMensual.PorVinculo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ReporteMensual.Totales;

/**
 * Reporte mensual (solo Coordinador, lo exige el endpoint). Arma el reporte desde conteos granulares de la base (por curso,
 * vinculo y genero), asi las filas, los totales y la distribucion por vinculo salen de los mismos numeros y concilian siempre.
 */
@Service
public class ReporteServicio implements IReporteServicio {

    static final String FUNCIONARIOS = "Funcionarios";
    static final List<String> VINCULOS = List.of("Estudiante", "Docente", "Administrativo", "Contratista");
    private static final List<String> VINCULOS_FUNCIONARIOS = List.of("Docente", "Administrativo", "Contratista");
    static final List<String> PUBLICOS = List.of("ESTUDIANTE", "FUNCIONARIO", "MIXTO");
    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = 2100;

    private final IReporteGateway gateway;
    private final Clock clock;

    public ReporteServicio(IReporteGateway gateway, Clock clock) {
        this.gateway = gateway;
        this.clock = clock;
    }

    @Override
    public ReporteMensual mensual(int anio, int mes, String disciplina, String modalidad, String publico,
            List<String> vinculos, String genero, String facultad, boolean agruparFuncionarios) {
        if (anio < ANIO_MINIMO || anio > ANIO_MAXIMO) {
            throw new DatosInvalidosExcepcion("El anio debe estar entre " + ANIO_MINIMO + " y " + ANIO_MAXIMO);
        }
        if (mes < 1 || mes > 12) {
            throw new DatosInvalidosExcepcion("El mes debe estar entre 1 y 12");
        }
        String disciplinaFiltro = vacioANulo(disciplina);
        String modalidadFiltro = vacioANulo(modalidad) == null ? null : modalidad.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        String publicoFiltro = vacioANulo(publico) == null ? null : publico.trim().toUpperCase(Locale.ROOT);
        if (publicoFiltro != null && !PUBLICOS.contains(publicoFiltro)) {
            throw new DatosInvalidosExcepcion("El publico del curso debe ser ESTUDIANTE, FUNCIONARIO o MIXTO");
        }
        String generoFiltro = vacioANulo(genero) == null ? null : genero.trim().toUpperCase(Locale.ROOT);
        if (generoFiltro != null && !generoFiltro.equals("M") && !generoFiltro.equals("F")) {
            throw new DatosInvalidosExcepcion("El genero debe ser M o F");
        }
        String facultadFiltro = vacioANulo(facultad);
        List<String> vinculosFiltro = expandirVinculos(vinculos);

        YearMonth ym = YearMonth.of(anio, mes);
        ConsultaReporte consulta = new ConsultaReporte(ym.atDay(1), ym.plusMonths(1).atDay(1), LocalDate.now(clock),
                disciplinaFiltro, modalidadFiltro, publicoFiltro, vinculosFiltro, generoFiltro, facultadFiltro);

        List<FilaCrudaReporte> inscritos = gateway.inscritos(consulta);
        List<FilaCrudaReporte> atenciones = gateway.atenciones(consulta);

        List<Fila> filas = armarFilas(inscritos, atenciones);
        Totales totales = armarTotales(filas, gateway.personasDistintasInscritas(consulta), gateway.personasDistintasAtendidas(consulta));
        List<PorVinculo> porVinculo = armarPorVinculo(inscritos, atenciones, vinculosFiltro, agruparFuncionarios);
        long sinInscripcion = atenciones.stream().mapToLong(FilaCrudaReporte::sinInscripcion).sum();
        Calidad calidad = new Calidad(gateway.clasesSinConfirmar(consulta), sinInscripcion);

        Filtros filtros = new Filtros(disciplinaFiltro, modalidadFiltro == null ? null : normalizarModalidad(modalidad), publicoFiltro,
                vinculosFiltro, generoFiltro, facultadFiltro, agruparFuncionarios);
        return new ReporteMensual(anio, mes, filtros, filas, totales, porVinculo, calidad);
    }

    @Override
    public OpcionesReporte opciones() {
        LocalDate primera = gateway.primeraFechaConDatos();
        LocalDate ultima = gateway.ultimaFechaConDatos();
        String primerMes = primera == null ? null : YearMonth.from(primera).toString();
        String ultimoMes = ultima == null ? primerMes : YearMonth.from(ultima).toString();
        Set<String> modalidades = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        gateway.modalidades().forEach(m -> modalidades.add(normalizarModalidad(m)));
        return new OpcionesReporte(primerMes, ultimoMes, gateway.disciplinas(), new ArrayList<>(modalidades), PUBLICOS,
                VINCULOS, gateway.facultades());
    }

    /** "seleccionado " -> "Seleccionado": sin espacios sobrantes, todo en minuscula salvo la primera letra. */
    public static String normalizarModalidad(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return "Sin modalidad";
        }
        String limpio = titulo.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return limpio.substring(0, 1).toUpperCase(Locale.ROOT) + limpio.substring(1);
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /** Sin filtro = los cuatro vinculos; "Funcionarios" = Docente + Administrativo + Contratista. */
    private static List<String> expandirVinculos(List<String> pedidos) {
        if (pedidos == null || pedidos.stream().allMatch(v -> v == null || v.isBlank())) {
            return VINCULOS;
        }
        Set<String> resultado = new LinkedHashSet<>();
        for (String pedido : pedidos) {
            if (pedido == null || pedido.isBlank()) {
                continue;
            }
            String v = pedido.trim();
            if (v.equalsIgnoreCase(FUNCIONARIOS)) {
                resultado.addAll(VINCULOS_FUNCIONARIOS);
                continue;
            }
            String conocido = VINCULOS.stream().filter(x -> x.equalsIgnoreCase(v)).findFirst()
                    .orElseThrow(() -> new DatosInvalidosExcepcion(
                            "El vinculo debe ser Estudiante, Docente, Administrativo, Contratista o Funcionarios"));
            resultado.add(conocido);
        }
        // Siempre en el orden canonico, para que la respuesta no dependa del orden pedido.
        return VINCULOS.stream().filter(resultado::contains).toList();
    }

    private static List<Fila> armarFilas(List<FilaCrudaReporte> inscritos, List<FilaCrudaReporte> atenciones) {
        Map<String, long[]> acum = new LinkedHashMap<>();   // [inscH, inscM, atH, atM]
        Map<String, String[]> datos = new LinkedHashMap<>(); // [modalidad, curso, disciplina, publico]
        sumar(inscritos, 0, acum, datos);
        sumar(atenciones, 2, acum, datos);
        List<Fila> filas = new ArrayList<>();
        acum.forEach((clave, v) -> {
            String[] d = datos.get(clave);
            filas.add(new Fila(d[0], d[1], d[2], d[3], v[0], v[1], v[0] + v[1], v[2], v[3], v[2] + v[3]));
        });
        filas.sort(Comparator.comparing(Fila::modalidad, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Fila::curso, String.CASE_INSENSITIVE_ORDER));
        return filas;
    }

    private static void sumar(List<FilaCrudaReporte> crudas, int desplazamiento, Map<String, long[]> acum, Map<String, String[]> datos) {
        for (FilaCrudaReporte f : crudas) {
            String modalidad = normalizarModalidad(f.categoria());
            String clave = modalidad + "\u0000" + f.curso();
            long[] v = acum.computeIfAbsent(clave, k -> new long[4]);
            datos.putIfAbsent(clave, new String[] {modalidad, f.curso(), f.disciplina(), f.publico()});
            if ("M".equals(f.sexo())) {
                v[desplazamiento] += f.total();
            } else if ("F".equals(f.sexo())) {
                v[desplazamiento + 1] += f.total();
            }
        }
    }

    private static Totales armarTotales(List<Fila> filas, long distintasInscritas, long distintasAtendidas) {
        long ih = filas.stream().mapToLong(Fila::inscritosHombres).sum();
        long im = filas.stream().mapToLong(Fila::inscritosMujeres).sum();
        long ah = filas.stream().mapToLong(Fila::atencionesHombres).sum();
        long am = filas.stream().mapToLong(Fila::atencionesMujeres).sum();
        return new Totales(ih, im, ih + im, ah, am, ah + am, distintasInscritas, distintasAtendidas);
    }

    private static List<PorVinculo> armarPorVinculo(List<FilaCrudaReporte> inscritos, List<FilaCrudaReporte> atenciones,
            List<String> vinculosFiltro, boolean agruparFuncionarios) {
        Map<String, long[]> acum = new LinkedHashMap<>();
        for (String v : vinculosFiltro) {
            acum.put(etiquetaVinculo(v, agruparFuncionarios), new long[2]);
        }
        for (FilaCrudaReporte f : inscritos) {
            acum.computeIfAbsent(etiquetaVinculo(f.vinculo(), agruparFuncionarios), k -> new long[2])[0] += sexoValido(f) ? f.total() : 0;
        }
        for (FilaCrudaReporte f : atenciones) {
            acum.computeIfAbsent(etiquetaVinculo(f.vinculo(), agruparFuncionarios), k -> new long[2])[1] += sexoValido(f) ? f.total() : 0;
        }
        List<PorVinculo> lista = new ArrayList<>();
        acum.forEach((v, n) -> lista.add(new PorVinculo(v, n[0], n[1])));
        return lista;
    }

    /** Solo cuentan M y F (el esquema no admite otro valor); asi la distribucion por vinculo suma igual que los totales. */
    private static boolean sexoValido(FilaCrudaReporte f) {
        return "M".equals(f.sexo()) || "F".equals(f.sexo());
    }

    private static String etiquetaVinculo(String vinculo, boolean agruparFuncionarios) {
        return agruparFuncionarios && VINCULOS_FUNCIONARIOS.contains(vinculo) ? FUNCIONARIOS : vinculo;
    }
}
