package co.edu.unicartagena.transcaribe.pqrs.domain.port;

/**
 * Asignación del consecutivo anual del radicado.
 *
 * <p>La implementación debe garantizar unicidad bajo concurrencia; el dominio
 * solo exige el contrato "dame el siguiente número de este año".</p>
 */
public interface ConsecutivoRadicadoPort {

    long siguienteConsecutivo(int anio);
}
