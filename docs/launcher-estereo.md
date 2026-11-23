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

## Prueba en estéreo · compilación 20

La edición G² Dash 20 se instala junto a la compilación 19 con ajustes independientes. Al mostrar el tablero monta una superficie interactiva de Google Maps en el widget. Requiere internet para cargar calles. El SDK nativo usa su clave configurada y la vista web de Google Maps sirve de respaldo. El buscador centra un destino. La visualización de una ruta ya iniciada en CarPlay pertenece a CarLink.

Música abre CarLink para conectar e iniciar reproducción. Si hay una sesión con metadatos, Música abre el reproductor de Dash. La portada abre el reproductor ampliado. El botón de conexión del reproductor abre CarLink. En Personalización se puede seleccionar otro nombre de receptor. La barra permite adelantar cuando la sesión publica duración y la acción de búsqueda.

En Personalización → Fuente de ubicación se elige GPS del estéreo o GPS del celular. Para el celular, ambos equipos deben poder comunicarse por la misma red privada. Usa en el teléfono un emisor GPS en modo servidor TCP con sentencias NMEA RMC y suma de comprobación. Escribe su dirección IPv4 privada y puerto en Dash. Concede ubicación al emisor y mantenlo activo según las opciones de tu teléfono. Dash recuerda el destino de conexión y reintenta al volver al tablero. El transporte NMEA TCP de esta opción es local y sin cifrar. Dash descarta datos inválidos o caducados y distingue ambos orígenes en pantalla.

## Distribución y apariencia · compilación 21

El engranaje superior abre Ajustes. En Editar widgets se pueden activar de uno a cuatro módulos, arrastrarlos en la vista previa y redimensionarlos con la esquina inferior derecha o los botones Ancho y Alto. El tablero guarda la composición. Cuando falta espacio, reajusta los módulos vecinos y mantiene un área independiente para cada uno. La tarjeta del vehículo muestra su fotografía y la velocidad GPS en km/h.

En Fotos de mi vehículo se pueden seleccionar hasta cuatro fotos y escribir el nombre del vehículo. Usar el Ibiza original recupera su galería. Hay nueve tonos de ventanas y widgets. Los acabados de las ventanas y su opacidad se ajustan independientemente de los widgets. Radio se ha retirado del dock.
