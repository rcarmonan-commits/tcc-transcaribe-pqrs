package co.edu.unicartagena.transcaribe.pqrs.infrastructure.config;

import co.edu.unicartagena.transcaribe.pqrs.domain.port.ConsecutivoRadicadoPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.GeneradorRadicado;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.GeneradorRadicadoConsecutivo;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.PoliticaPrioridad;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.Reloj;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publica como beans las piezas del dominio.
 *
 * <p>Las clases de dominio no llevan anotaciones de Spring: si las llevaran, el
 * núcleo dependería del framework y dejaría de poder probarse —o reutilizarse—
 * sin él. Se instancian aquí, en el borde, que es justamente el trabajo de una
 * clase de configuración.</p>
 */
@Configuration
public class ConfiguracionDominio {

    /** Zona horaria oficial de la operación. */
    public static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    @Bean
    Reloj reloj() {
        Clock reloj = Clock.system(ZONA_COLOMBIA);
        return () -> LocalDateTime.now(reloj);
    }

    @Bean
    PoliticaPrioridad politicaPrioridad() {
        return new PoliticaPrioridad();
    }

    @Bean
    GeneradorRadicado generadorRadicado(ConsecutivoRadicadoPort consecutivos,
                                        PropiedadesRadicado propiedades) {
        return new GeneradorRadicadoConsecutivo(consecutivos, propiedades.prefijo());
    }
}
