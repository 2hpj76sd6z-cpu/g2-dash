# G² Dash — Música Android (v0.4)

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
