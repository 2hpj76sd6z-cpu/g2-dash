# G² Dash para Android — v0.4.1 (música detallada)

Este módulo es una aplicación Android nativa que **reutiliza exactamente**
el diseño web aprobado (archivos del directorio principal). No contiene un
modelo 360° del Ibiza: mantiene las fotografías originales del vehículo.

## Funcionamiento
- Presentación horizontal e inmersiva del tablero sin requerir Internet.
- Reloj, apariencia, fondo personalizado, difuminado y opacidad guardados localmente.
- Botones nativos: aplicaciones instaladas, teléfono, Bluetooth y mapas externos.
- Selección de fondo mediante el selector de archivos de Android.
- GPS opcional al pulsar el icono; se solicita permiso en ese momento.
- El mapa dentro del panel sigue siendo ilustrativo.
- Música: autorización opcional a sesiones multimedia locales, título, artista,
  portada, progreso y controles disponibles. Consulta [MÚSICA](MUSICA.md).
- No se configura como pantalla de inicio predeterminada en esta versión.

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
- Solicita ubicación solo al tocar GPS.
- No contiene analítica ni SDK publicitarios.
- Las imágenes y recursos del proyecto están sujetos a la [licencia propietaria](../LICENSE).

## Compatibilidad
Mínimo Android 6.0 (API 23); pantalla optimizada para 1280 × 720.
En estéreos antiguos, la fluidez y los efectos de desenfoque dependerán
de la versión instalada de Android System WebView.

**Estado:** código fuente y workflow de compilación. La integración
Wi-Fi remoto, OBD, launcher HOME y giro 360° se desarrollarán por separado.

## Paridad visual

El paquete Android incluye `style.css`, `brand.css`, `widgets.css`, `ibiza-cutout.css`, `media.css`, el JavaScript y las imágenes de `assets/` para conservar los mismos ajustes y el Ibiza real que en GitHub Pages.
