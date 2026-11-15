<div align="center">

# G² Dash

**Interfaz automotriz personalizable · Web y Android · v0.4.2**

Tablero horizontal para SEAT Ibiza 6J, con identidad G², widgets ahumados configurables y acceso a las funciones Android del estéreo.

![Encabezado actual de G² Dash](docs/capturas/actual/02-encabezado.png)

[Galería visual por sección](docs/capturas/README.md) · [Estado y hoja de ruta](ROADMAP.md)

</div>

## Cómo va el proyecto

**Versión actual: v0.4.2.** La demo web y el APK Android de prueba ya están disponibles. El APK integra acceso a apps instaladas, GPS opcional y música local mediante MediaSession; todavía no sustituye el launcher principal ni se ha validado en el estéreo físico.

### Ya está implementado

- Tres estilos de logotipo G² DASH: APEX, PULSE y ORBIT.
- Widgets ahumados con cuatro tonalidades, opacidad y difuminado ajustables.
- Ajustes visuales guardados localmente.
- Sesión de música Android local con título, artista, portada y controles disponibles; requiere permiso explícito.
- Capturas automáticas por sección después de cada subida de código a `main`.

### Lo siguiente

- Revisar y aprobar la máscara corregida del Ibiza v0.4.2. La foto original no se modificó.
- Validar el APK en el estéreo Gearelec y medir fluidez con sus 2 GB de RAM.
- Definir una solución para música desde el teléfono por Wi-Fi: el punto de acceso no comparte por sí solo sus metadatos.
- Mantener pendientes el mapa real, la conexión OBD y el launcher predeterminado.

### Funciones disponibles en la web

- Diseño con distribución de reloj, reproductor de demostración, mapa ilustrativo y panel del auto.
- Reloj y fecha con el horario del dispositivo.
- Panel «Mi Ibiza» con el recorte transparente y las dos fotografías originales. El recorte sigue en revisión para ajustar su acabado a la imagen aprobada; no es un modelo 3D ni un giro 360°. Permite cargar un PNG/WebP desde Ajustes sin modificar el fondo.
- Fondos predefinidos o imagen seleccionada por el usuario.
- Widgets de cristal ahumado con **cuatro tonalidades** (grafito, azul humo, verde humo y negro), **opacidad 20–88%** y difuminado independiente (0–16 px), además del color de acento y el fondo personalizable.
- Tres estilos de logotipo (APEX, PULSE y ORBIT) configurables. Los ajustes visuales se guardan en el navegador.
- Modos automático, día y noche; ajustes guardados localmente cuando el navegador lo permite.
- Opción de solicitar velocidad GPS **si el navegador proporciona esos datos y el usuario autoriza su ubicación**.
- Búsqueda de destinos que abre Google Maps en una pestaña nueva.

### Funciones pendientes

- Configuración del launcher Android como pantalla de inicio predeterminada. El APK de prueba ya incluye acceso a aplicaciones instaladas.
- Controles de música remotos por Wi-Fi (pendiente; distinto a MediaSession local).

- Integración opcional con OBD para parámetros reales del vehículo.
- Mapa integrado con proveedor cartográfico.
- Modelo 360° coherente del Ibiza; actualmente solo existen dos fotografías, no un modelo giratorio.

> **Aviso:** el mapa es una ilustración, no un mapa en tiempo real. Los campos de autonomía y voltaje permanecen vacíos hasta que exista una conexión real; no se inventan valores del vehículo. Los iconos de Wi-Fi y Bluetooth son decorativos en esta demostración web.

## Aplicación Android (v0.4.2 — prueba instalable)

El código de la primera aplicación Android está en [android/](android/README.md).
Reutiliza el tablero y las fotografías del Ibiza. El APK permite abrir apps instaladas,
solicitar GPS y mostrar metadatos y portada de la sesión musical local con autorización.
Es una versión de prueba: aún no reemplaza el launcher principal. El control remoto
de música por Wi-Fi, OBD y mapa real continúan pendientes.

El flujo [Android APK](.github/workflows/android-apk.yml) compila un APK de depuración
en GitHub Actions. Los artefactos aparecen en la pestaña **Actions** cuando el flujo termina correctamente.

[**Mi Ibiza: comparación del recorte anterior y la máscara corregida**](docs/capturas/RECORTE_IBIZA_v0.4.2.md)

## Registro visual por cada actualización

![Vista actual de G² Dash](docs/capturas/actual/01-inicio-1280x720.png)

[**Ver comparación visual v0.4: Música y Mi Ibiza**](docs/capturas/CAMBIOS_v0.4.md) · [Galería de todas las secciones](docs/capturas/README.md)

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
├── index.html            # Interfaz
├── style.css             # Diseño y estructura base
├── brand.css             # Estilos configurables del logotipo
├── widgets.css           # Widgets ahumados y tonalidades
├── app.js                # Interacciones y preferencias
├── assets/
│   ├── ibiza_real_recortado.svg # Recorte transparente basado en foto original
│   ├── ibiza_frente_original.webp
│   └── ibiza_trasera_original.webp
├── vista_previa.png      # Captura de la demo
├── README.md
├── LICENSE              # Aviso de licencia propietaria
├── ROADMAP.md
└── .gitignore
```

## Privacidad y créditos

- Las matrículas se difuminaron únicamente en las copias destinadas a este repositorio público; la carrocería y sus detalles permanecen intactos.
- Las fotografías no incluyen permiso de reutilización por terceros.
- La aplicación no envía las fotografías ni los fondos seleccionados a ningún servidor propio.
- **Licencia propietaria — Todos los derechos reservados.** El código fuente, el diseño y la documentación originales de G² Dash no están disponibles bajo una licencia de código abierto. Consulta [LICENSE](LICENSE) para conocer el alcance y las condiciones.
- Los nombres, logotipos y marcas de terceros (incluidos SEAT e Ibiza) pertenecen a sus respectivos titulares; su presencia no implica afiliación.

---

**G² Dash** · Diseño y desarrollo iterativo · Proyecto personal de interfaz automotriz.
