# LedStripController
Адресные светодиодные ленты на контроллере WS2812 не зря так полюбились радиолюбителям. 
Доступная цена и простота в использовании сделали своё дело. Не одна сотня проектов реализована
на базе данной модели светодиодов - от простых гирлянд и Ambilight-подсветки монитора, до поворотников в автомобилях. 
Я, как и другие разработчики, разделяю любовь к этим светодиодам и этим проектом, хотел бы внести свой вклад в труды "Ардуинщиков" и радиолюбителей.

## Проект Led Strip Controller
Основной целью проекта является предоставление интерфейса для простого и быстрого управления адресной лентой WS2812B.

### Задачи проекта:
* Написание серверной части (для Atmega и Nodemcu).
* Написание кросплатформенной клиентской части (ОС Windows, Android).
* Обеспечение совместимости клиент-серверной составляющей в любой доступной конфигурации.
* Реализация беспроводного соединения посредством Bluetooth 4.0.

За основу сервеной части был взят проект Alex Gyver'a - [WS2812_FX](https://github.com/AlexGyver/WS2812_FX), среда разработки - Arduino IDE.
Основа клиентской части для ПК, устройств на базе ОС Windows, была написана на Java версии 8.0 среда разработки - Intellij IDEA.

## Что уже реализовано
### Серверная часть (Alpha Version)
* Алгоритм обмена сообщениями.
* Возможность исключать/добавлять режимы в общий список.
* Возможность сохранения и считывания (при запуске, либо, в случае внештатной ситуации) настроек в/из EEPROM - стартовый (избранный) режим, яркость, актуальный список режимов
* Возможность изменения скорости и яркости текущего режима.
* Возможность поставить текущий режим на паузу.
* Возможность установки произвольного цвета (RGB).

### Клиентская часть Windows (Pre Alpha Version)
Протестировано на OS Windows 7 и Windows 10.
![alt text](https://github.com/Guha5277/LedStripController/blob/master/win7.png)
      
* Написанный с нуля клиент.
* Довольно архаичный (по современным меркам) графический интерфейс (в виду использования устаревшего графического ядра для Java - Swing).
* Алгоритм обмена сообщениями с сервером.
* Управление лентой - включение/выключение, переключение режимов, редактирование текущего списка, управление яркостью и скоростью и т.д.


### Клиентская часть Android (В стадии разработки)
Протестировано на Android 6.0 и Android 8.0
* API Level 21.
* Реализован вывод списка сопряженных и поиск доступных Bluetooth-устройств.
* Реализовано подключение к Bluetooth-устройству и инициализация первичными данными.
* Реализован обмен сообщениями.

### Планы
* Завершить логику клиентской части Android-приложения.
* Завершить дизайн графической части Android-приложения.
* Провести UNIT-тестирование.
* Опублиговать готовые приложения в тематических группах, собрать баг-репорты.

## Авторы
* **[Пригода Алексей](https://vk.com/guhasan)** - *Разработчик*
* **[Дмитрий Редькин](https://vk.com/dmitrij_redkin)** - *Code Reviewer*

## Лицензия
Вы можете свободно использовать любую часть кода, готовые приложения, в своих проектах.
