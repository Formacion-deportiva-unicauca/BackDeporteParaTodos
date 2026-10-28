package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum CodigoError {

        ERROR_GENERICO("GC-0001", "ERROR GENERICO"),
        LISTADO_VACIO("GC-0002", "El listado a retornar se encuentra vacio"),
        NO_EXISTE("GC-0003","No se encuentra registrado el elemento"),
        ARCHIVO_NO_CONVERTIBLE("GC-0004","El archivo no puede ser transformado"),
        INSERCION_FALLIDA("GC-0005", "El elemento no pudo ser insertado en la base de datos"),
        NO_IMPLEMENTADO("GC-0006", "El recurso no ha sido implementado aun"),
        YA_EXISTE("GC-0007", "Ya existe el mismo registro en el sistema"),
        NO_CONVERTIBLE("GC-0008", "Datos no son compatibles, no pueden ser transformados"),
        ERROR_INTERNO("GC-0009", "No se ha logrado completar la peticion"),
        DEPENDICEA_FALLIDA("GC-0010", "El recurso necesita de una dependencia que no se encuentra en el sistema"),
        ENTIDAD_NO_PROCESABLE("GC-0011", "La entidad no puede ser procesada"),
        INSCRIPCIONES_CERRADAS("GC-0012", "Las inscripciones de este curso estan cerradas"),
        CUPOS_AGOTADOS("GC-0013", "No hay cupos disponibles en el grupo"),
        LIMITE_CURSOS_ALUMNO("GC-0014", "El alumno ha alcanzado el limite de cursos activos"),
        FECHAS_GRUPO_INVALIDAS("GC-0015", "Las fechas del grupo no son validas"),
        INSCRIPCION_FUERA_DE_FECHAS("GC-0016", "La inscripcion no esta disponible en este momento"),
        PLAZO_CARGA_INVALIDO("GC-0017", "La fecha limite de plazo de carga no es valida"),
        FECHA_CLASE_FUTURA("GC-0018", "La fecha de la clase no puede ser futura"),
        REGISTRO_ASISTENCIA_VENCIDO("GC-0019", "El plazo para registrar la asistencia ya venció"),
        PRORROGA_MES_FUTURO("GC-0020", "No se puede asignar una prorroga para un mes futuro"),
        JUSTIFICACION_INVALIDA("GC-0021", "La justificacion de la clase no es valida"),
        DURACION_MANUAL_INVALIDA("GC-0022", "La duracion de la clase no es valida"),
        CONSULTA_MES_FUTURO("GC-0023", "No se puede consultar un mes futuro"),
        DIA_FUERA_DE_HORARIO("GC-0024", "La fecha no corresponde a un dia con horario asignado"),
        ACCESO_GRUPO_DENEGADO("GC-0025", "El Instructor no tiene a cargo el grupo indicado"),
        CORREO_NO_MODIFICABLE_INSTRUCTOR("GC-0026", "El Instructor no puede modificar el correo del alumno"),
        ALUMNO_FUERA_DE_GRUPO("GC-0027", "El alumno no tiene una inscripcion activa en el grupo de esta clase"),
        CONFIRMACION_SIN_ASISTENTES("GC-0028", "Debe marcar al menos un alumno presente, o confirmar que todos estan ausentes"),
        ;

        private final String codigo;
        private final String llaveMensaje;
}
