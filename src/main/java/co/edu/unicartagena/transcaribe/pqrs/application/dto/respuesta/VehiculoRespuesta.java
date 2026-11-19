package co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta;

import java.time.LocalDate;

/** Vehículo de la flota tal como se publica en el API. */
public record VehiculoRespuesta(String placa,
                                String numeroInterno,
                                String tipoVehiculo,
                                int capacidadNominal,
                                String estadoOperativo,
                                LocalDate fechaVinculacion) {
}
