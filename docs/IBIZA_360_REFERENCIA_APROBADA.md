# G² Dash — Ibiza 6J: referencia conceptual de 12 ángulos

**Estado:** propuesta visual confirmada por el usuario el 15 de noviembre de 2026.

## Alcance aprobado

- Modelo de referencia: SEAT Ibiza 6J, carrocería blanca de cinco puertas.
- Acentos negros: boca frontal, techo, rines, retrovisores y manijas.
- Cristales polarizados.
- Doce vistas ilustrativas cada 30°: 0° hasta 330°.
- Presentación de estilo oscuro, premium y compacto; acentos verdes solo donde corresponde.
- Interacción propuesta: flechas anterior/siguiente, selector por grados, toque sobre miniaturas y recorrido automático opcional.
- La apariencia visual aprobada **no equivale a un modelo 3D real** ni a fotografías verificadas de cada ángulo.

## Restricciones de diseño del launcher

1. No reemplazar el coche del usuario por un modelo genérico.
2. No alterar el fondo del tablero ya aprobado.
3. Preservar la biblioteca de fotografías reales (frente y trasera) como apartado separado de esta galería conceptual.
4. Etiquetar el material de 12 vistas como **PROPUESTA VISUAL / NO ES MODELO 3D**.
5. Optimizar memoria y movimiento para el estéreo Android de 2 GB RAM.
6. Al incorporar la galería al código, generar capturas reales de la sección, documentar la modificación y comprobar el APK.

## Estado de implementación

**Integrado en G² Dash v0.5.0.** El botón **12 VISTAS** muestra la galería aprobada en la web y en el proyecto Android.

- 12 WebP comprimidos: [assets/ibiza360](../assets/ibiza360/).
- Flechas, deslizamiento, barra de ángulos, miniaturas y recorrido automático.
- Sin dependencia de red ni WebGL, con el fondo y las fotos reales intactos.
- Capturas reales y pruebas de 12 miniaturas: [evidencia visual](capturas/README.md).
- Estado explícito: **PROPUESTA VISUAL, NO ES MODELO 3D**.

El material puede presentar incoherencias entre ángulos porque corresponde a ilustraciones conceptuales; no debe anunciarse como reconstrucción exacta del vehículo.

**Licencia:** consultar [LICENSE](../LICENSE); los nombres y marcas de terceros
siguen perteneciendo a sus respectivos titulares.

## Actualización v0.5.1 — nitidez y control

Los archivos conceptuales originales son de **500 × 314 píxeles**, por lo que ampliarlos no añade detalle. El modo predeterminado 1:1 evita reescalado adicional; 1.3× amplía la presentación cuando se prefiere mayor tamaño. El visor limita la precarga a dos vistas vecinas, permite varios ángulos por deslizamiento y detiene el recorrido al cerrarse. Ninguna fotografía o modelo del vehículo se sustituye. [Capturas comparativas](capturas/MEJORA_IBIZA_v0.5.1.md).
