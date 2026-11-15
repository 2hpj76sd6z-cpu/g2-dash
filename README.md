<div align="center">

# G² Dash

**Interfaz automotriz personalizable · Web y Android · v0.5.4**

Tablero horizontal para SEAT Ibiza 6J, con identidad G², widgets ahumados configurables y acceso a las funciones Android del estéreo.

![Encabezado actual de G² Dash](docs/capturas/actual/02-encabezado.png)

[Galería visual por sección](docs/capturas/README.md) · [Reproductor ampliado](docs/capturas/README.md) · [Estado y hoja de ruta](ROADMAP.md)

</div>

## Cómo va el proyecto

**Versión actual: v0.5.4 (desarrollo).** Integra la galería conceptual, el centro de Apps y un reproductor ampliado para la sesión musical local de Android. Falta validar en el Gearelec; la música desde el teléfono por Wi‑Fi sigue pendiente.

### Bienvenida al iniciar G² Dash

El símbolo SEAT de la primera pantalla ya utiliza el **vector original**, no una recreación. Fuente del emblema: [Simple Icons – SEAT](https://github.com/simple-icons/simple-icons/blob/develop/icons/seat.svg). La marca SEAT pertenece a su titular; su aparición identifica el vehículo del proyecto y no implica afiliación comercial.


La apertura de la app (web o APK Android) muestra tres momentos: emblema **SEAT / G² DASH**, saludo dinámico a Doble G y un resumen de avance. El resumen indica que el Ibiza tiene fotos originales y un visor conceptual de 12 vistas, y que la música local muestra título y portada; la música del teléfono por Wi‑Fi sigue pendiente de definir. Después aparece el tablero de siempre. Toda la secuencia dura aproximadamente **4.6 segundos**, incluye **Omitir** y se puede volver a abrir desde **Ajustes → Ver bienvenida**.

El saludo aparece **al abrir la aplicación**. Para verlo al encender el vehículo, Android debe elegir G² Dash como HOME y el estéreo debe ejecutarlo al inicio; la elección es manual, no automática. Algunos estéreos reanudan desde suspensión sin reiniciar la aplicación y, en ese caso, puede no repetirse. Esta conducta requiere validación en el Gearelec.

### Mi Ibiza real y conceptual — acceso directo

El visor de ilustraciones incluye ahora el botón **FOTOS REALES** y una miniatura del Ibiza auténtico. La ventana de fotos originales tiene **12 VISTAS** para volver al concepto sin ventanas superpuestas. El ángulo seleccionado se conserva; las fotos, la matrícula protegida y el fondo permanecen sin alteraciones.

[Ver la comparación visual verificable](docs/capturas/COMPARADOR_IBIZA_v0.5.3.md).

### Ibiza — 12 vistas conceptuales

**Detalle visual v0.5.1:** modo **1:1** que respeta los 500×314 píxeles originales y opción **1.3×** para ver el auto más grande; también gestos multitramo y precarga limitada. [Comparación real de ambos modos](docs/capturas/MEJORA_IBIZA_v0.5.1.md).

El botón **12 VISTAS** abre una galería de doce ilustraciones de la propuesta aprobada. Flechas, miniaturas, selector de 0° a 330°, deslizamiento y recorrido automático opcional. **No es un modelo 3D ni doce fotos reales**. La fotografía original del Ibiza y el fondo del tablero continúan intactos.

[Ver detalles y capturas del visor](docs/IBIZA_360_REFERENCIA_APROBADA.md).

### Apps — centro de aplicaciones v0.5.1

El botón **Apps** abre un panel visual con iconos de las aplicaciones realmente instaladas en Android, búsqueda inmediata, pestañas **Todas / ★ Favoritas**, apertura táctil y favoritos guardados únicamente en el dispositivo. No requiere `QUERY_ALL_PACKAGES`, cuenta en línea ni conexión de red.

En **la demo web** se muestran seis iconos de muestra con la advertencia **EJEMPLO VISUAL — NO SON APPS INSTALADAS**. No se inventa qué aplicaciones tiene el estéreo. Al instalar el APK, Android presenta su lista real. El diseño se adapta a 1280×720 y pantallas compactas.

### Música — reproductor ampliado v0.5.2

El icono de ampliar en el widget musical abre una pantalla con portada, título, artista, servicio de origen, estado, tiempo transcurrido y controles grandes. En Android, recibe datos de **MediaSession** del reproductor que está sonando **en ese mismo estéreo**, con autorización del usuario.

La barra de progreso permite adelantar/retroceder únicamente cuando la app musical publica `ACTION_SEEK_TO`; de otro modo permanece bloqueada. Los controles de anterior/siguiente/reproducir respetan las acciones disponibles. La web no reproduce audio: su captura de ejemplo indica expresamente **EJEMPLO VISUAL** y mantiene los controles desactivados.

La conexión musical desde un iPhone/Android por el punto de acceso Wi-Fi **no está implementada**; se requiere un canal de transmisión de metadatos compatible o una aplicación complementaria. No afecta las fotografías ni el fondo del launcher.

### iPhone por Wi-Fi / AirPlay — v0.5.3

Se añadió **Música → AIRPLAY** con un asistente para conectar el iPhone al punto de acceso del estéreo y reproducir a través de [AirPlay Receiver & Server](https://github.com/jqssun/android-airplay-server), un **receptor externo independiente** (Android 7.0+). Este receptor recibe audio desde AirPlay e integra una MediaSession de Android con información de canción, carátula y controles cuando el emisor los proporciona.

G² Dash verifica la versión de Android, detecta si el paquete `io.github.jqssun.airplay` está instalado y permite abrirlo o visitar su página oficial de instalación desde el estéreo, siempre **por decisión del usuario**. Si el receptor publica una sesión multimedia, G² Dash podrá mostrar su información y enviar los controles permitidos por la sesión.

**Estado: integración en código; todavía no verificada físicamente en el Gearelec.** No se puede afirmar que el hotspot del fabricante anuncie AirPlay por mDNS, que el receptor siga activo en segundo plano o que cada servicio musical permita compartir carátula/controles. El iPhone y Android deben estar en una red local que permita comunicación entre ambos; la música en línea puede usar datos móviles del iPhone. Android 6 o anterior no es compatible con este receptor.

El sitio web no realiza una conexión AirPlay: el asistente muestra de forma explícita «VISTA WEB» y oculta los botones nativos de instalar/abrir. No se ha incorporado código de terceros con licencia GPL al proyecto G² Dash; el receptor se instala como app separada.

### Rendimiento — modo ligero para estéreos de 2 GB (v0.5.4)

En **Ajustes → Rendimiento gráfico** puedes seleccionar **Visual normal** o **Modo ligero**. El segundo desactiva el desenfoque de vidrio, el difuminado del fondo y transiciones decorativas, pero conserva las imágenes, distribución, color, ajustes previos y funcionalidad. La preferencia se guarda en este dispositivo. Esta es una **opción de ahorro de efectos**, no una mejora de FPS o RAM ya medida físicamente. [Capturas comparativas verificables](docs/capturas/RENDIMIENTO_v0.5.4.md).

### Ya está implementado

- Bienvenida SEAT / G² DASH con saludo a Doble G y resumen del estado v0.5.2.
- Asistente para iPhone por Wi-Fi/AirPlay, con consulta del receptor instalado y conexión a la MediaSession que publique (pendiente prueba física).
- Reproductor musical ampliado con carátula y barra de avance condicionada a las capacidades de MediaSession.
- Centro de Apps con iconos de Android, búsqueda, favoritos y apertura segura (en web, demostración marcada).
- Visor conceptual del Ibiza con 12 vistas y controles de ángulo, separado de las fotos reales.
- Tres estilos de logotipo G² DASH: APEX, PULSE y ORBIT.
- Widgets ahumados con cuatro tonalidades, opacidad y difuminado ajustables.
- Ajustes visuales guardados localmente.
- Capturas automáticas por sección después de cada subida de código a `main`.

### Lo siguiente

- Revisar el recorte transparente opcional del Ibiza; la vista inicial usa la fotografía frontal completa para evitar depender de un contorno aproximado.
- Validar el APK en el estéreo Gearelec y medir fluidez con sus 2 GB de RAM.
- Definir una solución para música desde el teléfono por Wi-Fi: el punto de acceso no comparte por sí solo sus metadatos.
- Mantener pendientes el mapa real, la conexión OBD y el launcher predeterminado.

### Funciones disponibles en la web

- Tablero con reloj, fecha, mapa ilustrativo, panel del Ibiza y accesos.
- Logotipo seleccionable entre APEX, PULSE y ORBIT.
- Widgets ahumados con cuatro tonalidades, opacidad de 20–88% y difuminado de 0–16 px.
- Apariencia automática, día y noche; fondos y preferencias guardados en el navegador.
- Mi Ibiza abre con la foto frontal completa y ofrece la foto trasera y el recorte transparente como opciones. El recorte sigue en revisión; no es un modelo 3D.
- Selección opcional de una imagen personal en formato PNG o WebP.
- GPS del navegador con permiso y HTTPS; búsqueda que abre Google Maps fuera del panel.
- El reproductor web, incluido el panel ampliado, es una demostración visual; no reproduce audio real.

### Funciones pendientes

- Configurar G² Dash como pantalla HOME predeterminada de Android.
- Recibir música del teléfono por Wi-Fi. El punto de acceso por sí solo no comparte canción ni portada; se requiere un protocolo compatible o una app complementaria.
- Integrar mapa en tiempo real y datos OBD del vehículo.
- El giro 360° real sigue pendiente de un modelo 3D validado o fotografías consistentes de todos los lados.

> **Aviso:** el mapa es una ilustración, no un mapa en tiempo real. Los campos de autonomía y voltaje permanecen vacíos hasta que exista una conexión real; no se inventan valores del vehículo. Los iconos de Wi-Fi y Bluetooth son decorativos en esta demostración web.

## Aplicación Android (v0.5.2 — APK de prueba)

El APK reutiliza la interfaz y los recursos del proyecto. Incluye acceso a aplicaciones instaladas y GPS opcional.

La música local funciona mediante la sesión multimedia del reproductor abierto en el estéreo: puede mostrar título, artista, portada, avance y los controles que esa app permita. Requiere habilitar el acceso multimedia en Android. El reproductor ampliado se identifica como demostración en la web y no simula sonido. La conexión remota desde el teléfono por Wi-Fi todavía no está implementada; [aquí está el detalle](android/MUSICA.md).

El APK no sustituye todavía el launcher HOME y falta validarlo en el estéreo Gearelec. GitHub Actions compila el APK de depuración en el flujo [Android APK](.github/workflows/android-apk.yml); consulta los artefactos en la pestaña **Actions**.

[Ver bienvenida actualizada v0.4.5](docs/capturas/CAMBIOS_v0.4.5.md) · [Galería por sección](docs/capturas/README.md) · [Historial del recorte v0.4.2](docs/capturas/RECORTE_IBIZA_v0.4.2.md)


## Registro visual por cada actualización

![Resumen de actualización al abrir G² Dash](docs/capturas/actual/00c-arranque-estado-v052.png)

![Vista actual de G² Dash](docs/capturas/actual/01-inicio-1280x720.png)

[**Visor conceptual Ibiza 12 vistas v0.5.0**](docs/capturas/CAMBIOS_v0.5.0.md) · [Fotos reales del Ibiza v0.4.4](docs/capturas/CAMBIOS_v0.4.4.md) · [Galería de todas las secciones](docs/capturas/README.md)

Las capturas automáticas de **cada sección real de la interfaz** se publican
en [docs/capturas/](docs/capturas/README.md). El flujo
[Capturas visuales](.github/workflows/capturas-visuales.yml)
captura la pantalla principal y cada panel (encabezado, reloj, música,
mapa, Ibiza, iconos, personalización y modo día) después de cada subida
de código a `main`. Git conserva el historial de versiones.

## Probar localmente

1. Descarga el repositorio como ZIP y descomprímelo.
2. Abre `index.html` en un navegador moderno.
3. Para habilitar permisos como GPS y evitar restricciones de archivos locales, usa un servidor web HTTPS.

No se requieren Node.js, dependencias externas, cuentas ni conexión a Internet para visualizar el tablero básico.

## Publicar demo con GitHub Pages

En **Settings → Pages**, selecciona **Deploy from a branch → main → /(root)** y guarda. La URL seguirá la forma `https://USUARIO.github.io/g2-dash/` cuando GitHub termine de desplegar.

## Estructura

```text
g2-dash/
├── index.html                # Interfaz
├── style.css                 # Distribución base
├── adaptive.css              # Ajustes para pantallas compactas
├── brand.css                 # Variantes del logotipo
├── widgets.css               # Tonalidad y transparencia de widgets
├── ibiza-cutout.css          # Presentación del recorte
├── media.css                 # Widget de música
├── app.js                    # Interacciones y preferencias
├── android/                  # APK de prueba y código Android
├── assets/                   # Fotos, máscara y galería conceptual del Ibiza
├── docs/capturas/            # Evidencia visual por sección y versión
├── .github/workflows/        # APK y capturas automáticas
├── README.md
├── ROADMAP.md
└── LICENSE
```

## Privacidad y créditos

- Las matrículas se difuminaron únicamente en las copias destinadas a este repositorio público; la carrocería y sus detalles permanecen intactos.
- Las fotografías no incluyen permiso de reutilización por terceros.
- La aplicación no envía las fotografías ni los fondos seleccionados a ningún servidor propio.
- **Licencia propietaria — Todos los derechos reservados.** El código fuente, el diseño y la documentación originales de G² Dash no están disponibles bajo una licencia de código abierto. Consulta [LICENSE](LICENSE) para conocer el alcance y las condiciones.
- Los nombres, logotipos y marcas de terceros (incluidos SEAT e Ibiza) pertenecen a sus respectivos titulares; su presencia no implica afiliación.

---

**G² Dash** · Diseño y desarrollo iterativo · Proyecto personal de interfaz automotriz.
