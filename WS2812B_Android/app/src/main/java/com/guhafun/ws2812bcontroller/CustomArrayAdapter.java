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
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TabHost;
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
        mInflater = context.getLayoutInflater();

        //Получаем цвет фона для активированного режима
      colorActive = context.getResources().getColor(R.color.colorActiveMode);
      colorDeactive = context.getResources().getColor(R.color.colorDeactiveMode);

    }

    static class ViewHolder {
        TextView mText;
        CheckBox mCheckBox;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null){
            convertView = mInflater.inflate(R.layout.list_item_multiple_choice, null);

            holder = new ViewHolder();
            holder.mText = convertView.findViewById(R.id.listModeName);
            holder.mCheckBox = convertView.findViewById(R.id.listCheckbox);

            convertView.setTag(holder);

            holder.mText.setTag(position);
            holder.mCheckBox.setTag(position);

            holder.mText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   int position = (Integer)v.getTag();

                   TextView txt = v.findViewById(R.id.listModeName);
                   txt.setText("Клик на элементе списка: " + position);

                }
            });

            holder.mCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (Integer)v.getTag();
                    CheckBox cbx = v.findViewById(R.id.listCheckbox);

                    byte state = (cbx.isChecked()) ? (byte) 1 : 0;

                    Log.d(TAG, "position: " + position + ", state: " + state);

                    mCommander.actDeactMode((byte)position, state);

                }
            });


        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        //Обновляем тег
        holder.mText.setTag(position);
        holder.mCheckBox.setTag(position);

        holder.mText.setText(names[position]);


        if (currentMode - 1 == position){
            convertView.setBackgroundColor(colorActive);

            holder.mText.setTextColor(Color.WHITE);
            holder.mCheckBox.setTextColor(Color.WHITE);
        }
        else {
//            convertView.setBackgroundColor(Color.WHITE);
            convertView.setBackgroundColor(colorDeactive);

            holder.mText.setTextColor(Color.BLACK);
            holder.mCheckBox.setTextColor(Color.BLACK);
        }


        if (activeModes[position] == 1) {
//            ((ListView) parent).setItemChecked(position, true);
//             CheckBox v = parent.findViewById(R.id.listCheckbox);
//             v.setChecked(true);
            holder.mCheckBox.setChecked(true);

        }
        else {
//            ((ListView) parent).setItemChecked(position, false);
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

    public byte getCurrentMode() {
        return currentMode;
    }


}
