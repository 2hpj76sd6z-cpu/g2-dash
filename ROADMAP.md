# Hoja de ruta — G² Dash

| Versión | Objetivo | Estado |
|---|---|---|
| v0.1 | Prototipo de distribución y fotografías | Completada |
| v0.2 | Iconos uniformes, apariencia, fondos, personalización, panel del auto y demostración web funcional | Completada |
| v0.3 | Ajustes de accesibilidad y diseño adaptativo, pruebas en pantalla de 1280 × 720 | Pendiente |
| v0.4 | Proyecto Android nativo, reloj y lanzador de aplicaciones instalado | Pendiente |
| v0.5 | Controles de música mediante Android MediaSession y accesos configurables | Pendiente |
| v0.6 | Velocidad GPS nativa y permisos contextualizados | Pendiente |
| v0.7 | Mapa real bajo proveedor seleccionado | Pendiente |
| v0.8 | Integración OBD opcional, solo con hardware compatible | Pendiente |
| v1.0 | APK estable con experiencia de launcher y pruebas en dispositivo | Pendiente |

## Diseño acordado

- Estética minimalista con paneles semitransparentes, bordes suaves y sombras discretas.
- Barra lateral, reproductor a la izquierda, mapa central, vehículo a la derecha y accesos inferiores.
- Fondo personalizable con difuminado.
- Carrocería del SEAT Ibiza original conservada; **boca negra, techo negro, cinco puertas, rines negros**.
- En versión pública, placas protegidas.

## Riesgos por resolver

- La memoria RAM de 2 GB del hardware objetivo exige medir el consumo en el dispositivo físico.
- El mapa requiere una API/proveedor y, potencialmente, condiciones de servicio o costos externos: no activarlo sin elección explícita.
- GPS necesita permisos y disponibilidad real del receptor.
- Datos como autonomía o voltaje requieren ECU/OBD y no pueden deducirse de una fotografía.
- Un giro 360° requiere geometría 3D o fotogramas consistentes del vehículo, no un collage de imágenes.
