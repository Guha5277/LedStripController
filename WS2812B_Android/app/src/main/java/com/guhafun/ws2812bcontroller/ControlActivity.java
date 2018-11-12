package com.guhafun.ws2812bcontroller;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ws2812bcontroller.R;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class ControlActivity extends AppCompatActivity implements View.OnClickListener {

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
    public static String DATA_MESSAGE = "com.guhafun.message";
    private boolean isStripEnable = false;

    //Флаг используемы для приема начальных значений от МК
    static boolean isInitialDataRecieved = false;

    //Массив принятных данных
    public static byte[] data;

    //Список с названиями и актуальным состоянием режимов, адаптер для списка
    ListView choiceModeList;
    CustomArrayAdapter adapter;
    //ArrayAdapter<String> adapter;

    //Элементы меню
    Menu controlMenu;
    Switch menuBtnOnOff;
    ImageButton menuBtnFav;
    ImageButton menuBtnSave;

    TextView txtCurMode;
    TextView txtCurModeNum;

    //Графические элементы - два ползунка с регулировкой скорости и яркости, кнопки переключения режимов, паузы
    SeekBar seekSpeed;
    SeekBar seekBright;
    ImageButton btnPrev;
    ImageButton btnNext;
    ImageButton btnPause;

    //Сокет и потоки, необходимые для обмена данными
    private static BluetoothSocket mmSocket = null;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    //Хэндлер для обмена сообщениями с потоком подключения (ConnectThread)
    HandlerControl mHandler = null;

    //Входящий поток приема данных
    InputThread mInputThread;
    Commander mCommander;

    //TAG для логов
    private String TAG = "ConLog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contlor);

        //Инициализация объектов графического интерфейса
        txtCurMode = findViewById(R.id.txtCurMode);
        txtCurModeNum = findViewById(R.id.txtCurNumber);
        choiceModeList = findViewById(R.id.modeListView);
        seekSpeed = findViewById(R.id.seekSpeed);
        seekBright = findViewById(R.id.seekBright);
        seekBright.setOnSeekBarChangeListener(seekBarListener);
        //seekBright.setMin(5);
        btnPrev = findViewById(R.id.btnPrev);
        btnPause = findViewById(R.id.btnPause);
        btnNext = findViewById(R.id.btnNext);

        btnPrev.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnPause.setOnClickListener(this);

        //Получаем потоки
        getStreams();

        mCommander = new Commander(mOutputStream);

        //Получаем список режимов из XML-файла
        modeList = this.getResources().getStringArray(
                R.array.mode_list);

        //Инициализируем Handler
        mHandler = new HandlerControl();

        //Регистрируем двух слушателей изменения состояния подключения
        registerReceiver(connectionStatusChanged, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        registerReceiver(connectionStatusChanged, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        LocalBroadcastManager.getInstance(this).registerReceiver(inputThreadListener, new IntentFilter(DATA_MESSAGE));

        //Инициализация и запуск фонового потока, отвечающего за прием данных
        mInputThread = new InputThread(mInputStream, this);
        mInputThread.setEnabled(true);
        mInputThread.start();

//        initializeData();
        ThreadInitialize initializeThread = new ThreadInitialize();
        initializeThread.start();

        Log.d(TAG, "ControlActivity создано");
    }

    //Обновление интерфейса в соотвествтии с актуальными данными
    private void updateUI(byte[] data){

                //Инициализируем адаптер - в конструкторе, помимо контекста, указываем также список режимов и принятые данные
                adapter = new CustomArrayAdapter(ControlActivity.this, modeList, data);
                choiceModeList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                choiceModeList.setAdapter(adapter);
                adapter.notifyDataSetChanged();

                btnPrev.setEnabled(true);
                btnPause.setEnabled(true);
                btnNext.setEnabled(true);
                seekBright.setEnabled(true);
                seekSpeed.setEnabled(true);

                choiceModeList.setEnabled(true);

                if(data[1] != 0) {
                    txtCurMode.setText(modeList[data[1] - 1]);
                } else {
                    txtCurMode.setText("Лента отключена");
                }

                    txtCurModeNum.setText(data[1] + "/49");


                seekBright.setProgress(data[2]);

                //Выключаем ползунок скорости, если у текущего режима отсутствует возможность ее регулировки
                if (data[4] == 0){
                    seekSpeed.setEnabled(false);
                    seekSpeed.setProgress(0);
                }
                else {
                    seekSpeed.setMax(data[4] * 2);
                    seekSpeed.setProgress(data[4]);
                }

                //Оповещаем меню
                invalidateOptionsMenu();
    }

    void disableUI() {
        isInitialDataRecieved = false;
        choiceModeList.setEnabled(false);
        seekBright.setEnabled(false);
        seekSpeed.setEnabled(false);
        btnPrev.setEnabled(false);
        btnPause.setEnabled(false);
        btnNext.setEnabled(false);
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

        menuBtnOnOff.setOnCheckedChangeListener(mOnCheckedChangeListener());

//        menuBtnOnOff.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });

        menuBtnFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCommander.addToFav(adapter.getCurrentMode());
            }
        });

        menuBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });


//        menu.findItem(R.id.swtOnOff).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                Toast.makeText(ControlActivity.this, "btnSwitch", Toast.LENGTH_SHORT).show();
//                return true;
//            }
//        });
//
//        menu.findItem(R.id.button_fav).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                Toast.makeText(ControlActivity.this, "btnFav", Toast.LENGTH_SHORT).show();
//                return true;
//            }
//        });
//
//        menu.findItem(R.id.button_save).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                Toast.makeText(ControlActivity.this, "btnSave", Toast.LENGTH_SHORT).show();
//                return true;
//            }
//        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        controlMenu = menu;
        if (isStripEnable){
            menuBtnOnOff.setEnabled(true);
            menuBtnOnOff.setOnCheckedChangeListener(null);
            menuBtnOnOff.setChecked(true);
            menuBtnOnOff.setOnCheckedChangeListener(mOnCheckedChangeListener());
            menuBtnSave.setEnabled(true);
            menuBtnSave.setEnabled(true);
        }
        else{
            menuBtnOnOff.setEnabled(false);
            menuBtnSave.setEnabled(false);
            menuBtnSave.setEnabled(false);
        }

       // menuBtnOnOff.setChecked(true);
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
    class HandlerControl extends Handler {
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

                    InputThread inputThread = new InputThread(mInputStream, ControlActivity.this);
                    inputThread.setEnabled(true);
                    inputThread.start();

                    mCommander = new Commander(mOutputStream);

                    ThreadInitialize initializeThread = new ThreadInitialize();
                    initializeThread.start();

                    break;
            }
        }
    }

    //Поток инициализации графическо интерфейса первичными данными
    class ThreadInitialize extends Thread {
        int count = 0;
        @Override
        public void run() {
            super.run();
            while (count < 10) {
                sendMessage(GET_INITIAL_DATA);
                try {
                    Thread.sleep(200);
                    if(isInitialDataRecieved){
                        //updateUI(isInitialDataRecieved);
                        Log.d(TAG, "Поток InitializeData завершен со счетчиком: " + count);
                        return;
                    }
                    count++;
                } catch (Exception ex) {
                    Log.e(TAG, "Ошибка потока Initialize Thread", ex);
                }

                disableUI();
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

    //Слушатель изменения состояния подключения к МК
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
                        disableUI();
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

    //Слушатель для обновления UI принятыми данными
    BroadcastReceiver inputThreadListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte returnedCommand = intent.getByteExtra("msg", (byte) 0);
            byte[] data = intent.getByteArrayExtra("data");
            switch (returnedCommand){

                case (byte) 1:
                    if (data.length == 54) {
                        isInitialDataRecieved = true;
                        isStripEnable = true;
                        updateUI(data);
                    }

                    Log.d(TAG, "ControlActivity приняты данные для инициализации в размере: " + data.length);
                    break;

                //Включение - выключение
//                case 2:
//                    if (data[1] == 1 && !menuBtnOnOff.isChecked()){
//                        menuBtnOnOff.setOnCheckedChangeListener(null);
//                        menuBtnOnOff.setChecked(true);
//                        menuBtnOnOff.setOnCheckedChangeListener(mOnCheckedChangeListener());
//                    }
//                    else if (data[1] == 0 && menuBtnOnOff.isChecked()){
//                        menuBtnOnOff.setOnCheckedChangeListener(null);
//                        menuBtnOnOff.setChecked(false);
//                        menuBtnOnOff.setOnCheckedChangeListener(mOnCheckedChangeListener());
//                    }
//                    break;

                //Переключение режимов
                case 3:
                case 4:
                    Log.d(TAG, "ControlActivity принята команда: " + returnedCommand);
                    adapter.setCurrentMode(data[1]);
                    txtCurMode.setText(modeList[data[1]-1]);
                    txtCurModeNum.setText(data[1] + "/49");
                    if(data[2] > 0){
                        seekSpeed.setEnabled(true);
                        seekSpeed.setMax(data[2] * 2);
                        seekSpeed.setProgress(data[2]);
                    }
                    else {
                        seekSpeed.setEnabled(false);
                        seekSpeed.setProgress(0);
                    }
                    break;

                case 6:
                    Toast.makeText(ControlActivity.this, "Режим " + modeList[data[1]-1] + " был установлен стартовым" , Toast.LENGTH_SHORT).show();
                    break;
            }


            Log.d(TAG, "ControlActivity приняты данные но ничего не изменено: " + returnedCommand);
        }
    };

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //Останавливаем поток входящих данных и закрываем сокет
        stopInputThread();

        //Снятие слушателей при уничтожении Активити
        unregisterReceiver(connectionStatusChanged);
        LocalBroadcastManager.getInstance(ControlActivity.this).unregisterReceiver(inputThreadListener);

        Log.d(TAG, "ControlActivity уничтожено");
    }

    @Override
    protected void onStop() {
        super.onStop();
       // closeSocket();

    }

    //Обработчик нажатий на элементы меню
    @Override
    public void onClick(View v) {
            switch (v.getId()){
                case R.id.btnPrev:
                    mCommander.prevMode();
                    break;

                case R.id.btnPause:
                    mCommander.pausePlay();
                    break;

                case R.id.btnNext:
                    mCommander.nextMode();
                    break;
            }
        }

    //private CompoundButton.OnCheckedChangeListener = new OnCheckedChangeListener(){

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener(){
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCommander.onOff();
            }
        };
    }

    private SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener () {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            switch(seekBar.getId()){
                case R.id.seekBright:
                    mCommander.setBright((byte)progress);
            }
        }
    };

    void stopInputThread(){
      //  if(mInputThread != null) mInputThread.setEnabled(false);
        //Убиваем поток InputThread
        if (mInputThread.isAlive()) {
            mInputThread.interrupt();
        } else {
            Log.d(TAG, "InputThread УЖЕ был остановлен!");
        }

        if (!mInputThread.isAlive()) Log.d(TAG, "InputThread был остановлен методом interrupt()");
    }
}

