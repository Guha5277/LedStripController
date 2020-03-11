package com.guhafun;

import jssc.SerialPortList;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;


/*TODO
 * 3. Рефакторинг кода, перенос на новый тип взаимодействия ГИ и класса работающего с портом (реалзиация интерфейса)*/
class MainFrame extends JFrame implements SerialPortListener, ChangeListener {
    private final SerialPortController serialPortController;
    private final CombComMouseListener comMouseListener;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame("WS2812B Controller"));
    }

    private boolean isInitialDataReceived = false;
    private boolean readyToSaveSet = false;     //Флаг наличия изменений, для сохрания
    private int currentLedMode = 0;             //Номер текущего режима
    private int prevLedMode = 1;                //Номер предыдущего режима
    private int[] ledModes = new int[49];       //Список активных режимов
    private int maxBright = 0;                  //Максимальная яркость
    private int autoMode = 0;                   //Состояние авторежима (0 - выключен, 1 - включен)
    private int delay = 0;                  //Значение задержки между итерациями эффектов (увеличивая это значение визуально изменить скорость воспроизведения, при уменьшении - наоборот)
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
    private CustomCheckBox[] chkModesList;        //Список режимов
    private ItemsChangeListener itemsChangeListener;
    private Box box;                         //Менеджер расположения графических элементов
    private JScrollPane scrollPane;          //Панель прокрутки
    private JCheckBox chkAutoMode;           //Чекбокс для ВКЛ/ВЫКЛ автоматической смены режима
    private JButton btColorChooser;          //Кнопка для вывода произвольного цвета на экран
    private JLabel lblBright;                //Лейбл "Яркость"
    private JLabel lblSpeed;                 //Лейбл "Скорость"
    private JSlider brightness;              //Слайдер "Яркость"
    private JSlider speed;                   //Слайдер "Скорость"
    private JTextArea jtxStatus;             //Текстовое поле с системной информацией

    private Color colorsCheckBoxesActive = Color.red;
    private Color colorsCheckBoxesInactive = Color.BLACK;

    private JFrame colorChooserFrame;
    private JColorChooser colorChooser;      //Сдандарная панель выбора цвета

    private MainFrame(String frameTitle) {
        super(frameTitle);
        serialPortController = new SerialPortController(this);
        comMouseListener = new CombComMouseListener();
        createUI();
        setupUI();
    }

    private void createUI() {
        chkModesList = new CustomCheckBox[WS2812B.modeNames.length];
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
        colorChooserFrame = new JFrame();
        colorChooser = new JColorChooser(new Color(0, 0, 0));
        itemsChangeListener = new ItemsChangeListener();
    }

    private void setupUI() {
        //Получаем список портов, добавляем его в выпадающий список
        String[] comPorts = serialPortController.getSerialPortList();
        combCom = new JComboBox<>(comPorts);
        combCom.setSelectedIndex(comPorts.length - 1);
        combBaud = new JComboBox<>(serialPortController.getBaudRateList());
        combBaud.setSelectedIndex(9);
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        //ВЕРХНАЯЯ ПАНЕЛЬ ПОДКЛЮЧЕНИЯ
        //Создаём верхнюю панель (для элементов подключения-отключения), задаём панели новый менеджер компоновки элементов
        JPanel connectPan = new JPanel();
        connectPan.setLayout(new GridBagLayout());
        GridBagConstraints contConnect1 = new GridBagConstraints(GridBagConstraints.RELATIVE,
                0, 1, 1, 0, 0, GridBagConstraints.WEST,
                0, new Insets(10, 20, 10, 0), 0, 0);
        GridBagConstraints contConnect2 = new GridBagConstraints(GridBagConstraints.RELATIVE,
                0, 1, 1, 0, 0, GridBagConstraints.WEST,
                0, new Insets(10, 15, 10, 0), 0, 0);

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
        btConnect.setFocusPainted(false);
        btDisconnect.setFocusPainted(false);

        //Меняем цвет кнопки отключения и делаем её неактивной
        btDisconnect.setBackground(Color.pink);
        btDisconnect.setEnabled(false);

        //Задаём команды для кнопок
        btConnect.setActionCommand("connect");
        btDisconnect.setActionCommand("disconnect");
        btOnOff.setActionCommand("onoff");
        btPrev.setActionCommand("prev");
        btPause.setActionCommand("pause");
        btNext.setActionCommand("next");
        btAddToFav.setActionCommand("fav");
        btSaveSettings.setActionCommand("saveset");
        btColorChooser.setActionCommand("color");

        //Добавялем слушателей для кнопок
        combCom.addMouseListener(comMouseListener);
        ButtonsListener buttonsListener = new ButtonsListener();
        btConnect.addActionListener(buttonsListener);
        btDisconnect.addActionListener(buttonsListener);

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
        btOnOff.addActionListener(buttonsListener);
        btPrev.addActionListener(buttonsListener);
        btPause.addActionListener(buttonsListener);
        btNext.addActionListener(buttonsListener);
        btAddToFav.addActionListener(buttonsListener);
        btSaveSettings.addActionListener(buttonsListener);

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
            chkModesList[i] = new CustomCheckBox(i, WS2812B.modeNames[i]);
            chkModesList[i].setFocusPainted(false);
            //chkModesList[i].addItemListener(new ModesListItemListener(i));
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
        btColorChooser.addActionListener(buttonsListener);

        //Настраиваем слайдеры ("Главные" шкалы деления и второстепенные, их отображение, задаём хэш-таблицу для прорисовки отдельных значений и её отображение, задаём размеры)
        brightness.setMajorTickSpacing(63);
        brightness.setMinorTickSpacing(21);
        brightness.setPaintTicks(true);
        brightness.setLabelTable(brightTable);
        brightness.setPaintLabels(true);
        brightness.setPreferredSize(new Dimension(160, 65));
//        brightness.addMouseListener(new BrightMouseListener());
        brightness.addChangeListener(new BrightnessChangeListener());
        speed.setMajorTickSpacing(5);
        speed.setMinorTickSpacing(1);
        speed.setPaintTicks(true);
        speed.setLabelTable(speedTable);
        speed.setPaintLabels(true);
        speed.setSnapToTicks(true);
        speed.setPreferredSize(new Dimension(160, 65));
        speed.addChangeListener(new SpeedChangeListener());

        //Добавляем компоненты на панель
        setUpPanel.add(chkAutoMode, contSettings1);
        setUpPanel.add(btColorChooser, contSettings2);
        setUpPanel.add(lblBright, contSettings3);
        setUpPanel.add(lblSpeed, contSettings3);
        setUpPanel.add(brightness, contSettings4);
        setUpPanel.add(speed, contSettings4);
        jtxStatus.setPreferredSize(new Dimension(450, 20));
        jtxStatus.setEditable(false);

        //Настройка панели выбора произвольного цвета
        JPanel colorChooserPanel = new JPanel();
        colorChooserPanel.add(colorChooser);
        colorChooserFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        colorChooserFrame.add(colorChooserPanel);
        colorChooser.removeChooserPanel(colorChooser.getChooserPanels()[0]);
        colorChooser.getSelectionModel().addChangeListener(this);

        //Настройка главного окна
        disableAll(true);
        int frameHeight = 650;
        int frameWidth = 450;
        this.setSize(frameWidth, frameHeight);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double screenHeight = screenSize.getHeight();
        double screenWidth = screenSize.getWidth();
        this.setLocation((int) screenWidth / 2 - frameWidth / 2, (int) screenHeight / 2 - frameHeight / 2);
        this.add(connectPan);
        this.add(mainControlPan);
        this.add(mainControlPan2);
        this.add(scrollPane);
        this.add(setUpPanel);
        this.add(jtxStatus);
        jtxStatus.setBackground(new Color(238, 238, 238));
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
            } else {
                combCom.setSelectedIndex(comPorts.length - 1);
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

    //Общий слушатель для кнопок
    private class ButtonsListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String code = e.getActionCommand();
            switch (code) {
                case "connect":
                    if (combCom.getSelectedItem() == null) {
                        showErrorMessage("Нет доступных устройств для подключения!\nПроверьте правильность подключения и повторите попытку");
                    } else {
                        btConnect.setEnabled(false);
                        combCom.setEnabled(false);
                        combBaud.setEnabled(false);
                        serialPortController.connect(combCom.getSelectedItem().toString(), (Integer) combBaud.getSelectedItem());
                    }
                    break;
                case "disconnect":
                    serialPortController.disconnect();
                    break;
                case "onoff":
                    serialPortController.sendMessage(WS2812B.ON_OFF);
                    break;
                case "prev":
                    serialPortController.sendMessage(WS2812B.PREV);
                    break;
                case "pause":
                    serialPortController.sendMessage(WS2812B.PAUSE);
                    break;
                case "next":
                    serialPortController.sendMessage(WS2812B.NEXT);
                    break;
                case "fav":
                    serialPortController.sendMessage(WS2812B.FAV, currentLedMode);
                    break;
                case "saveset":
                    serialPortController.sendMessage(WS2812B.SAVE_SETTINGS);
                    break;
                case "color":
                    int x = MainFrame.this.getX();
                    int y = MainFrame.this.getY();
                    colorChooserFrame.setLocation(x - 85, y + 150);
                    colorChooserFrame.pack();
                    colorChooserFrame.setVisible(true);
                    break;
            }
        }
    }

    //Слушатель чекбоксов режимов
    private class ItemsChangeListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            CustomCheckBox checkBox = (CustomCheckBox) e.getSource();
            int index = checkBox.getIndex();
            byte modeState = chkModesList[index].isSelected() ? (byte) 1 : 0;
            serialPortController.sendMessage(WS2812B.MODE_STATE, index, modeState);
        }
    }

    //Слушатель для слайдера яркости
    private class BrightnessChangeListener implements ChangeListener {
        long timePassed = 0;
        long lastTimeCalled = System.currentTimeMillis();

        @Override
        public void stateChanged(ChangeEvent e) {
            timePassed += System.currentTimeMillis() - lastTimeCalled;
            lastTimeCalled = System.currentTimeMillis();

            if (timePassed > 10) {
                maxBright = brightness.getValue();
                serialPortController.sendMessage(WS2812B.SET_BRIGHT, maxBright);
                timePassed = 0L;
            }
        }
    }

    private class SpeedChangeListener implements ChangeListener {
        long timePassed = 0;
        long lastTimeCalled = System.currentTimeMillis();

        @Override
        public void stateChanged(ChangeEvent e) {
            timePassed += System.currentTimeMillis() - lastTimeCalled;
            lastTimeCalled = System.currentTimeMillis();

            if (timePassed > 10) {
                int arrayIndex = speed.getValue();
                int newDelay = (int) (delay / WS2812B.speedMultiplier[arrayIndex]);
                serialPortController.sendMessage(WS2812B.SET_SPEED, newDelay);
                timePassed = 0L;
            }
        }
    }

    //Слушаель для чекбокса "Авто"
    private class AutoModeItemListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            int value = chkAutoMode.isSelected() ? 1 : 0;
            serialPortController.sendMessage(WS2812B.SET_AUTO, value);
        }
    }

    //Поток получения инициализационных данных
    private class InitialDataKeeper extends Thread {
        int counter = 0;

        @Override
        public void run() {
            while (true) {
                if (counter == 10) {
                    failedToGetInitialData();
                    return;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    MainFrame.this.onException(e);
                }
                if (isInitialDataReceived) return;
                serialPortController.sendMessage(WS2812B.CONNECT);
                counter++;
            }
        }
    }

    private void failedToGetInitialData() {
        showErrorMessage("Не могу инициализировать данные!\nПроверьте правильность скетча и настройки подключения (скорость)!");
        serialPortController.disconnect();
    }

    //Инициализация ГПИ начальными значениями, полученными от микроконтроллера
    private void initializeGUI(int[] receivedData) {
        //Вывод системной информации
        jtxStatus.setText("Подключено к " + combCom.getSelectedItem() + " на скорости " + combBaud.getSelectedItem());
        jtxStatus.setBackground(Color.GREEN);

        //Заполнение переменных полученными от ардуины данными
        int index = 0;
        try {
            currentLedMode = receivedData[1];
            maxBright = receivedData[2];
            autoMode = receivedData[3];
            delay = receivedData[4];
            for (int arrIndex = 5; arrIndex < 54; arrIndex++) {
                ledModes[index] = receivedData[arrIndex];
                index++;
            }
        } catch (ArrayIndexOutOfBoundsException ae) {
            this.onException(ae);
        }
        //Обновление лейблов с именами режимов и порядковым номером
        if (currentLedMode >= 50) {
            lblCurModeName.setText("Произвольный цвет");
            lblCurNumOfModes.setText("");
        } else if (currentLedMode == 0) {
            lblCurModeName.setText("Лента выключена!");
            lblCurNumOfModes.setText("");
        } else {
            chkModesList[currentLedMode - 1].setForeground(colorsCheckBoxesActive);
            prevLedMode = currentLedMode;
            lblCurModeName.setText(WS2812B.modeNames[currentLedMode - 1]);
            lblCurNumOfModes.setText(currentLedMode + "/" + modesCount);
        }

        //Установка галочки для режима "Авто" если этот режим включён в Ардуино
        if (autoMode == 1) {
            chkAutoMode.setSelected(true);
        }

        //Установка значения яркости
        brightness.setValue(maxBright);

        //Установление флагов активированных/деактивированных режимов в общем плейлисте
        for (int i = 0; i < modesCount; i++) {
            if (ledModes[i] == 1) {
                chkModesList[i].setSelected(true);
            }
        }

        //Применение настроек - установка состояния интерфейса в зависимости от состояния ленты
        if (currentLedMode > 0) {
            btOnOff.setSelected(true);
            enableAll();
        } else {
            btOnOff.setSelected(false);
            disableAll(false);
        }

        int r = 0;
        int b = 0;
        while (r != 238) {
            jtxStatus.setBackground(new Color(r, 238, b));
            r++;
            b++;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                this.onException(e);
            }
        }
    }

    //Метод, который обновляет ГПИ в соответствии с принятыми данными от микроконтроллера
    private void syncGUI(int... receivedData) {
        jtxStatus.setBackground(Color.WHITE);
        try {
            switch (receivedData[0]) {
                case WS2812B.ON_OFF:
                    if (receivedData[1] == 0) {
                        jtxStatus.setText("Лента выключена!");
                        btOnOff.setSelected(false);
                        readyToSaveSet = false;
                        disableAll(false);
                        btOnOff.setEnabled(true);
                    } else {
                        jtxStatus.setText("Лента включена!");
                        btOnOff.setSelected(true);
                        readyToSaveSet = true;
                        enableAll();
                    }
                    break;

                case WS2812B.PREV:
                case WS2812B.NEXT:
                    currentLedMode = receivedData[1];
                    delay = receivedData[2];
                    lblCurModeName.setText(WS2812B.modeNames[currentLedMode - 1]);
                    lblCurNumOfModes.setText(currentLedMode + "/" + modesCount);
                    chkModesList[currentLedMode - 1].setForeground(colorsCheckBoxesActive);
                    chkModesList[prevLedMode - 1].setForeground(colorsCheckBoxesInactive);
                    prevLedMode = currentLedMode;

                    if (delay == 0) {
                        speed.setEnabled(false);
                    } else {
                        speed.setEnabled(true);
                    }
                    speed.setValue(5);

                    readyToSaveSet = true;

                    jtxStatus.setText("Режим № " + currentLedMode + " - " + WS2812B.modeNames[currentLedMode - 1]);
                    break;

                case WS2812B.PAUSE:
                    if (receivedData[1] == 1) {
                        jtxStatus.setText("Пауза");
                        brightness.setEnabled(false);
                        speed.setEnabled(false);
                    } else {
                        jtxStatus.setText("Текущий режим " + currentLedMode);
                        if (delay == 0) {
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

                case WS2812B.MODE_STATE:
                    boolean modeState;

                    modeState = receivedData[2] == 1;
                    jtxStatus.setText("Режим " + WS2812B.modeNames[receivedData[1]] + "(" + receivedData[1] + ") " + "Установлен в значение - " + modeState);

                    readyToSaveSet = true;
                    break;

                case WS2812B.SET_AUTO:
                    if (receivedData[1] == 1) {
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
                    jtxStatus.setText("Цвет ленты установлен в R: " + receivedData[1] + ", G: " + receivedData[2] + ", B: " + receivedData[3]);
                    chkModesList[prevLedMode - 1].setForeground(colorsCheckBoxesInactive);
                    break;

                case WS2812B.SET_BRIGHT:
                    maxBright = receivedData[1];
                    brightness.setValue(maxBright);
                    jtxStatus.setText("Яркость установлена в значение " + maxBright + " единиц");
                    readyToSaveSet = true;
                    break;

                case WS2812B.SET_SPEED:
                    if (receivedData[1] > 0) {
                        jtxStatus.setText("Скорость режима " + WS2812B.modeNames[currentLedMode - 1] + "(" + currentLedMode + ") " + "установлена в значение - " + receivedData[1]);
                    }
                    break;

                case WS2812B.SAVE_SETTINGS:
                    if (receivedData[1] > 0) {
                        readyToSaveSet = false;
                        jtxStatus.setText("Данные сохранены!");
                    }
                    break;
            }
        } catch (ArrayIndexOutOfBoundsException ae) {
            this.onException(ae);
        }
        btSaveSettings.setEnabled(readyToSaveSet);
    }

    //Метод для отключения всех компонентов при первом запуске и при нажатии на кнопку "ВЫКЛ"
    private void disableAll(boolean connectElementsState) {
        btConnect.setEnabled(connectElementsState);
        combCom.setEnabled(connectElementsState);
        combBaud.setEnabled(connectElementsState);
        btDisconnect.setEnabled(!connectElementsState);
        btOnOff.setEnabled(false);
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

        for (byte i = 0; i < chkModesList.length; i++) {
            chkModesList[i].removeItemListener(itemsChangeListener);
        }
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

        for (byte i = 0; i < chkModesList.length; i++) {
            chkModesList[i].addItemListener(itemsChangeListener);
        }

        //Центральная панель
        for (int i = 0; i < modesCount; i++) {
            chkModesList[i].setEnabled(true);
        }
    }

    //Color Changed Event
    @Override
    public void stateChanged(ChangeEvent e) {
        Color newColor = colorChooser.getColor();
        int r = newColor.getRed();
        int g = newColor.getGreen();
        int b = newColor.getBlue();
        serialPortController.sendMessage(WS2812B.SET_COLOR, r, g, b);
    }

    //SerialPort Events
    @Override
    public void onSerialPortConnected(String port, int baudRate) {
        combCom.removeMouseListener(comMouseListener);
        btConnect.setEnabled(false);
        combCom.setEnabled(false);
        combBaud.setEnabled(false);
        serialPortController.sendMessage(WS2812B.CONNECT);
        new InitialDataKeeper().start();
    }

    @Override
    public void onSerialPortFailedToConnect(String port, int baudRate, Exception e) {
        showErrorMessage("Ошибка подключения: " + e.getMessage());
        btConnect.setEnabled(true);
        combCom.setEnabled(true);
        combBaud.setEnabled(true);
    }

    @Override
    public void onSerialPortClosed(String port) {
        combCom.addMouseListener(comMouseListener);
        isInitialDataReceived = false;
        disableAll(true);
    }

    @Override
    public void onSerialPortFailedToClose(String port, Exception e) {
        showErrorMessage(e.getMessage());
        disableAll(true);
    }

    @Override
    public void onSerialPortDataReceived(int[] data) {
        syncGUI(data);
    }

    @Override
    public void onInitialDataReceived(int[] data) {
        isInitialDataReceived = true;
        initializeGUI(data);
        enableAll();
    }

    @Override
    public void onFailedToSendData() {
        showErrorMessage("Соединение потеряно!");
        disableAll(true);
    }

    @Override
    public void onException(Exception e) {
        showErrorMessage(e.getMessage());
    }

    private void showErrorMessage(String body) {
        JOptionPane.showMessageDialog(this, body, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
}

