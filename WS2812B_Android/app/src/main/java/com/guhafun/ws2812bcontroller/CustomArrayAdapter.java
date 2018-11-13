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
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.example.ws2812bcontroller.R;

import java.util.Arrays;


//Кастомный ArrayAdapter для работы со списком режимов
class CustomArrayAdapter extends ArrayAdapter<String> {
    private String TAG = "ConLog";
    private final Context context;
    private byte[] activeModes = null;
    private String[] names;
    private int colorActive;
    private byte currentMode;

    LayoutInflater mInflater;

    public CustomArrayAdapter(Activity context, String[] names, byte[] activeModes) {
        super(context, android.R.layout.simple_list_item_multiple_choice, names);
        this.context = context;
        this.names = names;

        //Получаем номер текущего режима
        currentMode = activeModes[1];

        //Копируем из принятых данных только часть массива, содержащую информацию о включенных/выключенных режимах
        this.activeModes = Arrays.copyOfRange(activeModes, 5, activeModes.length);
        mInflater = context.getLayoutInflater();

        //Получаем цвет фона для активированного режима
      colorActive = context.getResources().getColor(R.color.colorActiveMode);

    }

    static class ViewHolder {
        CheckedTextView mCheckedTextView;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null){
            convertView = mInflater.inflate(android.R.layout.simple_list_item_multiple_choice, null);

            holder = new ViewHolder();
            holder.mCheckedTextView = convertView.findViewById(android.R.id.text1);


            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.mCheckedTextView.setText(names[position]);

        if (currentMode - 1 == position){
            holder.mCheckedTextView.setBackgroundColor(colorActive);
            holder.mCheckedTextView.setTextColor(Color.WHITE);

        }
        else {
            holder.mCheckedTextView.setBackgroundColor(Color.WHITE);
            holder.mCheckedTextView.setTextColor(Color.BLACK);
        }

//        if (activeModes[position + 5] == 1) {
        if (activeModes[position] == 1) {
            ((ListView) parent).setItemChecked(position, true);
        }
        else {
            ((ListView) parent).setItemChecked(position, false);
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
