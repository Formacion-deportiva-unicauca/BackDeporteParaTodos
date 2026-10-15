package co.edu.unicauca.deporteParaTodos.infraestructura.controladorExcepciones;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.*;
import co.edu.unicauca.deporteParaTodos.infraestructura.controladorExcepciones.excepciones.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RestExceptionHandlerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // DTO mínimo para disparar MethodArgumentNotValidException vía @Valid
    static class DtoDeTest {
        @NotBlank(message = "El titulo no puede estar vacio")
        private String titulo;
        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
    }

    // Controller de prueba: un endpoint por excepción a cubrir
    @RestController
    @Validated
    static class TestController {

        // --- Grupo B: excepciones de negocio (lanzadas directamente) ---

        @GetMapping("/test/noexiste")
        void noExiste() { throw new NoExisteExcepcion("elemento de prueba"); }

        @GetMapping("/test/yaexiste")
        void yaExiste() { throw new YaExisteElementoExcepcion("prueba"); }

        @GetMapping("/test/listadovacio")
        void listadoVacio() { throw new ListadoVacioExcepcion("prueba"); }

        @GetMapping("/test/errorinterno")
        void errorInterno() { throw new ErrorInternoException("prueba"); }

        @GetMapping("/test/dependenciafallida")
        void dependenciaFallida() { throw new DependenciaFallida("prueba"); }

        @GetMapping("/test/noimplementado")
        void noImplementado() { throw new NoImplementadoException(); }

        @GetMapping("/test/noconvertible")
        void noConvertible() { throw new NoConvertibleException(); }

        @GetMapping("/test/noprocesable")
        void noProcesable() { throw new NoProcesableEntidadException("prueba"); }

        @GetMapping("/test/insercionfallida")
        void insercionFallida() { throw new InsercionFallidaExepcion("prueba"); }

        @GetMapping("/test/archivonc")
        void archivoNoConvertible() { throw new ArchivoNoConvertibleExcepcion("prueba"); }

        @GetMapping("/test/inscripcionescerradas")
        void inscripcionesCerradas() { throw new InscripcionesCerradasExcepcion("curso cerrado"); }

        @GetMapping("/test/cuposagotados")
        void cuposAgotados() { throw new CuposAgotadosExcepcion("sin cupos"); }

        @GetMapping("/test/limitecursos")
        void limiteCursos() { throw new LimiteCursosExcepcion("limite alcanzado"); }

        // SCRUM-80 (rediseno, bloque 1): sin endpoint real todavia (se conecta en
        // el bloque 2, cuando POST /claseGrupo y POST /atenciones empiecen a
        // llamar a VentanaRegistroAsistencia) -- se cubre el handler aqui.
        @GetMapping("/test/fechaclasefutura")
        void fechaClaseFutura() { throw new FechaClaseFuturaExcepcion("fecha futura de prueba"); }

        @GetMapping("/test/registroasistenciavencido")
        void registroAsistenciaVencido() { throw new RegistroAsistenciaVencidoExcepcion("plazo vencido de prueba"); }

        // ConstraintViolationException es de capa de servicio (AOP); se lanza
        // manualmente para ejercer el handler directamente en standaloneSetup.
        @GetMapping("/test/constraintviolation")
        void constraintViolation() {
            throw new ConstraintViolationException(Collections.emptySet());
        }

        // --- Grupo A: excepciones del framework (disparadas por Spring MVC) ---

        @PostMapping("/test/bodyvalido")
        // Cuerpo vacio a proposito: Spring lanza MethodArgumentNotValidException antes de llegar aqui
        void bodyValido(@RequestBody @Valid DtoDeTest dto) {}

        @PostMapping("/test/jsonroto")
        // Cuerpo vacio a proposito: Jackson lanza HttpMessageNotReadableException antes de llegar aqui
        void jsonRoto(@RequestBody DtoDeTest dto) {}

        // Endpoint utilizado por handlerMethodValidation_retorna400 via webAppContextSetup
        @GetMapping("/test/paramblank")
        // Cuerpo vacio a proposito: MethodValidationInterceptor lanza HandlerMethodValidationException antes de llegar aqui
        void paramBlank(@RequestParam @NotBlank String titulo) {}
    }

    // Configuración mínima con @EnableWebMvc para que RequestMappingHandlerAdapter
    // configure MethodValidationAdapter y dispare HandlerMethodValidationException.
    @Configuration
    @EnableWebMvc
    static class MinimalWebMvcConfig implements WebMvcConfigurer {

        @Bean
        public LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }

        @Override
        public Validator getValidator() {
            return validator();
        }

        @Bean
        public TestController testController() {
            return new TestController();
        }

        @Bean
        public RestExceptionHandler restExceptionHandler() {
            return new RestExceptionHandler();
        }

        // En Spring 6.1 el MethodValidationInterceptor lanza HandlerMethodValidationException
        // (en vez de ConstraintViolationException) cuando el target tiene @Controller/@RestController.
        // MethodValidationPostProcessor crea el proxy AOP que activa ese interceptor.
        @Bean
        public MethodValidationPostProcessor methodValidationPostProcessor() {
            MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
            processor.setValidator(validator());
            return processor;
        }
    }

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new RestExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ====== Grupo B: body = objeto Error ======

    @Test
    void noExiste_retorna404() throws Exception {
        mockMvc.perform(get("/test/noexiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigoError").value("GC-0003"))
                .andExpect(jsonPath("$.codigoHttp").value(404));
    }

    @Test
    void yaExiste_retorna409() throws Exception {
        mockMvc.perform(get("/test/yaexiste"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigoError").value("GC-0007"))
                .andExpect(jsonPath("$.codigoHttp").value(409));
    }

    @Test
    void listadoVacio_retorna200() throws Exception {
        mockMvc.perform(get("/test/listadovacio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoError").value("GC-0002"))
                .andExpect(jsonPath("$.codigoHttp").value(200));
    }

    @Test
    void errorInterno_retorna500() throws Exception {
        // Bug en ErrorInternoException: ambos constructores usan CodigoError.NO_CONVERTIBLE
        // (GC-0008) en vez de CodigoError.ERROR_INTERNO (GC-0009). El test documenta el
        // comportamiento real actual; la corrección corresponde a SCRUM-146 o similar.
        mockMvc.perform(get("/test/errorinterno"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigoError").value("GC-0008"))
                .andExpect(jsonPath("$.codigoHttp").value(500));
    }

    @Test
    void dependenciaFallida_retorna424() throws Exception {
        mockMvc.perform(get("/test/dependenciafallida"))
                .andExpect(status().is(424))
                .andExpect(jsonPath("$.codigoError").value("GC-0010"))
                .andExpect(jsonPath("$.codigoHttp").value(424));
    }

    @Test
    void noImplementado_retorna501() throws Exception {
        mockMvc.perform(get("/test/noimplementado"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.codigoError").value("GC-0006"))
                .andExpect(jsonPath("$.codigoHttp").value(501));
    }

    @Test
    void noConvertible_retorna406() throws Exception {
        mockMvc.perform(get("/test/noconvertible"))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.codigoError").value("GC-0008"))
                .andExpect(jsonPath("$.codigoHttp").value(406));
    }

    @Test
    void noProcesable_retorna500() throws Exception {
        mockMvc.perform(get("/test/noprocesable"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigoError").value("GC-0011"))
                .andExpect(jsonPath("$.codigoHttp").value(500));
    }

    @Test
    void insercionFallida_retorna409_bodyTiene304() throws Exception {
        // BUG documentado (SCRUM-145): el handler construye Error con
        // codigoHttp=304 (NOT_MODIFIED) pero retorna ResponseEntity con 409 (CONFLICT).
        mockMvc.perform(get("/test/insercionfallida"))
                .andExpect(status().isConflict())                     // HTTP real: 409
                .andExpect(jsonPath("$.codigoError").value("GC-0005"))
                .andExpect(jsonPath("$.codigoHttp").value(304));      // body.codigoHttp: 304 (bug)
    }

    @Test
    void archivoNoConvertible_retorna400() throws Exception {
        mockMvc.perform(get("/test/archivonc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoError").value("GC-0004"))
                .andExpect(jsonPath("$.codigoHttp").value(400));
    }

    @Test
    void inscripcionesCerradas_retorna422() throws Exception {
        mockMvc.perform(get("/test/inscripcionescerradas"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0012"))
                .andExpect(jsonPath("$.codigoHttp").value(422));
    }

    @Test
    void cuposAgotados_retorna409() throws Exception {
        mockMvc.perform(get("/test/cuposagotados"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigoError").value("GC-0013"))
                .andExpect(jsonPath("$.codigoHttp").value(409));
    }

    @Test
    void limiteCursos_retorna422() throws Exception {
        mockMvc.perform(get("/test/limitecursos"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0014"))
                .andExpect(jsonPath("$.codigoHttp").value(422));
    }

    @Test
    void fechaClaseFutura_retorna422() throws Exception {
        mockMvc.perform(get("/test/fechaclasefutura"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0018"))
                .andExpect(jsonPath("$.codigoHttp").value(422));
    }

    @Test
    void registroAsistenciaVencido_retorna422() throws Exception {
        mockMvc.perform(get("/test/registroasistenciavencido"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0019"))
                .andExpect(jsonPath("$.codigoHttp").value(422));
    }

    // ====== Grupo A: body = Map<String,String> ======

    @Test
    void bodyInvalido_retorna400_conCampos() throws Exception {
        DtoDeTest dto = new DtoDeTest();
        dto.setTitulo("");  // blank → @NotBlank falla → MethodArgumentNotValidException

        mockMvc.perform(post("/test/bodyvalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.titulo").exists());
    }

    @Test
    void constraintViolation_retorna400() throws Exception {
        // ConstraintViolationException lanzada con set vacío: el handler itera
        // las violations (ninguna) y retorna Map vacío con status 400.
        mockMvc.perform(get("/test/constraintviolation"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jsonMalformado_retorna400() throws Exception {
        mockMvc.perform(post("/test/jsonroto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{campo_invalido"))  // JSON mal formado
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void handlerMethodValidation_retorna400() throws Exception {
        // standaloneSetup no activa MethodValidationAdapter en Spring MVC 6.1.
        // Se usa webAppContextSetup con @EnableWebMvc para que RequestMappingHandlerAdapter
        // configure correctamente HandlerMethodValidationException.
        AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
        ctx.setServletContext(new MockServletContext());
        ctx.register(MinimalWebMvcConfig.class);
        ctx.refresh();

        MockMvc hmvMvc = MockMvcBuilders.webAppContextSetup(ctx).build();

        try {
            hmvMvc.perform(get("/test/paramblank").param("titulo", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.titulo").exists());
        } finally {
            ctx.close();
        }
    }
}
