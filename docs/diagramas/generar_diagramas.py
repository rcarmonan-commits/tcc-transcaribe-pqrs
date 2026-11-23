"""Genera los diagramas del proyecto como imágenes PNG.

Los diagramas se producen con código y no a mano para que puedan regenerarse
cuando el diseño cambie. Un diagrama dibujado una vez en una herramienta
externa envejece en silencio: este se vuelve a ejecutar.

El tamaño del lienzo no es arbitrario. Al insertarse en una página carta con
márgenes de una pulgada, la imagen se escala al ancho útil de 465 puntos, de
modo que el tamaño real del texto sobre el papel es:

    puntos_efectivos = tamano_en_pixeles * 465 / ancho_del_lienzo

Con un lienzo de 1900 px y texto de 13 px el resultado son 3,2 puntos: una
figura que cabe en la página pero que nadie puede leer. Por eso los lienzos de
este archivo son estrechos y la tipografía grande, y por eso el modelo
entidad-relación se reparte en dos figuras en lugar de apiñar diecinueve
tablas en una. El objetivo es no bajar de 6,5 puntos efectivos en el cuerpo.

Uso:  py -3.14 docs/diagramas/generar_diagramas.py
"""

from __future__ import annotations

import os
from dataclasses import dataclass

from PIL import Image, ImageDraw, ImageFont

# ---------------------------------------------------------------------
# Paleta, tipografía y métrica
# ---------------------------------------------------------------------

FONDO = (255, 255, 255)
TINTA = (24, 30, 38)
TINTA_SUAVE = (86, 97, 111)
LINEA = (130, 142, 156)

AZUL = (31, 92, 160)
AZUL_CLARO = (226, 237, 250)
VERDE = (20, 112, 88)
VERDE_CLARO = (221, 242, 235)
AMBAR = (164, 104, 14)
AMBAR_CLARO = (253, 242, 220)
MORADO = (102, 60, 148)
MORADO_CLARO = (239, 231, 249)
GRIS_CLARO = (240, 242, 246)

TAM_CUERPO = 17
TAM_TITULO_CAJA = 21
TAM_ETIQUETA = 15
TAM_ACTOR = 19
TAM_TITULO = 34
TAM_SUBTITULO = 19

INTERLINEA = 23
ANCHO_UTIL_PT = 465

DIRECTORIO = os.path.dirname(os.path.abspath(__file__))

_RUTAS_FUENTE = [
    r"C:\Windows\Fonts\arial.ttf",
    r"C:\Windows\Fonts\segoeui.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
]
_RUTAS_FUENTE_NEGRITA = [
    r"C:\Windows\Fonts\arialbd.ttf",
    r"C:\Windows\Fonts\segoeuib.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
]


def fuente(tamano: int, negrita: bool = False) -> ImageFont.FreeTypeFont:
    for ruta in (_RUTAS_FUENTE_NEGRITA if negrita else _RUTAS_FUENTE):
        if os.path.exists(ruta):
            return ImageFont.truetype(ruta, tamano)
    return ImageFont.load_default()


def alto_de_caja(num_lineas: int, tam_titulo: int = TAM_TITULO_CAJA, lineas_titulo: int = 1) -> int:
    """Alto necesario para una caja con encabezado y n líneas de cuerpo."""
    return 12 + lineas_titulo * (tam_titulo + 7) + 6 + num_lineas * INTERLINEA + 10


# ---------------------------------------------------------------------
# Utilidades de dibujo
# ---------------------------------------------------------------------


@dataclass
class Caja:
    x: int
    y: int
    ancho: int
    alto: int

    @property
    def derecha(self) -> int:
        return self.x + self.ancho

    @property
    def abajo(self) -> int:
        return self.y + self.alto

    @property
    def centro_x(self) -> int:
        return self.x + self.ancho // 2

    @property
    def centro_y(self) -> int:
        return self.y + self.alto // 2


class Lienzo:
    def __init__(self, ancho: int, alto: int, titulo: str, subtitulo: str = ""):
        self.imagen = Image.new("RGB", (ancho, alto), FONDO)
        self.d = ImageDraw.Draw(self.imagen)
        self.ancho = ancho
        self.alto = alto
        self.d.text((34, 26), titulo, font=fuente(TAM_TITULO, True), fill=TINTA)
        if subtitulo:
            self.d.text((34, 72), subtitulo, font=fuente(TAM_SUBTITULO), fill=TINTA_SUAVE)
        self.d.line([(34, 112), (ancho - 34, 112)], fill=LINEA, width=2)

    def caja(self, caja: Caja, titulo: str, lineas=None, relleno=GRIS_CLARO,
             borde=LINEA, color_titulo=TINTA, radio=10, tam_titulo=TAM_TITULO_CAJA):
        self.d.rounded_rectangle(
            [caja.x, caja.y, caja.derecha, caja.abajo],
            radius=radio, fill=relleno, outline=borde, width=2)

        y = caja.y + 12
        for linea_titulo in titulo.split("\n"):
            self.d.text((caja.x + 14, y), linea_titulo,
                        font=fuente(tam_titulo, True), fill=color_titulo)
            y += tam_titulo + 7

        if lineas:
            y += 6
            f = fuente(TAM_CUERPO)
            for linea in lineas:
                self.d.text((caja.x + 14, y), linea, font=f, fill=TINTA_SUAVE)
                y += INTERLINEA

    def elipse(self, caja: Caja, texto: str, relleno=AZUL_CLARO, borde=AZUL):
        self.d.ellipse([caja.x, caja.y, caja.derecha, caja.abajo],
                       fill=relleno, outline=borde, width=2)
        lineas = texto.split("\n")
        alto_total = len(lineas) * (TAM_CUERPO + 6)
        y = caja.centro_y - alto_total // 2
        for i, linea in enumerate(lineas):
            f = fuente(TAM_CUERPO, i == 0)
            ancho = self.d.textlength(linea, font=f)
            self.d.text((caja.centro_x - ancho / 2, y), linea, font=f, fill=TINTA)
            y += TAM_CUERPO + 6

    def flecha(self, inicio, fin, color=LINEA, grosor=2, punta=13, discontinua=False):
        import math
        x1, y1 = inicio
        x2, y2 = fin
        if discontinua:
            self._linea_discontinua(inicio, fin, color, grosor)
        else:
            self.d.line([inicio, fin], fill=color, width=grosor)
        angulo = math.atan2(y2 - y1, x2 - x1)
        for signo in (1, -1):
            a = angulo + signo * math.radians(25)
            self.d.line([(x2, y2), (x2 - punta * math.cos(a), y2 - punta * math.sin(a))],
                        fill=color, width=grosor)

    def _linea_discontinua(self, inicio, fin, color, grosor, trazo=10, hueco=7):
        import math
        x1, y1 = inicio
        x2, y2 = fin
        distancia = math.hypot(x2 - x1, y2 - y1)
        if distancia == 0:
            return
        pasos = int(distancia // (trazo + hueco))
        dx = (x2 - x1) / distancia
        dy = (y2 - y1) / distancia
        for i in range(pasos + 1):
            a = i * (trazo + hueco)
            b = min(a + trazo, distancia)
            self.d.line([(x1 + dx * a, y1 + dy * a), (x1 + dx * b, y1 + dy * b)],
                        fill=color, width=grosor)

    def linea(self, inicio, fin, color=LINEA, grosor=2):
        self.d.line([inicio, fin], fill=color, width=grosor)

    def etiqueta(self, posicion, texto, tam=TAM_ETIQUETA, color=TINTA_SUAVE, fondo=FONDO):
        f = fuente(tam)
        ancho = self.d.textlength(texto, font=f)
        x, y = posicion
        self.d.rectangle([x - 4, y - 2, x + ancho + 4, y + tam + 4], fill=fondo)
        self.d.text((x, y), texto, font=f, fill=color)

    def actor(self, x, y, nombre):
        """Monigote de UML."""
        r = 16
        self.d.ellipse([x - r, y - r, x + r, y + r], outline=TINTA, width=3)
        self.d.line([(x, y + r), (x, y + r + 38)], fill=TINTA, width=3)
        self.d.line([(x - 24, y + r + 14), (x + 24, y + r + 14)], fill=TINTA, width=3)
        self.d.line([(x, y + r + 38), (x - 19, y + r + 68)], fill=TINTA, width=3)
        self.d.line([(x, y + r + 38), (x + 19, y + r + 68)], fill=TINTA, width=3)
        f = fuente(TAM_ACTOR, True)
        for i, linea in enumerate(nombre.split("\n")):
            ancho = self.d.textlength(linea, font=f)
            self.d.text((x - ancho / 2, y + r + 78 + i * 24), linea, font=f, fill=TINTA)

    def leyenda(self, caja: Caja, texto: str):
        self.d.rounded_rectangle([caja.x, caja.y, caja.derecha, caja.abajo],
                                 radius=6, fill=AMBAR_CLARO, outline=AMBAR, width=2)
        f = fuente(TAM_ETIQUETA)
        y = caja.y + 10
        for linea in texto.split("\n"):
            self.d.text((caja.x + 12, y), linea, font=f, fill=TINTA)
            y += 21

    def guardar(self, nombre: str):
        ruta = os.path.join(DIRECTORIO, nombre)
        self.imagen.save(ruta, "PNG")
        efectivo = TAM_CUERPO * ANCHO_UTIL_PT / self.ancho
        marca = "OK" if efectivo >= 6.5 else "REVISAR"
        print(f"  {nombre:34} {self.ancho}x{self.alto}  "
              f"cuerpo: {efectivo:.1f} pt efectivos  [{marca}]")


# ---------------------------------------------------------------------
# 1. Casos de uso
# ---------------------------------------------------------------------


def diagrama_casos_de_uso():
    L = Lienzo(1250, 1340,
               "Diagrama de casos de uso",
               "Servicio de PQRS e incidencias de flota — Transcaribe S.A.")

    L.d.rounded_rectangle([290, 150, 1215, 1200], radius=16,
                          fill=(252, 253, 255), outline=AZUL, width=3)
    f = fuente(24, True)
    ancho = L.d.textlength("Servicio de PQRS", font=f)
    L.d.text((752 - ancho / 2, 166), "Servicio de PQRS", font=f, fill=AZUL)

    casos = [
        ("CU-01\nRadicar solicitud",      525, 275, AZUL_CLARO, AZUL),
        ("CU-02\nConsultar por radicado", 525, 425, AZUL_CLARO, AZUL),
        ("CU-06\nListar y filtrar",       525, 575, VERDE_CLARO, VERDE),
        ("CU-09\nRegistrar incidencia",   525, 725, VERDE_CLARO, VERDE),
        ("CU-10\nConsultar indicadores",  525, 875, AMBAR_CLARO, AMBAR),
        ("CU-11\nAutenticarse",           525, 1025, GRIS_CLARO, LINEA),
        ("CU-03\nCambiar estado",         980, 275, VERDE_CLARO, VERDE),
        ("CU-04\nResponder de fondo",     980, 425, VERDE_CLARO, VERDE),
        ("CU-05\nAsignar a funcionario",  980, 575, VERDE_CLARO, VERDE),
        ("CU-07\nAdministrar flota",      980, 725, MORADO_CLARO, MORADO),
        ("CU-08\nAdministrar rutas",      980, 875, MORADO_CLARO, MORADO),
        ("CU-12\nMarcar vencidas",        980, 1025, AMBAR_CLARO, AMBAR),
    ]

    posiciones = {}
    for texto, cx, cy, relleno, borde in casos:
        caja = Caja(cx - 190, cy - 58, 380, 116)
        L.elipse(caja, texto, relleno, borde)
        posiciones[texto.split("\n")[0]] = caja

    for x, y, nombre in [
        (130, 240, "Ciudadano"),
        (130, 470, "Funcionario\nde PQRS"),
        (130, 740, "Administrador"),
        (130, 980, "Auditor"),
        (130, 1190, "Programador\nde tareas"),
    ]:
        L.actor(x, y, nombre)

    def conectar(ax, ay, clave):
        caja = posiciones[clave]
        L.linea((ax, ay), (caja.x + 6, caja.centro_y), LINEA, 2)

    for destino in ("CU-01", "CU-02"):
        conectar(170, 275, destino)
    for destino in ("CU-03", "CU-04", "CU-06", "CU-09", "CU-11"):
        conectar(170, 505, destino)
    for destino in ("CU-05", "CU-07", "CU-08"):
        conectar(170, 775, destino)
    conectar(170, 1015, "CU-10")
    conectar(170, 1225, "CU-12")

    L.guardar("01-casos-de-uso.png")


# ---------------------------------------------------------------------
# 2 y 3. Modelo entidad-relación, repartido en dos figuras
# ---------------------------------------------------------------------

ANCHO_TABLA = 330


def _dibujar_tablas(L, tablas):
    cajas = {}
    for nombre, (x, y, campos, relleno, borde) in tablas.items():
        caja = Caja(x, y, ANCHO_TABLA, alto_de_caja(len(campos)))
        L.caja(caja, nombre.upper(), campos, relleno, borde, borde, radio=8, tam_titulo=19)
        cajas[nombre] = caja
    return cajas


_PUNTOS = {
    "derecha": lambda c: (c.derecha, c.centro_y),
    "izquierda": lambda c: (c.x, c.centro_y),
    "arriba": lambda c: (c.centro_x, c.y),
    "abajo": lambda c: (c.centro_x, c.abajo),
}


def _relacion(L, cajas, origen, destino, lado_origen, lado_destino, cardinalidad="1 : N"):
    pa = _PUNTOS[lado_origen](cajas[origen])
    pb = _PUNTOS[lado_destino](cajas[destino])
    L.linea(pa, pb, LINEA, 2)
    L.etiqueta(((pa[0] + pb[0]) // 2 - 16, (pa[1] + pb[1]) // 2 - 10), cardinalidad)


def diagrama_entidad_relacion_nucleo():
    L = Lienzo(1200, 1090,
               "Modelo entidad-relación — núcleo de solicitudes",
               "Normalizado en 3FN. PK: clave primaria · FK: clave foránea · UQ: unicidad")

    tablas = {
        "tipo_solicitud": (40, 150, ["PK  id", "    codigo (UQ)", "    nombre",
                                     "    dias_habiles_respuesta", "    fundamento_legal"],
                           AMBAR_CLARO, AMBAR),
        "categoria_pqrs": (430, 150, ["PK  id", "    codigo (UQ)", "    nombre",
                                      "    prioridad_base", "    activa"], AMBAR_CLARO, AMBAR),
        "canal_recepcion": (820, 150, ["PK  id", "    codigo (UQ)", "    nombre"],
                            AMBAR_CLARO, AMBAR),
        "estado_pqrs": (820, 330, ["PK  id", "    codigo (UQ)", "    nombre",
                                   "    es_final", "    orden"], AMBAR_CLARO, AMBAR),
        "tipo_documento": (40, 420, ["PK  id", "    codigo (UQ)", "    nombre"],
                           AMBAR_CLARO, AMBAR),
        "ciudadano": (40, 620, ["PK  id", "FK  tipo_documento_id", "    numero_documento",
                                "    nombres", "    apellidos", "    correo", "    telefono",
                                "    fecha_registro", "UQ  (tipo_doc, numero)"],
                      AZUL_CLARO, AZUL),
        "pqrs": (430, 400, ["PK  id", "    radicado (UQ)", "FK  tipo_solicitud_id",
                            "FK  categoria_id", "FK  canal_id", "FK  estado_id",
                            "FK  ciudadano_id", "FK  ruta_id (nulo)", "FK  vehiculo_id (nulo)",
                            "FK  usuario_asignado_id", "    prioridad", "    asunto",
                            "    descripcion", "    respuesta", "    fecha_radicacion",
                            "    fecha_vencimiento", "    fecha_respuesta",
                            "    respondida_en_plazo"], AZUL_CLARO, AZUL),
        "seguimiento_pqrs": (820, 620, ["PK  id", "FK  pqrs_id", "FK  estado_anterior_id",
                                        "FK  estado_nuevo_id", "FK  usuario_id",
                                        "    observacion", "    fecha_registro"],
                             AZUL_CLARO, AZUL),
    }

    cajas = _dibujar_tablas(L, tablas)

    _relacion(L, cajas, "tipo_solicitud", "pqrs", "abajo", "arriba")
    _relacion(L, cajas, "categoria_pqrs", "pqrs", "abajo", "arriba")
    _relacion(L, cajas, "canal_recepcion", "pqrs", "izquierda", "arriba")
    _relacion(L, cajas, "estado_pqrs", "pqrs", "izquierda", "derecha")
    _relacion(L, cajas, "tipo_documento", "ciudadano", "abajo", "arriba")
    _relacion(L, cajas, "ciudadano", "pqrs", "derecha", "izquierda")
    _relacion(L, cajas, "pqrs", "seguimiento_pqrs", "derecha", "izquierda")

    L.leyenda(Caja(40, 930, 1120, 140),
              "Tercera forma normal: el plazo legal de respuesta vive en TIPO_SOLICITUD y no se repite en cada PQRS.\n"
              "Un cambio normativo se aplica modificando una fila de catálogo, no un millón de registros.\n"
              "RESPONDIDA_EN_PLAZO no es un dato derivado redundante: es el cumplimiento registrado en el instante de\n"
              "responder, porque debe juzgarse con el calendario vigente ese día y no con el de meses después.\n"
              "Las relaciones de PQRS con RUTA, VEHICULO, INCIDENCIA y USUARIO continúan en la figura siguiente.")

    L.guardar("02-entidad-relacion-nucleo.png")


def diagrama_entidad_relacion_soporte():
    L = Lienzo(1200, 1370,
               "Modelo entidad-relación — flota, seguridad y soporte",
               "Normalizado en 3FN. PK: clave primaria · FK: clave foránea · UQ: unicidad")

    tablas = {
        "tipo_vehiculo": (40, 150, ["PK  id", "    codigo (UQ)", "    nombre",
                                    "    capacidad_nominal"], AMBAR_CLARO, AMBAR),
        "tipo_incidencia": (430, 150, ["PK  id", "    codigo (UQ)", "    nombre",
                                       "    prioridad_base"], AMBAR_CLARO, AMBAR),
        "rol": (820, 150, ["PK  id", "    codigo (UQ)", "    nombre", "    descripcion"],
                VERDE_CLARO, VERDE),
        "vehiculo": (40, 390, ["PK  id", "    placa (UQ)", "    numero_interno (UQ)",
                               "FK  tipo_vehiculo_id", "    estado_operativo",
                               "    fecha_vinculacion"], MORADO_CLARO, MORADO),
        "incidencia": (430, 390, ["PK  id", "    consecutivo (UQ)", "FK  vehiculo_id",
                                  "FK  ruta_id", "FK  tipo_incidencia_id", "FK  pqrs_id (nulo)",
                                  "FK  usuario_reporta_id", "    descripcion", "    estado",
                                  "    fecha_hora"], MORADO_CLARO, MORADO),
        "usuario": (820, 390, ["PK  id", "    nombre_usuario (UQ)", "    correo (UQ)",
                               "    hash_contrasena", "    nombres", "    apellidos",
                               "    activo", "    intentos_fallidos", "    bloqueado_hasta",
                               "    fecha_creacion"], VERDE_CLARO, VERDE),
        "ruta": (40, 680, ["PK  id", "    codigo (UQ)", "    nombre", "    origen",
                           "    destino", "    activa"], MORADO_CLARO, MORADO),
        "usuario_rol": (820, 760, ["PK,FK  usuario_id", "PK,FK  rol_id"], VERDE_CLARO, VERDE),
        "festivo": (430, 760, ["PK  fecha", "    descripcion"], GRIS_CLARO, LINEA),
        "consecutivo_radicado": (430, 910, ["PK  anio", "    ultimo_valor"], GRIS_CLARO, LINEA),
        "auditoria_acceso": (820, 920, ["PK  id", "    nombre_usuario", "    accion",
                                        "    recurso", "    ip_hash", "    exitoso",
                                        "    fecha"], GRIS_CLARO, LINEA),
    }

    cajas = _dibujar_tablas(L, tablas)

    _relacion(L, cajas, "tipo_vehiculo", "vehiculo", "abajo", "arriba")
    _relacion(L, cajas, "tipo_incidencia", "incidencia", "abajo", "arriba")
    _relacion(L, cajas, "vehiculo", "incidencia", "derecha", "izquierda")
    _relacion(L, cajas, "ruta", "incidencia", "derecha", "izquierda")
    _relacion(L, cajas, "rol", "usuario", "abajo", "arriba")
    _relacion(L, cajas, "usuario", "usuario_rol", "abajo", "arriba")

    # En lugar de arrastrar una flecha hasta la figura anterior, que tendría
    # que atravesar media lámina, se anota el vínculo junto a la tabla.
    inc = cajas["incidencia"]
    L.etiqueta((inc.x, inc.abajo + 14),
               "pqrs_id referencia PQRS (figura anterior) — 0..1 : N", TAM_ETIQUETA, AZUL)

    L.leyenda(Caja(40, 1180, 1120, 160),
              "La relación N:M entre usuarios y roles se resuelve con la tabla puente USUARIO_ROL, de clave primaria\n"
              "compuesta, como exige la primera forma normal. La capacidad nominal vive en TIPO_VEHICULO y no en cada\n"
              "bus, lo que elimina la dependencia transitiva que rompería la tercera forma normal.\n"
              "INCIDENCIA se vincula opcionalmente con PQRS: ese enlace es el que permite cruzar la queja del\n"
              "ciudadano con el hecho operativo que la originó, que es justamente lo que un registro en hojas de\n"
              "cálculo no puede responder. Los catálogos (ámbar) son inmutables en ejecución.")

    L.guardar("03-entidad-relacion-soporte.png")


# ---------------------------------------------------------------------
# 4. Arquitectura
# ---------------------------------------------------------------------


def diagrama_arquitectura():
    L = Lienzo(1200, 1200,
               "Arquitectura en capas con inversión de dependencias",
               "Las flechas indican la dirección de las dependencias: siempre hacia el dominio")

    capas = [
        (150, "Capa de entrada  ·  infrastructure.web", AZUL_CLARO, AZUL,
         ["PqrsController · AutenticacionController · FlotaController",
          "IncidenciaController · IndicadorController · CatalogoController",
          "Traduce HTTP a llamadas de caso de uso. Sin reglas de negocio."]),
        (330, "Capa de seguridad  ·  infrastructure.security", MORADO_CLARO, MORADO,
         ["ConfiguracionSeguridad · FiltroAutenticacionJwt · ProveedorJwt",
          "FiltroLimitePeticiones · ManejadorAccesoNoAutorizado",
          "Deniega por omisión; autoriza por método con @PreAuthorize."]),
        (510, "Capa de aplicación  ·  application", VERDE_CLARO, VERDE,
         ["RadicarPqrs · GestionarPqrs · ConsultarPqrs (puertos de entrada)",
          "RadicacionPqrsService · GestionPqrsService · AutenticacionService",
          "Orquesta: resuelve referencias, pide cálculos, abre la transacción."]),
        (690, "Capa de dominio  ·  domain", AMBAR_CLARO, AMBAR,
         ["Pqrs · Ciudadano · Usuario · Incidencia · Vehiculo · Ruta",
          "CodigoEstadoPqrs · CalendarioHabil · PoliticaPrioridad",
          "Puertos: PqrsRepositoryPort · CatalogoRepositoryPort · AuditoriaPort",
          "Sin una sola anotación de framework. Se prueba sin levantar Spring."]),
        (920, "Capa de persistencia  ·  infrastructure.persistence", GRIS_CLARO, LINEA,
         ["PqrsRepositoryAdapter · CatalogoRepositoryAdapter · AdaptadoresOperacion",
          "EspecificacionesPqrs · TraductorPaginacion · ConsecutivoRadicadoAdapter",
          "Implementa los puertos que declara el dominio. PostgreSQL + Flyway."]),
    ]

    for y, titulo, relleno, borde, lineas in capas:
        caja = Caja(40, y, 1120, alto_de_caja(len(lineas)))
        L.caja(caja, titulo, lineas, relleno, borde, borde, radio=12)

    for origen, destino in [(306, 326), (486, 506), (666, 686)]:
        L.flecha((600, origen), (600, destino), TINTA_SUAVE, 3)

    L.flecha((600, 916), (600, 896), VERDE, 3)
    L.etiqueta((620, 896), "implementa los puertos que el dominio declara", TAM_ETIQUETA, VERDE)

    L.leyenda(Caja(40, 1080, 1120, 95),
              "Inversión de dependencias: la persistencia depende del dominio y no al revés. El dominio declara qué\n"
              "necesita (PqrsRepositoryPort) y la infraestructura lo satisface (PqrsRepositoryAdapter). Esa es la razón\n"
              "de que las 135 pruebas de dominio y aplicación corran en 6,8 segundos, sin base de datos ni contexto de\n"
              "Spring, mientras una sola prueba de integración consume 38,7 segundos casi todos de arranque.")

    L.guardar("04-arquitectura.png")


# ---------------------------------------------------------------------
# 5. Componentes
# ---------------------------------------------------------------------


def diagrama_componentes():
    L = Lienzo(1200, 1190,
               "Diagrama de componentes",
               "Componentes del servicio y las interfaces que ofrecen y consumen")

    componentes = [
        (40, 150, "«componente»\nAPI REST", AZUL_CLARO, AZUL,
         ["Ofrece:  /api/v1/**", "Consume: casos de uso"]),
        (430, 150, "«componente»\nSeguridad", MORADO_CLARO, MORADO,
         ["Ofrece:  cadena de filtros", "Consume: UsuarioRepositoryPort"]),
        (820, 150, "«componente»\nDocumentación", GRIS_CLARO, LINEA,
         ["Ofrece:  /v3/api-docs", "Desactivable en producción"]),
        (40, 450, "«componente»\nCasos de uso", VERDE_CLARO, VERDE,
         ["Ofrece:  RadicarPqrs", "         GestionarPqrs", "         ConsultarPqrs"]),
        (430, 450, "«componente»\nReglas de dominio", AMBAR_CLARO, AMBAR,
         ["Ofrece:  CalendarioHabil", "         PoliticaPrioridad", "Sin dependencias externas"]),
        (820, 450, "«componente»\nTareas programadas", AMBAR_CLARO, AMBAR,
         ["TareaVencimientoPqrs", "ProveedorCalendarioHabil", "Consume: GestionarPqrs"]),
        (40, 790, "«componente»\nPersistencia JPA", GRIS_CLARO, LINEA,
         ["Implementa los puertos", "Consume: DataSource"]),
        (430, 790, "«componente»\nMigraciones Flyway", GRIS_CLARO, LINEA,
         ["V1 esquema inicial", "V2 catálogos y calendario"]),
        (820, 790, "«base de datos»\nPostgreSQL 16", VERDE_CLARO, VERDE,
         ["19 tablas en 3FN", "Índices sobre estado y", "fecha de vencimiento"]),
    ]

    cajas = {}
    for x, y, titulo, relleno, borde, lineas in componentes:
        caja = Caja(x, y, 340, alto_de_caja(len(lineas), lineas_titulo=2))
        L.caja(caja, titulo, lineas, relleno, borde, borde, radio=10, tam_titulo=19)
        cajas[titulo] = caja

    def unir(a, b, texto=""):
        ca, cb = cajas[a], cajas[b]
        if abs(ca.centro_y - cb.centro_y) > 60:
            inicio, fin = (ca.centro_x, ca.abajo), (cb.centro_x, cb.y)
        elif ca.centro_x < cb.centro_x:
            inicio, fin = (ca.derecha, ca.centro_y), (cb.x, cb.centro_y)
        else:
            inicio, fin = (ca.x, ca.centro_y), (cb.derecha, cb.centro_y)
        L.flecha(inicio, fin, TINTA_SUAVE, 2, discontinua=True)
        if texto:
            L.etiqueta(((inicio[0] + fin[0]) // 2 + 8, (inicio[1] + fin[1]) // 2 - 10), texto)

    unir("«componente»\nAPI REST", "«componente»\nCasos de uso", "usa")
    unir("«componente»\nSeguridad", "«componente»\nReglas de dominio", "usa")
    unir("«componente»\nCasos de uso", "«componente»\nReglas de dominio", "usa")
    unir("«componente»\nCasos de uso", "«componente»\nPersistencia JPA", "puertos")
    unir("«componente»\nTareas programadas", "«componente»\nReglas de dominio", "usa")
    unir("«componente»\nMigraciones Flyway", "«base de datos»\nPostgreSQL 16", "DDL")

    persistencia = cajas["«componente»\nPersistencia JPA"]
    postgres = cajas["«base de datos»\nPostgreSQL 16"]
    canal = persistencia.abajo + 40
    L.linea((persistencia.centro_x, persistencia.abajo), (persistencia.centro_x, canal), TINTA_SUAVE)
    L.linea((persistencia.centro_x, canal), (postgres.centro_x, canal), TINTA_SUAVE)
    L.flecha((postgres.centro_x, canal), (postgres.centro_x, postgres.abajo + 2), TINTA_SUAVE, 2)
    L.etiqueta((persistencia.centro_x + 270, canal - 24), "JDBC")

    L.leyenda(Caja(40, 1070, 1120, 95),
              "Las flechas discontinuas son dependencias de uso. El componente de reglas de dominio no tiene ninguna\n"
              "saliente: es el centro de la arquitectura y nada de lo que hay fuera le condiciona. La persistencia\n"
              "implementa los puertos que el dominio declara, de ahí que su flecha atraviese el diagrama hacia arriba\n"
              "en lugar de salir del dominio hacia la base de datos.")

    L.guardar("05-componentes.png")


# ---------------------------------------------------------------------
# 6. Despliegue
# ---------------------------------------------------------------------


def diagrama_despliegue():
    L = Lienzo(1200, 1240,
               "Diagrama de despliegue",
               "Nodos de ejecución, contenedores y protocolos de comunicación")

    L.caja(Caja(40, 170, 300, alto_de_caja(4, lineas_titulo=2)),
           "«dispositivo»\nEquipo del ciudadano",
           ["Navegador web", "Aplicación móvil", "", "Radica y consulta"],
           AZUL_CLARO, AZUL, AZUL, tam_titulo=19)

    L.caja(Caja(40, 430, 300, alto_de_caja(4, lineas_titulo=2)),
           "«dispositivo»\nEstación de funcionario",
           ["Navegador web", "Cliente Postman", "", "Gestiona con JWT"],
           AZUL_CLARO, AZUL, AZUL, tam_titulo=19)

    L.d.rounded_rectangle([400, 150, 1165, 730], radius=14,
                          fill=(252, 253, 255), outline=VERDE, width=3)
    L.d.text((422, 168), "«nodo de ejecución»  Servidor Docker / Render",
             font=fuente(22, True), fill=VERDE)
    L.d.text((422, 200), "Red interna; la base de datos no se publica al exterior",
             font=fuente(TAM_ETIQUETA), fill=TINTA_SUAVE)

    L.caja(Caja(425, 240, 350, alto_de_caja(5, lineas_titulo=2)),
           "«contenedor»\ntranscaribe-pqrs-api",
           ["eclipse-temurin:21-jre-alpine", "Spring Boot 3.3.5", "Usuario sin privilegios",
            "Archivos de solo lectura", "Escucha en $PORT"],
           VERDE_CLARO, VERDE, VERDE, tam_titulo=19)

    L.caja(Caja(800, 240, 340, alto_de_caja(5, lineas_titulo=2)),
           "«contenedor»\ntranscaribe-pqrs-bd",
           ["postgres:16-alpine", "Volumen persistente", "Puerto 5432 solo en",
            "la red interna", "Esquema por Flyway"],
           MORADO_CLARO, MORADO, MORADO, tam_titulo=19)

    L.caja(Caja(425, 530, 715, alto_de_caja(3)),
           "«artefacto»  aplicacion.jar",
           ["Construido en una etapa separada: la imagen final no lleva Maven,",
            "ni el código fuente, ni el repositorio local de dependencias.",
            "Variables: DB_URL · DB_USER · DB_PASSWORD · JWT_SECRET · CORS_ORIGENES"],
           GRIS_CLARO, LINEA, TINTA)

    L.caja(Caja(40, 800, 545, alto_de_caja(4, lineas_titulo=2)),
           "«entorno de integración»\nGitHub Actions",
           ["Compila y ejecuta las 143 pruebas", "Construye la imagen Docker",
            "Analiza dependencias vulnerables", "Sin secretos en el repositorio"],
           AMBAR_CLARO, AMBAR, AMBAR, tam_titulo=19)

    L.caja(Caja(625, 800, 535, alto_de_caja(4, lineas_titulo=2)),
           "«repositorio»\nGitHub",
           ["Código, migraciones y Postman", "Una rama por capa de arquitectura",
            "render.yaml para el despliegue", "El archivo .env nunca se versiona"],
           GRIS_CLARO, LINEA, TINTA, tam_titulo=19)

    L.flecha((342, 255), (422, 320), TINTA_SUAVE, 2)
    L.etiqueta((350, 232), "HTTPS / JSON")
    L.flecha((342, 515), (422, 430), TINTA_SUAVE, 2)
    L.etiqueta((345, 525), "HTTPS + Bearer")
    L.flecha((777, 355), (798, 355), TINTA_SUAVE, 2)
    L.etiqueta((762, 318), "JDBC")
    L.flecha((587, 860), (622, 860), TINTA_SUAVE, 2)
    L.flecha((330, 798), (415, 700), TINTA_SUAVE, 2, discontinua=True)
    L.etiqueta((180, 752), "publica la imagen verificada")

    L.leyenda(Caja(40, 1030, 1120, 70),
              "El mismo Dockerfile se usa en local con Compose y en Render mediante render.yaml, de modo que lo que se\n"
              "verifica en la máquina es exactamente lo que se despliega. Render inyecta el puerto en la variable PORT\n"
              "y las credenciales de la base desde su servicio gestionado: nada de eso vive en el repositorio.")

    L.guardar("06-despliegue.png")


# ---------------------------------------------------------------------
# 7. Scrum
# ---------------------------------------------------------------------


def diagrama_scrum():
    L = Lienzo(1250, 1270,
               "Gestión del proyecto con Scrum",
               "Cuatro sprints de una semana · Velocidad medida en puntos de historia")

    sprints = [
        ("Sprint 1\nCimientos", 40, VERDE, VERDE_CLARO,
         ["Modelo entidad-relación", "Migraciones Flyway", "Entidades y agregados",
          "Máquina de estados", "Calendario hábil", "", "21 puntos"]),
        ("Sprint 2\nCasos de uso", 345, AZUL, AZUL_CLARO,
         ["Radicación", "Gestión de estados", "Respuesta de fondo", "Consulta ciudadana",
          "Pruebas unitarias", "", "26 puntos"]),
        ("Sprint 3\nSeguridad y flota", 650, MORADO, MORADO_CLARO,
         ["Autenticación JWT", "Controles OWASP", "Flota e incidencias", "Indicadores",
          "Pruebas de integración", "", "24 puntos"]),
        ("Sprint 4\nEntrega", 955, AMBAR, AMBAR_CLARO,
         ["Docker y Compose", "Integración continua", "Colección de Postman",
          "Despliegue en Render", "Documentación técnica", "", "18 puntos"]),
    ]

    for titulo, x, color, claro, historias in sprints:
        caja = Caja(x, 150, 255, alto_de_caja(len(historias), lineas_titulo=2))
        L.caja(caja, titulo, historias, claro, color, color, radio=10, tam_titulo=19)
        if x < 955:
            L.flecha((caja.derecha + 3, caja.centro_y), (caja.derecha + 44, caja.centro_y),
                     TINTA_SUAVE, 2)

    L.d.text((40, 580), "Trabajo pendiente (burndown) — 89 puntos comprometidos",
             font=fuente(24, True), fill=TINTA)

    ox, oy = 150, 1090
    ancho, alto = 820, 400

    L.linea((ox, oy - alto), (ox, oy), LINEA, 2)
    L.linea((ox, oy), (ox + ancho, oy), LINEA, 2)

    maximo = 89
    f = fuente(TAM_CUERPO)
    for i in range(5):
        valor = round(maximo * (4 - i) / 4)
        y = oy - int(alto * (valor / maximo))
        L.d.text((ox - 58, y - 10), f"{valor:>3}", font=f, fill=TINTA_SUAVE)
        L.d.line([(ox - 6, y), (ox + ancho, y)], fill=(232, 236, 240), width=1)

    etiquetas = ["Inicio", "Fin S1", "Fin S2", "Fin S3", "Fin S4"]
    ideal = [89, 67, 45, 22, 0]
    real = [89, 70, 44, 19, 0]

    pi, pr = [], []
    for i in range(5):
        x = ox + int(ancho * i / 4)
        pi.append((x, oy - int(alto * ideal[i] / maximo)))
        pr.append((x, oy - int(alto * real[i] / maximo)))
        w = L.d.textlength(etiquetas[i], font=f)
        L.d.text((x - w / 2, oy + 16), etiquetas[i], font=f, fill=TINTA_SUAVE)

    L._linea_discontinua(pi[0], pi[-1], TINTA_SUAVE, 2)
    for i in range(4):
        L.d.line([pr[i], pr[i + 1]], fill=AZUL, width=5)
    for punto in pr:
        L.d.ellipse([punto[0] - 8, punto[1] - 8, punto[0] + 8, punto[1] + 8],
                    fill=AZUL, outline=FONDO, width=2)

    L.d.line([(1010, 700), (1070, 700)], fill=TINTA_SUAVE, width=2)
    L.d.text((1082, 690), "Ideal", font=f, fill=TINTA_SUAVE)
    L.d.line([(1010, 736), (1070, 736)], fill=AZUL, width=5)
    L.d.text((1082, 726), "Real", font=f, fill=TINTA)

    L.leyenda(Caja(1000, 790, 220, 250),
              "Desviación del\nsprint 1: tres puntos\nsin cerrar por el\ncálculo de días\nhábiles, más costoso\nde lo estimado.\n\nSe recuperaron en el\nsprint 2 al reutilizar\nel calendario ya\nprobado.")

    L.guardar("07-scrum.png")


# ---------------------------------------------------------------------

if __name__ == "__main__":
    print("Generando diagramas en", DIRECTORIO)
    for antiguo in os.listdir(DIRECTORIO):
        if antiguo.endswith(".png"):
            os.remove(os.path.join(DIRECTORIO, antiguo))

    diagrama_casos_de_uso()
    diagrama_entidad_relacion_nucleo()
    diagrama_entidad_relacion_soporte()
    diagrama_arquitectura()
    diagrama_componentes()
    diagrama_despliegue()
    diagrama_scrum()
    print("Listo.")
