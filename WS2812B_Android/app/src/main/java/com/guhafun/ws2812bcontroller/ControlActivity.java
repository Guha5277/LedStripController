package com.example.ws2812bcontroller;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;

public class ControlActivity extends AppCompatActivity {

    private static BluetoothSocket mmSocket = null;
    private static OutputStream mOutputStream;
    private static InputStream mIntputStream;

    protected static boolean isNeedToStopInputThread = false;

    private int reconnectCount = 1;

    Handler mHandler = null;

    //private InputThread inThread = null;

    private String TAG = "ConLog";

    public static void setMmSocket(BluetoothSocket socket){
        if (socket != null) {
            mmSocket = socket;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contlor);

        //Получаем потоки
        getStreams();


        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                //По результатам сообщения(0 - ошибка подключения и 1 - соединение установлено...
                switch(msg.what){
                    case 0:
                        //... В случае ошибки подключения, закрываем диалог и выводим сообщение об ошибке
                        Toast.makeText(ControlActivity.this, "Ошибка повторного подключения!", Toast.LENGTH_SHORT).show();
                        if (reconnectCount == 10) {
                            Log.d(TAG, "ControlActivity: Достигнуто максимальное количество попыток подключения(10)!");
                            Log.d(TAG, "ControlActivity: Запуск MainActivity...");
                            Intent intent = new Intent(ControlActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                        else{
                            reconnectCount++;
                            Log.d(TAG, "ControlActivity: Попытка переподключения №" + reconnectCount + "...");
                            ConnectThread connectThread = new ConnectThread(mmSocket.getRemoteDevice(), mHandler);
                            connectThread.start();
                        }
                        break;

                    case 1:
                        isNeedToStopInputThread = false;
                        getStreams();
                        InputThread inThread = new InputThread(mIntputStream);
                        inThread.start();

                        break;
                }
            }
        };

        registerReceiver(connectionStatusChanged, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        registerReceiver(connectionStatusChanged, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

        InputThread inThread = new InputThread(mIntputStream);
        inThread.start();

        Log.d(TAG, "ControlActivity создано");
    }

    BroadcastReceiver connectionStatusChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        Toast.makeText(ControlActivity.this, "Соединение восстановлено!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "ControlActivity: Соединение восстановлено!");
                        break;

                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        isNeedToStopInputThread = true;
                        Log.d(TAG, "ControlActivity: Соединение потеряно, попытка переподключения");
                        Toast.makeText(ControlActivity.this, "Соединение потеряно!!", Toast.LENGTH_SHORT).show();
                        closeSocket();

                        ConnectThread connectThread = new ConnectThread(mmSocket.getRemoteDevice(), mHandler);
                        connectThread.start();
                        break;
                }
            }
        }
    };

    private void getStreams(){
        try {
            mOutputStream = mmSocket.getOutputStream();
            mIntputStream = mmSocket.getInputStream();
        }
        catch (IOException ie){
            Log.e(TAG, "ControlActivity: Не могу получить Input/Output потоки",  ie);
        }
        Log.d(TAG, "ControlActivity: Потоки получены");
    }

    private void getConnectionStatus(){
        if(mmSocket.isConnected()){

        }
        else{

        }
    }

    private void closeSocket(){
        try{
            Log.d(TAG, "ControlActivity: Попытка закрытия сокета closeSocket()...");
            mmSocket.close();
        }catch(IOException ie){
            Log.e(TAG, "ControlActivity: Не удалось закрыть соединение! closeSocket()", ie);
        }
        Log.d(TAG, "ControlActivity: Соединение закрыто closeSocket()!");
    }

    private void openSocket() throws IOException{
        try{
            mmSocket.connect();
        }catch(IOException ie){
            Log.e(TAG, "ControlActivity: Не удалось установить соединение! openSocket", ie);
        }
    }

    private void writeData(byte ... data){
        try{
            Log.d(TAG, "ControlActivity: Попытка отправки данных: " + Arrays.toString(data) + " ...");
            mOutputStream.write(data);
        }catch (IOException ie){
            Log.e(TAG, "ControlActivity: Ошибка отправки данных!", ie);
        }
        Log.d(TAG, "ControlActivity: Данные отправлены");
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(connectionStatusChanged);


        Log.d(TAG, "ControlActivity уничтожено");
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeSocket();
    }
}


class InputThread extends Thread{
    private String TAG = "ConLog";
    private InputStream inputStream = null;

    public InputThread(InputStream inStream){
        inputStream = inStream;
    }
    @Override
    public void run(){
        int count;

        while(!ControlActivity.isNeedToStopInputThread){
            try{
                if(inputStream.available() > 0){
                                        try {
                        Thread.sleep(100);
                    }catch (InterruptedException ie){
                        Log.e(TAG, "ControlActivity: Ошибка приостановки потока!", ie);
                    }

                    count = inputStream.available();
                    byte[] data = new byte[count];
                    count = inputStream.read(data);

                    Log.d(TAG, "ControlActivity: Принято байт: " + count + ", Содердимое: " + Arrays.toString(data));
                }
            }catch (IOException ie){
                ControlActivity.isNeedToStopInputThread = true;
                Log.e(TAG, "ControlActivity: Ошибка при получении данных!", ie);
            }
        }
    }


}
