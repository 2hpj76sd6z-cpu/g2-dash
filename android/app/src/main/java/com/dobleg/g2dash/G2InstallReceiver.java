package com.dobleg.g2dash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.widget.Toast;

/** Receives the system PackageInstaller result and launches its confirmation screen. */
public final class G2InstallReceiver extends BroadcastReceiver {
    public static final String ACTION_INSTALL_STATUS =
            "com.dobleg.g2dash.action.INSTALL_STATUS";

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
            } else {
                //noinspection deprecation
                confirmation = (Intent) intent.getParcelableExtra(Intent.EXTRA_INTENT);
            }
            if (confirmation != null) {
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(confirmation);
            }
        } else if (status == PackageInstaller.STATUS_FAILURE_INCOMPATIBLE) {
            Toast.makeText(context,
                    "La firma de la actualización no coincide. Se requiere instalar la versión inicial firmada.",
                    Toast.LENGTH_LONG).show();
        } else if (status != PackageInstaller.STATUS_SUCCESS) {
            Toast.makeText(context,
                    "Android no pudo completar la actualización de G² Dash.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
