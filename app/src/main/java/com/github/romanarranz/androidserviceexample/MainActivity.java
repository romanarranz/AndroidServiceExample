package com.github.romanarranz.androidserviceexample;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.romanarranz.androidserviceexample.helpers.Utility;
import com.github.romanarranz.androidserviceexample.sync.MyService;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    /**
     * Objetivo donde los clienes envian los mensajes al IncomingHandler
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Vistas
     */
    private Button mBtnStart, mBtnStop, mBtnBind, mBtnUnbind, mBtnUpby1, mBtnUpby10, mBtnOpen3;
    private TextView mTextStatus, mTextIntValue, mTextStrValue;

    /**
     * Messenger para comunicarnos con el servicio
     */
    private Messenger mService = null;

    /**
     * Flag que indica si hemos hecho bind con el servicio.
     */
    private boolean mIsBound;

    /**
     * Instancia para interctuar con la interfaz principal del servicio.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        /**
         * Metodo que se llama cuando la conexion con el servicio ha sido establecida, dandonos la instancia de servicio
         * que podemos usar para interactuar con el. Estamos comunicando con nuestro servicio a traves de una interfaz IDL.
         *
         * @param className
         * @param service
         */
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            mTextStatus.setText("Attached.");
            try {
                Message msg = Message.obtain(null, MyService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            }
            catch (RemoteException e) {
                // En el caso en el que el servicio se detenga no hacemos nada
            }
        }

        /**
         * Metodo que se llama cuando la conexion con el servicio ha sido desconectada inesperadamente.
         *
         * @param className
         */
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            mTextStatus.setText("Disconnected.");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnStart = (Button)findViewById(R.id.btnStart);
        mBtnStop = (Button)findViewById(R.id.btnStop);
        mBtnOpen3 = (Button) findViewById(R.id.open3);
        mBtnBind = (Button)findViewById(R.id.btnBind);
        mBtnUnbind = (Button)findViewById(R.id.btnUnbind);
        mTextStatus = (TextView)findViewById(R.id.textStatus);
        mTextIntValue = (TextView)findViewById(R.id.textIntValue);
        mTextStrValue = (TextView)findViewById(R.id.textStrValue);
        mBtnUpby1 = (Button)findViewById(R.id.btnUpby1);
        mBtnUpby10 = (Button)findViewById(R.id.btnUpby10);

        initBtnListeners();

        if (savedInstanceState != null) {
            mTextStatus.setText(savedInstanceState.getString("status"));
            mTextIntValue.setText(savedInstanceState.getString("int_value"));
            mTextStrValue.setText(savedInstanceState.getString("str_value"));
        }

        checkIfServiceIsRunning();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "onDestroy");
        try {
            doUnbindService();
            //stopService(new Intent(MainActivity.this, MyService.class));
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Failed to unbind from the service", t);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("status", mTextStatus.getText().toString());
        outState.putString("int_value", mTextIntValue.getText().toString());
        outState.putString("str_value", mTextStrValue.getText().toString());
    }

    /**
     * Bind de los listeners de los botones
     */
    private void initBtnListeners() {
        final Activity activity = this;
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(new Intent(MainActivity.this, MyService.class));
            }
        });

        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this, MyService.class));
            }
        });

        mBtnOpen3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                Utility.openApp(activity, "com.github.romanarranz.servicereceiver", bundle);
            }
        });

        mBtnBind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doBindService();
            }
        });

        mBtnUnbind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doUnbindService();
            }
        });

        mBtnUpby1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessageToService(1);
            }
        });

        mBtnUpby10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessageToService(10);
            }
        });
    }

    /**
     * Comprueba si el servicio esta corriendo
     */
    private void checkIfServiceIsRunning() {
        // Si el servicio esta corriendo cuando se inicia la activity, queremos bindearlo automaticamente
        if (MyService.isRunning()) {
            Toast.makeText(this, "Esta corriendo", Toast.LENGTH_SHORT).show();
            doBindService();
        } else {
            Toast.makeText(this, "Esta detenido", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Envia un mensaje al servicio
     *
     * @param value
     */
    private void sendMessageToService(int value) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MyService.MSG_SET_INT_VALUE, value, 0);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }

    /**
     * Establece una conexion con el servicio.
     */
    private void doBindService() {
        bindService(new Intent(this, MyService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        mTextStatus.setText("Binding.");
    }

    /**
     * Cierra una conexion con el servicio.
     */
    private void doUnbindService() {
        if (mIsBound) {
            // Si hemos recibido el servicio, y con ello lo hemos registrado, entones ahora es el momento de desregistarlo
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MyService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    // Noa hay nada especial que necesitemos hacer si el servicio se crashea
                }
            }

            // Desvincular nuestra conexion
            unbindService(mConnection);
            mIsBound = false;
            mTextStatus.setText("Unbinding.");
        }
    }

    /**
     * Clase para interactuar con la interfaz del servicio en el paso de mensajes
     */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MyService.MSG_SET_INT_VALUE:
                    Integer i = msg.getData().getInt("my_int");
                    mTextIntValue.setText("Int Message: " + i);
                    break;
                case MyService.MSG_SET_STRING_VALUE:
                    String str1 = msg.getData().getString("my_str");
                    mTextStrValue.setText("Str Message: " + str1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
