package com.guhafun.ws2812bcontroller;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;
import com.example.ws2812bcontroller.R;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class ControlActivity extends AppCompatActivity {

    //Строковый массив данных, содержащий названия режимов ленты.
//    private String[] modeNames = {"Rainbow Fade", "Rainbow Loop", "Random Burst", "Color Bounce", "Color Bounce Fade", "EMS Light One",
//            "EMS Light ALL", "Flicker", "Pulse One Color", "Pulse with change color", "Fade Vertical", "Rule 30",
//            "Random March", "RWB March", "Radiation", "Color Loop Verdelay", "White Temps", "Sin Bright Wave",
//            "Pop Horizontal", "Quad Bright Cirve", "Flame", "Rainbow Vertical", "Pacman", "Random Color Pop",
//            "EMS Lights Strobe", "RGB Propeller", "Kitt", "Matrix", "NEW! Rainbow Loop", "Color Wipe",
//            "Cylon Bounce", "Fire", "Rainbow Cycle", "Twinkle Random", "Running Lights", "Sparkle",
//            "Snow Sparkle", "Theater Chase", "Theater Chase Rainbow", "Strobe", "Bouncing Ball", "Bouncing Colored Ball",
//            "Red", "Green", "Blue", "Yellow", "Cyan", "Purple", "White"};

    //Строковый массив с названием режимов
    String[] modeList;

    //Константы-запросы к МК
    private final Byte GET_INITIAL_DATA = 1;

    //Флаг используемый в фоновом потоке приема данных
    static boolean isNeedToStopInputThread = false;
    //Флаг используемы для приема начальных значений от МК
    static boolean isInitialDataRecieved = false;

    //Массив принятных данных
    public static byte[] data;

    //Список с названиями и актуальным состоянием режимов, адаптер для списка
    ListView choiceModeList;
    ArrayAdapter<String> adapter;

    //Элементы меню
    Menu controlMenu;
    Switch menuBtnOnOff;
    ImageButton menuBtnFav;
    ImageButton menuBtnSave;

    //Графические элементы - два ползунка с регулировкой скорости и яркости, кнопки переключения режимов, паузы
    SeekBar seekSpeed;
    SeekBar seekBright;
    ImageButton btnPrev;
    ImageButton btnNext;
    ImageButton btnPause;

//    ProgressDialog mProgressDialog = null;

    //Сокет и потоки, необходимые для обмена данными
    private static BluetoothSocket mmSocket = null;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    //Хэндлер для обмена сообщениями с потоком подключения (ConnectThread)
    HandlerControl mHandler = null;

    InputThread mInputThread;

    //private InputThread inThread = null;
    //TAG для логов
    private String TAG = "ConLog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contlor);

        //Инициализация объектов графического интерфейса
        choiceModeList = findViewById(R.id.modeListView);
        seekSpeed = findViewById(R.id.seekSpeed);
        seekBright = findViewById(R.id.seekBright);
        btnPrev = findViewById(R.id.btnPrev);
        btnPause = findViewById(R.id.btnPause);
        btnNext = findViewById(R.id.btnNext);

        //Временный слушатель
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    Log.d(TAG, "ControlActivity: Попытка отправки данных: 1 ...");
                    mOutputStream.write(1);
                }catch (IOException ie){
                    Log.e(TAG, "ControlActivity: Ошибка отправки данных!", ie);
                }
                Log.d(TAG, "ControlActivity: Данные отправлены");
            }

        });

        //Получаем потоки
        getStreams();

        //Получаем список режимов из XML-файла
        modeList = this.getResources().getStringArray(
                R.array.mode_list);

        //Инициализируем адаптер
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, modeList);
       // adapter = ArrayAdapter.createFromResource(this, R.array.mode_list, android.R.layout.simple_list_item_multiple_choice);//new MyArrayAdapter(this, modeList);
        //Настраиваем список и добавляем адаптер
        choiceModeList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        choiceModeList.setAdapter(adapter);

        //Инициализируем Handler
        mHandler = new HandlerControl();

        //Регистрируем двух слушателей изменения состояния подключения
        registerReceiver(connectionStatusChanged, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        registerReceiver(connectionStatusChanged, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

        //Инициализация и запуск фонового потока, отвечающего за прием данных
        mInputThread = new InputThread(mInputStream);
        mInputThread.setEnabled(true);
        mInputThread.start();

//        initializeData();
        ThreadInitialize initializeThread = new ThreadInitialize();
        initializeThread.start();

        Log.d(TAG, "ControlActivity создано");
    }

    //Обновление интерфейса в соотвествтии с актуальными данными
    private void updateUI(boolean result){
        //isInitialDataRecieved = !result;

        btnPrev.setEnabled(result);
        btnPause.setEnabled(result);
        btnNext.setEnabled(result);
        seekBright.setEnabled(result);
        seekSpeed.setEnabled(result);

        choiceModeList.setEnabled(result);
      
        Log.d(TAG, "First: " + choiceModeList.getFirstVisiblePosition() + ", Last: " + choiceModeList.getLastVisiblePosition());

//        for(int i = 0; i <= choiceModeList.getLastVisiblePosition() - choiceModeList.getFirstVisiblePosition(); i++) {
//            CheckedTextView chkd = choiceModeList.getChildAt(i).findViewById(android.R.id.text1);
//            chkd.setChecked(data[i]);
//        }

       // choiceModeList.setItemChecked();

//        MyArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.mode_list, android.R.layout.simple_list_item_multiple_choice);
//        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.mode_list, android.R.layout.simple_list_item_multiple_choice);


       // adapter = new MyArrayAdapter(this, data, modeList);


        //Метод который сообщает меню о том, что необходимо вызвать метод onPrepareOptionsMenu() для обновления элементов меню
        invalidateOptionsMenu();
    }

    //Статичный метод, вызываем в случае успешной установки/восстановления соединения (класс ConnectThread)
    public static void setMmSocket(BluetoothSocket socket){
        if (socket != null) {
            mmSocket = socket;
        }
    }

    //Получение потоков
    private void getStreams(){
        try {
            mOutputStream = mmSocket.getOutputStream();
            mInputStream = mmSocket.getInputStream();
        }
        catch (IOException ie){
            Log.e(TAG, "ControlActivity: Не могу получить Input/Output потоки",  ie);
        }
        Log.d(TAG, "ControlActivity: Потоки получены");
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

    //Основное меню приложения - включени/выключение ленты
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_control, menu);

        menuBtnOnOff = menu.findItem(R.id.swtOnOff).getActionView().findViewById(R.id.switchButton);
        menuBtnFav = menu.findItem(R.id.button_fav).getActionView().findViewById(R.id.favButton);
        menuBtnSave = menu.findItem(R.id.button_save).getActionView().findViewById(R.id.saveButton);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        controlMenu = menu;
        if (isInitialDataRecieved){
            menuBtnOnOff.setEnabled(false);
            menuBtnSave.setEnabled(false);
            menuBtnSave.setEnabled(false);
        }
        else{
            menuBtnOnOff.setEnabled(true);
            menuBtnSave.setEnabled(true);
            menuBtnSave.setEnabled(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    //Обработка нажатий на элементы меню
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        Toast.makeText(this, item.getItemId(), Toast.LENGTH_SHORT).show();
        switch(item.getItemId()){
            case R.id.swtOnOff:
                Toast.makeText(this, "switch on off", Toast.LENGTH_SHORT).show();
                break;
            case R.id.button_fav:
                Toast.makeText(this, "button fav", Toast.LENGTH_SHORT).show();
                break;
            case R.id.button_save:
                Toast.makeText(this, "button save", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //Handler для связи с потоком ConnectThread
    class HandlerControl extends Handler{
        //Счетчик переподключений в случае потери связи
        int reconnectCount = 1;
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //По результатам сообщения(0 - ошибка подключения и 1 - соединение установлено...
            switch(msg.what){
                case 0:
                    //... В случае ошибки подключения, закрываем диалог и выводим сообщение об ошибке
                    Toast.makeText(ControlActivity.this, "Ошибка повторного подключения!", Toast.LENGTH_SHORT).show();
                    if (reconnectCount == 10) {
                        reconnectCount = 1;
                        Log.d(TAG, "ControlActivity: Достигнуто максимальное количество попыток подключения(10)!");
                        Log.d(TAG, "ControlActivity: Запуск MainActivity...");
                        Intent intent = new Intent(ControlActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                    else{
                        reconnectCount++;
                        Log.d(TAG, "ControlActivity: Попытка переподключения №" + reconnectCount + "...");
                        ConnectThread connectThread = new ConnectThread(mmSocket.getRemoteDevice(), this);
                        connectThread.start();
                    }
                    break;

                case 1:
                    getStreams();
                    InputThread inputThread = new InputThread(mInputStream);
                    inputThread.setEnabled(true);
                    inputThread.start();

                    break;
            }
        }
    }



    class ThreadInitialize extends Thread{
        int count = 0;
        @Override
        public void run() {
            super.run();
            while (count < 10) {
                sendMessage(GET_INITIAL_DATA);
                try {
                    Thread.sleep(50);
                    if(isInitialDataRecieved){
//                        while((choiceModeList.getLastVisiblePosition() == -1)){
//                            Thread.sleep(500);
//                            Log.d(TAG, "getFirtstVisible = -1");
//                        };
                        updateUI(isInitialDataRecieved);
                        Log.d(TAG, "Поток InitializeData завершен со счетчиком: " + count);
                        return;
                    }
                    count++;
                } catch (Exception ex) {
                    Log.e(TAG, "Ошибка потока Initialize Thread", ex);
                }
            }
            Log.d(TAG, "Ошибка инициализации данных");
        }
        private void sendMessage(byte ... data){
            try{
                Log.d(TAG, "ControlActivity: Попытка отправки данных: " + Arrays.toString(data) + " ...");
                mOutputStream.write(data);
            }catch (IOException ie){
                Log.e(TAG, "ControlActivity: Ошибка отправки данных!", ie);
            }
            Log.d(TAG, "ControlActivity: Данные отправлены");
        }
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
                        //Останавливаем поток входящих данных
                        stopInputThread();
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

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //Снятие слушателей при уничтожении Активити
        unregisterReceiver(connectionStatusChanged);
        Log.d(TAG, "ControlActivity уничтожено");
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Останавливаем поток входящих данных и закрываем сокет
        stopInputThread();
        closeSocket();
    }

    void stopInputThread(){
        if(mInputThread != null) mInputThread.setEnabled(false);
    }
}

class MyArrayAdapter extends ArrayAdapter<String> {
    private String TAG = "ConLog";
    private final Activity context;
   // private byte[] mData = null;
    private String[] names;

    public MyArrayAdapter(Activity context, byte[] data, String[] names) {
        super(context, android.R.layout.simple_list_item_multiple_choice, names);
        this.context = context;

//        if (data != null) {
//            mData = data;
//        }

        this.names = names;
    }

    public MyArrayAdapter(Activity context, String[] names) {
        super(context, android.R.layout.simple_list_item_multiple_choice, names);
        //ArrayAdapter.createFromResource(context, R.array.mode_list, android.R.layout.simple_list_item_multiple_choice);
        this.context = context;
        this.names = names;
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        CheckedTextView checkbox;
        View tempView = convertView;
        if(tempView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            tempView = inflater.inflate(android.R.layout.simple_list_item_multiple_choice, null, true);
            checkbox = tempView.findViewById(android.R.id.text1);
            CharSequence[] chars = names;
            checkbox.setText(chars[position]);
            tempView.setTag(checkbox);

        } else {
            checkbox = (CheckedTextView) tempView.getTag();
        }

        if(ControlActivity.data != null) {
           Log.d(TAG, "MyArrayAdapter position: " + position);
            if (ControlActivity.data[position + 4] == 1) {
               // checkbox.setChecked(true);
                ((ListView)parent).setItemChecked((position + 4), true);
            } else {
               // checkbox.setChecked(false);
                ((ListView)parent).setItemChecked((position + 4), false);
            }
        }

        return tempView;
    }
}

//Слушатель-считыватель входящих данных
class InputThread extends Thread{
    private String TAG = "ConLog";
    private InputStream inputStream;
    byte[] data;

    //Флаг с актуальным состоянием потока
    private boolean isNeedToListenData = false;

    protected InputThread(InputStream inStream){
        Log.d(TAG, "InputThread инициализирован");
        inputStream = inStream;
    }

    @Override
    public void run(){
        Log.d(TAG, "InputThread поток запущен");
        int count;

        while(isNeedToListenData){
            try{
                if(inputStream.available() > 0 && inputStream.available() < 10){
                    try {
                        Thread.sleep(10);
                    }catch (InterruptedException ie){
                        Log.e(TAG, "ControlActivity: Ошибка приостановки потока!", ie);
                    }

                    count = inputStream.available();
                    data = new byte[count];
                    count = inputStream.read(data);
                    ControlActivity.data = data;

                    Log.d(TAG, "ControlActivity: Принято байт: " + count + ", Содердимое: " + Arrays.toString(data));
                }

                if(inputStream.available() == 54 ){
                    try {
                        Thread.sleep(10);
                    }catch (InterruptedException ie){
                        Log.e(TAG, "ControlActivity: Ошибка приостановки потока!", ie);
                    }

                    count = inputStream.available();
                    data = new byte[count];
                    count = inputStream.read(data);
                    ControlActivity.data = data;
                    ControlActivity.isInitialDataRecieved = true;

                    Log.d(TAG, "ControlActivity: Принято байт: " + count + ", Содердимое: " + Arrays.toString(data));
                }
            }catch (IOException ie){
                setEnabled(false);
                Log.e(TAG, "ControlActivity: Ошибка при получении данных!", ie);
            }
        }
        Log.d(TAG, "InputThread поток завершен");
    }

    public void setEnabled(boolean status){
        isNeedToListenData = status;
    }

    public boolean getEnabled(){
        return isNeedToListenData;
    }

//    public byte[] getData(){
//        if(data != null){
//            return data;
//        }
//        else {
//            return null;
//        }
//    }
}

class Commander {
    private OutputStream mOutputStream;

    private String TAG = "ConLog";

    private final Byte ON_OFF = 2;
    private final Byte PREV_MODE = 3;
    private final Byte NEXT_MODE = 4;
    private final Byte PAUSE_PLAY = 5;
    private final Byte FAV_MODE = 6;
    private final Byte ACT_DEACT_MODE = 7;
    private final Byte AUTO_MODE = 8;
    private final Byte SET_COLOR = 9;
    private final Byte SET_BRIGHT = 10;
    private final Byte SET_SPEED = 11;
    private final Byte SAVE_SETTINGS = 12;


    Commander(OutputStream outputStream){
        this.mOutputStream = outputStream;
    }

    private void sendMessage(byte ... data){
        try{
            Log.d(TAG, "Output: Попытка отправки данных: " + Arrays.toString(data) + " ...");
            mOutputStream.write(data);
        }catch (IOException ie){
            Log.e(TAG, "Output: Ошибка отправки данных!", ie);
        }
        Log.d(TAG, "Output: Данные отправлены");
    }

    public void mOnOff(){
        sendMessage(ON_OFF);
    }

    public void prevMode(){
        sendMessage(PREV_MODE);
    }

    public void nextMode(){
        sendMessage(NEXT_MODE);
    }

    public void pausePlay(){
        sendMessage(PAUSE_PLAY);
    }

    public void addToFav(){
        sendMessage(FAV_MODE);
    }

    public void mActDeactMode(byte mode, byte state){
        sendMessage(ACT_DEACT_MODE, mode, state);
    }

    public void setAuto(boolean state){
        byte msg;
        if (state) msg = 1;
        else msg = 0;
        sendMessage(AUTO_MODE, msg);
    }


}