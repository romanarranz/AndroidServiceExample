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
 * Servicio de enlace que permite que los componentes se enlacen a el llamando a bindService(),
 * generalmente no lo inician con startService().
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

    private Thread mTimer;
    private int mCounter = 0, mIncrementBy = 1;

    private Messenger mClient; // cliente registrado
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(LOG_TAG, "OnCreate");
        Log.i(LOG_TAG, this.toString());

        showNotification();

        sRunning = true;

        mTimer = new Thread(new Runnable() {
            @Override
            public void run() {
                while(sRunning) {
                    try {
                        Thread.sleep(1000);
                        onTimerTick();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mTimer.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

        // El PendingIntent lanzara el MainActivity si el usuario pulsa en esta notificacion
        // si la activity estaba en OnStop, OnPause pasará a OnResume, en lugar de destruirse y crearse
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);

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
