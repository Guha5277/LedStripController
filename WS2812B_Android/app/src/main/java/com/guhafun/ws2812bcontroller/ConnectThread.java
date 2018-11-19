package com.guhafun.ws2812bcontroller;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final String TAG = "ConLog";


    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Context context;
    //Context mContext;

    public ConnectThread(BluetoothDevice device, Context context) {

        this.context = context;
        BluetoothSocket tmp = null;
        mmDevice = device;

        try {
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "ConnectThread: Ошибка получения Socket'a", e);
        }
        mmSocket = tmp;
        Log.d(TAG, "ConnectThread: Socket получен " + mmSocket.toString());
    }

    @Override
    public void run() {
        final int ERROR = 0;
        final int DONE = 1;

        Intent intent = new Intent(ControlActivity.ACTION_CONNECT);

        try {
            mmSocket.connect();
        } catch (IOException connectException) {
           // mHandler.sendEmptyMessage(0);
            intent.putExtra("result", ERROR);
            Log.d(TAG, "ConnectThread: Не удалось установить соединение (what 0)");

            try {
                //В случае ошибки отправляем сообщение в основной поток Handler'y
                mmSocket.close();
            } catch (IOException closeException) {

                Log.e(TAG, "ConnectThread: Ошибка закрытия Socket'a (what 0)", closeException);
            }
        }

        if(mmSocket.isConnected()) {
            //В случае успешного соединения,  задаем сокет для ControlActivity и отправляем соответствующее сообщение Handler'y
            ControlActivity.setMmSocket(mmSocket);
            intent.putExtra("result", DONE);

            Log.d(TAG, "ConnectThread: Соедниение установлено! (what 1)");
        }

        else {
            intent.putExtra("result", ERROR);
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

    }
}

