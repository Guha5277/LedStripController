package com.guhafun;

import jssc.SerialPortList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutionException;

//import static com.guhafun.WS2812B.modeNames;

//import static com.guhafun.WS2812B.*;

/*TODO
* 1. Наследование класса от JFrame
* 2. Вынесение в отдельный класс данных, не относящихся к ГИ (константы, названия режимов и т.д.)
* 3. Рефакторинг кода, перенос на новый тип взаимодействия ГИ и класса работающего с портом (реалзиация интерфейса)
* 4. Добавление и удаление слушателей ГИ по необходимости (выпадающий список COM-портов) */
class MainFrame extends JFrame implements SerialPortListener {
    private final SerialPortController serialPortController;
    private final CombComMouseListener comMouseListener;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame("WS2812B Controller"));
    }

    private boolean readyActDeactMode = false;  //Флаг для готовности изменения списка режимов
    private boolean readyToSaveSet = false;     //Флаг наличия изменений, для сохрания
    private int currentLedMode = 0;             //Номер текущего режима
    private int[] ledModes = new int[49];       //Список активных режимов
    private int maxBright = 0;                  //Максимальная яркость
    private int autoMode = 0;                   //Состояние авторежима (0 - выключен, 1 - включен)
    private int thisdelay = 0;                  //Значение задержки между итерациями эффектов (увеличивая это значение визуально изменить скорость воспроизведения, при уменьшении - наоборот)
    private int modesCount = WS2812B.modeNames.length; //общее количество режимов

    //Элементы графического интерфейса
    private JComboBox<String> combCom;       //Выпадающий список доступных COM-портов
    private JComboBox<Integer> combBaud;     //Выпадающий список доступной скорости подключения
    private JButton btConnect;               //Кнопка "Поключить"
    private JButton btDisconnect;            //Кнопка "Отключить"
    private JToggleButton btOnOff;           //Кнопка ВКЛ/ВЫКЛ ленты
    private JLabel lblCurModeName;           //Тектовый лейбл с именем текущего режима
    private JLabel lblCurNumOfModes;         //Текстовый лейбл с порядковым номером текущего режима
    private JButton btPrev;                  //Кнопка "Предыдущий" режим
    private JButton btPause;                 //Кнопка "Пауза"
    private JButton btNext;                  //Кнопка "Следующий" режим
    private JButton btAddToFav;              //Кнопка "Favorite" - для добавления режима в роли стартового
    private JButton btSaveSettings;          //Кнопка "Сохранить" - для сохранения настроек в EEPROM (ПЗУ) арудино
    private JCheckBox[] chkModesList;        //Список режимов
    private Box box;                         //Менеджер расположения графических элементов
    private JScrollPane scrollPane;          //Панель прокрутки
    private JCheckBox chkAutoMode;           //Чекбокс для ВКЛ/ВЫКЛ автоматической смены режима
    private JButton btColorChooser;          //Кнопка для вывода произвольного цвета на экран
    private JLabel lblBright;                //Лейбл "Яркость"
    private JLabel lblSpeed;                 //Лейбл "Скорость"
    private JSlider brightness;              //Слайдер "Яркость"
    private JSlider speed;                   //Слайдер "Скорость"
    private JTextArea jtxStatus;             //Текстовое поле с системной информацией
    private JColorChooser colorChooser;      //Сдандарная панель выбора цвета
//    AbstractColorChooserPanel colorPanels[] = colorChooser.getChooserPanels();

    MainFrame(String frameTitle) {
        super(frameTitle);
        serialPortController = new SerialPortController(this);
        comMouseListener = new CombComMouseListener();
        createUI();
        setupUI();
    }

    private void createUI() {
        chkModesList = new JCheckBox[WS2812B.modeNames.length];
        combBaud = new JComboBox<>(serialPortController.getBaudRateList());
        btConnect = new JButton("Подключиться");
        btDisconnect = new JButton("Отключиться");
        btOnOff = new JToggleButton();
        lblCurModeName = new JLabel(WS2812B.modeNames[0]);
        lblCurNumOfModes = new JLabel("1/" + modesCount);
        btPrev = new JButton();
        btPause = new JButton();
        btNext = new JButton();
        btAddToFav = new JButton();
        btSaveSettings = new JButton();
        box = Box.createVerticalBox();
        scrollPane = new JScrollPane(box);
        chkAutoMode = new JCheckBox("Авто");
        btColorChooser = new JButton("Цвет");
        lblBright = new JLabel("Яркость");
        lblSpeed = new JLabel("Скорость");
        brightness = new JSlider(JSlider.HORIZONTAL, 1, 255, 255);
        speed = new JSlider(JSlider.HORIZONTAL, 0, 15, 5);
        jtxStatus = new JTextArea();
        colorChooser = new JColorChooser();
    }

    private void setupUI() {
        //Получаем список портов, добавляем его в выпадающий список
        String[] comPorts = SerialPortList.getPortNames();
        combCom = new JComboBox<>(comPorts);
        combCom.setSelectedIndex(comPorts.length - 1);
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        //ВЕРХНАЯЯ ПАНЕЛЬ ПОДКЛЮЧЕНИЯ
        //Создаём верхнюю панель (для элементов подключения-отключения), задаём панели новый менеджер компоновки элементов
        JPanel connectPan = new JPanel();
        connectPan.setLayout(new GridBagLayout());

        /*Настраиваем стили для наших элементов. Параметры, проинимаемые конструктором GridBagConstraints:
         *gridx - номер ячейки расположения компонента по оси X (отсчёт идёт с левого верхнего угла, RELATIVE - означает положение предыдущего + 1);
         *gridy - номер ячейки расположения компонента по оси Y
         * gridwidth - количество ячеек, занимаемых элементом по вертикали
         * gridheidth - количество ячеек, занимаемых элементов по горизонтали
         * weightx, weighty - "вес" элемента - чем больше число(от 0.0 до 1.0), тем больше свободного места займёт элемент
         * anchor - вырванивание внутри ячейки
         * Insets - отступы
         * ipadx - растягивание элемента по выосте
         * ipady - растягивание элемента по ширине         */
        GridBagConstraints contConnect1 = new GridBagConstraints(GridBagConstraints.RELATIVE, 0, 1, 1, 0, 0, GridBagConstraints.WEST, 0, new Insets(10, 20, 10, 0), 0, 0);
        GridBagConstraints contConnect2 = new GridBagConstraints(GridBagConstraints.RELATIVE, 0, 1, 1, 0, 0, GridBagConstraints.WEST, 0, new Insets(10, 15, 10, 0), 0, 0);

        //Задаём размеры компонентов
        combCom.setPreferredSize(new Dimension(80, 25));
        combBaud.setPreferredSize(new Dimension(80, 25));
        btConnect.setPreferredSize(new Dimension(100, 25));
        btDisconnect.setPreferredSize(new Dimension(90, 25));

        //Задаём внутренние отступы у кнопки, чтобы сделать её меньше
        //    btRefresh.setMargin(new Insets(0, 0, 0, 0));
        btConnect.setMargin(new Insets(5, 2, 5, 2));
        btDisconnect.setMargin(new Insets(5, 2, 5, 2));

        //Отключаем прорисовку фокуса у кнопок
        //   btRefresh.setFocusPainted(false);
        btConnect.setFocusPainted(false);
        btDisconnect.setFocusPainted(false);

        //Задаём выбранный элемент у раскрывающегося списка (сделал для себя, чтобы каждый раз не выбирать необходимую скорость)
        combBaud.setSelectedIndex(9);

        //Меняем цвет кнопки отключения и делаем её неактивной
        btDisconnect.setBackground(Color.pink);
        btDisconnect.setEnabled(false);

        //Добавялем слушателей для кнопок
        combCom.addMouseListener(comMouseListener);
        btConnect.addActionListener(new ButtonConnectListener());
        btDisconnect.addActionListener(new ButtonDisconnectListener());

        //Добавляем компоненты на панель
        //   connectPan.add(btRefresh, contConnect1);
        connectPan.add(combCom, contConnect1);
        connectPan.add(combBaud, contConnect2);
        connectPan.add(btConnect, contConnect1);
        connectPan.add(btDisconnect, contConnect2);


        //ПАНЕЛЬ ЭЛЕМЕНТОВ УПРАВЛЕНИЯ
        //Создаём две панели и добавляем им менедждеры компонновки, задаём размеры панели - две панели нужны, чтобы при изменении названия режима (а у них разные размеры строки) не скакали элементы управления  - "вперёд", "назад" и т.д.
        JPanel mainControlPan = new JPanel();
        JPanel mainControlPan2 = new JPanel();
        mainControlPan.setLayout(new BoxLayout(mainControlPan, BoxLayout.X_AXIS));
        mainControlPan2.setLayout(new BoxLayout(mainControlPan2, BoxLayout.X_AXIS));

        mainControlPan.setBackground(Color.PINK);
        mainControlPan2.setBackground(Color.PINK);
        mainControlPan.setPreferredSize(new Dimension(260, 60));
        mainControlPan2.setPreferredSize(new Dimension(174, 60));

        //Задаём размеры кнопки ВКЛ/ВЫКЛ, добавляем слушателя
        btOnOff.addActionListener(new ButtonOnOffListener());
        btPrev.addActionListener(new ButtonPrevListener());
        btPause.addActionListener(new ButtonPauseListener());
        btNext.addActionListener(new ButtonNextListener());
        btAddToFav.addActionListener(new ButtonFavoriteListener());
        btSaveSettings.addActionListener(new ButtonSaveSetListener());

        //Отключаем прорисовку рамки у кнопок
        btOnOff.setBorderPainted(false);
        btPrev.setBorderPainted(false);
        btPause.setBorderPainted(false);
        btNext.setBorderPainted(false);
        btAddToFav.setBorderPainted(false);
        btSaveSettings.setBorderPainted(false);

        //Устанавливаем внутренние отступы
        btOnOff.setMargin(new Insets(0, 0, 0, 0));
        btPrev.setMargin(new Insets(0, 0, 0, 0));
        btPause.setMargin(new Insets(0, 0, 0, 0));
        btNext.setMargin(new Insets(0, 0, 0, 0));
        btAddToFav.setMargin(new Insets(0, 0, 0, 0));
        btSaveSettings.setMargin(new Insets(0, 0, 0, 0));

        //Отключаем прорисовку фокуса у кнопок
        btOnOff.setFocusPainted(false);
        btPrev.setFocusPainted(false);
        btPause.setFocusPainted(false);
        btNext.setFocusPainted(false);
        btAddToFav.setFocusPainted(false);
        btSaveSettings.setFocusPainted(false);

        ImageIcon icOff = new ImageIcon(getClass().getResource("/icons/switchOFF.png"));
        ImageIcon icOn = new ImageIcon(getClass().getResource("/icons/switchON.png"));
        ImageIcon icPrev = new ImageIcon(getClass().getResource("/icons/prev.png"));
        ImageIcon icPrevRoll = new ImageIcon(getClass().getResource("/icons/prevRoll.png"));
        ImageIcon icPrevPress = new ImageIcon(getClass().getResource("/icons/prevClicked.png"));
        ImageIcon icPause = new ImageIcon(getClass().getResource("/icons/pause.png"));
        ImageIcon icPauseRoll = new ImageIcon(getClass().getResource("/icons/pauseRoll.png"));
        ImageIcon icPausePress = new ImageIcon(getClass().getResource("/icons/pauseClicked.png"));
        ImageIcon icNext = new ImageIcon(getClass().getResource("/icons/next.png"));
        ImageIcon icNextRoll = new ImageIcon(getClass().getResource("/icons/nextRoll.png"));
        ImageIcon icNextPress = new ImageIcon(getClass().getResource("/icons/nextClicked.png"));
        ImageIcon icFav = new ImageIcon(getClass().getResource("/icons/fav.png"));
        ImageIcon icFavRoll = new ImageIcon(getClass().getResource("/icons/favRoll.png"));
        ImageIcon icFavPress = new ImageIcon(getClass().getResource("/icons/favClicked.png"));
        ImageIcon icSave = new ImageIcon(getClass().getResource("/icons/saveset.png"));

        //Подготавливаем и добавляем иконки для переключателя ВКЛ/ВЫКЛ
        btOnOff.setBorder(null);
        btOnOff.setContentAreaFilled(false);
        btOnOff.setIcon(icOff);
        btOnOff.setSelectedIcon(icOn);

        //Подготавливаем и добавляем иконки для кнопки Назад
        btPrev.setBorder(null);
        btPrev.setContentAreaFilled(false);
        btPrev.setIcon(icPrev);
        btPrev.setRolloverIcon(icPrevRoll);
        btPrev.setPressedIcon(icPrevPress);

        //Подготавливаем и добавляем иконки для кнопки Пауза
        btPause.setBorder(null);
        btPause.setContentAreaFilled(false);
        btPause.setIcon(icPause);
        btPause.setPressedIcon(icPauseRoll);
        btPause.setRolloverIcon(icPausePress);

        //Подготавливаем и добавляем иконки для кнопки Вперёд
        btNext.setBorder(null);
        btNext.setContentAreaFilled(false);
        btNext.setIcon(icNext);
        btNext.setRolloverIcon(icNextRoll);
        btNext.setPressedIcon(icNextPress);

        //Подготавливаем и добавляем иконки для кнопки "Добавить в избранное"
        btAddToFav.setBorder(null);
        btAddToFav.setContentAreaFilled(false);
        btAddToFav.setIcon(icFav);
        btAddToFav.setRolloverIcon(icFavRoll);
        btAddToFav.setPressedIcon(icFavPress);

        //Подготавливаем и добавляем иконки для кнопки сохранения настроек
        btSaveSettings.setBorder(null);
        btSaveSettings.setContentAreaFilled(false);
        btSaveSettings.setIcon(icSave);

        //Добавляем компоненты на панель #1, добавляем отступы
        mainControlPan.add(Box.createHorizontalStrut(15));
        mainControlPan.add(btOnOff);
        mainControlPan.add(Box.createHorizontalStrut(15));
        mainControlPan.add(lblCurNumOfModes);
        mainControlPan.add(Box.createHorizontalStrut(15));
        mainControlPan.add(lblCurModeName);

        //Добавляем компоненты на панель #2, добавляем отступы
        mainControlPan2.add(btPrev);
        mainControlPan2.add(Box.createHorizontalStrut(2));
        mainControlPan2.add(btPause);
        mainControlPan2.add(Box.createHorizontalStrut(2));
        mainControlPan2.add(btNext);
        mainControlPan2.add(Box.createHorizontalStrut(10));
        mainControlPan2.add(btAddToFav);
        mainControlPan2.add(Box.createHorizontalStrut(2));
        mainControlPan2.add(btSaveSettings);

        //ЦЕНТРАЛЬНАЯ ПАНЕЛЬ
        //Инициализируем чекбоксы, добавляя в качестве параметра строки с названиями режимов, тутже добавляем чекбоксы в менеджер компоновки "коробка"
        for (byte i = 0; i < chkModesList.length; i++) {
            chkModesList[i] = new JCheckBox(WS2812B.modeNames[i]);
            chkModesList[i].setFocusPainted(false);
            chkModesList[i].addItemListener(new ModesListItemListener(i));
            box.add(chkModesList[i]);
        }

        //Задаём размеры панели прокрутки
        scrollPane.setPreferredSize(new Dimension(435, 400));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);


        //НИЖНЯЯ ПАНЕЛЬ
        //Создаём хэш-таблицу для слайдеров, чтобы сделать их более информативными
        Hashtable<Integer, Component> speedTable = new Hashtable<>();
        speedTable.put(0, new JLabel("0.5x"));
        speedTable.put(5, new JLabel("1x"));
        speedTable.put(10, new JLabel("1.5x"));
        speedTable.put(15, new JLabel("2x"));

        //Тоже самое, только для
        Hashtable<Integer, Component> brightTable = new Hashtable<>();
        brightTable.put(1, new JLabel("min"));
        brightTable.put(127, new JLabel("50%"));
        brightTable.put(255, new JLabel("max"));

        //Создаём нижнюю панель и добавляем ей менеджера компоновки
        JPanel setUpPanel = new JPanel();
        setUpPanel.setLayout(new GridBagLayout());

        //Настраиваем стили для наших элементов
        GridBagConstraints contSettings1 = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, 0, new Insets(5, 5, 0, 5), 0, 0);
        GridBagConstraints contSettings2 = new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, 0, new Insets(-5, 5, 0, 5), 0, 0);
        GridBagConstraints contSettings3 = new GridBagConstraints(GridBagConstraints.RELATIVE, 0, 2, 1, 0, 0, GridBagConstraints.CENTER, 0, new Insets(0, 0, 0, 0), 0, 0);
        GridBagConstraints contSettings4 = new GridBagConstraints(GridBagConstraints.RELATIVE, 1, 2, 1, 0, 0, GridBagConstraints.CENTER, 0, new Insets(-8, 10, 0, 8), 0, 0);
        chkAutoMode.addItemListener(new AutoModeItemListener());
        btColorChooser.setForeground(Color.WHITE);
        btColorChooser.setBackground(Color.BLACK);
        btColorChooser.addActionListener(new ButtonColorListener());

        //Настраиваем слайдеры ("Главные" шкалы деления и второстепенные, их отображение, задаём хэш-таблицу для прорисовки отдельных значений и её отображение, задаём размеры)
        brightness.setMajorTickSpacing(63);
        brightness.setMinorTickSpacing(21);
        brightness.setPaintTicks(true);
        brightness.setLabelTable(brightTable);
        brightness.setPaintLabels(true);
        brightness.setPreferredSize(new Dimension(160, 65));
        brightness.addMouseListener(new BrightMouseListener());
        speed.setMajorTickSpacing(5);
        speed.setMinorTickSpacing(1);
        speed.setPaintTicks(true);
        speed.setLabelTable(speedTable);
        speed.setPaintLabels(true);
        speed.setSnapToTicks(true);
        speed.setPreferredSize(new Dimension(160, 65));
        speed.addMouseListener(new SpeedMouseListener());


        //Добавляем компоненты на панель
        setUpPanel.add(chkAutoMode, contSettings1);
        setUpPanel.add(btColorChooser, contSettings2);
        setUpPanel.add(lblBright, contSettings3);
        setUpPanel.add(lblSpeed, contSettings3);
        setUpPanel.add(brightness, contSettings4);
        setUpPanel.add(speed, contSettings4);
        jtxStatus.setPreferredSize(new Dimension(450, 20));
        jtxStatus.setEditable(false);

        //Настройка главного окна
        disableAll();
        this.setSize(450, 650);
        this.add(connectPan);
        this.add(mainControlPan);
        this.add(mainControlPan2);
        this.add(scrollPane);
        this.add(setUpPanel);
        this.add(jtxStatus);
        this.setResizable(false);
        this.setVisible(true);
    }


    //Слушатель для выпадающего списка COM-порртов
    private class CombComMouseListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            //Обновляем список доступных COM-портов для подключения
            if (!Main.isConnected) {
                int lastSelectedIndex;
                String[] comPorts;

                lastSelectedIndex = combCom.getSelectedIndex();
                combCom.removeAllItems();

                comPorts = SerialPortList.getPortNames();
                for (String s : comPorts) {
                    combCom.addItem(s);
                }
                if (lastSelectedIndex <= comPorts.length - 1) {
                    combCom.setSelectedIndex(lastSelectedIndex);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }
    }

    //Слушатель кнопки подключения
    private class ButtonConnectListener implements ActionListener {
        public void actionPerformed(ActionEvent e) throws NullPointerException {

            btConnect.setEnabled(false);
            combCom.setEnabled(false);
            combBaud.setEnabled(false);

            try {
                Main.openPort(combCom.getSelectedItem().toString(), (Integer) combBaud.getSelectedItem());
            } catch (NullPointerException ne) {
                ne.printStackTrace();
            }
            System.out.println(Main.com + " " + Main.baudRate);

            //Поток подключения в котором проихсодит попытка установить соединение
            TryToConnect ttc = new TryToConnect();
            ttc.execute();

            //Запускаем фоновый поток, отслеживающий приходящие данные и изменяющий, в соответствии с ними, ГПИ
            new UIUpdater().execute();

        }
    }

    //Слушатель кнопки отключения
    private class ButtonDisconnectListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Main.disconnect();
            disableAll();
        }
    }

    //Слушатель кнопки ВКЛ/ВЫКЛ
    private class ButtonOnOffListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {

            Main.sendData(WS2812B.ON_OFF);
        }
    }

    //Слушатель для кнопки "Назад"
    private class ButtonPrevListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {


            Main.sendData(WS2812B.PREV);
        }
    }

    //Слушатель для кнопки "Пауза"
    private class ButtonPauseListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Main.sendData(WS2812B.PAUSE);
        }
    }

    //Слушатель для кнопки "Вперёд"
    private class ButtonNextListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Main.sendData(WS2812B.NEXT);
        }
    }

    //Слушатель для кнопки "Добавить в избранное"
    private class ButtonFavoriteListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Main.sendData(WS2812B.FAV, currentLedMode);
        }
    }

    //Слушатель для кнопки "Сохранить настройки"
    private class ButtonSaveSetListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Main.sendData(WS2812B.SAVE_SETTINGS);
        }
    }

    //Слушатель для изменения списка режимов
    private class ModesListItemListener implements ItemListener {
        //Переменная индекса чекбоса, значения активности режима (1 - добавить в список/активен, 0 - удалить из списка/деактивирован
        byte index;
        byte actDeactValue;

        //Конструктор принимает в качестве параметра индекс чекбокса, который вызвал ивент, в зависимости от текущего состояния чекбокса формируется код для отправки
        ModesListItemListener(byte index) {
            this.index = index;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (readyActDeactMode) {
                if (chkModesList[index].isSelected()) {
                    actDeactValue = 1;
                } else {
                    actDeactValue = 0;
                }
                Main.sendData(WS2812B.ACT_DEACT_MODE, index, actDeactValue);
                System.out.println("Режим " + WS2812B.modeNames[index] + "(" + index + ") " + "Установлен в значение - " + chkModesList[index].isSelected());
            }
        }
    }

    //Слушатель для слайдера яркости(реализован через ивенты мыши)
    private class BrightMouseListener implements MouseListener {
        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maxBright = brightness.getValue();
            Main.sendData(WS2812B.SET_BRIGHT, maxBright);
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }
    }

    //Слушатель для слайдера скорости(реализован через ивенты мыши)
    private class SpeedMouseListener implements MouseListener {
        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            //Скорость регулируется путём изменения перменной задержки - чем меньше её значение, тем быстрее работает эффект
            //Считываем значение слайдера (0-15), каждое значение соответствует своей ячейке в массиве hashSpeed
            int arrayIndex = speed.getValue();
            int thisdelayChanged = (int) (thisdelay / WS2812B.speedMultiplier[arrayIndex]);

            System.out.println(thisdelayChanged);
            Main.sendData(WS2812B.SET_SPEED, thisdelayChanged);
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }
    }

    //Слушаель для чекбокса "Авто"
    private class AutoModeItemListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            boolean currentState = chkAutoMode.isSelected();
            int value;
            if (currentState) {
                value = 1;
            } else {
                value = 0;
            }
            Main.sendData(WS2812B.SET_AUTO, value);
        }
    }

    //Слушатель для кнопки "Цвет"
    private class ButtonColorListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {

            Color initColor = btColorChooser.getBackground();
            Color bg = JColorChooser.showDialog(scrollPane, "Выберите цвет", initColor);
            if (bg != null) {
                btColorChooser.setBackground(bg);

                int r = bg.getRed();
                int g = bg.getGreen();
                int b = bg.getBlue();

                Main.sendData(WS2812B.SET_COLOR, r, g, b);

            }
        }
    }

    //Фоновый поток для обновления данных GUI
    private class UIUpdater extends SwingWorker<Void, Integer> {
        @Override
        protected Void doInBackground() {
            while (Main.isConnected) {
                if (Main.isRecponceRecived) {
                    Main.isRecponceRecived = false;
                    syncGUI(Main.returnRecivedData());
                }
                try {
                    Thread.sleep(300);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Main.connectCounter++;
                if (Main.connectCounter == 16) {
                    Main.disconnect();
                    disableAll();
                }
            }
            return null;
        }
    }

    //Отдельный поток для работы с подключением к клиенту
    private class TryToConnect extends SwingWorker<Boolean, Integer> {
        int i = 0;

        @Override
        public Boolean doInBackground() throws Exception {
            Main.sendData(WS2812B.CONNECT);
            Thread.sleep(200);
            for (i = 1; i < 11; i++) {
                publish(i);             //методом publish() можно передавать промежуточные результаты работы в метод process()
                if (Main.isConnected) {
                    return true;
                }

                Main.sendData(WS2812B.CONNECT);
                Thread.sleep(500);
            }
            return false;
        }

        //Этот метод нужен для отображения прогресса работы
        @Override
        protected void process(List<Integer> chunks) {
            jtxStatus.setText("Попытка подключения №" + chunks.get(0));          //метод get(), в данном случае, возвращает то, что мы передали с помощью publish()
            jtxStatus.setBackground(Color.WHITE);
        }

        //Вызывается по завершению работы (после завершения метода doInBackground())
        @Override
        protected void done() {
            boolean result;
            try {
                result = this.get();   //спомощью метода get() можно узнать, какое значение было возвращено методом doInBackground()
                if (result) {

                    initializeGUI(Main.returnRecivedData());
                } else {
                    Main.disconnect();

//                   combCom.setEnabled(true);
//                   combBaud.setEnabled(true);
//                   btConnect.setEnabled(true);

                    jtxStatus.setText("Ошибка подключения!");
                    jtxStatus.setBackground(Color.RED);
                }

            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
            }

            Main.clearData();
        }
    }

    //Метод, который обновляет ГПИ в соответствии с принятыми данными от ардуины
    private void syncGUI(int... recivedData) throws ArrayIndexOutOfBoundsException {
        jtxStatus.setBackground(Color.WHITE);
        try {
            switch (recivedData[0]) {
                case WS2812B.ON_OFF:
                    if (recivedData[1] == 0) {
                        jtxStatus.setText("Лента выключена!");
                        btOnOff.setSelected(false);
                        readyToSaveSet = false;
                        disableAll();
                    } else {
                        jtxStatus.setText("Лента включена!");
                        btOnOff.setSelected(true);
                        readyToSaveSet = true;
                        enableAll();
                    }
                    break;

                case WS2812B.PREV:
                case WS2812B.NEXT:
                    currentLedMode = recivedData[1];
                    thisdelay = recivedData[2];
                    lblCurModeName.setText(WS2812B.modeNames[currentLedMode - 1]);
                    lblCurNumOfModes.setText(currentLedMode + "/" + modesCount);

                    if (thisdelay == 0) {
                        speed.setEnabled(false);
                    } else {
                        speed.setEnabled(true);
                    }
                    speed.setValue(5);

                    readyToSaveSet = true;

                    jtxStatus.setText("Режим № " + currentLedMode + " - " + WS2812B.modeNames[currentLedMode - 1]);
                    break;

                case WS2812B.PAUSE:
                    if (recivedData[1] == 1) {
                        jtxStatus.setText("Пауза");
                        brightness.setEnabled(false);
                        speed.setEnabled(false);
                    } else {
                        jtxStatus.setText("Текущий режим " + currentLedMode);
                        if (thisdelay == 0) {
                            speed.setEnabled(false);
                        } else {
                            speed.setEnabled(true);
                        }
                        brightness.setEnabled(true);
                    }
                    break;

                case WS2812B.FAV:
                    jtxStatus.setText("Режим " + WS2812B.modeNames[currentLedMode - 1] + "(" + currentLedMode + ")" + " установлен стартовым режимом");
                    break;

                case WS2812B.ACT_DEACT_MODE:
                    boolean actDeactResult;

                    actDeactResult = recivedData[2] == 1;
                    jtxStatus.setText("Режим " + WS2812B.modeNames[recivedData[1]] + "(" + recivedData[1] + ") " + "Установлен в значение - " + actDeactResult);

                    readyToSaveSet = true;
                    break;

                case WS2812B.SET_AUTO:
                    if (recivedData[1] == 1) {
                        jtxStatus.setText("Авторежим включен!");
                    } else {
                        jtxStatus.setText("Авторежим выключен!");
                    }

                    readyToSaveSet = true;
                    break;

                case WS2812B.SET_COLOR:
                    lblCurModeName.setText("Произвольный цвет");
                    lblCurNumOfModes.setText("");
                    speed.setEnabled(false);
                    jtxStatus.setText("Цвет ленты установлен в R: " + recivedData[1] + ", G: " + recivedData[2] + ", B: " + recivedData[3]);
                    break;

                case WS2812B.SET_BRIGHT:
                    maxBright = recivedData[1];
                    brightness.setValue(maxBright);
                    jtxStatus.setText("Яркость установлена в значение " + maxBright + " единиц");
                    readyToSaveSet = true;
                    break;

                case WS2812B.SET_SPEED:
                    if (recivedData[1] > 0) {
                        jtxStatus.setText("Скорость режима " + WS2812B.modeNames[currentLedMode - 1] + "(" + currentLedMode + ") " + "установлена в значение - " + recivedData[1]);
                    }
                    break;

                case WS2812B.SAVE_SETTINGS:
                    if (recivedData[1] > 0) {
                        readyToSaveSet = false;
                        jtxStatus.setText("Данные сохранены!");
                    }
                    break;
            }
        } catch (ArrayIndexOutOfBoundsException ae) {
            ae.printStackTrace();
        }

        btSaveSettings.setEnabled(readyToSaveSet);

    }

    //Инициализация ГПИ начальными значениями, полученными от дуины
    private void initializeGUI(int[] recivedData) throws ArrayIndexOutOfBoundsException {
        //Вывод системной информации
        jtxStatus.setText("Подключено к " + Main.com + " на скорости " + Main.baudRate);
        jtxStatus.setBackground(Color.GREEN);

        //Заполнение переменных полученными от ардуины данными
        int indx1 = 0;
        try {
            currentLedMode = recivedData[1];
            maxBright = recivedData[2];
            autoMode = recivedData[3];
            thisdelay = recivedData[4];
            for (int indx2 = 5; indx2 < 54; indx2++) {
                ledModes[indx1] = recivedData[indx2];
                indx1++;
            }
        } catch (ArrayIndexOutOfBoundsException ae) {
            ae.printStackTrace();
        }
        //Применение настроек - установка состояния интерфейса в зависимости от состояния ленты
        if (currentLedMode > 0) {
            btOnOff.setSelected(true);
            enableAll();
        } else {
            btOnOff.setSelected(false);
            disableAll();
        }

        //Обновление лейблов с именами режимов и порядковым номером
        lblCurModeName.setText(WS2812B.modeNames[currentLedMode - 1]);
        lblCurNumOfModes.setText(currentLedMode + "/" + modesCount);

        //Установка галочки для режима "Авто" если этот режим включён в Ардуино
        if (autoMode == 1) {
            chkAutoMode.setSelected(true);
        }

        //Установка значения яркости
        brightness.setValue(maxBright);
        for (int i = 0; i < modesCount; i++) {
            if (ledModes[i] == 1) {
                chkModesList[i].setSelected(true);
            }
        }

        //Установка флага готовности к изменению основного списка режимов
        readyActDeactMode = true;
    }

    //Метод для отключения всех компонентов при первом запуске и при нажатии на кнопку "ВЫКЛ"
    private void disableAll() {
        //Если нет подключения, то отключаем все компоненты, включаем компоненты подключения
        if (!Main.isConnected) {
            btConnect.setEnabled(true);
            combCom.setEnabled(true);
            combBaud.setEnabled(true);
            btDisconnect.setEnabled(false);
            btOnOff.setEnabled(false);
        }

        btOnOff.setEnabled(true);
        lblCurModeName.setEnabled(false);
        lblCurNumOfModes.setEnabled(false);
        btPrev.setEnabled(false);
        btPause.setEnabled(false);
        btNext.setEnabled(false);
        btAddToFav.setEnabled(false);
        btSaveSettings.setEnabled(false);

        //Нижняя панель
        chkAutoMode.setEnabled(false);
        btColorChooser.setEnabled(false);
        lblBright.setEnabled(false);
        lblSpeed.setEnabled(false);
        brightness.setEnabled(false);
        speed.setEnabled(false);

        //Центральная панель
        for (int i = 0; i < modesCount; i++) {
            chkModesList[i].setEnabled(false);
        }

        readyActDeactMode = false;

        jtxStatus.setBackground(Color.WHITE);

    }

    //Метод для включения всех компонентов при удачном получении настроек и при нажатии на кнопку "ВКЛ"
    private void enableAll() {
        //Верхняя панель
        btDisconnect.setEnabled(true);

        //Панель управления
        btOnOff.setEnabled(true);
        lblCurModeName.setEnabled(true);
        lblCurNumOfModes.setEnabled(true);
        btPrev.setEnabled(true);
        btPause.setEnabled(true);
        btNext.setEnabled(true);
        btAddToFav.setEnabled(true);
        if (readyToSaveSet) btSaveSettings.setEnabled(true);

        //Нижняя панель
        chkAutoMode.setEnabled(true);
        btColorChooser.setEnabled(true);
        lblBright.setEnabled(true);
        lblSpeed.setEnabled(true);
        brightness.setEnabled(true);
        speed.setEnabled(true);

        //Центральная панель
        for (int i = 0; i < modesCount; i++) {
            chkModesList[i].setEnabled(true);
        }

    }

    //SerialPort Events
    @Override
    public void onSerialPortConnected(String port, int baudRate) {
        combCom.removeMouseListener(comMouseListener);
    }

    @Override
    public void onSerialPortFailedToConnect(String port, int baudRate) {

    }

    @Override
    public void onSerialPortClosed(String port) {
        combCom.addMouseListener(comMouseListener);
    }

    @Override
    public void onSerialPortDataSent(int... message) {

    }

    @Override
    public void onSerialPortDataReceived(int[] data) {

    }

    @Override
    public void onSerialPortConfigReceived(int[] data) {

    }

    @Override
    public void onException(Exception e) {

    }
}

