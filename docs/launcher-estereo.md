# G² Dash para estéreo · v0.5.8-dev (19)

## Instalación y pantalla de inicio

El paquete Android es `com.dobleg.g2dash`. Requiere Android 6.0 (API 23) o posterior y usa orientación horizontal. El usuario puede abrirlo como aplicación o elegirlo como pantalla de inicio en los ajustes del sistema cuando el firmware ofrece esa opción.

## Distribución y personalización

- Revisar el tablero a 1280 × 720, 1024 × 600 y 960 × 540, tanto con el mapa centrado como a la derecha. El reloj, el nombre del vehículo, las métricas y los textos del reproductor deben leerse dentro de sus paneles.
- Las dos vistas recortadas del Ibiza deben conservar sus proporciones y mostrar las cuatro esquinas de la fotografía dentro del área reservada. El centro del contenido visible debe quedar a menos de dos píxeles CSS del centro del área. La leyenda ocupa su propia fila.
- Personalización ofrece Ahumado y Cristal transparente. El tono inicial de widgets y ventanas es Verde humo. Se respetan las preferencias guardadas, el emblema oficial y las fotografías originales.
- Los fondos Atardecer, Azul humo y Grafito se pueden cambiar en Personalización. El selector de Android permite cargar una imagen propia. La elección debe sobrevivir al cierre y reapertura.

## Funciones que dependen del equipo

El GPS requiere permiso de ubicación precisa. La música usa los datos y acciones que publique la sesión multimedia del estéreo. Portada, aplicación de origen y desplazamiento temporal dependen de esa sesión. CarLink 2.0 gestiona la conexión con el teléfono. Su compatibilidad con el reproductor se comprueba en el vehículo. El acceso a Google Maps en Dash abre la vista de mapa de la aplicación. La proyección de una ruta de CarLink dentro del widget queda pendiente.

## Criterios para entregar una APK de prueba

GitHub Actions debe compilar el APK, validar su firma, comprobar el paquete, la actividad principal y los recursos empacados. Las pruebas visuales deben decodificar las dos fotografías y comprobar su centrado en tres tamaños, el cambio de fondo y la persistencia de los ajustes. El montaje con el estéreo y un teléfono conectado completa la verificación de GPS, multimedia y retorno desde CarLink.
