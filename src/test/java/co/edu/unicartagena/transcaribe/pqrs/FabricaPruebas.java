package co.edu.unicartagena.transcaribe.pqrs;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.CanalRecepcion;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.CategoriaPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ciudadano;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.EstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Pqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Rol;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ruta;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.TipoDocumento;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.TipoIncidencia;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.TipoSolicitud;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.TipoVehiculo;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Usuario;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Vehiculo;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Construye objetos de dominio para las pruebas.
 *
 * <p>Los catálogos son inmutables y sin constructor público a propósito: el
 * código de producción no debe poder fabricar un estado o un plazo legal de la
 * nada. Esa restricción es correcta, y la consecuencia es que las pruebas
 * necesitan esta puerta trasera, que vive solo en el árbol de pruebas.</p>
 *
 * <p>La alternativa —abrir constructores públicos solo para poder probar—
 * debilitaría el modelo real a cambio de comodidad en las pruebas, que es
 * justo el intercambio que no conviene hacer.</p>
 */
public final class FabricaPruebas {

    private FabricaPruebas() {
        // Fábrica estática.
    }

    public static TipoDocumento tipoDocumento(String codigo) {
        return instanciar(TipoDocumento.class, Map.of(
                "id", 1,
                "codigo", codigo,
                "nombre", "Documento " + codigo));
    }

    public static TipoSolicitud tipoSolicitud(String codigo, int diasHabiles) {
        return instanciar(TipoSolicitud.class, Map.of(
                "id", 1,
                "codigo", codigo,
                "nombre", "Tipo " + codigo,
                "diasHabilesRespuesta", diasHabiles,
                "fundamentoLegal", "Ley 1755 de 2015, art. 14"));
    }

    public static CategoriaPqrs categoria(String codigo, Prioridad prioridadBase) {
        return instanciar(CategoriaPqrs.class, Map.of(
                "id", 1,
                "codigo", codigo,
                "nombre", "Categoría " + codigo,
                "prioridadBase", prioridadBase,
                "activa", true));
    }

    public static CanalRecepcion canal(String codigo) {
        return instanciar(CanalRecepcion.class, Map.of(
                "id", 1,
                "codigo", codigo,
                "nombre", "Canal " + codigo));
    }

    public static EstadoPqrs estado(CodigoEstadoPqrs codigo) {
        return instanciar(EstadoPqrs.class, Map.of(
                "id", codigo.ordinal() + 1,
                "codigo", codigo.name(),
                "nombre", codigo.name(),
                "esFinal", codigo.esFinal(),
                "orden", codigo.ordinal() + 1));
    }

    public static TipoVehiculo tipoVehiculo(String codigo, int capacidad) {
        return instanciar(TipoVehiculo.class, Map.of(
                "id", 1,
                "codigo", codigo,
                "nombre", "Tipo " + codigo,
                "capacidadNominal", capacidad));
    }

    public static TipoIncidencia tipoIncidencia(String codigo, Prioridad prioridadBase) {
        return instanciar(TipoIncidencia.class, Map.of(
                "id", 1,
                "codigo", codigo,
                "nombre", "Incidencia " + codigo,
                "prioridadBase", prioridadBase));
    }

    public static Rol rol(String codigo) {
        return instanciar(Rol.class, Map.of(
                "id", 1,
                "codigo", codigo,
                "nombre", codigo,
                "descripcion", "Rol de prueba " + codigo));
    }

    public static Ciudadano ciudadano(String numeroDocumento) {
        return new Ciudadano(
                tipoDocumento("CC"),
                numeroDocumento,
                "Carlos Andrés",
                "Pacheco",
                "capacheco@correo.com",
                "3015558841",
                LocalDateTime.of(2026, 2, 2, 9, 12));
    }

    public static Usuario usuario(String nombreUsuario) {
        return new Usuario(
                nombreUsuario,
                nombreUsuario + "@transcaribe.gov.co",
                "$2b$12$hashDePrueba",
                "Jorge",
                "López",
                LocalDateTime.of(2026, 1, 15, 8, 0));
    }

    public static Vehiculo vehiculo(String placa) {
        return new Vehiculo(placa, "1101",
                tipoVehiculo("ARTICULADO", 160), LocalDate.of(2024, 3, 1));
    }

    public static Ruta ruta(String codigo) {
        return new Ruta(codigo, "Ruta " + codigo, "Portal", "Centro");
    }

    /** Solicitud radicada el 2026-03-02, con vencimiento el 2026-03-23. */
    public static Pqrs pqrsRadicada() {
        return pqrsRadicada(LocalDateTime.of(2026, 3, 2, 10, 0), LocalDate.of(2026, 3, 23));
    }

    public static Pqrs pqrsRadicada(LocalDateTime radicacion, LocalDate vencimiento) {
        return new Pqrs(
                "TC-2026-000001",
                tipoSolicitud("QUEJA", 15),
                categoria("SERVICIO_RUTA", Prioridad.MEDIA),
                canal("WEB"),
                estado(CodigoEstadoPqrs.RADICADA),
                ciudadano("1047398221"),
                Prioridad.MEDIA,
                "Bus de la ruta T100 no cumple la frecuencia anunciada",
                "El bus de la ruta T100 pasó con cuarenta minutos de retraso en hora pico.",
                radicacion,
                vencimiento);
    }

    private static <T> T instanciar(Class<T> tipo, Map<String, Object> valores) {
        try {
            Constructor<T> constructor = tipo.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instancia = constructor.newInstance();

            for (Map.Entry<String, Object> valor : valores.entrySet()) {
                Field campo = tipo.getDeclaredField(valor.getKey());
                campo.setAccessible(true);
                campo.set(instancia, valor.getValue());
            }
            return instancia;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "No se pudo construir el objeto de prueba " + tipo.getSimpleName(), e);
        }
    }
}
