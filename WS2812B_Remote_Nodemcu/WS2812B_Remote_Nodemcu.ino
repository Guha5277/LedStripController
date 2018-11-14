#define FASTLED_ESP8266_RAW_PIN_ORDER
#define FASTLED_ALLOW_INTERRUPTS 0
#include "FastLED.h"          // библиотека для работы с лентой
#include <EEPROM.h>           // библиотека для работы с ПЗУ

#define ARRAY_SIZE 10       //Размер массива
#define LED_COUNT 27          // число светодиодов в кольце/ленте
#define LED_DT D7             // пин, куда подключен DIN ленты
#define COLOR_ORDER RGB

byte command[ARRAY_SIZE];  //Массив содержащий данные от клиента
byte indx = 0;             //Индекс массива

byte isStripOn = 1;
byte isStripPaused = 0;
byte ledMode = 1;
byte ledModel = 1;
byte startMode = 0;
boolean haveSpeed = false;
byte ledModes[49] = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
int max_bright = 5;
boolean auto_mode = 0;
int thisdelay = 0;

byte r, g, b = 0;

long last_change = 0;
long change_time = 20000;

//Переменные для хранения значения времени, необходимые для подтверждения подключенного состояния
long last_change2 = 0;  //Временная переменная, в которой будут храниться время с последней отправки сообщения клиенту
long change_time2 = 4000;  //Периодичность отправки сообщения клиенту (мс)

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

//    EEPROM.begin(64);
//    
//      EEPROM.write(0, 100);
//      EEPROM.write(1, 3);
//      EEPROM.write(2, 1);
//  
//       int index = 0;
//       for(int z = 3; z < 52; z++){
//       EEPROM.write(z, ledModes[index]);
//       index++;
//     }
//
//    EEPROM.commit(); 
//    EEPROM.end();

}

void loop() {
  serialEventRead();
  
  if (command[0]) {
    readCommand(command[0]);
  }

  if (auto_mode) {
    if (millis() - last_change > change_time) {
      //change_time = (20000);
      nextMode();
      last_change = millis();
      sendResponce(4);
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
void serialEventRead() {
  while (Serial.available() > 0) {
    delay(1);
    Serial.readBytes(command, Serial.available());
    indx++;
  }
}



/****Загрузка настроек из ПЗУ*****/
void loadSettings() {
  EEPROM.begin(64);
  
  max_bright = EEPROM.read(0);
  ledMode = EEPROM.read(1);
  auto_mode = EEPROM.read(2);
  int index = 0;
  for (int z = 3; z < 52; z++) {
    ledModes[index] = EEPROM.read(z);
    index++;
  }

    EEPROM.end();
}

/****Сохранение настроек в ПЗУ *****/
void saveSettingsToMem () {
  byte b = 3;
  EEPROM.begin(64);

  EEPROM.write(0, max_bright);
  if (startMode != 0) {
    EEPROM.write(1, startMode);
  }
  EEPROM.write(2, auto_mode);

  for (byte i = 0; i < 52; i++) {
    EEPROM.write(b, ledModes[i]);
    b++;
  }
      EEPROM.commit();
      EEPROM.end();
  
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
    //Отправка начальных значений (текущих настроек) клиенту
    case 2: onOff(); break; //Включение - выключение
    case 3: prevMode();  break; //Предыдущий режим
    case 4: nextMode(); break; // Пауза - воспроизведение
    case 5: pausePlay(); break; //Следующий режим
    case 6: favMode(); break;//Режим по умолчанию (при включении ленты)
    case 7: actDeactMode();  break; //ВКЛ - ВЫКЛ режимов из плейлиста
    case 8: autoMode(); break; //ВКЛ - ВЫКЛ авторежима
    case 9: setColor(); break; //
    case 10: setBright(); break;//setBright
    case 11: set_Speed(); break;
    case 12: saveSettingsToMem(); break; //SaveModesToMem
    case 13: setModeTo(); break;
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
    case 4: sendCurrentMode(); break;
    case 5: Serial.write(isStripPaused); break;
    case 6: Serial.write(startMode); break;
    case 7: Serial.write(command[1]); Serial.write(ledModes[command[1]]); break;
    case 8: Serial.write(auto_mode); break;
    case 9: Serial.write(r); Serial.write(g); Serial.write(b); break;
    case 10: Serial.write(max_bright); break;
    case 11: Serial.write(thisdelay); break;
    case 12: Serial.write(1); break;
    case 13: Serial.write(ledMode); break;

  }
}

/****Отправка настроек клиенту*****/
void sendSettings() {
  Serial.write(ledMode);
  Serial.write(max_bright);
  Serial.write(auto_mode);
  Serial.write(thisdelay);
  Serial.write(ledModes, 49);
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
      change_mode(ledModel);
    }
    isStripOn = 1;
  }

}

/*****Предыдущий режим *****/
void prevMode() {      // Включить предыдущий режим
  if (ledMode < 2) {
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
  // sendCurrentMode();
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

  ledMode = (ledMode % 49) + 1;
  while (!ledModes[ledMode - 1]) {
    ledMode = (ledMode % 49) + 1;
  }

  change_mode(ledMode);
  //  sendCurrentMode();
}

/*****Стартовый режим *****/
void favMode() {
    EEPROM.begin(64);
  
  startMode = command[1]; //в command[1] - номер режима
  EEPROM.write(1, startMode);

    EEPROM.commit();
    EEPROM.end();
}

/*****Включение-выключение режимов *****/
void actDeactMode() {
  ledModes[command[1]] = command[2];  //в command[1] приходит номер режима, в command[2] - включение или выключение(0 или 1);

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

void setModeTo(){
  if (command[1] > 0 && command[1] <= 49){
    ledMode = command[1];
    change_mode(ledMode);
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
