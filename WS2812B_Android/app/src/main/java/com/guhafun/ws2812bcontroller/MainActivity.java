package com.guhafun.ws2812bcontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ws2812bcontroller.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    //Создание потока подключения
    ConnectThread tryToConnect = null;

    //Handler для взаимодействия с потоком подключения
    Handler handler = null;

    //Блютуз-адаптер
    BluetoothAdapter bluetoothLocal;

    //Объявление элементов графического интерфейса
    ListView pairedListView;
    ListView discoveredListView;
    TextView pairedTextView;
    TextView discoveredTextView;

    //Объявление меню
    Menu myMenu;

    //Объявление диалога
    ProgressDialog mProgressDialog = null;

    //Поля с запросами для наших Intent'ов (startActivityForResult)
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_COARSE_LOCATION = 2;

    //Поля для построения элементов списка
    private final String ADAPTER_NAME = "deviceName";
    private final String ADAPTER_MAC = "macAdress";

    //Поле с количеством найденных устройств (не включает сопряженные устройства)
    private int countDevicesFound = 0;

    //Объекты необходимые для построения списка
    ArrayList<HashMap<String, String>> pairedHashList = new ArrayList<>();
    HashMap<String, String> hashMap = new HashMap<>();
    SimpleAdapter pairedAdapter;
    SimpleAdapter discoveredAdapter;

    //Тэг для логгирования
    private final String TAG = "ConLog";

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Находим View-элементы и присваиваем ссылки на них
        pairedListView = findViewById(R.id.pairedDevList);
        discoveredListView = findViewById(R.id.discoveredDevList);
        pairedTextView = findViewById(R.id.pairedTextView);
        discoveredTextView = findViewById(R.id.discoveredTextView);

        //Настраиваем адаптеры для списков
        pairedAdapter = new SimpleAdapter(this, pairedHashList,
                R.layout.header, new String[]{ADAPTER_NAME, ADAPTER_MAC},
                new int[]{R.id.listTitle, R.id.listContent});

        discoveredAdapter = new SimpleAdapter(this, pairedHashList,
                R.layout.header, new String[]{ADAPTER_NAME, ADAPTER_MAC},
                new int[]{R.id.listTitle, R.id.listContent});

        //Получаем данные о локальном блютуз-адаптере (null - в случае, если устройство не поддерживает функцию Bluetooth)
        bluetoothLocal = BluetoothAdapter.getDefaultAdapter();

        //Проверяем, поддерживает ли устройство функцию блютуз вообще...
        //... если поддержка есть, то заправшиваем у пользователя включение блютуз
        if (bluetoothLocal != null) {
            requestBTEnable();
        }
        //... если его нет, то выводим сообщение об ошибке...
        else {
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
            //... и закрываем приложение
            finish();
        }

        //Инициализация Handler'a
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                //По результатам сообщения(0 - ошибка подключения и 1 - соединение установлено...
                switch (msg.what) {
                    case 0:
                        //... В случае ошибки подключения, закрываем диалог и выводим сообщение об ошибке
                        if (mProgressDialog != null && mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Не удалось установить соединение!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "MainActivity: Не удалось установить соединение! Проверьте доступность Bluetooth-устройства");
                        }
                        break;
                    case 1:
                        //... В случае успешного подключения, закрываем диалог и вызываем ControlActivity
                        Intent intent = new Intent(MainActivity.this, ControlActivity.class);
                        startActivity(intent);
                        if (mProgressDialog != null && mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }
                        break;
                }
            }
        };


        //Задаем слушателя для событий нажатия на элементы списка
        pairedListView.setOnItemClickListener(itemClickListener);
        discoveredListView.setOnItemClickListener(itemClickListener);

        Log.d(TAG, "MainActivity создано");
    }

    //Реализация обработки нажатий на элементы списка
    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            HashMap<String, String> itemHashMap = (HashMap<String, String>) parent.getItemAtPosition(position);
            String adapterName = itemHashMap.get(ADAPTER_NAME);
            String adapterMac = itemHashMap.get(ADAPTER_MAC);

            if (BluetoothAdapter.checkBluetoothAddress(adapterMac)) {
                Log.d(TAG, "MainActivity: MAC-адресс: " + adapterMac + " имеет валидный формат!");
                BluetoothDevice device = bluetoothLocal.getRemoteDevice(adapterMac);
                Log.d(TAG, "MainActivity: Bluetooth устройство: " + device.getName() + ", MAC: " + device.getAddress() + " успешно инициализировано!");
                bluetoothLocal.cancelDiscovery();

                //Проверяем, включен ли блютуз перед подключением
                if (bluetoothLocal.isEnabled()) {
                    Log.d(TAG, "MainActivity: подключение к " + device.getName() + ", " + device.getAddress() + "...");

                    //Создаем диалог информирующий пользователя о инициации подключения
                    mProgressDialog = new ProgressDialog(MainActivity.this);
                    mProgressDialog.setCancelable(true);
                    mProgressDialog.setTitle("Подключение");
                    mProgressDialog.setMessage("Подключаюсь к " + device.getName());
                    mProgressDialog.show();

                    tryToConnect = new ConnectThread(device, handler);
                    tryToConnect.start();
                } else {
                    Log.d(TAG, "MainActivity: ошибка подключения к " + device.getName() + "! Bluetooth Adapter отключен!");
                    Toast.makeText(MainActivity.this, "Для подключения к устройству включите Bluetooth!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "MainActivity: MAC-адрес: " + adapterMac + "имеет неверный формат!");
            }

            Toast.makeText(getApplicationContext(),
                    "Device " + adapterName + ". Mac " + adapterMac, Toast.LENGTH_SHORT)
                    .show();
        }
    };

    //Метод инициализирующий запрос разрешний(Location) у приложения, необходимых для запуска процесса поиска новых устройств
    protected void checkLocationPermission() {
        //Если нет соответствующих разрешений - отправляем запрос пользователю
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
        }

        //Иначе - запускаем процесс поиска новых устройств
        else {
            bluetoothLocal.startDiscovery();
        }
    }

    //Метод вызываемый по результатом ответа пользователя, на запрос разрешений(Location)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION: {
                //Если разрешения предоставлены пользователем, то начинаем поиск устройств
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bluetoothLocal.startDiscovery();

                    //Иначе - выводим всплывающее сообщение
                } else {
                    Toast.makeText(this, "For start Discovery Devices Give the Location Permission", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    //Основное меню приложения - поиск устройств...
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        myMenu = menu;
        getMenuInflater().inflate(R.menu.main, menu);
        if (bluetoothLocal.isEnabled()) {
            myMenu.findItem(R.id.btnRefresh).setEnabled(true);
        } else {
            myMenu.findItem(R.id.btnRefresh).setEnabled(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    //Обработка нажатий на элементы меню
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btnRefresh:
                checkLocationPermission();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //Метод, который проверяет включен ли Bluetooth и если нет, то отправляет пользователю запрос на его включение
    private void requestBTEnable() {
        if (!bluetoothLocal.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

        } else {
            updateListOfDevices(pairedAdapter, null, pairedListView, false);
            //pairedTextView.setVisibility(View.VISIBLE);
        }
    }

    //Метод, который обрабатывает результаты вызова метода startActivityForResult     <---- нужно доработать requestCode(код запроса)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Если пользователь включил блютуз, то происходит настройка ГПИ
        if (resultCode == Activity.RESULT_OK) {
            myMenu.findItem(R.id.btnRefresh).setEnabled(true);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            myMenu.findItem(R.id.btnRefresh).setEnabled(false);
            Toast.makeText(this, "Включите Bluetooth для продолжения работы с программой", Toast.LENGTH_SHORT).show();
        }
    }


    //Слушатель уведомляющий о найденных устройствах
    BroadcastReceiver discoveryBR = new BroadcastReceiver() {
        private final String DISCOVERY_STARTED = BluetoothAdapter.ACTION_DISCOVERY_STARTED;
        private final String DISCOVERY_FINISHED = BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
        private final String DISCOVERY_FOUND = BluetoothDevice.ACTION_FOUND;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case DISCOVERY_STARTED:
                        countDevicesFound = 0;
                        pairedHashList.clear();
                        discoveredAdapter.notifyDataSetChanged();
                        Toast.makeText(context, "Discovery started", Toast.LENGTH_SHORT).show();
                        break;

                    case DISCOVERY_FINISHED:
                        Toast.makeText(context, "Discovery finished", Toast.LENGTH_SHORT).show();
                        break;

                    case DISCOVERY_FOUND:
                        discoveredTextView.setVisibility(View.VISIBLE);
                        BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        updateListOfDevices(discoveredAdapter, remoteDevice, discoveredListView, countDevicesFound == 0);
                        countDevicesFound++;
                        break;

                }
            }

        }
    };

    //Слушатель изменения состояния Bluetooth
    BroadcastReceiver btStateChangeBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int ON = BluetoothAdapter.STATE_ON;
            int OFF = BluetoothAdapter.STATE_OFF;
            int currState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);

            //Если текущее состояние BT стало STATE_OFF а было STATE_ON, то выводим всплывающее собщение...
            if (currState == OFF && prevState == ON) {
                //Обновляем список сопряженных устройств
                updateListView(pairedAdapter, false, false, R.layout.header_deactivate, false, View.VISIBLE, Color.GRAY);

                //Выводим всплывающее сообщение с предложением включить Bluetooth
                Toast.makeText(MainActivity.this, "Для продолжения работы, включите Bluetooth!", Toast.LENGTH_SHORT).show();
            }
            //Если текущее состояние BT стало STATE_ON..
            if (currState == ON) {
                //Обновляем список сопряженных устройств
                updateListView(pairedAdapter, true, true, R.layout.header, true, View.VISIBLE, Color.BLACK);
            }

        }
    };

    //Метод который обновляет список устройств
    private void updateListOfDevices(SimpleAdapter adapter, BluetoothDevice device, ListView listView, boolean isNeedToClearList) {
        if (isNeedToClearList) {
            //Очищаем хэш-таблицу со списком устройств, чтобы исключить клонирование элементов
            pairedHashList.clear();
        }

        if (device == null) {
            //Получаем список сопряженных устройств, хранящийся локально
            Set<BluetoothDevice> pairedDevice = bluetoothLocal.getBondedDevices();
            // pairedDevCount = pairedDevice.
            //перебираем список сопряженных устройств и добавляем их в хэш-таблицу
            if (pairedDevice.size() == 0) {
                pairedTextView.setVisibility(View.INVISIBLE);
                pairedTextView.setPadding(0, 0, 0, 0);
                pairedTextView.setHeight(0);
            } else {
                pairedTextView.setVisibility(View.VISIBLE);
                for (BluetoothDevice mDevice : pairedDevice) {
                    hashMap = new HashMap<>();
                    hashMap.put(ADAPTER_NAME, mDevice.getName());
                    hashMap.put(ADAPTER_MAC, mDevice.getAddress());
                    pairedHashList.add(hashMap);
                }
            }
        } else {
            hashMap = new HashMap<>();
            hashMap.put(ADAPTER_NAME, device.getName());
            hashMap.put(ADAPTER_MAC, device.getAddress());
            pairedHashList.add(hashMap);
        }

        listView.setAdapter(adapter);
        //Уведомляем адаптер об изменении списка
        adapter.notifyDataSetChanged();
    }

    private void updateListView(SimpleAdapter adapter, boolean isNeedToUpdateList, boolean isNeedToClearList, int layoutResourceID, boolean setEnabled, int visible, int setColor) {

        if (isNeedToUpdateList) {
            updateListOfDevices(adapter, null, pairedListView, isNeedToClearList);
        }

        //pairedHashList.clear();
        adapter = new SimpleAdapter(MainActivity.this, pairedHashList, layoutResourceID, new String[]{ADAPTER_NAME, ADAPTER_MAC}, new int[]{R.id.listTitle, R.id.listContent});
        pairedListView.setAdapter(adapter);
        pairedListView.setEnabled(setEnabled);
        pairedTextView.setTextColor(setColor);
        // pairedTextView.setVisibility(visible);
        myMenu.findItem(R.id.btnRefresh).setEnabled(setEnabled);
    }

    @Override
    protected void onStop() {
        super.onStop();

        //Убираем слушателя изменений состояния блютуз
        unregisterReceiver(btStateChangeBR);
        unregisterReceiver(discoveryBR);

        Log.d(TAG, "MainActivity BoadcastReceiver деактивирован");
        Log.d(TAG, "MainActivity остановлено");

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        //Регистрируем слушателя изменений состояния блютуз
        registerReceiver(btStateChangeBR, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        //Регистррируем слушателя уведомляющих о этапах поиска устройств и найденных устройствах
        registerReceiver(discoveryBR, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(discoveryBR, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(discoveryBR, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));


        Log.d(TAG, "MainActivity возобновлено");
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //Убираем слушателя изменений состояния блютуз
//        unregisterReceiver(btStateChangeBR);
//        unregisterReceiver(discoveryBR);

        Log.d(TAG, "MainActivity уничтожено");
    }


}
