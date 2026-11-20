package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.ConsecutivoRadicado;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.ConsecutivoRadicadoPort;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa.ConsecutivoRadicadoJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asigna el consecutivo anual del radicado bajo bloqueo exclusivo de fila.
 *
 * <p>Si el año no existe todavía en la tabla, se crea sobre la marcha. Eso
 * evita que el primer día de enero el servicio deje de radicar porque nadie
 * ejecutó una migración, que es la clase de fallo que solo aparece en
 * producción y siempre en mal momento.</p>
 */
@Repository
public class ConsecutivoRadicadoAdapter implements ConsecutivoRadicadoPort {

    private final ConsecutivoRadicadoJpaRepository repositorio;

    public ConsecutivoRadicadoAdapter(ConsecutivoRadicadoJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long siguienteConsecutivo(int anio) {
        ConsecutivoRadicado contador = repositorio.bloquearPorAnio(anio)
                .orElseGet(() -> repositorio.save(new ConsecutivoRadicado(anio)));

        long valor = contador.siguiente();
        repositorio.save(contador);
        return valor;
    }
}
