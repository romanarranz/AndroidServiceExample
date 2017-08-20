package com.github.romanarranz.androidserviceexample.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.github.romanarranz.androidserviceexample.MainActivity;
import com.github.romanarranz.androidserviceexample.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by romanarranzguerrero on 20/8/17.
 */

public class MyService extends Service {

    private static final String LOG_TAG = MyService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 3004;

    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_SET_INT_VALUE = 3;
    public static final int MSG_SET_STRING_VALUE = 4;

    private static boolean sRunning = true;

    private Timer mTimer;
    private int mCounter = 0, mIncrementBy = 1;

    private Messenger mClient; // cliente registrado
    private Messenger mMessenger;

    @Override
    public void onCreate() {
        Toast.makeText(this, "OnCreate", Toast.LENGTH_SHORT).show();
        mMessenger = new Messenger(new IncomingHandler());
        super.onCreate();
        showNotification();

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                onTimerTick();
            }
        }, 0, 1000);
        sRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(this, "OnBind", Toast.LENGTH_SHORT).show();
        Log.i(LOG_TAG, mMessenger.toString());
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(this, "OnUnbind", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        Toast.makeText(this, "OnRebind", Toast.LENGTH_SHORT).show();
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "OnDestroy", Toast.LENGTH_SHORT).show();
        super.onDestroy();
        mTimer = null;
        sRunning = false;
    }

    /**
     * Devuelve true si el servicio esta corriendo
     *
     * @return
     */
    public static boolean isRunning() {
        return sRunning;
    }

    /**
     * Muestra una notificacion cuando el servicio esta corriendo
     */
    private void showNotification() {

        // En este ejemplo, usaremos el mismo texto para el ticker y la notificacion
        CharSequence title = getText(R.string.app_name);
        CharSequence contentText = getText(R.string.service_started);

        Resources resources = getResources();
        int iconId = R.drawable.ic_icon;
        int artResourceId = iconId;

        Bitmap largeIcon = BitmapFactory.decodeResource(resources, artResourceId);

        // NotificationCompatBuilder es una forma muy conveniente de construir notificaciones con compatibilidad
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setColor(resources.getColor(R.color.colorPrimary))
                        .setSmallIcon(iconId)
                        .setLargeIcon(largeIcon)
                        .setContentTitle(title)
                        .setContentText(contentText);

        // El PendingIntent lanzara nuestra activity si el usuario selecciona esta notificacion
        Intent resultIntent = new Intent(this, MainActivity.class);

        // El TaskStackBuikder contendra un back stack artificial para la Activity que abrimos.
        // Esto asegura que cuando naveguemos hacia atras el usuario vaya al Home Screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(resultPendingIntent);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Lanzar la notificacion
        nm.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    /**
     * La operativa del servicio
     */
    private void onTimerTick() {
        Log.i(LOG_TAG, "Timer doing work " + mCounter);
        try {
            mCounter += mIncrementBy;
            if (mClient != null) {
                sendMessageToUI(mCounter);
            }
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Timer tick failed", t);
        }
    }

    /**
     * Enviar datos de vuelta a la UI del usuario, en caso de que esté visible
     *
     * @param value
     */
    private void sendMessageToUI(int value) {
        try {
            Message msg;

            // Enviar el dato como un Integer
            Bundle bundleInt = new Bundle();
            bundleInt.putInt("my_int", value);
            msg = Message.obtain(null, MyService.MSG_SET_INT_VALUE);
            msg.setData(bundleInt);
            mClient.send(msg);

            // Enviar el dato como un String
            Bundle bundleStr = new Bundle();
            bundleStr.putString("my_str", "ab" + value + "cd");
            msg = Message.obtain(null, MyService.MSG_SET_STRING_VALUE);
            msg.setData(bundleStr);
            mClient.send(msg);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handler para los mensajes entrantes de los clientes
     */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MyService.MSG_REGISTER_CLIENT:
                    mClient = msg.replyTo;
                    break;

                case MyService.MSG_UNREGISTER_CLIENT:
                    mClient = null;
                    break;

                case MyService.MSG_SET_INT_VALUE:
                    mIncrementBy = msg.arg1;
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }
}
