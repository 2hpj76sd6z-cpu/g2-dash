# G² Dash — Música Android (v0.5.2)

## Qué funciona

La aplicación Android puede mostrar, con permiso explícito del usuario, el
título, artista, duración, posición aproximada y portada que un reproductor
local publique mediante **Android MediaSession**. Puede enviar órdenes
**Reproducir/Pausar, Anterior y Siguiente** cuando la aplicación reproductora
ofrece esas acciones.

La música **NO se reproduce dentro de G² Dash**: el reproductor instalado es
quien reproduce el audio. No se requiere crear una cuenta ni enviar datos a
servidores externos.

### Activación en el estéreo

1. Instalar el APK de G² Dash en el dispositivo Android.
2. Abrir una app de música instalada en ese mismo dispositivo.
3. En G² Dash tocar **Activar acceso a música**.
4. Android abrirá **Acceso a notificaciones**: autorizar G² Dash.
5. Regresar a G² Dash y reproducir una canción. Si la app publica una
   MediaSession, aparecerán sus metadatos y portada; puede tomar unos segundos.

**Privacidad:** Android concede acceso amplio a notificaciones para este
mecanismo, aunque G² Dash deliberadamente no examina, guarda ni envía el
contenido de las notificaciones. Solo consulta sesiones multimedia activas.
El permiso es opcional y revocable en Ajustes de Android.

**Limitaciones:** conectar otro teléfono por el punto de acceso Wi-Fi del
estéreo **no** comparte automáticamente sus metadatos por MediaSession.
Para esa conexión remota se necesita un protocolo compatible, como un
reproductor/controlador remoto específico, o una app complementaria autorizada
en el teléfono. No se anuncia como implementado. Algunas apps de música no
publican portadas o no permiten todos los controles.

En la [galería de capturas](../docs/capturas/README.md), la vista musical
con metadatos de ejemplo está etiquetada como **prueba visual**, no evidencia
de reproducción real.

## Estado técnico

- Compatible con API 23+.
- Consulta por `MediaSessionManager.getActiveSessions()` con
  `NotificationListenerService` habilitado.
- Portadas reducidas a 180 píxeles en memoria, sin almacenamiento en disco.
- Sin cambios a los fondos, fotografías de Ibiza o distribución del tablero.
- Los botones Aleatorio/Repetir no están integrados todavía.

## Panel ampliado v0.5.2

Desde el icono de **ampliar** del widget se abre un reproductor sin salir de G² Dash. Presenta portada grande (si la app musical la publica), título, artista, estado y fuente real. Los botones grandes de reproducción tienen habilitación condicional según MediaSession.

La barra permite avanzar solo si la sesión anuncia **`PlaybackState.ACTION_SEEK_TO`**, existe duración positiva y Android concedió el acceso multimedia. El puente nativo valida estos requisitos otra vez antes de llamar a `MediaController.TransportControls.seekTo()`. Esto evita ofrecer un control que la app no admite. API Android 21+, compatible con el mínimo API 23 del proyecto.

La **demo web** siempre deja los controles ampliados desactivados y muestra expresamente que es un ejemplo, no música conectada. La conexión remota por Wi-Fi permanece pendiente.

## Detalles del widget v0.4.1

- Estado independiente: `Permiso pendiente`, `Sin reproducción`, `Reproduciendo` y `En pausa`.
- Identificación visual del reproductor local cuando el paquete está reconocido.
- Metadatos truncados dentro de su tarjeta para pantallas pequeñas, sin modificar el fondo.
- Aleatorio/repetir permanecen desactivados mientras no exista integración real.
- El sitio web **no simula que esté sonando música** al pulsar reproducir.
- El ejemplo de la galería está marcado `EJEMPLO VISUAL` / `NO ES AUDIO REAL`.


## Reproductor ampliado local v0.5.2

El botón de ampliar en el widget abre la portada y los metadatos en una vista mayor, con barra de avance y controles. **Sigue controlando la MediaSession que ya está activa en el Android del estéreo**; no es todavía un reproductor de música del teléfono por Wi‑Fi.

La barra de avance solo se habilita cuando el reproductor informa la capacidad `ACTION_SEEK_TO`. Anterior, siguiente, reproducir y pausar también se habilitan según lo que publique cada sesión. En la demo web se muestran ejemplos marcados; no hay audio real.

## Siguiente etapa — teléfono por Wi‑Fi (v0.5.3)

El punto de acceso crea la red local, pero no comparte por sí solo audio, título ni portada. Antes de escribir una app emisora para el iPhone, conviene comprobar si **CarLink 2.0 ya publica la reproducción del teléfono como una sesión multimedia de Android**. Si lo hace, el puente MediaSession existente puede mostrar la canción y sus controles.

### Prueba en el estéreo Gearelec

1. Instala el APK de prueba v0.5.2 desde **GitHub → Actions → Android APK → Artifacts**.
2. Conecta el iPhone al sistema que ya usas para CarLink 2.0 y reproduce una canción; confirma primero que el audio se escucha en el coche.
3. En G² Dash, activa el acceso a música de Android y autorízalo en Ajustes.
4. Regresa al tablero y revisa el reproductor:
   - **Aparecen título, artista y portada:** CarLink está exponiendo una sesión a Android; se puede conservar la integración actual.
   - **Se oye la canción, pero G² Dash dice “Sin música activa”:** CarLink está enviando audio sin publicar una sesión que G² Dash pueda leer. Hará falta un puente de metadatos.
   - **No se oye ni aparece información:** primero hay que corregir la conexión de audio de CarLink; el punto de acceso, por sí solo, no confirma que exista una ruta de reproducción.

Esta prueba requiere el estéreo físico; no se puede confirmar desde la demo web.

### Alternativa si CarLink no entrega metadatos

Para Apple Music, una app complementaria de iPhone puede consultar el elemento actual del reproductor del sistema mediante las API oficiales de Apple y enviar **título, artista, portada y órdenes de control** a G² Dash por la red local. Apple documenta el reproductor del sistema y el elemento que está sonando en [MPMusicPlayerController](https://developer.apple.com/documentation/mediaplayer/mpmusicplayercontroller). El acceso a la biblioteca de música y a la red local requiere las autorizaciones de iOS correspondientes ([privacidad de red local](https://developer.apple.com/documentation/BundleResources/Information-Property-List/NSLocalNetworkUsageDescription)).

Ese puente de metadatos **no extrae ni retransmite el audio protegido de Apple Music o Spotify**. El sonido tendría que seguir llegando al estéreo por CarLink, Bluetooth, AirPlay u otra ruta de audio que el equipo ya admita. Si el objetivo es que G² Dash reciba y reproduzca el audio directamente por Wi‑Fi, el camino viable es permitir que el usuario elija archivos de audio locales sin protección y transmitirlos dentro de la red del coche.

### Alcance aún pendiente

- Todavía no hay un receptor Wi‑Fi ni una app emisora para iPhone en el código.
- No se guardará música, portada ni metadatos en servidores externos.
- Antes de implementar el puente, hay que elegir la fuente: **Apple Music con audio por CarLink**, **archivos locales transmitidos por Wi‑Fi** o una integración oficial del servicio que uses.
