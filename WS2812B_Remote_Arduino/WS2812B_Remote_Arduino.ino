#include "FastLED.h"          // библиотека для работы с лентой
#include <EEPROM.h>           // библиотека для работы с ПЗУ

#define ARRAY_SIZE 10       //Размер массива
#define LED_COUNT 27          // число светодиодов в кольце/ленте
#define LED_DT 13             // пин, куда подключен DIN ленты


byte command[ARRAY_SIZE];  //Массив содержащий данные от клиента
byte indx = 0;             //Индекс массива

byte isStripOn = 1;
byte isStripPaused = 0;
byte ledMode = 1;
byte ledModel = 1;
byte startMode = 0;
boolean haveSpeed = false;
byte ledModes[49] = {1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1};
byte autoModeCountrer[49] = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
int max_bright = 5;
int thisdelay = 0;

boolean isRandomEnabled = false;
byte randomCounter = 0;
boolean isAutoSaveEnabled = false;
boolean isNeedToSaveData = false;

boolean auto_mode = false;
boolean temp_auto_mode = false;
byte auto_mode_duration = 10;
byte auto_save_duration = 5;

long last_change_autosave = 0;

byte r, g, b = 0;

long last_change = 0;
long change_time = 20000;

long last_change2 = 0;
long change_time2 = 4000;

// цвета мячиков для режима
byte ballColors[3][3] = {
  {0xff, 0, 0},
  {0xff, 0xff, 0xff},
  {0   , 0   , 0xff}
};

// ---------------СЛУЖЕБНЫЕ ПЕРЕМЕННЫЕ-----------------
int BOTTOM_INDEX = 0;        // светодиод начала отсчёта
int TOP_INDEX = int(LED_COUNT / 2);
int EVENODD = LED_COUNT % 2;
struct CRGB leds[LED_COUNT];
int ledsX[LED_COUNT][3];     //-ARRAY FOR COPYING WHATS IN THE LED STRIP CURRENTLY (FOR CELL-AUTOMATA, MARCH, ETC)

//int thisdelay = 20;          //-FX LOOPS DELAY VAR
int thisstep = 10;           //-FX LOOPS DELAY VAR
int thishue = 0;             //-FX LOOPS DELAY VAR
int thissat = 255;           //-FX LOOPS DELAY VAR

int thisindex = 0;
int thisRED = 0;
int thisGRN = 0;
int thisBLU = 0;

int idex = 0;                //-LED INDEX (0 to LED_COUNT-1
int ihue = 0;                //-HUE (0-255)
int ibright = 0;             //-BRIGHTNESS (0-255)
int isat = 0;                //-SATURATION (0-255)
int bouncedirection = 0;     //-SWITCH FOR COLOR BOUNCE (0-1)
float tcount = 0.0;          //-INC VAR FOR SIN LOOPS
int lcount = 0;              //-ANOTHER COUNTING VAR
// ---------------СЛУЖЕБНЫЕ ПЕРЕМЕННЫЕ-----------------

void setup() {
  Serial.begin(115200);
  loadSettings();

  LEDS.setBrightness(max_bright);  // ограничить максимальную яркость
  LEDS.addLeds<WS2812B, LED_DT, GRB>(leds, LED_COUNT);  // настрйоки для нашей ленты (ленты на WS2811, WS2812, WS2812B)
  one_color_all(0, 0, 0);          // погасить все светодиоды
  LEDS.show();                     // отослать команду

  change_mode(ledMode);


          //Яркость
  //      EEPROM.update(0, 100);
  
          //Стартовый режим
  //      EEPROM.update(1, 3);
  
          //Авторежим
  //      EEPROM.update(2, 1);
  //
  //     int index = 0;
  //     for(int z = 3; z < 52; z++){
  //     EEPROM.update(z, ledModes[index]);
  //     index++;
  //   }
  
     //Продолжительность воспроизведения одного эффекта в авторежиме (0-255 секунд)
   // EEPROM.update(52, 10);
   
      //Случайное переключение режимов (1 - вкл, 0 - выкл)
  //  EEPROM.update(53, 0);
  
      //Автосохранение (1 - вкл, 0 - выкл)
  //  EEPROM.update(54, 0);
  
      //Периодичность автосохранения (0 - 255 секунд).
  //  EEPROM.update(55, 5);

}

void loop() {
//  if (command[0]) {
//    readCommand(command[0]);
//  }

 //Если включен авторежим
  if (auto_mode) {
    //Проверяем, истекло ли время таймера
    if (millis() - last_change > auto_mode_duration * 1000) {

      //Если включен режим случайной смены эффектов
      if (isRandomEnabled) {
        changeModeRandom();
      }

      //Иначе - просто включаем следующий режим
      else {
        nextMode();
      }

      last_change = millis();
      sendResponce(4);
    }
  }

  //Если включен режим автосохранения и есть необходимость сохранить настройки
  if (isAutoSaveEnabled && isNeedToSaveData) {

    //Если таймер автосохранения истек
    if (millis() - last_change_autosave > auto_save_duration * 1000) {
      //Сбрасываем флаг необходимости сохранения настрок
      isNeedToSaveData = false;

      //Сохраняем настройки
      saveSettingsToMem();

      //Отправляем уведомление клиенту
      sendResponce(12);

      //Обновляем переменную времени текущее время
      last_change_autosave = millis();
    }
  }

  //  if (millis() - last_change2 > change_time2){
  //    last_change2 = millis();
  //    Serial.write(100);
  //  }


  switch (ledMode) {
    case 99: break;                           // пазуа
    case  1: rainbow_fade(); break;            // плавная смена цветов всей ленты
    case  2: rainbow_loop(); break;            // крутящаяся радуга
    case  3: random_burst(); break;            // случайная смена цветов
    case  4: color_bounce(); break;            // бегающий светодиод
    case  5: color_bounceFADE(); break;        // бегающий паровозик светодиодов
    case  6: ems_lightsONE(); break;           // вращаются красный и синий
    case  7: ems_lightsALL(); break;           // вращается половина красных и половина синих
    case  8: flicker(); break;                 // случайный стробоскоп
    case 9: pulse_one_color_all(); break;     // пульсация одним цветом
    case 10: pulse_one_color_all_rev(); break; // пульсация со сменой цветов
    case 11: fade_vertical(); break;           // плавная смена яркости по вертикали (для кольца)
    case 12: rule30(); break;                  // безумие красных светодиодов
    case 13: random_march(); break;            // безумие случайных цветов
    case 14: rwb_march(); break;               // белый синий красный бегут по кругу (ПАТРИОТИЗМ!)
    case 15: radiation(); break;               // пульсирует значок радиации
    case 16: color_loop_vardelay(); break;     // красный светодиод бегает по кругу
    case 17: white_temps(); break;             // бело синий градиент (?)
    case 18: sin_bright_wave(); break;         // тоже хрень какая то
    case 19: pop_horizontal(); break;          // красные вспышки спускаются вниз
    case 20: quad_bright_curve(); break;       // полумесяц
    case 21: flame(); break;                   // эффект пламени
    case 22: rainbow_vertical(); break;        // радуга в вертикаьной плоскости (кольцо)
    case 23: pacman(); break;                  // пакман
    case 24: random_color_pop(); break;        // безумие случайных вспышек
    case 25: ems_lightsSTROBE(); break;        // полицейская мигалка
    case 26: rgb_propeller(); break;           // RGB пропеллер
    case 27: kitt(); break;                    // случайные вспышки красного в вертикаьной плоскости
    case 28: matrix(); break;                  // зелёненькие бегают по кругу случайно
    case 29: new_rainbow_loop(); break;        // крутая плавная вращающаяся радуга
    case 30: colorWipe(0x00, 0xff, 0x00, thisdelay);
      colorWipe(0x00, 0x00, 0x00, thisdelay); break;                                // плавное заполнение цветом
    case 31: CylonBounce(0xff, 0, 0, 4, 10, thisdelay); break;                      // бегающие светодиоды
    case 32: Fire(55, 120, thisdelay); break;                                      // линейный огонь
    case 33: rainbowCycle(thisdelay); break;                                        // очень плавная вращающаяся радуга
    case 34: TwinkleRandom(20, thisdelay, 1); break;                                // случайные разноцветные включения (1 - танцуют все, 0 - случайный 1 диод)
    case 35: RunningLights(0xff, 0xff, 0x00, thisdelay); break;                     // бегущие огни
    case 36: Sparkle(0xff, 0xff, 0xff, thisdelay); break;                           // случайные вспышки белого цвета
    case 37: SnowSparkle(0x10, 0x10, 0x10, thisdelay, random(100, 1000)); break;    // случайные вспышки белого цвета на белом фоне
    case 38: theaterChase(0xff, 0, 0, thisdelay); break;                            // бегущие каждые 3 (ЧИСЛО СВЕТОДИОДОВ ДОЛЖНО БЫТЬ КРАТНО 3)
    case 39: theaterChaseRainbow(thisdelay); break;                                 // бегущие каждые 3 радуга (ЧИСЛО СВЕТОДИОДОВ ДОЛЖНО БЫТЬ КРАТНО 3)
    case 40: Strobe(0xff, 0xff, 0xff, 10, thisdelay, 1000); break;                  // стробоскоп
    case 41: BouncingBalls(0xff, 0, 0, 3); break;                                   // прыгающие мячики
    case 42: BouncingColoredBalls(3, ballColors); break;                            // прыгающие мячики цветные

  }
}



/****Считывание данных из COM-порта*****/
void serialEvent() {
  while (Serial.available() > 0) {
    delay(1);
    Serial.readBytes(command, Serial.available());
    indx++;
  }
}



/****Загрузка настроек из ПЗУ*****/
void loadSettings() {
  max_bright = EEPROM.read(0);
  ledMode = EEPROM.read(1);
  auto_mode = EEPROM.read(2);
  int index = 0;
  for (int z = 3; z < 52; z++) {
    ledModes[index] = EEPROM.read(z);
    index++;
  }

  auto_mode_duration = EEPROM.read(52);
  isRandomEnabled = EEPROM.read(53);
  isAutoSaveEnabled = EEPROM.read(54);
  auto_save_duration = EEPROM.read(55);
}

/****Сохранение настроек в ПЗУ *****/
void saveSettingsToMem () {
  byte b = 3;

  EEPROM.update(0, max_bright);
  if (startMode != 0) {
    EEPROM.update(1, startMode);
  }
  EEPROM.update(2, auto_mode);

  for (byte i = 0; i < 52; i++) {
    EEPROM.update(b, ledModes[i]);
    b++;
  }
    
  EEPROM.update(52, auto_mode_duration);
  EEPROM.update(53, isRandomEnabled);
  EEPROM.update(54, isAutoSaveEnabled);
  EEPROM.update(55, auto_save_duration);
  
}

/****Очистка данных*****/
void clear_data() {
  for (int i = 0; i < ARRAY_SIZE; i++) {
    command[indx] = 0;

    indx = 0;
  }
}

/****Выполнение принятых комманд*****/
void readCommand(byte a) {
  switch (a) {
    case 2: onOff(); break;               //Включение - выключение
    case 3: prevMode();  break;           //Предыдущий режим
    case 4: nextMode(); break;            //Следующий режим
    case 5: pausePlay(); break;           // Пауза - воспроизведение
    case 6: favMode(); break;             //Режим по умолчанию (при включении ленты)
    case 7: actDeactMode(); isNeedToSaveData = true; break;        //ВКЛ - ВЫКЛ режимов из плейлиста
    case 8: autoMode(); isNeedToSaveData = true; break;            //ВКЛ - ВЫКЛ авторежима
    case 9: setColor(); break;                                     //Установка цвета
    case 10: setBright(); isNeedToSaveData = true; break;          //Установка яркости
    case 11: set_Speed(); break;                                   //Установка скорости
    case 12: saveSettingsToMem(); break;            //Сохранение настроек в ПЗУ
    case 13: setModeTo(); break;                    //Включить режим №...
    case 14: autoSave(); isNeedToSaveData = true; break;           //Автосохранение
    case 15: autoSaveDuration(); isNeedToSaveData = true; break;   //Периодичность автосохранения
    case 16: autoModeDuration(); isNeedToSaveData = true; break;   //Периодичность смены режимов в авторежиме
    case 17: setRandom(command[1]); isNeedToSaveData = true; break;   //Случайная смена режимов
  }
  sendResponce(a);
  clear_data();
}

void sendResponce (byte a) {
  Serial.write (a);
  switch (a) {
    case 1: sendSettings(); break;
    case 2: Serial.write(isStripOn); break;
    case 3:
    case 4:
    case 13: sendCurrentMode(); break;
    case 5: Serial.write(isStripPaused); break;
    case 6: Serial.write(startMode); break;
    case 7: Serial.write(command[1]); Serial.write(ledModes[command[1]]); break;
    case 8: Serial.write(auto_mode); break;
    case 9: Serial.write(r); Serial.write(g); Serial.write(b); break;
    case 10: Serial.write(max_bright); break;
    case 11: Serial.write(thisdelay); break;
    case 12: Serial.write(1); break;
    // case 13: Serial.write(ledMode); break;
    case 14: Serial.write(isAutoSaveEnabled); break;
    case 15: Serial.write(auto_save_duration); break;
    case 16: Serial.write(auto_mode_duration); break;
  }
}

/****Отправка настроек клиенту*****/
void sendSettings() {
  Serial.write(ledMode);
  Serial.write(max_bright);
  Serial.write(auto_mode);
  Serial.write(thisdelay);
  Serial.write(ledModes, 49);
  Serial.write(auto_mode_duration);
  Serial.write(isRandomEnabled);
  Serial.write(isAutoSaveEnabled);
  Serial.write(auto_save_duration);
}



void sendCurrentMode() {
  Serial.write(ledMode);
  if (haveSpeed) {
    Serial.write(thisdelay);
  }
  else {
    Serial.write(0);
  }
}


/*****Включение и выключение ленты*****/
void onOff() {
  if (ledMode > 0) {
    temp_auto_mode = auto_mode;
    auto_mode = false;

    ledModel = ledMode;
    change_mode(0);
    isStripOn = 0;
  }
  else {
    if (ledModel == 50) {
      one_color_all(r, g, b);
      LEDS.show();
    }
    else {
      auto_mode = temp_auto_mode;
      change_mode(ledModel);
    }
    isStripOn = 1;
  }

}

/*****Предыдущий режим *****/
void prevMode() {      // Включить предыдущий режим
  if (isRandomEnabled) {
    changeModeRandom();
  }
  else {
    if (ledMode < 2 || ledMode == 99) {
      ledMode = 49;
    }
    else {
      ledMode--;
    }
    while (!ledModes[ledMode - 1]) {
      ledMode--;
      if (ledMode == 0) {
        ledMode = 49;
      }
    }

    change_mode(ledMode);
  }
}

/*****Пауза *****/
void pausePlay() {
  if (ledMode != 99) {
    ledModel = ledMode;
    change_mode(99);
    isStripPaused = 1;
  }
  else {
    ledMode = ledModel;
    isStripPaused = 0;
  }
}

/*****Следующий режим *****/
void nextMode() {

  if (isRandomEnabled) {
    changeModeRandom();
  }
  else {
    ledMode = (ledMode % 49) + 1;
    while (!ledModes[ledMode - 1]) {
      ledMode = (ledMode % 49) + 1;
    }

    change_mode(ledMode);

  }
}

/*****Стартовый режим *****/
void favMode() {
  startMode = command[1]; //в data1 - номер режима
  EEPROM.update(1, startMode);
}

/*****Включение-выключение режимов *****/
void actDeactMode() {
  ledModes[command[1]] = command[2];  //в command[1] приходит номер режима, в command[2] - включение или выключение(0 или 1);

  if (isRandomEnabled) {
    if (ledModes[command[1]]) {
      randomCounter++;
      autoModeCountrer[command[1]] = 1;
    }
    else {
      randomCounter--;
      autoModeCountrer[command[1]] = 0;
    }
  }
}

/*****Авторежим *****/
void autoMode() {

  auto_mode = command[1];

}

/*****Установка цвета *****/
void setColor() {
  //byte r, g, b;
  r = command[1];
  g = command[2];
  b = command[3];

  one_color_all(r, g, b);
  LEDS.show();
  ledMode = 50;

}

/*****Установка яркости *****/
void setBright() {
  max_bright = command[1];  //в command[1] величина яркости (1 - 255)
  LEDS.setBrightness(max_bright);
  LEDS.show();                     // отослать команду

  if (ledMode > 42 && ledMode < 99) {
    switch (ledMode) {
      case 43: one_color_all(255, 0, 0); LEDS.show();  break; //---ALL RED
      case 44: one_color_all(0, 255, 0); LEDS.show(); break; //---ALL GREEN
      case 45: one_color_all(0, 0, 255); LEDS.show(); break; //---ALL BLUE
      case 46: one_color_all(255, 255, 0); LEDS.show(); break; //---ALL COLOR X
      case 47: one_color_all(0, 255, 255); LEDS.show(); break; //---ALL COLOR Y
      case 48: one_color_all(255, 0, 255); LEDS.show(); break; //---ALL COLOR Z
      case 49: one_color_all(255, 255, 255); LEDS.show(); break; //---ALL ON
      case 50: one_color_all(r, g, b); LEDS.show(); break;
    }
  }
}

/*****Установка скорости *****/
void set_Speed() {
  if (haveSpeed) {
    thisdelay = command[1];
  }
}

//Включить конкретный режи
void setModeTo() {
  if (command[1] > 0 && command[1] <= 49) {
    ledMode = command[1];
    change_mode(ledMode);
  }
}

//Случайное переключение режима
void changeModeRandom () {
  
  //Если не осталось режимов для воспроизведения
  if (!randomCounter) {
    for (int i = 0; i < 49; i++) {
      //Получаем количество доступных режимов
      if (ledModes[i]) {
        randomCounter++;
      }
      //Копируем массив с текущим состоянием режимов
      autoModeCountrer[i] = ledModes[i];
    }    
  }

  //Помечаем текущий режим как "пройденный"
  autoModeCountrer[ledMode-1] = 0;

  //Генерируем новый режим
  ledMode = random(1, 50);

  //Генерируем режимы до тех пор, пока не будут выполнены два условия:
  //1 - сгенерированный режим активирован в общем плейлисте
  //2 - сгенерированный режим не был помечен как пройденный (чтобы исключить повторения режимов)
  while (!ledModes[ledMode - 1] || !autoModeCountrer[ledMode - 1]) {
    ledMode = random(1, 50);
  }

  //Уменьшаем счетчик случайных режимов и меняем режим
  randomCounter--;
  change_mode(ledMode);
}

//Автосохранение
void autoSave() {
  isAutoSaveEnabled = command[1];
}

//Периодичность автосохранения
void autoSaveDuration() {
  auto_save_duration = command[1];
}

//Периодичность переключения эффектов в авторежиме
void autoModeDuration() {
  auto_mode_duration = command[1];
}

//Случайная смена эффектов
void setRandom (byte result) {
  isRandomEnabled = result;

  //Если случайный режим был включен
  if (isRandomEnabled) {
    //Обнуляем счётчик случайного режима
    randomCounter = 0;
    
    for (int i = 0; i < 49; i++) {
      //Получаем количество доступных режимов (заносим в счётчик случайного режима)
      if (ledModes[i]) {
        randomCounter++;
      }
      
      //Копируем массив с текущим состоянием режимов
      autoModeCountrer[i] = ledModes[i];
    }

  }
}

void change_mode(int newmode) {
  thissat = 255;
  switch (newmode) {
    case 0: one_color_all(0, 0, 0); haveSpeed = 0; LEDS.show(); break; //---ALL OFF
    case 1: thisdelay = 60; haveSpeed = 1; break;                      //---STRIP RAINBOW FADE
    case 2: thisdelay = 20; haveSpeed = 1; thisstep = 10; break;       //---RAINBOW LOOP
    case 3: thisdelay = 20; haveSpeed = 1; break;                      //---RANDOM BURST
    case 4: thisdelay = 20; haveSpeed = 1; thishue = 0; break;         //---CYLON v1
    case 5: thisdelay = 80; haveSpeed = 1;  thishue = 0; break;         //---CYLON v2
    case 6: thisdelay = 40; haveSpeed = 1; thishue = 0; break;         //---POLICE LIGHTS SINGLE
    case 7: thisdelay = 40; haveSpeed = 1; thishue = 0; break;         //---POLICE LIGHTS SOLID
    case 8: thishue = 160; thissat = 50; break;    haveSpeed = 0;      //---STRIP FLICKER
    case 9: thisdelay = 15; haveSpeed = 1; thishue = 0; break;        //---PULSE COLOR BRIGHTNESS
    case 10: thisdelay = 30; haveSpeed = 1; thishue = 0; break;        //---PULSE COLOR SATURATION
    case 11: thisdelay = 60; haveSpeed = 1; thishue = 180; break;      //---VERTICAL SOMETHING
    case 12: thisdelay = 100; haveSpeed = 1;  break;                    //---CELL AUTO - RULE 30 (RED)
    case 13: thisdelay = 80; haveSpeed = 1; break;                     //---MARCH RANDOM COLORS
    case 14: thisdelay = 80; haveSpeed = 1;  break;                     //---MARCH RWB COLORS
    case 15: thisdelay = 60; haveSpeed = 1; thishue = 95; break;       //---RADIATION SYMBOL
    //---PLACEHOLDER FOR COLOR LOOP VAR DELAY VARS
    case 18: thisdelay = 35; haveSpeed = 1; thishue = 180; break;      //---SIN WAVE BRIGHTNESS
    case 19: thisdelay = 100; haveSpeed = 1; thishue = 0; break;       //---POP LEFT/RIGHT
    case 20: thisdelay = 100; haveSpeed = 1; thishue = 180; break;     //---QUADRATIC BRIGHTNESS CURVE
    //---PLACEHOLDER FOR FLAME VARS
    case 22: thisdelay = 50; haveSpeed = 1; thisstep = 15; break;      //---VERITCAL RAINBOW
    case 23: thisdelay = 50; haveSpeed = 1; break;                     //---PACMAN
    case 24: thisdelay = 35; haveSpeed = 1; break;                     //---RANDOM COLOR POP
    case 25: thisdelay = 25; haveSpeed = 1; thishue = 0; break;        //---EMERGECNY STROBE
    case 26: thisdelay = 100; haveSpeed = 1;  thishue = 0; break;        //---RGB PROPELLER
    case 27: thisdelay = 100; haveSpeed = 1; thishue = 0; break;       //---KITT
    case 28: thisdelay = 100; haveSpeed = 1; thishue = 95; break;       //---MATRIX RAIN
    case 29: thisdelay = 15; haveSpeed = 1; break;                      //---NEW RAINBOW LOOP
    case 30: thisdelay = 50; haveSpeed = 1; break;                     // colorWipe
    case 31: thisdelay = 50; haveSpeed = 1;  break;                     // CylonBounce
    case 32: thisdelay = 15; haveSpeed = 1; break;                     // Fire
    case 33: thisdelay = 20; haveSpeed = 1; break;                     // rainbowCycle
    case 34: thisdelay = 10; haveSpeed = 1; break;                     // rainbowTwinkle
    case 35: thisdelay = 50; haveSpeed = 1; break;                     // RunningLights
    case 36: thisdelay = 0; haveSpeed = 0; break;                      // Sparkle
    case 37: thisdelay = 30; haveSpeed = 1; break;                     // SnowSparkle
    case 38: thisdelay = 50; haveSpeed = 1; break;                     // theaterChase
    case 39: thisdelay = 50; haveSpeed = 1; break;                     // theaterChaseRainbow
    case 40: thisdelay = 100; haveSpeed = 1; break;                    // Strobe
    case 43: one_color_all(255, 0, 0); LEDS.show(); haveSpeed = 0; break; //---ALL RED
    case 44: one_color_all(0, 255, 0); LEDS.show(); haveSpeed = 0; break; //---ALL GREEN
    case 45: one_color_all(0, 0, 255); LEDS.show(); haveSpeed = 0; break; //---ALL BLUE
    case 46: one_color_all(255, 255, 0); LEDS.show(); haveSpeed = 0; break; //---ALL COLOR X
    case 47: one_color_all(0, 255, 255); LEDS.show(); haveSpeed = 0; break; //---ALL COLOR Y
    case 48: one_color_all(255, 0, 255); LEDS.show(); haveSpeed = 0; break; //---ALL COLOR Z
    case 49: one_color_all(255, 255, 255); LEDS.show(); haveSpeed = 0; break; //---ALL ON
    default: haveSpeed = 0; break;
  }
  bouncedirection = 0;
  one_color_all(0, 0, 0);
  ledMode = newmode;
}
