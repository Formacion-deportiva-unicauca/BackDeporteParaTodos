package co.edu.unicauca.deporteParaTodos.configurations;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// SCRUM-185: "hoy" para la ventana de inscripcion se calcula siempre con este
// Clock (zona America/Bogota), nunca con CURRENT_DATE de MySQL ni con
// LocalDate.now() sin zona explicita -- inyectable para poder fijarlo en tests.
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }
}
