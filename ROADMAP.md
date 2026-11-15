# Hoja de ruta — G² Dash

## Situación actual

La versión **v0.4.5 (desarrollo)** actualiza la bienvenida con el estado real del proyecto: fotos del Ibiza y música local ya disponibles; conexión de música del teléfono por Wi‑Fi pendiente. La foto frontal completa sigue como vista inicial. La demo web y el APK siguen pendientes de validación en el estéreo físico. Ya están configurables el logotipo y el acabado ahumado de los widgets; Android puede leer la sesión musical local con permiso del usuario. Android permite elegir manualmente G² Dash como HOME, pero no lo configura por sí mismo. Falta validarlo en el estéreo físico.

| Versión | Objetivo | Estado |
|---|---|---|
| v0.1 | Distribución inicial y fotografías del Ibiza | Completada |
| v0.2 | Iconos, apariencia, fondos y demo web | Completada |
| v0.3 | Tres estilos de logotipo y widgets ahumados ajustables | Completada |
| v0.4 | APK Android de prueba, acceso a apps instaladas y GPS opcional | Implementada; validación en estéreo pendiente |
| v0.4.1 | Música Android local: título, artista, portada y controles compatibles | Implementada; requiere permiso multimedia |
| v0.4.2 | Máscara corregida sobre la foto real y diseño adaptable | En código; falta revisar y aprobar la máscara |
| v0.4.3 | Emblema SEAT → saludo Doble G → tablero; selección HOME opcional | Implementado en código; probar encendido real |
| v0.4.4 | Foto frontal completa por defecto; vista trasera y recorte transparente opcional | Implementada; comprobada en capturas; falta validar en el estéreo |
| v0.4.5 | Bienvenida con saludo y resumen honesto de avances y siguientes pasos | Implementada en web y APK; captura real publicada |
| v0.5 | Música desde el teléfono por Wi-Fi con protocolo o app complementaria | Pendiente de definir |
| v0.6 | Mapa real con proveedor seleccionado | Pendiente |
| v0.7 | Integración OBD opcional para datos reales del vehículo | Pendiente |
| v0.8 | Medir rendimiento en pantalla 1280 × 720 y estéreo con 2 GB de RAM | Pendiente |
| v1.0 | APK estable y launcher probado en el dispositivo | Pendiente |

## Qué ya funciona

- **Marca:** G² verde, DASH blanco y línea verde; variantes APEX, PULSE y ORBIT.
- **Widgets:** grafito, azul humo, verde humo o negro; opacidad y difuminado ajustables.
- **Música local Android:** muestra metadatos y portada del reproductor que funciona en el estéreo, y envía reproducir/pausar, anterior y siguiente cuando el reproductor lo permite.
- **Capturas visuales:** el flujo de GitHub documenta la pantalla y cada sección después de cada subida a `main`.

## Revisión visual del Ibiza

La v0.4.4 abre el panel con la fotografía frontal completa del Ibiza, sin aplicar el contorno todavía aproximado. La fotografía trasera y el recorte transparente siguen disponibles como opciones. No se editaron las fotos originales ni el fondo, la distribución, el logotipo o los widgets del tablero.

![Panel de Mi Ibiza con la foto frontal completa](docs/capturas/actual/06-mi-ibiza.png)

### Bienvenida v0.4.5

Al abrir G² Dash, la tercera pantalla resume el estado: fotos originales y música local están listas; la conexión musical del teléfono por Wi‑Fi sigue pendiente de definir. La secuencia dura aproximadamente 4.6 segundos y se puede omitir.

![Resumen v0.4.5 al abrir](docs/capturas/actual/00c-arranque-estado-v045.png)

[Ver comparación v0.4.4](docs/capturas/CAMBIOS_v0.4.4.md) · [Ver recorte opcional ampliado](docs/capturas/actual/15-ibiza-mascara-ampliada.png) · [Consultar propuesta de 12 ángulos](docs/IBIZA_360_REFERENCIA_APROBADA.md)

## Decisiones visuales

- Al ajustar el logotipo, cambiar únicamente sus letras; conservar fondo y fotografías.
- Mantener las fotografías reales del Ibiza 6J blanco de cinco puertas con sus detalles negros.
- Proteger las placas en las capturas públicas.
- Mantener la foto completa como vista inicial; dejar el recorte aproximado disponible como alternativa hasta completar la revisión visual.

## Límites y siguientes trabajos

- MediaSession muestra música reproducida en el Android del estéreo. Conectar un teléfono al punto de acceso no comparte automáticamente canción ni portada; para eso hace falta un canal remoto compatible.
- El mapa sigue siendo ilustrativo.
- Autonomía y voltaje requieren conexión OBD compatible; no se inventan valores.
- El concepto visual de 12 ángulos está aprobado y documentado, pero no es un modelo 3D ni una galería integrada; el giro real requiere un modelo 3D coherente o fotografías suficientes. [Consultar referencia](docs/IBIZA_360_REFERENCIA_APROBADA.md).
- La memoria objetivo es de 2 GB; la fluidez se debe comprobar en el estéreo Gearelec antes de considerar estable el APK.
