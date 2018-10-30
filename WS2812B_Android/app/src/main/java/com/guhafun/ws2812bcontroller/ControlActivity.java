package com.guhafun.ws2812bcontroller;


import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import com.example.ws2812bcontroller.R;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class ControlActivity extends AppCompatActivity {

    //Строковый массив данных, содержащий названия режимов ленты.
    private String[] modeNames = {"Rainbow Fade", "Rainbow Loop", "Random Burst", "Color Bounce", "Color Bounce Fade", "EMS Light One",
            "EMS Light ALL", "Flicker", "Pulse One Color", "Pulse with change color", "Fade Vertical", "Rule 30",
            "Random March", "RWB March", "Radiation", "Color Loop Verdelay", "White Temps", "Sin Bright Wave",
            "Pop Horizontal", "Quad Bright Cirve", "Flame", "Rainbow Vertical", "Pacman", "Random Color Pop",
            "EMS Lights Strobe", "RGB Propeller", "Kitt", "Matrix", "NEW! Rainbow Loop", "Color Wipe",
            "Cylon Bounce", "Fire", "Rainbow Cycle", "Twinkle Random", "Running Lights", "Sparkle",
            "Snow Sparkle", "Theater Chase", "Theater Chase Rainbow", "Strobe", "Bouncing Ball", "Bouncing Colored Ball",
            "Red", "Green", "Blue", "Yellow", "Cyan", "Purple", "White"};

    ListView choiceModeList;

    Menu controlMenu;
    Button btnConnect;
    ProgressDialog mProgressDialog = null;

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

        choiceModeList = findViewById(R.id.modeListView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, modeNames);
        choiceModeList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        choiceModeList.setAdapter(adapter);

       // btnConnect = findViewById(R.id.btnConnect);
//        btnConnect.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//              //  controlMenu.findItem(R.id.swtOnOff).setChecked(!controlMenu.getItem(R.id.swtOnOff).isChecked());
//                if (!isNeedToStopInputThread){
//                    byte a = 0x01;
//                    writeData(a);
//                }
//            }
//        });


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

        isNeedToStopInputThread = false;
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
        isNeedToStopInputThread = true;
        closeSocket();
    }

    //Основное меню приложения - включени/выключение ленты
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        controlMenu = menu;
        getMenuInflater().inflate(R.menu.menu_control, menu);

        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        controlMenu = menu;

        MenuItem item = menu.findItem(R.id.swtOnOff);
        //MenuItem item2 = menu.getItem(R.id.swtOnOff);

        if(!isNeedToStopInputThread) {
            item.setChecked(true);
            item.setEnabled(true);
           // controlMenu.findItem(R.id.swtOnOff).setEnabled(true);
           // controlMenu.findItem(R.id.swtOnOff).setEnabled(false);
//            controlMenu.findItem(R.id.app_bar_switch).setCheckable(true);
          //  controlMenu.findItem(R.id.swtOnOff).setChecked(true);



            Log.d(TAG, "ControlActivity: !!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
        else{
            item.setChecked(false);
            item.setEnabled(false);
//            controlMenu.findItem(R.id.app_bar_switch).setEnabled(false);
//            controlMenu.findItem(R.id.app_bar_switch).setChecked(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    //Обработка нажатий на элементы меню
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
//        switch(item.getItemId()){
//            case R.id.btnConnect:
//                onOff();
//                break;
//        }
        return super.onOptionsItemSelected(item);
    }

    //Метод включения/выключения ленты
    private void onOff(){

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
                        Thread.sleep(10);
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
