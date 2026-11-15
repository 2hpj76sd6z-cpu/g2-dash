# Hoja de ruta — G² Dash

## Estado actual

La versión **v0.4.1** tiene demo web y APK Android de prueba. La música local ya puede compartir metadatos y portada mediante MediaSession con permiso del usuario. Faltan pruebas en el estéreo físico y definir la conexión de música remota por Wi-Fi.

| Versión | Objetivo | Estado |
|---|---|---|
| v0.1 | Distribución inicial y fotografías del Ibiza | Completada |
| v0.2 | Iconos, apariencia, fondos, panel del auto y demo web | Completada |
| v0.3 | Logotipo configurable, widgets ahumados y ajustes visuales | Completada |
| v0.4 | APK Android de prueba, acceso a apps y GPS opcional | Completada en código; falta validar en el estéreo |
| v0.4.1 | Música local Android: título, artista, portada y controles compatibles | Implementada; requiere permiso multimedia |
| v0.4.2 | Pulido visual del Ibiza con la imagen aprobada y revisión de pantalla | En curso |
| v0.5 | Música remota desde teléfono mediante Wi-Fi, protocolo y app complementaria | Pendiente de definir |
| v0.6 | Mapa real con proveedor seleccionado | Pendiente |
| v0.7 | Integración OBD opcional para datos reales del vehículo | Pendiente |
| v0.8 | Validación de rendimiento y compatibilidad en pantalla 1280 × 720 | Pendiente |
| v1.0 | APK estable y experiencia de launcher probada en el dispositivo | Pendiente |

## Diseño acordado

- Estética moderna y compacta; los cambios de marca se limitan a las letras del logotipo.
- G² en verde, DASH en blanco y línea verde fina debajo.
- Tres variantes de marca: APEX, PULSE y ORBIT; APEX es la opción recomendada por ser la más sobria.
- Widgets semitransparentes con tonalidad seleccionable y controles independientes de opacidad y difuminado.
- Conservar el fondo y las fotografías reales del Ibiza al modificar el logotipo.
- Usar como base la imagen aprobada del SEAT Ibiza 6J blanco, cinco puertas, con sus detalles negros; el recorte sigue en revisión.
- Proteger las placas en las capturas públicas.

## Límites y riesgos por resolver

- El hardware objetivo tiene 2 GB de RAM; hay que medir rendimiento y fluidez en el equipo físico.
- MediaSession lee música reproducida en el Android del estéreo. Conectar un teléfono al punto de acceso no comparte automáticamente canción ni portada; requiere un canal remoto compatible.
- Un mapa en tiempo real necesita proveedor y conexión; revisar condiciones y costos antes de integrarlo.
- GPS depende del receptor y de los permisos del dispositivo.
- Autonomía y voltaje requieren conexión OBD compatible; no se estiman a partir de imágenes.
- El giro 360° requiere un modelo 3D coherente o fotografías suficientes; no se simulará con un collage.

## Evidencia visual

El flujo de GitHub captura la pantalla principal y cada sección después de cada actualización en `main`. Las capturas y comparaciones se consultan en [docs/capturas](docs/capturas/README.md); los ejemplos musicales están marcados para distinguirlos de reproducción real.
