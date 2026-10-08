package co.edu.unicauca.deporteParaTodos.infraestructura.controladorExcepciones;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import co.edu.unicauca.deporteParaTodos.infraestructura.controladorExcepciones.excepciones.ArchivoNoConvertibleExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.CuposAgotadosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DependenciaFallida;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ErrorInternoException;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechasGrupoInvalidasExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.InscripcionesCerradasExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.InsercionFallidaExepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.LimiteCursosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.infraestructura.controladorExcepciones.excepciones.NoConvertibleException;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.infraestructura.controladorExcepciones.excepciones.NoImplementadoException;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoProcesableEntidadException;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.CodigoError;
import co.edu.unicauca.deporteParaTodos.infraestructura.controladorExcepciones.formatoError.ErrorUtils;
import co.edu.unicauca.deporteParaTodos.infraestructura.controladorExcepciones.formatoError.Error;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class RestExceptionHandler {
        private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);

        private void logExcepcion(String tipo, HttpServletRequest req, Exception ex) {
                LOGGER.error(
                                "\n=============================\nExcepcion: {}\nPeticion: {}\nDireccion: {}\nMotivo: {}\n=============================",
                                tipo,
                                req != null ? req.getMethod() : "N/A",
                                req != null ? req.getRequestURL().toString() : "N/A",
                                ex != null ? ex.getMessage() : "Sin detalle",
                                ex);
        }
        /***
         * Captura las excepciones generadas por los argumentos en los endpoint al no
         * coincidir con los constraint establecidos.
         * Los constraint se encuentran denotados en los tipos de datos DTO, mediante
         * jakarta validation
         * Para su uso en los endpoints debe establecese la notacion @Validated a nivel
         * de clase y la notacion @Valid a nivel de metodo
         * 
         * @param ex excepcion a capturar
         * @return response entity con la lista de errores o reglas infringidas en los
         *         argumentos
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidationExceptions(HttpServletRequest req, MethodArgumentNotValidException ex) {
                System.out.println("Retornando respuesta con los errores identificados");
                Map<String, String> errores = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach((error) -> {
                        String campo = ((FieldError) error).getField();
                        String mensajeDeError = error.getDefaultMessage();
                        errores.put(campo, mensajeDeError);
                });

                logExcepcion("MethodArgumentNotValidException", req, ex);
                return new ResponseEntity<Map<String, String>>(errores, HttpStatus.BAD_REQUEST);
        }

        /**
         * Captura las excepciones de validacion lanzadas sobre parametros simples
         * como query params, path params o request params cuando se usa @Validated.
         */
        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<Map<String, String>> handleConstraintViolationExceptions(HttpServletRequest req, ConstraintViolationException ex) {
                Map<String, String> errores = new HashMap<>();
                for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
                        String ruta = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "parametro";
                        String campo = ruta.contains(".") ? ruta.substring(ruta.lastIndexOf('.') + 1) : ruta;
                        errores.put(campo, violation.getMessage());
                }
                logExcepcion("ConstraintViolationException", req, ex);
                return new ResponseEntity<Map<String, String>>(errores, HttpStatus.BAD_REQUEST);
        }

        /**
         * Captura validaciones de parametros de metodo en Spring Framework 6.1+.
         */
        @ExceptionHandler(HandlerMethodValidationException.class)
        public ResponseEntity<Map<String, String>> handleHandlerMethodValidationException(
                        HttpServletRequest req,
                        HandlerMethodValidationException ex) {
                Map<String, String> errores = new HashMap<>();
                ex.getAllValidationResults().forEach(resultado -> {
                        String campo = resultado.getMethodParameter().getParameterName();
                        resultado.getResolvableErrors().forEach(error -> {
                                errores.put(campo, error.getDefaultMessage());
                        });
                });
                logExcepcion("HandlerMethodValidationException", req, ex);
                return new ResponseEntity<Map<String, String>>(errores, HttpStatus.BAD_REQUEST);
        }

        /**
         * Captura excepcion por json mal formados que ingresan como peticion,
         * es un error del usuario al introducir de forma inadecuada los datos
         * @param ex
         * @return
         */
        @ExceptionHandler(HttpMessageNotReadableException.class)
                public ResponseEntity<Map<String, String>> handleJsonParseError(HttpServletRequest req, HttpMessageNotReadableException ex) {
                Map<String, String> error = new HashMap<>();
                error.put("mensaje", "El cuerpo de la solicitud contiene JSON mal formado o datos no válidos.");
                error.put("detalle", ex.getMostSpecificCause().getMessage());
                logExcepcion("HttpMessageNotReadableException", req, ex);
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        /**
         * Captura exception cuando no se encuentran elementos en un listado
         * para su uso lance una exception de tipo ListadoVacioException
         * 
         * @param req
         * @param ex  excepcion a capturar
         * @return
         */
        @ExceptionHandler(ListadoVacioExcepcion.class)
        @ResponseBody
        public ResponseEntity<Error> GenericException(final HttpServletRequest req, final ListadoVacioExcepcion ex) {
                logExcepcion("ListadoVacioExcepcion", req, ex);
                final Error error = ErrorUtils.crearError(
                                CodigoError.LISTADO_VACIO.getCodigo(),
                                String.format("%s, %s", CodigoError.LISTADO_VACIO.getLlaveMensaje(), ex.getMessage()),
                                HttpStatus.OK.value())
                                .setUrl(req.getRequestURL().toString())
                                .setMetodo(req.getMethod());

                return new ResponseEntity<Error>(error, HttpStatus.OK);
        }

        // Cuando intenta buscar un elemento por código y no se encuentra
        @ExceptionHandler(NoExisteExcepcion.class)
        public ResponseEntity<Error> GenericException(final HttpServletRequest req, final NoExisteExcepcion ex) {
                logExcepcion("NoExisteExcepcion", req, ex);
                final Error error = ErrorUtils.crearError(
                                ex.getCodigo(),
                                ex.getLlaveMensaje() +": "+ ex.getMessage(),
                                HttpStatus.NOT_FOUND.value())
                                .setUrl(req.getRequestURL().toString())
                                .setMetodo(req.getMethod());
                return new ResponseEntity<Error>(error, HttpStatus.NOT_FOUND);
        }

        /**
         * Captura excepcion de tipo Archivo no convertible
         * 
         * @param req
         * @param ex  excepcion a capturar
         * @return ResponseEntity con reporte de error generado.
         */
        @ExceptionHandler(ArchivoNoConvertibleExcepcion.class)
        public ResponseEntity<Error> GenericException(final HttpServletRequest req,
                        final ArchivoNoConvertibleExcepcion ex) {
                logExcepcion("ArchivoNoConvertibleExcepcion", req, ex);
                final Error error = ErrorUtils.crearError(
                                ex.getCodigo(),
                                String.format("%s, %s", ex.getLlaveMensaje(), ex.getMessage()),
                                HttpStatus.BAD_REQUEST.value())
                                .setUrl(req.getRequestURL().toString())
                                .setMetodo(req.getMethod());
                return new ResponseEntity<Error>(error, HttpStatus.BAD_REQUEST);
        }

        /**
         * 
         * 
         */
        @ExceptionHandler(InsercionFallidaExepcion.class)
        public ResponseEntity<Error> GenericException(final HttpServletRequest req, final InsercionFallidaExepcion ex) {
                logExcepcion("InsercionFallidaExepcion", req, ex);
                final Error error = ErrorUtils.crearError(
                                ex.getCodigo(),
                                String.format("%s, %s", ex.getLlaveMensaje(), ex.getMessage()),
                                HttpStatus.NOT_MODIFIED.value())
                                .setUrl(req.getRequestURL().toString())
                                .setMetodo(req.getMethod());
                return new ResponseEntity<Error>(error, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(YaExisteElementoExcepcion.class)
        public ResponseEntity<Error> GenericException(final HttpServletRequest req,
                        final YaExisteElementoExcepcion ex) {
                logExcepcion("YaExisteElementoExcepcion", req, ex);
                final Error error = ErrorUtils
                                .crearError(ex.getCodigo(),
                                                String.format("%s, %s", ex.getLlaveMensaje(), ex.getMessage()),
                                                HttpStatus.CONFLICT.value())
                                .setUrl(req.getRequestURL().toString()).setMetodo(req.getMethod());
                return new ResponseEntity<Error>(error, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(NoImplementadoException.class)
        public ResponseEntity<Error> GenericException(final HttpServletRequest req,
                        final NoImplementadoException ex) {
                logExcepcion("NoImplementadoException", req, ex);
                final Error error = ErrorUtils
                                .crearError(ex.getCodigo(),
                                                String.format("%s, %s", ex.getLlaveMensaje(), ex.getMessage()),
                                                HttpStatus.NOT_IMPLEMENTED.value())
                                .setUrl(req.getRequestURL().toString()).setMetodo(req.getMethod());
                return new ResponseEntity<Error>(error, HttpStatus.NOT_IMPLEMENTED);
        }

        @ExceptionHandler(NoConvertibleException.class)
        public ResponseEntity<Error> GenericException(final HttpServletRequest req, final NoConvertibleException ex){
                logExcepcion("NoConvertibleException", req, ex);
                HttpStatusCode codigoHttp = HttpStatus.NOT_ACCEPTABLE;
                String mensaje = String.format("%s, %s", ex.getLlaveMensaje(), ex.getMessage());

                final Error error = ErrorUtils.crearError(ex.getCodigo(),mensaje,codigoHttp.value());

                error.setUrl(req.getRequestURL().toString());
                error.setMetodo(req.getMethod());
                
                return new ResponseEntity<Error>(error, codigoHttp);
        }

         @ExceptionHandler(ErrorInternoException.class)
        public ResponseEntity<Error> GenericException(final HttpServletRequest req, final ErrorInternoException ex){
                logExcepcion("ErrorInternoException", req, ex);
                HttpStatusCode codigoHttp = HttpStatus.INTERNAL_SERVER_ERROR;
                String mensaje = String.format("%s, %s", ex.getLlaveMensaje(), ex.getMessage());

                final Error error = ErrorUtils.crearError(ex.getCodigo(),mensaje,codigoHttp.value());

                error.setUrl(req.getRequestURL().toString());
                error.setMetodo(req.getMethod());
                
                return new ResponseEntity<Error>(error, codigoHttp);
        }

        /**
         * cuando es necesaria una dependencia pero esta no corresponde con las reglas del negocio, como llave foranea erronea
         * @param req
         * @param ex
         * @return
         */
        @ExceptionHandler(DependenciaFallida.class)
        public ResponseEntity<Error> GenericException(final HttpServletRequest req, final DependenciaFallida ex){
                logExcepcion("DependenciaFallida", req, ex);
                HttpStatusCode codigoHttp = HttpStatus.FAILED_DEPENDENCY;
                String mensaje = String.format("%s, %s", ex.getLlaveMensaje(), ex.getMessage());

                final Error error = ErrorUtils.crearError(ex.getCodigo(),mensaje,codigoHttp.value());

                error.setUrl(req.getRequestURL().toString());
                error.setMetodo(req.getMethod());
                
                return new ResponseEntity<Error>(error, codigoHttp);
        }

        @ExceptionHandler(NoProcesableEntidadException.class)
        public ResponseEntity<Error> GenericException(final HttpServletRequest req, final NoProcesableEntidadException ex){
                logExcepcion("NoProcesableEntidadException", req, ex);
                HttpStatusCode codigoHttp = HttpStatus.INTERNAL_SERVER_ERROR;
                String mensaje = String.format("%s, %s", ex.getLlaveMensaje(), ex.getMessage());

                final Error error = ErrorUtils.crearError(ex.getCodigo(),mensaje,codigoHttp.value());

                error.setUrl(req.getRequestURL().toString());
                error.setMetodo(req.getMethod());

                return new ResponseEntity<Error>(error, codigoHttp);
        }

        @ExceptionHandler(InscripcionesCerradasExcepcion.class)
        public ResponseEntity<Error> GenericException(final HttpServletRequest req, final InscripcionesCerradasExcepcion ex) {
                logExcepcion("InscripcionesCerradasExcepcion", req, ex);
                return buildDomainErrorResponse(req,
                                ex != null ? ex.getCodigo()       : "",
                                ex != null ? ex.getLlaveMensaje() : "",
                                ex != null ? ex.getMessage()      : "",
                                HttpStatus.UNPROCESSABLE_ENTITY);
        }

        @ExceptionHandler(CuposAgotadosExcepcion.class)
        public ResponseEntity<Error> GenericException(final HttpServletRequest req, final CuposAgotadosExcepcion ex) {
                logExcepcion("CuposAgotadosExcepcion", req, ex);
                return buildDomainErrorResponse(req,
                                ex != null ? ex.getCodigo()       : "",
                                ex != null ? ex.getLlaveMensaje() : "",
                                ex != null ? ex.getMessage()      : "",
                                HttpStatus.CONFLICT);
        }

        @ExceptionHandler(LimiteCursosExcepcion.class)
        public ResponseEntity<Error> GenericException(final HttpServletRequest req, final LimiteCursosExcepcion ex) {
                logExcepcion("LimiteCursosExcepcion", req, ex);
                return buildDomainErrorResponse(req,
                                ex != null ? ex.getCodigo()       : "",
                                ex != null ? ex.getLlaveMensaje() : "",
                                ex != null ? ex.getMessage()      : "",
                                HttpStatus.UNPROCESSABLE_ENTITY);
        }

        @ExceptionHandler(FechasGrupoInvalidasExcepcion.class)
        public ResponseEntity<Error> GenericException(final HttpServletRequest req, final FechasGrupoInvalidasExcepcion ex) {
                logExcepcion("FechasGrupoInvalidasExcepcion", req, ex);
                return buildDomainErrorResponse(req,
                                ex != null ? ex.getCodigo()       : "",
                                ex != null ? ex.getLlaveMensaje() : "",
                                ex != null ? ex.getMessage()      : "",
                                HttpStatus.UNPROCESSABLE_ENTITY);
        }

        private ResponseEntity<Error> buildDomainErrorResponse(
                        HttpServletRequest req,
                        String codigo,
                        String llave,
                        String detalle,
                        HttpStatusCode codigoHttp) {
                String mensaje = String.format("%s, %s", llave, detalle);
                final Error error = ErrorUtils.crearError(codigo, mensaje, codigoHttp.value());
                String url = (req != null && req.getRequestURL() != null)
                                ? req.getRequestURL().toString() : "";
                error.setUrl(url);
                error.setMetodo(req != null ? req.getMethod() : "");
                return new ResponseEntity<>(error, codigoHttp);
        }
}
