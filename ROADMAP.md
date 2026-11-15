# Hoja de ruta — G² Dash

## Situación actual

La versión **v0.4.3** mantiene una demo web y un APK Android de prueba, y añade la bienvenida animada. Ya están configurables el logotipo y el acabado ahumado de los widgets; Android puede leer la sesión musical local con permiso del usuario. Android permite elegir manualmente G² Dash como HOME, pero no lo configura por sí mismo. Falta validarlo en el estéreo físico.

| Versión | Objetivo | Estado |
|---|---|---|
| v0.1 | Distribución inicial y fotografías del Ibiza | Completada |
| v0.2 | Iconos, apariencia, fondos y demo web | Completada |
| v0.3 | Tres estilos de logotipo y widgets ahumados ajustables | Completada |
| v0.4 | APK Android de prueba, acceso a apps instaladas y GPS opcional | Implementada; validación en estéreo pendiente |
| v0.4.1 | Música Android local: título, artista, portada y controles compatibles | Implementada; requiere permiso multimedia |
| v0.4.2 | Máscara corregida sobre la foto real y diseño adaptable | En código; falta revisar y aprobar la máscara |
| v0.4.3 | Emblema SEAT → saludo Doble G → tablero; selección HOME opcional | Implementado en código; probar encendido real |
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

La v0.4.2 vuelve a trazar la máscara alrededor de las llantas para quitar bloques de pavimento. La fotografía WebP original permanece intacta: no se cambió la carrocería, el color ni el fondo del tablero. La máscara sigue pendiente de tu aprobación.

![Mi Ibiza con la máscara corregida](docs/capturas/actual/14-ibiza-mascara-corregida.png)

[Ver ampliación de la máscara](docs/capturas/actual/15-ibiza-mascara-ampliada.png) · [Comparación antes y después](docs/capturas/RECORTE_IBIZA_v0.4.2.md)

## Decisiones visuales

- Al ajustar el logotipo, cambiar únicamente sus letras; conservar fondo y fotografías.
- Mantener las fotografías reales del Ibiza 6J blanco de cinco puertas con sus detalles negros.
- Proteger las placas en las capturas públicas.
- No presentar una máscara como final hasta revisar el contorno a tamaño de pantalla.

## Límites y siguientes trabajos

- MediaSession muestra música reproducida en el Android del estéreo. Conectar un teléfono al punto de acceso no comparte automáticamente canción ni portada; para eso hace falta un canal remoto compatible.
- El mapa sigue siendo ilustrativo.
- Autonomía y voltaje requieren conexión OBD compatible; no se inventan valores.
- El giro 360° requiere un modelo 3D coherente o fotografías suficientes.
- La memoria objetivo es de 2 GB; la fluidez se debe comprobar en el estéreo Gearelec antes de considerar estable el APK.
