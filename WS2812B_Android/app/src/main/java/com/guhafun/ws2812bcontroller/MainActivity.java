package com.guhafun.ws2812bcontroller;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ws2812bcontroller.R;

import java.util.HashMap;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "ConLog";
    private final int REQUEST_ENABLE_BT_FIRST = 1;
    private final int REQUEST_ENABLE_BT_SEARCH = 2;
    private final int REQUEST_ENABLE_BT_CONNECT = 3;
    private final int  REQUEST_COARSE_LOCATION = 1;
    public static final String  ACTION_CONNECT = "com.guhafun.connect";

    //Блютуз-адптер
    BluetoothAdapter localBluetoothDev;

    //TextView предлагающий выбрать устройство для подключения
    TextView tvWelcome;

    //Переменная для хранения адреса устройства к которому была попытка подключения
    String lastAdress;

    //Элементы списка устройств
    ArrayAdapter<String> adaper;
    ListView lvDevices;

    //Карта для хранения имени устройств и их MAC-адресов
    HashMap<String, String> devicesMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Получаем ссылки на графические элементы
        tvWelcome = findViewById(R.id.tvWelcome);
        lvDevices = findViewById(R.id.lvDevList);

        //Инициализируем адаптер для работы со списком
        adaper = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);

        //Добавляем адаптер для ListView, назначаем слушателя
        lvDevices.setAdapter(adaper);
        lvDevices.setOnItemClickListener(listViewListener);

        //Инициализируем локальный блютуз-адаптер
        localBluetoothDev = BluetoothAdapter.getDefaultAdapter();

        //Проверяем наличие блютуз, его включенность и получаем список устройств хранящихся в памяти
        checkLocalBT();
        requestBluetoothEnable(REQUEST_ENABLE_BT_FIRST);
        getPairedDevices();

        //Регистрируем слушателей поиска устройств
        registerReceiver(discoveryReceiever, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        //Регистрируем локального слушателя для приема данных от InputThread'a
        LocalBroadcastManager.getInstance(this).registerReceiver(discoveryReceiever, new IntentFilter(ACTION_CONNECT));

    }

    //Инициализируем элементы меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        //Инициализируем элемент меню
        MenuItem btnSearch = menu.findItem(R.id.btnRefresh);

        //Добавляем слушателя
        btnSearch.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //При нажатии на элемент меню, первым делом проверяем включен ли блютуз
                if (localBluetoothDev.isEnabled()) {
                    startDiscovery();
                }
                else {
                    requestBluetoothEnable(REQUEST_ENABLE_BT_SEARCH);
                }
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    //Проверка наличия Bluetooth в устройстве
    private void checkLocalBT() {
        if (localBluetoothDev == null ) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            builder.setTitle("Ошибка!")
                    .setMessage("В вашем устройстве нет Bluetooth, работа с приложением невозможна!")
                    .setCancelable(false)
                    .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

            builder.show();
            //... и закрываем приложение
            finish();
        }
    }

    //Проверка включенности Bluetooth с предложением его включить
    private void requestBluetoothEnable(int code) {
        if (!localBluetoothDev.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, code);

        }
    }

    //Получение списка сопряженных устройств
    private void getPairedDevices() {
        Set<BluetoothDevice> devices = localBluetoothDev.getBondedDevices();

        int size = devices.size();

//        Log.d(TAG, "Paired devices count: " + size);

        if (size != 0) {
            tvWelcome.setText(R.string.tvWelcome);

          //  devicesMap = new HashMap<>(size);

            for (BluetoothDevice mDevice : devices) {
                String name = mDevice.getName();
                devicesMap.put(name, mDevice.getAddress());
                adaper.add(name);
            }

            adaper.notifyDataSetChanged();
        }

        else {
           tvWelcome.setText(R.string.tvWelcomeNone);
        }
    }

    //Поиск Bluetooth-устройств
    private void startDiscovery() {
        //Перед началом поиска проверяем наличие разрешений "локация", без которых невозможно начать сам поиск
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
        }

        //Если всё хорошо - начинаем поиск устройств
        else {
            localBluetoothDev.startDiscovery();
        }
    }

    //Подключение к устройству
    private void connectToTarget (String adress) {
        lastAdress = adress;
        if (adress != null && BluetoothAdapter.checkBluetoothAddress(adress)) {

            BluetoothDevice device = localBluetoothDev.getRemoteDevice(adress);

            localBluetoothDev.cancelDiscovery();

            if (localBluetoothDev.isEnabled()) {

                tvWelcome.setText("Подключение к :" + lastAdress + "...");

                ConnectThread tryToConnect = new ConnectThread(device, this);
                tryToConnect.start();

            }

            else {
                requestBluetoothEnable(REQUEST_ENABLE_BT_CONNECT);
                Toast.makeText(MainActivity.this, "Для подключения к устройству включите Bluetooth!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_ENABLE_BT_FIRST:
                if (resultCode == Activity.RESULT_OK) {
                    //isBluetoothEnabled = true;
                    getPairedDevices();

                } else if (resultCode == Activity.RESULT_CANCELED) {
                    //isBluetoothEnabled = false;
                    Toast.makeText(this, "Для работы программы необходимо включить Bluetooth", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_ENABLE_BT_SEARCH:
                if (resultCode == Activity.RESULT_OK) {
                    startDiscovery();
                }
                else {
                    Toast.makeText(this, "Чтобы начать поиск устройств включите Bluetooth", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_ENABLE_BT_CONNECT:
                if (resultCode == Activity.RESULT_OK) {
                    connectToTarget(lastAdress);
                }
                else {
                    Toast.makeText(this, "Чтобы подключиться к устройству сначала включите Bluetooth", Toast.LENGTH_SHORT).show();
                }
                break;

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION: {
                //Если разрешения предоставлены пользователем, то начинаем поиск устройств
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    localBluetoothDev.startDiscovery();

                    //Иначе - выводим всплывающее сообщение
                } else {
                    Toast.makeText(this, "For start Discovery Devices Give the Location Permission", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    AdapterView.OnItemClickListener listViewListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //Получаем строку с именем элемента, по которой произошёл клик
            String onClickedName = adaper.getItem(position);
            String adress;

            for (String name : devicesMap.keySet()){
                if (name.equals(onClickedName)){
                    adress = devicesMap.get(name);
                    connectToTarget(adress);
                    break;
                }
            }
        }
    };

    //Слушатель уведомляющий о найденных устройствах и взаимодействующи  с потоком подключения к устройству
    BroadcastReceiver discoveryReceiever = new BroadcastReceiver() {
        private final String DISCOVERY_FOUND = BluetoothDevice.ACTION_FOUND;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case DISCOVERY_FOUND:
                        //Получаем найденное устройство
                        BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                        //Получаем имя устройства
                        String name = remoteDevice.getName();

                        //Выполняем проверку, есть ли уже это устройство в списке
                        if (!devicesMap.isEmpty()) {
                            for (String key : devicesMap.keySet()) {
                                //Если да - выходим из метода
                                if (key.equals(name)) {
                                    Log.d(TAG, "Найденное устройство уже есть в списке!");
                                    return;
                                }
                            }
                        }

                        Log.d(TAG, "Найденно новое устройство!");

                        //Добавляем устройство в коллекцию и адаптер
                        devicesMap.put(name, remoteDevice.getAddress());
                        adaper.add(name);

                        //Уведомляем адаптер об изменениях
                        adaper.notifyDataSetChanged();

                        tvWelcome.setText(R.string.tvWelcome);

                        break;

                    case ACTION_CONNECT:
                       final int ERROR = 0;
                       final int DONE = 1;

                        switch (intent.getIntExtra("result", 0)){
                            case ERROR:
                                Toast.makeText(MainActivity.this, "Ошибка подключения к " + lastAdress, Toast.LENGTH_SHORT).show();
                                tvWelcome.setText(R.string.tvWelcome);
                                break;

                            case DONE:
                                Intent intentConnect = new Intent(MainActivity.this, ControlActivity.class);
                                startActivity(intentConnect);
                                break;
                        }
                }
            }

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        tvWelcome.setText(R.string.tvWelcome);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(discoveryReceiever);
    }
}