package com.dobleg.g2dash;

import android.service.notification.NotificationListenerService;

/**
 * Declaración obligatoria para consultar MediaSessionManager.
 * No procesa ni almacena el contenido de las notificaciones.
 */
public final class G2MediaListener extends NotificationListenerService {
    // Deliberadamente sin métodos de lectura de notificaciones.
}
