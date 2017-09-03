package com.github.romanarranz.androidserviceexample.helpers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.github.romanarranz.androidserviceexample.R;

/**
 * Created by romanarranzguerrero on 3/9/17.
 */

public final class Utility {

    /**
     * Muestra una notificacion cuando la activity no está visible informando de los eventos
     */
    public static void showNotification(Context context, int textId, Class activity, int notificationId) {

        // En este ejemplo, usaremos el mismo texto para el ticker y la notificacion
        CharSequence title = context.getText(R.string.app_name);
        CharSequence contentText = context.getText(textId);

        Resources resources = context.getResources();
        int iconId = R.drawable.ic_icon;
        int artResourceId = R.drawable.ic_icon;

        Bitmap largeIcon = BitmapFactory.decodeResource(resources, artResourceId);

        // NotificationCompatBuilder es una forma muy conveniente de construir notificaciones con compatibilidad
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context)
                        .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                        .setSmallIcon(iconId)
                        .setLargeIcon(largeIcon)
                        .setContentTitle(title)
                        .setContentText(contentText);

        // El PendingIntent lanzara el MainActivity si el usuario pulsa en esta notificacion
        // si la activity estaba en OnStop, OnPause pasará a OnResume, en lugar de destruirse y crearse
        Intent resultIntent = new Intent(context, activity);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0);

        notificationBuilder.setContentIntent(resultPendingIntent);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Lanzar la notificacion
        nm.notify(notificationId, notificationBuilder.build());
    }

    /**
     * Abrir otra app con datos en el intent.
     *
     * https://stackoverflow.com/questions/2780102/open-another-application-from-your-own-intent
     *
     * @param context Context actual, como App, Activity o Service
     * @param packageName el nombre completo del nombre del paquete de la app que abriremos
     * @return true si se ha podido abrir
     */
    public static boolean openApp(Context context, String packageName, Bundle bundle) {
        PackageManager manager = context.getPackageManager();

        try {
            Intent i = manager.getLaunchIntentForPackage(packageName);
            i.putExtra("data", bundle);
            if (i == null) {
                throw new PackageManager.NameNotFoundException();
            }

            i.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(i);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
