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


//Кастомный ArrayAdapter для работы со списком режимов
class CustomArrayAdapter extends ArrayAdapter<String> {
    private String TAG = "ConLog";
    private final Context context;
    private byte[] activeModes = null;
    private String[] names;
    private int colorActive;

    LayoutInflater mInflater;

    public CustomArrayAdapter(Activity context, String[] names, byte[] activeModes) {
        super(context, android.R.layout.simple_list_item_multiple_choice, names);
        this.context = context;
        this.names = names;
        this.activeModes = activeModes;
        mInflater = context.getLayoutInflater();

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

        if (activeModes[0] - 1 == position){
            holder.mCheckedTextView.setBackgroundColor(colorActive);
            holder.mCheckedTextView.setTextColor(Color.WHITE);

        }
        else {
            holder.mCheckedTextView.setBackgroundColor(Color.WHITE);
            holder.mCheckedTextView.setTextColor(Color.BLACK);
        }

        if (activeModes[position + 5] == 1) {
            ((ListView) parent).setItemChecked(position, true);
        }
        else {
            ((ListView) parent).setItemChecked(position, false);
        }
        return convertView;
    }
}
