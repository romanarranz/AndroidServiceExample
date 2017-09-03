package com.github.romanarranz.androidserviceexample.sync;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.github.romanarranz.androidserviceexample.MainActivity;
import com.github.romanarranz.androidserviceexample.R;
import com.github.romanarranz.androidserviceexample.helpers.Utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private List<Messenger> mClients = new ArrayList<>(); // clientes registrado
    private List<Integer> mClientsIds = new ArrayList<>(); // ids de los clientes
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(LOG_TAG, "OnCreate");
        Log.i(LOG_TAG, this.toString());

        Utility.showNotification(this, R.string.service_started, MainActivity.class, NOTIFICATION_ID);

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
     * La operativa del servicio
     */
    private void onTimerTick() {
        Log.i(LOG_TAG, "Timer doing work " + mCounter);
        try {
            mCounter += mIncrementBy;
            for (int i = 0; i<mClients.size(); i++) {
                if (mClients.get(i) != null) {
                    sendMessageToClient(i, mCounter);
                }
            }
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Timer tick failed", t);
        }
    }

    /**
     * Enviar datos de vuelta al Messenger i
     *
     * @param clientIndex
     * @param value
     */
    private void sendMessageToClient(int clientIndex, int value) {
        try {
            Message msg;

            // Enviar el dato como un Integer
            Bundle bundleInt = new Bundle();
            bundleInt.putInt("my_int", value);
            msg = Message.obtain(null, MyService.MSG_SET_INT_VALUE);
            msg.setData(bundleInt);
            mClients.get(clientIndex).send(msg);

            // Enviar el dato como un String
            Bundle bundleStr = new Bundle();
            bundleStr.putString("my_str", "ab" + value + "cd");
            msg = Message.obtain(null, MyService.MSG_SET_STRING_VALUE);
            msg.setData(bundleStr);
            mClients.get(clientIndex).send(msg);

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

                    // no añadir duplicados
                    if (!mClientsIds.contains(msg.replyTo.hashCode())) {
                        Messenger newMessenger = msg.replyTo;
                        mClients.add(newMessenger);
                        mClientsIds.add(newMessenger.hashCode());

                        Log.i(LOG_TAG, "Registrar");
                        Log.i(LOG_TAG, "ID Clientes "+ Arrays.toString(mClientsIds.toArray()));
                    }

                    break;

                case MyService.MSG_UNREGISTER_CLIENT:

                    // borrar unicamente si tenemos clientes conectados
                    if (mClients.size() > 0) {
                        Messenger messenger = msg.replyTo;

                        int clientIndex = mClientsIds.indexOf(messenger.hashCode());
                        if (clientIndex >= 0) {
                            mClients.remove(clientIndex);
                            mClientsIds.remove(clientIndex);
                        }

                        Log.i(LOG_TAG, "Eliminar");
                        Log.i(LOG_TAG, "ID Clientes "+ Arrays.toString(mClientsIds.toArray()));
                    }

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
