package com.guhafun;

import jssc.SerialPortList;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainFrame {

    //Константы - комманды
    final private int CONNECT = 1;
    final private int ON_OFF = 2;
    final private int PREV = 3;
    final private int NEXT = 4;
    final private int PAUSE = 5;
    final private int FAV = 6;
    final private int ACT_DEACT_MODE = 7;
    final private int SET_AUTO = 8;
    final private int SET_COLOR = 9;
    final private int SET_BRIGHT = 10;
    final private int SET_SPEED = 11;
    final private int SAVE_SETTINGS = 12;

    //Флаг для готовности изменения списка режимов
    private boolean readyActDeactMode = false;


    private static int lastCommand = 0;

    //Создаём строковый массив данных, содержащий названия режимов ленты.
    private String[] modeNames = {"Rainbow Fade", "Rainbow Loop", "Random Burst", "Color Bounce", "Color Bounce Fade", "EMS Light One",
            "EMS Light ALL", "Flicker", "Pulse One Color",
            "Pulse with change color", "Fade Vertical", "Rule 30", "Random March", "RWB March", "Radiation", "Color Loop Verdelay",
            "White Temps", "Sin Bright Wave", "Pop Horizontal", "Quad Bright Cirve", "Flame", "Rainbow Vertical", "Pacman", "Random Color Pop", "EMS Lights Strobe",
            "RGB Propeller", "Kitt", "Matrix", "NEW! Rainbow Loop", "Color Wipe", "Cylon Bounce", "Fire",
            "Rainbow Cycle", "Twinkle Random", "Running Lights",
            "Sparkle", "Snow Sparkle", "Theater Chase", "Theater Chase Rainbow",
            "Strobe", "Bouncing Ball", "Bouncing Colored Ball",
            "Red", "Green", "Blue",
            "Yellow", "Cyan", "Purple", "White"};

    private int modesCount = modeNames.length;

    //Создаём эквивалентный modeNames массив чекбоксов
    private JCheckBox[] chkModesList = new JCheckBox[modeNames.length];

    //Создаём массив содержащий доступные COM-порты и список скоростей подключения к ленте. Добавляем эти данные в выпадающие списки JComboBox
    private String[] comPorts;
    private Integer[] baudRate = {600, 1200, 2400, 4800, 9600, 19200, 28800, 38400, 57600, 115200 };

    //Создаём компоненты в порядке их прорисовки - слева-направо, сверху-вниз
    private JComboBox<String> combCom;                                          //Выпадающий список доступных COM-портов
    private JComboBox<Integer> combBaud = new JComboBox<Integer>(baudRate);     //Выпадающий список доступной скорости подключения
    private JButton btnConnect = new JButton("Connect");                   //Кнопка "Поключить"
    private JButton btnDisconnect = new JButton("Disconnect");             //Кнопка "Отключить"


    private JToggleButton btOnOff = new JToggleButton("OFF");              //Кнопка ВКЛ/ВЫКЛ ленты
    private JLabel lblCurModeName = new JLabel("Quad Bright Cirve");     //Тектовый лейбл с именем текущего режима
    private JLabel lblcurNumOfModes = new JLabel("1/"  + modesCount);                //Текстовый лейбл с порядковым номером текущего режима
    private JButton btPrev = new JButton("P");                           //Кнопка "Предыдущий" режим
    private JButton btPause = new JButton("P");                          //Кнопка "Пауза"
    private JButton btNext = new JButton("N");                           //Кнопка "Следующий" режим
    private JButton btAddToFav = new JButton("F");                       //Кнопка "Favorite" - для добавления режима в роли стартового

    private Box box = Box.createVerticalBox();                                        //Менеджер компоновки "коробка"
    private JScrollPane scrollPane = new JScrollPane(box);                            //Панель прокрутки

    private JCheckBox chkAutoMode = new JCheckBox("Авто");                             //Чекбокс для ВКЛ/ВЫКЛ автоматической смены режима
    private JButton btnColorChooser = new JButton("Цвет");                             //Кнопка для вывода произвольного цвета на экран
    private JLabel lblBright = new JLabel("Яркость");                                  //Лейбл "Яркость"
    private JLabel lblSpeed = new JLabel("Скорость");                                  //Лейбл "Скорость"
    private JSlider brightness = new JSlider(JSlider.HORIZONTAL, 1, 255, 255);     //Слайдер "Яркость"
    private JSlider speed = new JSlider(JSlider.HORIZONTAL, 50, 200, 100);         //Слайдер "Скорость"

    private JTextArea jtxStatus = new JTextArea();                                                 //Текстовое поле с системной информацией
    private JColorChooser jcolchoose = new JColorChooser();


    MainFrame() {
        //Получаем список портов, добавляем его в выпадающий список
        comPorts = SerialPortList.getPortNames();
        combCom = new JComboBox<String>(comPorts);

        //Создаём и настраиваем главное окно
        JFrame frame = new JFrame("WS2812B Controller");
        frame.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

/***********************************ВЕРХНАЯЯ ПАНЕЛЬ ПОДКЛЮЧЕНИЯ****************************************************************/
        //Создаём верхнюю панель (для элементов подключения-отключения), задаём панели новый менеджер компоновки элементов
        JPanel connectPan2 = new JPanel();
        connectPan2.setLayout(new GridBagLayout());

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
        GridBagConstraints contConnect1 = new GridBagConstraints(GridBagConstraints.RELATIVE, 0, 1, 1, 0, 0, GridBagConstraints.WEST, 0, new Insets(10, 25, 10, 0), 0, 0);
        GridBagConstraints contConnect2 = new GridBagConstraints(GridBagConstraints.RELATIVE, 0, 1, 1, 0, 0, GridBagConstraints.WEST, 0, new Insets(10, 15, 10, 0), 0, 0);

        //Задаём размеры компонентов
        combCom.setPreferredSize(new Dimension(80, 25));
        combBaud.setPreferredSize(new Dimension(80, 25));
        btnConnect.setPreferredSize(new Dimension(65, 25));
        btnDisconnect.setPreferredSize(new Dimension(75, 25));

        //Задаём внутренние отступы у кнопки, чтобы сделать её меньше
        btnConnect.setMargin(new Insets(5, 2, 5, 2 ));
        btnDisconnect.setMargin(new Insets(5, 2, 5, 2 ));


        //Задаём выбранный элемент у раскрывающегося списка (сделал для себя, чтобы каждый раз не выбирать необходимую скорость)
        combBaud.setSelectedIndex(9);

        //Меняем цвет кнопки отключения и делаем её неактивной
        btnDisconnect.setBackground(Color.pink);
        btnDisconnect.setEnabled(false);
       // btnDisconnect.setEnabled(false);
       // btnDisconnect.setVisible(false);

        //Добавялем слушателей для кнопок
//        combCom.addActionListener(new ComboComListener());
        btnConnect.addActionListener(new ButtonConnectListener());
        btnDisconnect.addActionListener(new ButtonDisconnectListener());

        //Добавляем компоненты на панель
        connectPan2.add(combCom, contConnect1);
        connectPan2.add(combBaud, contConnect2);
        connectPan2.add(btnConnect, contConnect1);
        connectPan2.add(btnDisconnect, contConnect2);


/******************************ПАНЕЛЬ ЭЛЕМЕНТОВ УПРАВЛЕНИЯ******************************************************************/
        //Создаём две панели и добавляем им менедждеры компонновки, задаём размеры панели - две панели нужны, чтобы при изменении названия режима (а у них разные размеры строки) не скакали элементы управления  - "вперёд", "назад" и т.д.
        JPanel mainControlPan = new JPanel();
        JPanel mainControlPan2 = new JPanel();
        mainControlPan.setLayout(new BoxLayout(mainControlPan, BoxLayout.X_AXIS));
        mainControlPan2.setLayout(new BoxLayout(mainControlPan2, BoxLayout.X_AXIS));

        mainControlPan.setBackground(Color.PINK);              //
        mainControlPan2.setBackground(Color.GREEN);             //
        mainControlPan.setPreferredSize(new Dimension(339, 60));
        mainControlPan2.setPreferredSize(new Dimension(95,60));

        //Задаём размеры кнопки ВКЛ/ВЫКЛ, добавляем слушателя
        btOnOff.setPreferredSize(new Dimension(60, 10));
        btOnOff.addActionListener(new ButtonOnOffListener());
        btPrev.addActionListener(new ButtonPrevListener());
        btPause.addActionListener(new ButtonPauseListener());
        btNext.addActionListener(new ButtonNextListener());
        btAddToFav.addActionListener(new ButtonFavoriteListener());


        //Отключаем прорисовку рамки у кнопок
        btOnOff.setBorderPainted(false);
        btPrev.setBorderPainted(false);
        btPause.setBorderPainted(false);
        btNext.setBorderPainted(false);
        btAddToFav.setBorderPainted(false);

        //Устанавливаем внутренние отступы
        btOnOff.setMargin(new Insets(0, 0, 0, 0));
        btPrev.setMargin(new Insets(0, 0, 0, 0));
        btPause.setMargin(new Insets(0, 0, 0, 0));
        btNext.setMargin(new Insets(0, 0, 0, 0));
        btAddToFav.setMargin(new Insets(0, 0, 0, 0));

        //Отключаем прорисовку фокуса у кнопок
        btOnOff.setFocusPainted(false);
        btPrev.setFocusPainted(false);
        btPause.setFocusPainted(false);
        btNext.setFocusPainted(false);
        btAddToFav.setFocusPainted(false);

        //Добавляем компоненты на панель #1, добавляем отступы
        mainControlPan.add(Box.createHorizontalStrut(15));
        mainControlPan.add(btOnOff);
        mainControlPan.add(Box.createHorizontalStrut(15));
        mainControlPan.add(lblcurNumOfModes);
        mainControlPan.add(Box.createHorizontalStrut(15));
        mainControlPan.add(lblCurModeName);

        //Добавляем компоненты на панель #2, добавляем отступы
        mainControlPan2.add(btPrev);
        mainControlPan2.add(Box.createHorizontalStrut(2));
        mainControlPan2.add(btPause);
        mainControlPan2.add(Box.createHorizontalStrut(2));
        mainControlPan2.add(btNext);
        mainControlPan2.add(Box.createHorizontalStrut(15));
        mainControlPan2.add(btAddToFav);

/**************************Центральная панель**********************************************************************/
        //Инициализируем чекбоксы, добавляя в качестве параметра строки с названиями режимов, тутже добавляем чекбоксы в менеджер компоновки "коробка"
        for (byte i = 0; i < chkModesList.length; i++) {
            chkModesList[i] = new JCheckBox(modeNames[i]);
            chkModesList[i].setFocusPainted(false);
            chkModesList[i].addItemListener(new ModesListItemListener(i));
            box.add(chkModesList[i]);
        }

        //Задаём размеры панели прокрутки
        scrollPane.setPreferredSize(new Dimension(435, 400));


/*****************************НИЖНЯЯ ПАНЕЛЬ**************************************************************/
        //Создаём хэш-таблицу для слайдеров, чтобы сделать их более информативными
        Hashtable<Integer, Component> speedTable = new Hashtable<Integer, Component>();
        speedTable.put(50, new JLabel("0.5x"));
        speedTable.put(100, new JLabel("1x"));
        speedTable.put(150, new JLabel("1.5x"));
        speedTable.put(200, new JLabel("2x"));

        //Тоже самое, только для
        Hashtable<Integer, Component> brightTable = new Hashtable<Integer, Component>();
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

        btnColorChooser.setForeground(Color.WHITE);
        btnColorChooser.setBackground(Color.BLACK);

        //Настраиваем слайдеры ("Главные" шкалы деления и второстепенные, их отображение, задаём хэш-таблицу для прорисовки отдельных значений и её отображение, задаём размеры)
        brightness.setMajorTickSpacing(63);
        brightness.setMinorTickSpacing(21);
        brightness.setPaintTicks(true);
        brightness.setLabelTable(brightTable);
        brightness.setPaintLabels(true);
        brightness.setPreferredSize(new Dimension(160, 65));
//        brightness.addChangeListener(new BrightChangeLister());
        brightness.addMouseListener(new BrightMouseListener());

        speed.setMajorTickSpacing(50);
        speed.setMinorTickSpacing(10);
        speed.setPaintTicks(true);
        speed.setLabelTable(speedTable);
        speed.setPaintLabels(true);
        speed.setPreferredSize(new Dimension(160, 65));


        //Добавляем компоненты на панель
        setUpPanel.add(chkAutoMode, contSettings1);
        setUpPanel.add(btnColorChooser, contSettings2);
        setUpPanel.add(lblBright, contSettings3);
        setUpPanel.add(lblSpeed, contSettings3);
        setUpPanel.add(brightness, contSettings4);
        setUpPanel.add(speed, contSettings4);

        jtxStatus.setPreferredSize(new Dimension(450, 20));
        jtxStatus.setEditable(false);

/**********Настраиваем главное окно, добавляем компоненты и выводим всё на экран*******/
        disableAll();
        frame.setSize(450, 650);
        frame.add(connectPan2);
        frame.add(mainControlPan);
        frame.add(mainControlPan2);
        frame.add(scrollPane);
        frame.add(setUpPanel);
        frame.add(jtxStatus);
        frame.setResizable(false);
        frame.setVisible(true);

        new UIUpdater().execute();
    }

//    /*********Слушатель нажатия на список COM-портов***********/
//    class ComboComListener implements ActionListener{
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            System.out.println("COM are updated!");
//            //Обновляем список доступных COM-портов для подключения (раньше это было возможно только после перезагрузки приложения)
//            comPorts = SerialPortList.getPortNames();
//            combCom = new JComboBox<String>(comPorts);
//        }
//    }

    /*************Слушатель кнопки подключения*************/
    class ButtonConnectListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            btnConnect.setEnabled(false);
            combCom.setEnabled(false);
            combBaud.setEnabled(false);

            Main.openPort(combCom.getSelectedItem().toString(), (Integer) combBaud.getSelectedItem());
            System.out.println(Main.com + " " + Main.baudRate);

            TryToConnect ttc = new TryToConnect();
            ttc.execute();

        }
    }

    /*************Слушатель кнопки отключения*************/
    class ButtonDisconnectListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Main.disconnect();
            btnConnect.setEnabled(true);
            combCom.setEnabled(true);
            combBaud.setEnabled(true);
            btnDisconnect.setEnabled(false);
            disableAll();

            jtxStatus.setText("Отключено!");
            jtxStatus.setBackground(Color.WHITE);
        }
    }

    /************Слушатель кнопки ВКЛ/ВЫКЛ*****************/
    class ButtonOnOffListener implements ActionListener{
    @Override
    public void actionPerformed(ActionEvent e) {
        jtxStatus.setBackground(Color.WHITE);
        Main.sendData(ON_OFF);
     // new SendData(ON_OFF).execute();

//        AbstractButton abstractButton = (AbstractButton) e.getSource();
//        boolean selected = abstractButton.getModel().isSelected();
//        if(selected){
//            enableAll();
//        }
//        else {
//            disableAll();
//        }
    }
}

    /**********Слушатель для кнопки "Назад"************/
    class ButtonPrevListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            jtxStatus.setBackground(Color.WHITE);

            Main.sendData(PREV);
           // new SendData(PREV).execute();
        }
    }

    /**********Слушатель для кнопки "Пауза"************/
    class ButtonPauseListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            jtxStatus.setBackground(Color.WHITE);
            Main.sendData(PAUSE);
//            new SendData(NEXT).execute();
        }
    }

    /**********Слушатель для кнопки "Вперёд"************/
    class ButtonNextListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            jtxStatus.setBackground(Color.WHITE);
            Main.sendData(NEXT);
//            new SendData(NEXT).execute();
        }
    }

    /**********Слушатель для кнопки "Вперёд"************/
    class ButtonFavoriteListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            jtxStatus.setBackground(Color.WHITE);
            Main.sendData(FAV, Main.ledMode);
//            new SendData(NEXT).execute();
        }
    }

    /*********Слушатель для слайдера яркости(реализован через ивенты мыши)*********/
    class BrightMouseListener implements MouseListener{
        @Override
        public void mouseEntered(MouseEvent e) {

        }
        @Override
        public void mouseClicked(MouseEvent e){

        }
        @Override
        public void mouseReleased(MouseEvent e){
            Main.maxBright = brightness.getValue();

            System.out.println("Prepare to change brightness to " + Main.maxBright);
//            new SendData(SET_BRIGHT, Main.maxBright).execute();
            Main.sendData(SET_BRIGHT, Main.maxBright);
        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        public void mousePressed(MouseEvent e) {

        }
    }

    /*********Слушатель для изменения списка режимов*********/
    class ModesListItemListener implements ItemListener{
        //Переменная индекса чекбоса, значения активности режима (1 - добавить в список/активен, 0 - удалить из списка/деактивирован
        byte index;
        byte actDeactValue;

        //Конструктор принимает в качестве параметра индекс чекбокса, который вызвал ивент, в зависимости от текущего состояния чекбокса формируется код для отправки
        ModesListItemListener(byte index){
     //       if(readyActDeactMode) {
                this.index = index;
            }
      //  }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if(readyActDeactMode) {
                if (chkModesList[index].isSelected()) {
                    actDeactValue = 1;
                } else {
                    actDeactValue = 0;
                }
                Main.sendData(ACT_DEACT_MODE, index, actDeactValue);
                System.out.println("Режим " + modeNames[index] + "(" + index + ") " + "Установлен в значение - " + chkModesList[index].isSelected());
            }
        }
    }

    class ColorChooser extends AbstractColorChooserPanel{
        @Override
        public void buildChooser(){

        }
        @Override
        public void updateChooser(){

        }
        @Override
        public Icon getSmallDisplayIcon(){
            return null;
        }
        @Override
        public Icon getLargeDisplayIcon(){
            return null;
        }
        @Override
        public String getDisplayName(){
            return null;
        }




    }

    /********Фоновый поток для обновления данных GUI***********/
    class UIUpdater extends SwingWorker<Void, Integer>{
        @Override
        protected Void doInBackground() {
            while(true) {
                if (Main.isRecponceRecived) {
                    Main.isRecponceRecived = false;
                    syncGUI(Main.returnRecivedData());
                }
                try {
                    Thread.sleep(100);
                }catch (Exception e){
                    System.out.println(e);
                }
            }

        }


    }

    /*************Отдельный поток для работы с подключением к клиенту*************/
    class TryToConnect extends SwingWorker<Boolean, Integer>{
        int i = 0;

        @Override
        public Boolean doInBackground() throws Exception {
            Main.tryToConnect();
            Thread.sleep(200);
            for (i = 0; i < 10; i++){
                    publish(i);             //методом publish() можно передавать промежуточные результаты работы в метод process()
                if (Main.isConnected){
                    return true;
                }
                Main.tryToConnect();

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
           try{
               result = this.get();   //спомощью метода get() можно узнать, какое значение было возвращено методом doInBackground()
               if(result){
                   enableAll();
                   initializeGUI();
               }
               else {
                   Main.disconnect();

                   combCom.setEnabled(true);
                   combBaud.setEnabled(true);
                   btnConnect.setEnabled(true);
                   jtxStatus.setText("Ошибка подключения!");
                   jtxStatus.setBackground(Color.RED);
               }

           } catch(InterruptedException e) {
            } catch(ExecutionException e) {
            }

            Main.clearData();
    }
    }

    /**************Отдельный поток для отправки команд и обновления ГПИ, в случае успешного изменения параметров**************/
    class SendData extends SwingWorker<Boolean, Integer>{
        int[] dataToSend;

        private SendData(int...a){
            dataToSend = a;
            Main.sendData(dataToSend);
        }

        @Override
        protected Boolean doInBackground() throws Exception{
        Thread.sleep(200);
            for(int i = 0; i < 3; i++){

                if(Main.isRecponceRecived){
                    return true;
                    //Thread.sleep(100);
//                    int recived[] = Main.returnRecivedData();
//                    if(recived[0] > 0) {
//                        //Main.isRecponceRecived = false;  //Сбрасываем флаг принятых данных
//
                    }
                else {
                    Thread.sleep(500);

                }
            }
            return false;
        }

        @Override
        protected void process(List<Integer> chunks) {
            jtxStatus.setText("Попытка отправки данных №" + chunks.get(0));          //метод get(), в данном случае, возвращает то, что мы передали с помощью publish()
            jtxStatus.setBackground(Color.WHITE);
        }

        @Override
        protected void done() {

            boolean result;
            try{
                result = this.get();   //спомощью метода get() можно узнать, какое значение было возвращено методом doInBackground()
                if(result){
                    //Здесь запускаем метод, в котором обрабатывается ответ ардуины и синхронизиурется ГПИ
                    syncGUI(Main.returnRecivedData());
                }
                else {
                    jtxStatus.setText("Ошибка отправки данных!");
                }

            } catch(InterruptedException e) {
            } catch(ExecutionException e) {
            }

            Main.clearData();
        }
    }

    /*******Метод, который обновляет ГПИ в соответствии с актуальным состоянием ленты (переданным от арудины)************/
    private void syncGUI(int[] recivedData) throws ArrayIndexOutOfBoundsException{
        try {
            switch (recivedData[0]) {
                case ON_OFF:
                    if (recivedData[1] == 0) {
                        jtxStatus.setText("Лента выключена!");
                        btOnOff.setSelected(false);
                        disableAll();
                    } else {
                        jtxStatus.setText("Лента включена!");
                        btOnOff.setSelected(true);
                        enableAll();
                    }
                    break;

                case PREV:
                case NEXT:
                    Main.ledMode = recivedData[1];
                    Main.thisdelay = recivedData[2];
                    lblCurModeName.setText(modeNames[Main.ledMode - 1]);
                    lblcurNumOfModes.setText(Main.ledMode + "/" + modesCount);

                    if (Main.thisdelay == 0) {
                        speed.setEnabled(false);
                    } else {
                        speed.setEnabled(true);
                    }
                    jtxStatus.setText("Режим № " + Main.ledMode + " - " + modeNames[Main.ledMode - 1]);
                    break;

                case PAUSE:
                    if (recivedData[1] == 1) {
                        jtxStatus.setText("Пауза");
                        brightness.setEnabled(false);
                        speed.setEnabled(false);
                    } else {
                        jtxStatus.setText("Текущий режим " + Main.ledMode);
                        if (Main.thisdelay == 0) {
                            speed.setEnabled(false);
                        } else {
                            speed.setEnabled(true);
                        }
                        brightness.setEnabled(true);
                    }
                    break;
                case FAV:
                    jtxStatus.setText("Режим " + modeNames[Main.ledMode - 1] + "(" + Main.ledMode + ")" + " установлен стартовым режимом");
                    break;
                case ACT_DEACT_MODE:
                    boolean actDeactResult;

                    if (recivedData[2] == 1) {
                        actDeactResult = true;
                    } else {
                        actDeactResult = false;
                    }
                    jtxStatus.setText("Режим " + modeNames[recivedData[1]] + "(" + recivedData[1] + ") " + "Установлен в значение - " + actDeactResult);
                    ;
                    break;
                case SET_AUTO:
                    break;
                case SET_COLOR:
                    break;
                case SET_BRIGHT:
                    Main.maxBright = recivedData[1];
                    brightness.setValue(Main.maxBright);
                    jtxStatus.setText("Яркость установлена в значение " + Main.maxBright + " единиц");

                    break;
                case SET_SPEED:
                    break;
                case SAVE_SETTINGS:
                    break;
            }
        }catch (ArrayIndexOutOfBoundsException ae){
            System.out.println(ae);
        }

    }

    /*******Инициализация ГПИ начальными значениями, полученными от дуины********/
    private void initializeGUI(){
        jtxStatus.setText("Подключено к " + Main.com + " на скорости " + Main.baudRate);
        jtxStatus.setBackground(Color.GREEN);

        if (Main.ledMode > 0) {
            btOnOff.setSelected(true);
        }
        else{
            btOnOff.setSelected(false);
        }

            lblCurModeName.setText(modeNames[Main.ledMode - 1]);
            lblcurNumOfModes.setText(Main.ledMode + "/" + modesCount);



        if(Main.autoMode == 1){
            chkAutoMode.setSelected(true);
        }
        brightness.setValue(Main.maxBright);
        for(int i = 0; i < modesCount; i++){
            if(Main.ledModes[i] == 1){
                chkModesList[i].setSelected(true);
            }
        }
        readyActDeactMode = true;
    }

    /*****Метод для отключения всех компонентов при первом запуске и при нажатии на кнопку "ВЫКЛ"***/
    private void disableAll(){
        if (!Main.isConnected) {
            btOnOff.setEnabled(false);
        }
        lblCurModeName.setEnabled(false);
        lblcurNumOfModes.setEnabled(false);
        btPrev.setEnabled(false);
        btPause.setEnabled(false);
        btNext.setEnabled(false);
        btAddToFav.setEnabled(false);
        chkAutoMode.setEnabled(false);
        btnColorChooser.setEnabled(false);
        lblBright.setEnabled(false);
        lblSpeed.setEnabled(false);
        brightness.setEnabled(false);
        speed.setEnabled(false);

        readyActDeactMode = false;
        for (int i = 0; i < modesCount; i++){
            chkModesList[i].setEnabled(false);
        }

    }

    /*****Метод для включения всех компонентов при удачном получении настроек и при нажатии на кнопку "ВКЛ"***/
    private void enableAll(){
        btnDisconnect.setEnabled(true);
        btOnOff.setEnabled(true);
        lblCurModeName.setEnabled(true);
        lblcurNumOfModes.setEnabled(true);
        btPrev.setEnabled(true);
        btPause.setEnabled(true);
        btNext.setEnabled(true);
        btAddToFav.setEnabled(true);
        chkAutoMode.setEnabled(true);
        btnColorChooser.setEnabled(true);
        lblBright.setEnabled(true);
        lblSpeed.setEnabled(true);
        brightness.setEnabled(true);
        speed.setEnabled(true);

        for (int i = 0; i < modesCount; i++){
            chkModesList[i].setEnabled(true);
        }

    }

}

