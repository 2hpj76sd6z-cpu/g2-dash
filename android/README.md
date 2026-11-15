# G² Dash para Android — v0.5.2 (Apps y reproductor ampliado)

Este módulo es una aplicación Android nativa que **reutiliza exactamente**
el diseño web aprobado (archivos del directorio principal). No contiene un
modelo 360° del Ibiza: mantiene las fotografías originales del vehículo.

## Funcionamiento
- Presentación horizontal e inmersiva del tablero sin requerir Internet.
- Reloj, apariencia, fondo personalizado, difuminado y opacidad guardados localmente.
- Centro de Apps integrado: lista de aplicaciones instaladas con iconos, buscador, favoritos locales y apertura táctil; se validan las actividades LAUNCHER antes de iniciarlas.
- Botones Android: teléfono, Bluetooth y mapas externos.
- Selección de fondo mediante el selector de archivos de Android.
- GPS opcional al pulsar el icono; se solicita permiso en ese momento.
- El mapa dentro del panel sigue siendo ilustrativo.
- Música: autorización opcional a sesiones multimedia locales, título, artista,
  portada, progreso y controles disponibles. Consulta [MÚSICA](MUSICA.md).
- Pantalla de bienvenida SEAT → G² DASH → «Buenas Noches Doble G, ¿A dónde iremos hoy?» → tablero. Saludo dinámico y botón Omitir.
- Ajustes → Ver bienvenida para repetirla.
- G² Dash es una **opción HOME**, pero la selección es manual y reversible; nunca se activa automáticamente.
- Para volver al launcher anterior: Ajustes de Android → Apps → Aplicaciones predeterminadas → App de inicio → elegir el launcher original.
- **Encendido del vehículo:** algunos estéreos reinician Android; otros solo reanudan desde suspensión. La bienvenida aparece al abrir la aplicación, no necesariamente en cada reanudación. Probar en el estéreo antes de configurar HOME.

## Compilar
Requisitos: Android SDK 35, Java 17 y Gradle 8.9.
Desde la raíz del repositorio:

    gradle -p android :app:assembleDebug

APK generado: android/app/build/outputs/apk/debug/app-debug.apk

También se puede compilar en GitHub Actions desde el flujo "G² Dash · Android APK".
Los APK de CI son de prueba, firmados automáticamente con clave de depuración.
Si se descarga otra versión firmada con una clave diferente, Android puede
exigir desinstalar la anterior (se perderían sus ajustes locales). Para
distribuir actualizaciones estables se necesitará una clave de firma propia.

## Seguridad y privacidad
- El contenido local se sirve con WebViewAssetLoader en un origen HTTPS interno.
- Las páginas externas se abren en otras aplicaciones, nunca dentro del WebView con puente nativo.
- No solicita acceso general a todos los archivos ni permiso QUERY_ALL_PACKAGES.
- La lista de apps y sus iconos solo se consultan en Android y se muestran en el WebView interno; los favoritos se guardan localmente, sin enviarse a un servidor.
- Solicita ubicación solo al tocar GPS.
- No contiene analítica ni SDK publicitarios.
- Las imágenes y recursos del proyecto están sujetos a la [licencia propietaria](../LICENSE).

## Compatibilidad
Mínimo Android 6.0 (API 23); pantalla optimizada para 1280 × 720.
En estéreos antiguos, la fluidez y los efectos de desenfoque dependerán
de la versión instalada de Android System WebView.

**Estado:** código fuente y workflow de compilación. La integración
Wi-Fi remoto, OBD, puesta en marcha HOME en el equipo físico y giro 360° se verificarán por separado.

## Paridad visual

El paquete Android incluye `style.css`, `brand.css`, `widgets.css`, `ibiza-cutout.css`, `media.css`, `adaptive.css`, `intro.css`, `intro.js`, `apps.css`, `apps.js`, el JavaScript y las imágenes de `assets/` para conservar los mismos ajustes y el Ibiza real que en GitHub Pages.

## Máscara real del Ibiza v0.4.2

Se refinó solo la máscara de transparencia del recorte; la fotografía original WebP incrustada no se ha cambiado en absoluto. La matrícula sigue anonimizada. Las dos fotos reales originales permanecen disponibles. El resultado sigue siendo una máscara aproximada, sin modelo 360°.

## Galería visual Ibiza v0.5.0

La tarjeta Mi Ibiza incluye el botón **12 VISTAS**, con imágenes conceptuales aprobadas a 0°–330° en pasos de 30°. Se controla con miniaturas, deslizamiento, selector y flechas. Los WebP se incluyen localmente, sin WebGL ni Internet. **No es un modelo 3D**, y las fotos reales del usuario siguen intactas.

## Detallado del Ibiza v0.5.1

Se añadió un modo 1:1 para conservar la nitidez de los WebP aprobados (500×314), ampliación opcional 1.3×, navegación por gestos de hasta tres pasos y precarga acotada a dos vistas vecinas. La secuencia automática se detiene al cerrar o perder visibilidad. Las fotos reales, el panel Apps y el fondo permanecen sin modificar. El rendimiento de la Gearelec todavía requiere validación física.


## Reproductor ampliado local v0.5.2

El panel **Música** abre una vista amplia con portada, título, artista, avance y controles. Android recibe estos datos de la MediaSession del reproductor activo en el estéreo. El botón de avance solo se habilita si esa sesión anuncia `ACTION_SEEK_TO`; las órdenes de reproducción también dependen de las capacidades de cada app.

La demo web etiqueta el contenido como ejemplo y mantiene deshabilitados los controles. **La música del iPhone por Wi‑Fi no está implementada**; consulta [MUSICA.md](MUSICA.md) para la prueba de CarLink y las opciones de conexión.
