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

    //Строка для связи LocalBroadcastReceiver и InputThread
    public static String DATA_MESSAGE = "com.guhafun.message";

    //Два флага-состояния включенности ленты и авторежима
    private boolean isStripEnable = false;
    private boolean isAutoModeEnable = false;

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
    Switch menuAutoOnOff;

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
        seekBright = findViewById(R.id.seekBright);
        seekBright.setOnSeekBarChangeListener(seekBarListener);
        seekSpeed = findViewById(R.id.seekSpeed);
        seekSpeed.setOnSeekBarChangeListener(seekBarListener);
        btnPrev = findViewById(R.id.btnPrev);
        btnPause = findViewById(R.id.btnPause);
        btnNext = findViewById(R.id.btnNext);

        //Добавление слушателей для кнопок управления плейлистом
        btnPrev.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnPause.setOnClickListener(this);

        //Получаем потоки
        getStreams();

        //Инициализация фонового потока отправки комманд на МК
        mCommander = new Commander(mOutputStream);

        //Получаем список режимов из XML-файла
        modeList = this.getResources().getStringArray(
                R.array.mode_list);

        //Инициализируем Handler
        mHandler = new HandlerControl();

        //Регистрируем двух слушателей изменения состояния подключения
        registerReceiver(connectionStatusChanged, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        registerReceiver(connectionStatusChanged, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        //Регистрируем локального слушателя для приема данных от InputThread'a
        LocalBroadcastManager.getInstance(this).registerReceiver(inputThreadListener, new IntentFilter(DATA_MESSAGE));

        //Инициализация и запуск фонового потока, отвечающего за прием данных
        mInputThread = new InputThread(mInputStream, this);
        //mInputThread.setEnabled(true);
        mInputThread.start();

//        initializeData();
        ThreadInitialize initializeThread = new ThreadInitialize();
        initializeThread.start();

        Log.d(TAG, "ControlActivity создано");
    }



    //Обновление интерфейса в соотвествтии с актуальными данными
    private void updateUI(byte[] data){

                //Инициализируем адаптер - в конструкторе, помимо контекста, указываем также список режимов и принятые данные
                adapter = new CustomArrayAdapter(ControlActivity.this, modeList, data, mCommander);
                choiceModeList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                choiceModeList.setAdapter(adapter);
                adapter.notifyDataSetChanged();

                btnPrev.setEnabled(true);
                btnPause.setEnabled(true);
                btnNext.setEnabled(true);
                seekBright.setEnabled(true);
                seekSpeed.setEnabled(true);

                choiceModeList.setEnabled(true);

                setPlaylistTitle(data[1]);

                seekBright.setProgress(data[2] & 0xFF);

                Log.d(TAG, "Bright from (byte): " + data[2] + ", to(int): " + (data[2] & 0xFF) );

                //Выключаем ползунок скорости, если у текущего режима отсутствует возможность ее регулировки

                if (data[4] == 0){
                    seekSpeed.setEnabled(false);

                    seekSpeed.setOnSeekBarChangeListener(null);
                    seekSpeed.setProgress(0);
                    seekSpeed.setOnSeekBarChangeListener(seekBarListener);
                }
                else {
                    seekSpeed.setTag(0);
                    seekSpeed.setEnabled(true);
                    seekSpeed.setTag(data[4]);

                    seekSpeed.setOnSeekBarChangeListener(null);
                    seekSpeed.setProgress(9);
                    seekSpeed.setOnSeekBarChangeListener(seekBarListener);
                }

//                if (data[3] == 1){
//                    isAutoModeEnable = true;
//                }
//                else {
//                    isAutoModeEnable = false;
//                }

                isAutoModeEnable = (data[3] == 1);

                //Оповещаем меню
                invalidateOptionsMenu();
    }

    private void setPlaylistTitle(byte mode) {
        switch (mode){
            case 0:
                txtCurMode.setText("Лента отключена");
                txtCurModeNum.setText(R.string.modePausedOrOff);
                break;

            case 99:
                txtCurMode.setText("Пауза");
                txtCurModeNum.setText(R.string.modePausedOrOff);
                break;

            default:
                txtCurMode.setText(modeList[mode - 1]);
                txtCurModeNum.setText(mode + R.string.modesTotal);
                break;
        }
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
        menuAutoOnOff = menu.findItem(R.id.automode).getActionView().findViewById(R.id.switchButton);


        menuBtnOnOff.setOnCheckedChangeListener(mOnCheckedChangeListener());
        menuAutoOnOff.setOnCheckedChangeListener(mOnCheckedAutoChangeListener());

        menuBtnFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCommander.addToFav(adapter.getCurrentMode());
            }
        });

        menuBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCommander.saveSettings();
            }
        });

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
            menuAutoOnOff.setEnabled(true);
        }
        else {
            menuBtnOnOff.setEnabled(false);
            menuBtnSave.setEnabled(false);
            menuBtnSave.setEnabled(false);
            menuAutoOnOff.setEnabled(false);
        }

        if (isAutoModeEnable) {
            menuAutoOnOff.setOnCheckedChangeListener(null);
            menuAutoOnOff.setChecked(true);
            menuAutoOnOff.setOnCheckedChangeListener(mOnCheckedAutoChangeListener());
        }
        else {
            menuAutoOnOff.setOnCheckedChangeListener(null);
            menuAutoOnOff.setChecked(false);
            menuAutoOnOff.setOnCheckedChangeListener(mOnCheckedAutoChangeListener());
        }



        return super.onPrepareOptionsMenu(menu);
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
                    //inputThread.setEnabled(true);
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
        private final Byte GET_INITIAL_DATA = 1;
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
        private final byte INIT = 1;
        //private final byte ON_OFF = 2;
        private final byte PREV_MODE = 3;
        private final byte NEXT_MODE = 4;
        private final byte PAUSE_PLAY = 5;
        private final byte FAV_MODE = 6;
        private final byte ACT_DEACT_MODE = 7;
        private final byte AUTO_MODE = 8;
       // private final byte SET_COLOR = 9;
       // private final byte SET_BRIGHT = 10;
       // private final byte SET_SPEED = 11;
        private final byte SAVE_SETTINGS = 12;
        private final byte SET_MODE_TO = 13;

        @Override
        public void onReceive(Context context, Intent intent) {
            byte returnedCommand = intent.getByteExtra("msg", (byte) 0);
            byte[] data = intent.getByteArrayExtra("data");
            switch (returnedCommand){

                case INIT:
                    if (data.length == 54) {
                        isInitialDataRecieved = true;
                        isStripEnable = true;
                        updateUI(data);
                    }

                    Log.d(TAG, "ControlActivity приняты данные для инициализации в размере: " + data.length);
                    break;

                //Включение - выключение
//                case ON_OFF:
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
                case PREV_MODE:
                case NEXT_MODE:
                case SET_MODE_TO:
                    Log.d(TAG, "ControlActivity принята команда: " + returnedCommand);
                    adapter.setCurrentMode(data[1]);
                    setPlaylistTitle(data[1]);

                    if(data[2] > 0){
                        seekSpeed.setEnabled(true);
                        seekSpeed.setTag(data[2]);
                        seekSpeed.setOnSeekBarChangeListener(null);
                        seekSpeed.setProgress(9);
                        seekSpeed.setOnSeekBarChangeListener(seekBarListener);
                    }
                    else {
                        seekSpeed.setTag(0);
                        seekSpeed.setEnabled(false);
                        seekSpeed.setOnSeekBarChangeListener(null);
                        seekSpeed.setProgress(0);
                        seekSpeed.setOnSeekBarChangeListener(seekBarListener);
                    }
                    break;

                case PAUSE_PLAY:
                    //data[1] - находится ли лента на паузе (1 и 0)
                    if (data[1] == 1)
                        Toast.makeText(ControlActivity.this, "Пауза", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(ControlActivity.this, "Воспроизведение", Toast.LENGTH_SHORT).show();
                    break;

                case FAV_MODE:
                    Toast.makeText(ControlActivity.this, "Режим " + modeList[data[1]-1] + " был установлен стартовым" , Toast.LENGTH_SHORT).show();
                    break;

                case ACT_DEACT_MODE:
                    //data[1] - номер режима
                    //data[2] - состояние (1 и 0)
                    if (data[2] == 1) {
                        Toast.makeText(ControlActivity.this, "" + modeList[data[1]] + " добавлен в плейлист", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(ControlActivity.this, "" + modeList[data[1]] + " исключен из плейлиста", Toast.LENGTH_SHORT).show();
                    }

                    //Отправляем полученные данные в адаптер
                    adapter.setActiveModes(data[1], data[2]);

                    break;

                case AUTO_MODE:
                    //data[1] - состояние авторежима (1 и 0)
                    if (data[1] == 1){
                        //isAutoModeEnable = true;
                        Toast.makeText(ControlActivity.this, "Авторежим включен"  , Toast.LENGTH_SHORT).show();
                    }
                    else {
                        //isAutoModeEnable = false;
                        Toast.makeText(ControlActivity.this, "Авторежим выключен"  , Toast.LENGTH_SHORT).show();
                    }
                    //invalidateOptionsMenu();
                    break;
                    
                case SAVE_SETTINGS:
                    //data[1] - может принимать только значение 1 - настройки сохранены
                    if (data[1] == 1){
                        Toast.makeText(ControlActivity.this, "Настройки сохранены!", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(ControlActivity.this, "Ошибка сохранения настроек!", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
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

    //Слушатель переключения включения-выключения режима
    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener(){
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mCommander.onOff();
                }
        };
    }

    //Слушатель переключателя включения-выключения авторежима
    private CompoundButton.OnCheckedChangeListener mOnCheckedAutoChangeListener(){
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                       // isAutoModeEnable = !isChecked;
                        mCommander.setAutoMode(isChecked);
                      //  invalidateOptionsMenu();

                        Log.d(TAG, "Listener");
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

//            HashMap<Integer, Double> convertValue = new HashMap<>();
//            convertValue.put(0, 1.9);
//            convertValue.put(1, 1.8);
//            convertValue.put(2, 1.7);
//            convertValue.put(3, 1.6);
//            convertValue.put(4, 1.5);
//            convertValue.put(5, 1.4);
//            convertValue.put(6, 1.3);
//            convertValue.put(7, 1.2);
//            convertValue.put(8, 1.1);
//            convertValue.put(9, 1.0);
//            convertValue.put(10, 0.9);
//            convertValue.put(11, 0.8);
//            convertValue.put(12, 0.7);
//            convertValue.put(13, 0.6);
//            convertValue.put(14, 0.5);
//            convertValue.put(15, 0.4);
//            convertValue.put(16, 0.3);
//            convertValue.put(17, 0.2);
//            convertValue.put(18, 0.1);

            double[] miltiplier = {1.9, 1.8, 1.7, 1.6, 1.5, 1.4, 1.3, 1.2, 1.1, 1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};

            switch(seekBar.getId()){
                case R.id.seekBright:
                    mCommander.setBright((byte)progress);
                    break;

                case R.id.seekSpeed:
                    int value = (Byte) seekSpeed.getTag();

                    double speed = miltiplier[progress] * value;

                    //double speed = convertValue.get(progress) * value;

                    mCommander.setSpeed((byte) speed);

                    //Log.d(TAG, "SeekSpeed Value: " + convertValue.get(progress) * value);
                    break;
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