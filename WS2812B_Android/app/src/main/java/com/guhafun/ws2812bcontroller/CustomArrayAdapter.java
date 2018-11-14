package com.guhafun.ws2812bcontroller;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.ws2812bcontroller.R;

import java.util.Arrays;


//Кастомный ArrayAdapter для работы со списком режимов
class CustomArrayAdapter extends ArrayAdapter<String> {
    private String TAG = "ConLog";
    private final Context context;
    private byte[] activeModes = null;
    private String[] names;
    private int colorActive, colorDeactive;
    private byte currentMode;
    private Commander mCommander;

    LayoutInflater mInflater;

    public CustomArrayAdapter(Activity context, String[] names, byte[] activeModes, Commander commander) {
        super(context, R.layout.list_item_multiple_choice, names);
        this.context = context;
        this.names = names;
        mCommander = commander;

        //Получаем номер текущего режима
        currentMode = activeModes[1];

        //Копируем из принятых данных только часть массива, содержащую информацию о включенных/выключенных режимах
        this.activeModes = Arrays.copyOfRange(activeModes, 5, activeModes.length);

        //Получаем вызываемый контекст
        mInflater = context.getLayoutInflater();

        //Получаем цвет фона для активированного режима
      colorActive = context.getResources().getColor(R.color.colorActiveMode);
      colorDeactive = context.getResources().getColor(R.color.colorDeactiveMode);

    }

    //Класс хранящий составные элементы строки ListView
    static class ViewHolder {
        TextView mText;
        CheckBox mCheckBox;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        //Срабатывает только при первой инициализации ListView-элемента
        if (convertView == null){
            convertView = mInflater.inflate(R.layout.list_item_multiple_choice, null);

            //Инициализиурем объект Holder, присваиваем элементам ссылки на view-элементы
            holder = new ViewHolder();
            holder.mText = convertView.findViewById(R.id.listModeName);
            holder.mCheckBox = convertView.findViewById(R.id.listCheckbox);

            //Сохраняем объект Holder для последующего использования
            convertView.setTag(holder);

            //Добавляем теги с позицией для текущих элементов
            holder.mText.setTag(position);
            holder.mCheckBox.setTag(position);

            //Добавляем слушателя для текста
            holder.mText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Получаем позицию элемента в списке
                   int position = (Integer)v.getTag();

                  // TextView txt = v.findViewById(R.id.listModeName);

                    //Проверяем, происходит ли нажатие не на текущий воспроизводимый элемент (чтобы исключить лишнюю работу) и отправляем сообщение
                   if (currentMode - 1  != position) {
                       mCommander.setModeTo((byte) position);
                   }
                }
            });

            //Добавляем слушателя для CheckBox'a
            holder.mCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Получаем позицию элемента в списке
                    int position = (Integer)v.getTag();
                    //Получаем CheckBox
                    CheckBox cbx = v.findViewById(R.id.listCheckbox);

                    //Отправляем сообщение
                    mCommander.actDeactMode((byte)position, cbx.isChecked());

                }
            });


        }
        //Вызывается, если ListView элемент уже был инициализирован
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        //Обновляем позиции элементов
        holder.mText.setTag(position);
        holder.mCheckBox.setTag(position);

        //Задаем текст
        holder.mText.setText(names[position]);

        //Если текущий элемент вопспроизводится
        if (currentMode - 1 == position){
            //Устанавливаем соответствующие цвета для
            convertView.setBackgroundColor(colorActive);
            holder.mText.setTextColor(Color.WHITE);
            holder.mCheckBox.setTextColor(Color.WHITE);
        }
        //Если нет
        else {
            //Устанавливаем соответствующие цвета
            convertView.setBackgroundColor(colorDeactive);
            holder.mText.setTextColor(Color.BLACK);
            holder.mCheckBox.setTextColor(Color.BLACK);
        }

        //Если текущие ListView - элемент включен в плейлист то установить галку в CheckBox
        if (activeModes[position] == 1) {
            holder.mCheckBox.setChecked(true);

        }
        //... Иначе - снять, соответственно
        else {
            holder.mCheckBox.setChecked(false);
        }

        return convertView;
    }

    //Метод для установки текущего режима
    public void setCurrentMode(byte mode){
        currentMode = mode;
        notifyDataSetChanged();
    }

    //Метод для обновления текущего состояния режима (включен или исключен из плейлиста)
    public void setActiveModes(byte index, byte state){
        activeModes[index] = state;
    }

    //Метод возвращает текущйи режим
    public byte getCurrentMode() {
        return currentMode;
    }
}
