# G² Dash — Evidencia visual de cada actualización

Cada actualización en la rama principal dispara el flujo
[Capturas visuales](../../.github/workflows/capturas-visuales.yml), que abre **la
interfaz real en Chromium** a 1280 × 720 y crea capturas por sección.

> Importante: estas capturas representan el código real en GitHub.
> No son imágenes generadas, ni funciones simuladas como si fueran operativas.
> El mapa y OBD siguen pendientes. La música MediaSession local se integra opcionalmente en Android; las capturas musicales de ejemplo son únicamente pruebas visuales.

## Comparación antes y después

**[Detalle Música v0.4.1 — comparación antes y después](DETALLE_MUSICA_v0.4.1.md)**

**[Ver cambios visuales v0.4 — Música, Ibiza y pantalla completa](CAMBIOS_v0.4.md)**

![Pantalla principal de G² Dash](actual/01-inicio-1280x720.png)

### Vista rápida de los paneles

| Música | Mi Ibiza |
|---|---|
| ![Música sin reproducción real en web](actual/04-musica.png) | ![Mi Ibiza original](actual/06-mi-ibiza.png) |

| Ajustes | Modo día |
|---|---|
| ![Panel de ajustes](actual/08-personalizacion.png) | ![Modo día](actual/09-modo-dia-1280x720.png) |

## Galería visual actual

| Sección | Evidencia |
|---|---|
| Vista principal completa | [01 — Inicio](actual/01-inicio-1280x720.png) |
| Marca, fecha y barra superior | [02 — Encabezado](actual/02-encabezado.png) |
| Hora y fecha | [03 — Reloj](actual/03-reloj.png) |
| Reproductor | [04 — Música](actual/04-musica.png) |
| Mapa de demostración | [05 — Mapa](actual/05-mapa-ilustrativo.png) |
| Vehículo original | [06 — Mi Ibiza](actual/06-mi-ibiza.png) |
| Iconos de acceso | [07 — Accesos](actual/07-iconos-accesos.png) |
| Fondo, opacidad y tonalidad | [08 — Personalización](actual/08-personalizacion.png) |
| Contraste claro | [09 — Modo día](actual/09-modo-dia-1280x720.png) |
| Fotografía frontal ampliada | [10 — Ibiza frontal](actual/10-ibiza-detalle.png) |
| Fotografía trasera ampliada | [11 — Ibiza trasera](actual/11-ibiza-trasera.png) |
| Música Android con metadatos de prueba (no reproducción real) | [12 — Vista de integración](actual/12-musica-integracion-prueba.png) |
| Música detallada v0.4.1 — ejemplo visual | [13 — Widget detallado](actual/13-musica-detallada-ejemplo.png) |

### Histórico de versiones

Los archivos en `actual/` se actualizan después de cada subida al código;
**Git conserva las capturas anteriores en el historial de commits**.
El archivo `actual/meta.json` identifica el commit del código
que produjo las capturas, la fecha y la resolución.

Para nuevas funciones: agregar una captura específica en
`scripts/capture-ui.mjs`, documentarla aquí y adjuntarla a la subida.
No incorporar capturas que expongan placas o datos personales sin anonimizar.

### Cómo generar capturas localmente

```bash
npm install
python3 -m http.server 8765
# En otra terminal:
node scripts/capture-ui.mjs
```

Chrome/Chromium debe estar instalado en el sistema, o debe descargarse
el navegador de Playwright con `npx playwright install chromium`.
