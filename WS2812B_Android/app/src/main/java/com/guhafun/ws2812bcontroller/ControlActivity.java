package com.guhafun.ws2812bcontroller;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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

public class ControlActivity extends AppCompatActivity implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

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
    public static final String  ACTION_CONNECT = "com.guhafun.connect";

    //Два флага-состояния включенности ленты и авторежима
    private boolean isStripEnable = false;

    //Флаг используемы для приема начальных значений от МК
    static boolean isInitialDataRecieved = false;

    //Массив принятных данных
    public static byte[] data;

    //Массив содержащий множители для seekSpeed
    double[] miltiplier = {1.9, 1.8, 1.7, 1.6, 1.5, 1.4, 1.3, 1.2, 1.1, 1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};

    int flagSeekSpeed = 0;
    int tempDelay = 0;

    //Значение текущего положения seekSpeed

    //Список с названиями и актуальным состоянием режимов, адаптер для списка
    ListView choiceModeList;
    CustomArrayAdapter adapter;
    //ArrayAdapter<String> adapter;

    //Элементы меню
    Menu controlMenu;
    Switch menuBtnOnOff;
    ImageButton menuBtnFav, menuBtnSave, menuBtnPref;
    //Switch menuAutoOnOff;


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
    //HandlerControl mHandler = null;

    //Входящий поток приема данных
    InputThread mInputThread;
    Commander mCommander;

    //TAG для логов
    private String TAG = "ConLog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contlor);

        //Регистрируем слушателя изменения настроек приложения
        Context context = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);

        //Инициализация объектов графического интерфейса
        txtCurMode = findViewById(R.id.txtCurMode);
        txtCurModeNum = findViewById(R.id.txtCurNumber);
        choiceModeList = findViewById(R.id.modeListView);
        seekBright = findViewById(R.id.seekBright);
        seekSpeed = findViewById(R.id.seekSpeed);
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
        modeList = this.getResources().getStringArray(R.array.mode_list);

        //Инициализируем Handler
        //mHandler = new HandlerControl();

        //Регистрируем двух слушателей изменения состояния подключения
        registerReceiver(connectionStatusChanged, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        registerReceiver(connectionStatusChanged, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

        //Регистрируем локального слушателя для приема данных от InputThread'a
        LocalBroadcastManager.getInstance(this).registerReceiver(inputThreadListener, new IntentFilter(DATA_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(reconnectListener, new IntentFilter(ACTION_CONNECT));

        //Инициализация и запуск фонового потока, отвечающего за прием данных
        mInputThread = new InputThread(mInputStream, this);
        mInputThread.start();

        ThreadInitialize initializeThread = new ThreadInitialize();
        initializeThread.start();

//        Log.d(TAG, "ControlActivity создано");
    }

    //Обновление интерфейса в соотвествтии с актуальными данными
    private void updateUI(byte[] data){
                final byte MODE = 1;
                final byte BRIGHT = 2;
                final byte AUTO = 3;
                final byte SPEED = 4;
                final byte AUTO_DURATION = 54;
                final byte RANDOM = 55;
                final byte AUTO_SAVE = 56;
                final byte AUTO_SAVE_DURATION = 57;

                //Инициализируем адаптер - в конструкторе, помимо контекста, указываем также список режимов и принятые данные
                adapter = new CustomArrayAdapter(ControlActivity.this, modeList, data, mCommander);
                choiceModeList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                choiceModeList.setAdapter(adapter);
                adapter.notifyDataSetChanged();


                //Активируем список и кнопки
                choiceModeList.setEnabled(true);
                btnPrev.setEnabled(true);
                btnNext.setEnabled(true);
                btnPause.setEnabled(true);

                //Обновляем заголовок плейлиста
                setPlaylistTitle(data[MODE]);

                //Инициализируем ползунок яркости
                seekBright.setEnabled(true);
                seekBright.setProgress(data[BRIGHT] & 0xFF);

                //Выключаем ползунок скорости, если у текущего режима отсутствует возможность ее регулировки
                if (data[SPEED] == 0){
                    seekSpeed.setEnabled(false);
                }
                else {
                    seekSpeed.setEnabled(true);
                    tempDelay = data[SPEED];
                    seekSpeed.setProgress(9);
                    flagSeekSpeed = 9;

                //   Log.d(TAG, "ControlActivity: TempDelay установлен в: " + tempDelay + ", data[4]: " + data[4]);
                }

                //Устанавливаем слушатели для ползунков
                seekBright.setOnSeekBarChangeListener(seekBarListener);
                seekSpeed.setOnSeekBarChangeListener(seekBarListener);


                updatePreferences(data[AUTO], data[AUTO_SAVE], data[RANDOM], data[AUTO_DURATION], data[AUTO_SAVE_DURATION]);

                //Оповещаем меню
                invalidateOptionsMenu();
    }

    //Обновление настроек в соответствии с актуальными данными
    private void updatePreferences(byte autoState, byte autoSaveState, byte random, byte autoDuration, byte autoSaveDuration){
        //Конвертируем пришедшие переменые в соответствии с требуемыми типами
        boolean autoStateComed = (autoState == 1);
        boolean autoSaveSateComed = (autoSaveState == 1);
        boolean randomComed = (random == 1);
        String autoDurationComed = String.valueOf(autoDuration);
        String autoSaveDurationComed = String.valueOf(autoSaveDuration);

        //Получаем экземпляр класса для работы с настройками
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);

        //Загружаем параметры настроек из памяти устройства
        boolean autoStateLoad = sharedPreferences.getBoolean("auto_state", false);
        boolean autoSaveSateLoad = sharedPreferences.getBoolean("autosave_state", false);
        boolean randomLoad = sharedPreferences.getBoolean("random_state", false);
        String autoDurationLoad = sharedPreferences.getString("auto_duration", "5");
        String autoSaveDurationLoad = sharedPreferences.getString("autosave_duration", "5");

        //Получаем экземпляр класса для изменения настроек
        SharedPreferences.Editor edit = sharedPreferences.edit();

        // В случае необходимости - изменяем соовтетствующие параметры
        if (autoStateComed != autoStateLoad){
            edit.putBoolean("auto_state", autoStateComed);
        }
        if (autoSaveSateComed != autoSaveSateLoad){
            edit.putBoolean("autosave_state", autoSaveSateComed);
        }
        if (randomComed != randomLoad) {
            edit.putBoolean("random_state", randomComed);
        }
        if (!autoDurationComed.equals(autoDurationLoad)){
            edit.putString("auto_duration", autoDurationComed);
        }
        if (!autoSaveDurationComed.equals(autoSaveDurationLoad)){
            edit.putString("autosave_duration", autoSaveDurationComed);
        }

        //Сохраняем изменения
        edit.apply();
    }

    //Управление заголовком плейлиста в соответствии с актуальными данными
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
                txtCurModeNum.setText(mode + "/49");
                break;
        }
    }

    //Отключить ГИ
    void disableUI() {
        isInitialDataRecieved = false;
        isStripEnable = false;

        choiceModeList.setEnabled(false);
        seekBright.setEnabled(false);
        seekSpeed.setEnabled(false);
        btnPrev.setEnabled(false);
        btnPause.setEnabled(false);
        btnNext.setEnabled(false);
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
      //  Log.d(TAG, "ControlActivity: Потоки получены");
    }

    //Закрытие Bluetooth-сокета
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
        menuBtnPref = menu.findItem(R.id.preference).getActionView().findViewById(R.id.settings);

        menuBtnOnOff.setOnCheckedChangeListener(mOnCheckedChangeListener());

        menuBtnPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ControlActivity.this, AppPreference.class);
                startActivity(intent);
            }
        });

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
        if (isInitialDataRecieved){
            menuBtnOnOff.setEnabled(true);
            if (isStripEnable) {
                menuBtnOnOff.setOnCheckedChangeListener(null);
                menuBtnOnOff.setChecked(true);
                menuBtnOnOff.setOnCheckedChangeListener(mOnCheckedChangeListener());
            }
            else {
                menuBtnOnOff.setOnCheckedChangeListener(null);
                menuBtnOnOff.setChecked(false);
                menuBtnOnOff.setOnCheckedChangeListener(mOnCheckedChangeListener());
            }
            menuBtnSave.setEnabled(true);
            menuBtnSave.setEnabled(true);
        }
        else {
            menuBtnOnOff.setEnabled(false);
            menuBtnSave.setEnabled(false);
            menuBtnSave.setEnabled(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    //Поток инициализации графическо интерфейса первичными данными
    class ThreadInitialize extends Thread {
        int count = 0;

        @Override
        public void run() {
            super.run();

            //10 попыток на принятие данных инициализации
            while (count < 10) {
                //Отправляем запрос на данные инициализации
                mCommander.getInitialData();

                try {
                    //Небольшая пауза
                    Thread.sleep(200);

                    //Проверяем, пришли ли данные
                    if(isInitialDataRecieved){
                        //Если да - прерываем метод
                        return;
                    }
                    count++;
                } catch (Exception ex) {
                    Log.e(TAG, "Ошибка потока Initialize Thread", ex);
                }
                disableUI();
            }
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
//                        Log.d(TAG, "ControlActivity: Соединение восстановлено!");
                        break;

                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        //Останавливаем поток входящих данных, закрываем сокет
                        stopThread();
                        closeSocket();

                        //Отключаем UI
                        disableUI();

//                        Log.d(TAG, "ControlActivity: Соединение потеряно, попытка переподключения");
                        Toast.makeText(ControlActivity.this, "Соединение потеряно!!", Toast.LENGTH_SHORT).show();

                        //Инициализируем поток подключения
                        ConnectThread connectThread = new ConnectThread(mmSocket.getRemoteDevice(), ControlActivity.this);
                        connectThread.start();
                        break;
                }
            }
        }
    };
    
    //Слушатель для обновления UI принятыми данными
    BroadcastReceiver inputThreadListener = new BroadcastReceiver() {
        private final byte INIT = 1;
        private final byte ON_OFF = 2;
        private final byte PREV_MODE = 3;
        private final byte NEXT_MODE = 4;
        private final byte PAUSE_PLAY = 5;
        private final byte FAV_MODE = 6;
        private final byte ACT_DEACT_MODE = 7;
        private final byte AUTO_MODE = 8;
       // private final byte SET_COLOR = 9;
       // private final byte SET_BRIGHT = 10;
        private final byte SET_SPEED = 11;
        private final byte SAVE_SETTINGS = 12;
        private final byte SET_MODE_TO = 13;

        @Override
        public void onReceive(Context context, Intent intent) {
            byte returnedCommand = intent.getByteExtra("msg", (byte) 0);
            byte[] data = intent.getByteArrayExtra("data");
            switch (returnedCommand){

                case INIT:
                    if (data.length == 58) {
                        isInitialDataRecieved = true;
                        if (data[1] > 0) isStripEnable = true;
                        updateUI(data);
                    }

//                    Log.d(TAG, "ControlActivity приняты данные для инициализации в размере: " + data.length);
                    break;

                //Включение - выключение
                case ON_OFF:
                    if (data[1] == 1 && !menuBtnOnOff.isChecked()){
                        isStripEnable = true;
                        invalidateOptionsMenu();
                    }
                    else if (data[1] == 0 && menuBtnOnOff.isChecked()){
                        isStripEnable = false;
                        invalidateOptionsMenu();
                    }

                    setPlaylistTitle(data[2]);
                    adapter.setCurrentMode(data[2]);

                    break;

                //Переключение режимов
                case PREV_MODE:
                case NEXT_MODE:
                case SET_MODE_TO:
//                    Log.d(TAG, "ControlActivity принята команда: " + returnedCommand);
                    adapter.setCurrentMode(data[1]);
                    setPlaylistTitle(data[1]);

                    //Здесь проводится проверка приешдшего значения скорости
                    if(data[2] > 0){
                        //Если есть параметр скорости, то активируется ползунок скорости
                        seekSpeed.setEnabled(true);
                        //Пришедшее значение сохраняется во временную переменную
                        tempDelay = data[2];
//                        Log.d(TAG, "ControlActivity: tempDelay: " + tempDelay + ", data[2]: " + data[2]);

                        //Если текущее значение ползунка отличается от среднего значения (значения по умолчанию)
                        if (flagSeekSpeed != 9) {
                            //Вычислить новое значение скорости и отправить его МК
                            byte speed = (byte)(miltiplier[flagSeekSpeed] * tempDelay);
                            mCommander.setSpeed(speed);
//                            Log.d(TAG, "ControlActivity: flagSeekSpeed: " +  flagSeekSpeed);
//                            Log.d(TAG, "ControlActivity: отправлены значения скорости, потому-что так надо: " +  speed + ", (Int): " + (speed & 0xFF));
                        }
                    }
                    else {
                        //Если значение скорости нулевое, то отключить ползунок скорости
                        seekSpeed.setEnabled(false);
                    }

                    if (!menuBtnOnOff.isChecked()){
                        isStripEnable = true;
                        invalidateOptionsMenu();
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
                    if(data[1] != 0) {
                        Toast.makeText(ControlActivity.this, "Режим " + modeList[data[1] - 1] + " был установлен стартовым", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(ControlActivity.this, "Стартовый режим удален", Toast.LENGTH_SHORT).show();
                    }
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

                case SET_SPEED:
//                    if (data[1] != 0) {
//                        tempDelay = data[1];
//                    }
//                    else {
//                        tempDelay = 0;
//                    }
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

    //Слушатель при попытках переподключения
    BroadcastReceiver reconnectListener = new BroadcastReceiver() {
        int reconnectCount = 1;

        @Override
        public void onReceive(Context context, Intent intent) {
            final int ERROR = 0;
            final int DONE = 1;

            String action = intent.getAction();

            if (action.equals(ACTION_CONNECT)) {
                switch (intent.getIntExtra("result", 0)) {
                    case ERROR:
                        //Если достигнул лимит по попыткам переподключения
                        if (reconnectCount == 3) {
                            //Создаем и показываем диалоговое окно с текстом ошибки
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(ControlActivity.this);

                            builder.setTitle("Соединение потеряно!")
                                    .setMessage("Достигнуто максимальное количество попыток переподключения! Проверьте доступность устройства!")
                                    .setCancelable(false)
                                    .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                            finish();
                                        }
                                    });

                            builder.show();
                        }

                        else {
                            //Увеличиваем счётчи и пробуем переподключиться
                            reconnectCount++;
                            ConnectThread connectThread = new ConnectThread(mmSocket.getRemoteDevice(), ControlActivity.this);
                            connectThread.start();
                            // Log.d(TAG, "ControlActivity: Попытка переподключения №" + reconnectCount + "...");
                        }
                        break;

                    case DONE:
                        //Получаем потоки
                        getStreams();

                        //Заново запускаем поток-слушатель входящих данных
                        InputThread inputThread = new InputThread(mInputStream, ControlActivity.this);
                        inputThread.start();

                        //Заново инициализиурем объект отправки сообщений
                        mCommander = new Commander(mOutputStream);

                        //Заново инициализиурем данные
                        ThreadInitialize initializeThread = new ThreadInitialize();
                        initializeThread.start();
                        break;
                }
            }

        }
    };

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //Останавливаем поток входящих данных и закрываем сокет
        stopThread();
        closeSocket();

        //Снятие слушателей при уничтожении Активити
        unregisterReceiver(connectionStatusChanged);
        LocalBroadcastManager.getInstance(ControlActivity.this).unregisterReceiver(inputThreadListener);
        LocalBroadcastManager.getInstance(ControlActivity.this).unregisterReceiver(reconnectListener);

        Log.d(TAG, "ControlActivity уничтожено");
    }

    @Override
    protected void onStop() {
        super.onStop();
        //stopThread();
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

    //Слушатель ползунков
    private SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener () {

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        //Данный слушатель нужен только для полузнка скорости и сохраняет последнее его значение "progress"
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            switch(seekBar.getId()){
                case R.id.seekSpeed:
                    flagSeekSpeed = seekSpeed.getProgress();
                    Log.d(TAG, "ControlActivity: flagSeekSpeed (onStopListener) :" + flagSeekSpeed);
                    break;
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            //Получаем ID ползунка, значение которого было изменено
            switch(seekBar.getId()){
                //Яркость
                case R.id.seekBright:
                    //Отправляем новую яркость
                    mCommander.setBright((byte)progress);
                    break;

                //Скорость
                case R.id.seekSpeed:

                    //Сохраняем текущий прогресс в переменную

                  //  Log.d(TAG, "ControlActivity: flagSeekSpeed (listener) :" + flagSeekSpeed);

                    //Высчитываем новую скорость
                    double speed = miltiplier[progress] * tempDelay;

                    //Отправляем
                    mCommander.setSpeed((byte) speed);

                    //Log.d(TAG, "SeekSpeed Value: " + convertValue.get(progress) * value);
                    break;
            }
        }
    };

    //Остановка поток InputThread
    void stopThread( ){
        if (mInputThread.isAlive()) {
            Log.d(TAG, "Остановка потока InputThread");
            mInputThread.stopInputThread();
        } else {
            Log.d(TAG, "InputThread УЖЕ был остановлен!");
        }


        if (!mInputThread.isAlive()) Log.d(TAG, mInputThread.getName() + " был остановлен методом interrupt()");
    }

    //Слушатель изменения настроек приложения
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //Проверка измененного параметра по ключу
        switch (key) {
            //Активация/деактивация авторежима
            case "auto_state": {
                boolean result = sharedPreferences.getBoolean(key, false);
                mCommander.setAutoMode(result);

                Log.d(TAG, "Авторежим установлен в: " + result);
                break;
            }

            //Продолжительность воспроизведения одного эффекта в авторежиме
            case "auto_duration": {
                int result = Integer.parseInt(sharedPreferences.getString(key, "0"));
                mCommander.setAutoModeDuration((byte) result);

                Log.d(TAG, "Длительность авторежима установлена в: " + result);
                break;
            }

            //Случайное переключение режимов
            case "random_state": {
                boolean result = sharedPreferences.getBoolean(key, false);
                mCommander.setRandom(result);

                Log.d(TAG, "Случайное переключение: " + result);
                break;
            }

            //Автосохранение
            case "autosave_state": {
                boolean result = sharedPreferences.getBoolean(key, false);
                mCommander.setAutoSave(result);

                Log.d(TAG, "Автосохранение: " + result);
                break;
            }

            //Периодичность автосохранения
            case "autosave_duration": {
                int result = Integer.parseInt(sharedPreferences.getString(key, "0"));
                mCommander.setAutoSaveDuration((byte)result);

                Log.d(TAG, "Интервал автосохранения: " + result);
                break;
            }
        }
    }
}