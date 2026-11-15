package com.dobleg.g2dash;

import android.service.notification.NotificationListenerService;

/**
 * Enables Android's MediaSessionManager API after the user grants notification
 * access in system settings. The dashboard reads playback metadata only.
 */
public class MediaAccessService extends NotificationListenerService {
}
