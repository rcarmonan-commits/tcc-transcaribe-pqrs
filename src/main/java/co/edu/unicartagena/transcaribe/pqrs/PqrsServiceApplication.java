package co.edu.unicartagena.transcaribe.pqrs;

import co.edu.unicartagena.transcaribe.pqrs.infrastructure.config.PropiedadesRadicado;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.config.PropiedadesSeguridad;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servicio de gestión de PQRS e incidencias de flota de Transcaribe S.A.
 *
 * <p>Trabajo colaborativo contextualizado — Ingeniería de Software,
 * Universidad de Cartagena.</p>
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({PropiedadesSeguridad.class, PropiedadesRadicado.class})
public class PqrsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PqrsServiceApplication.class, args);
    }
}
