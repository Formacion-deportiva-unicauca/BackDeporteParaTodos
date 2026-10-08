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
        ;

        private final String codigo;
        private final String llaveMensaje;
}
