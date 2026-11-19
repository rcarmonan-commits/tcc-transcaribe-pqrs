package co.edu.unicartagena.transcaribe.pqrs.domain.service;

/** Construye el número de radicado con el que el ciudadano consultará su solicitud. */
public interface GeneradorRadicado {

    String generar(int anio);
}
