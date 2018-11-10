package com.guhafun.ws2812bcontroller;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;


//Кастомный ArrayAdapter для работы со списком режимов
class CustomArrayAdapter extends ArrayAdapter<String> {
    private String TAG = "ConLog";
    private final Context context;
    private byte[] activeModes = null;
    private String[] names;

    LayoutInflater mInflater;

    public CustomArrayAdapter(Activity context, String[] names, byte[] activeModes) {
        super(context, android.R.layout.simple_list_item_multiple_choice, names);
        this.context = context;
        this.names = names;
        this.activeModes = activeModes;
        mInflater = context.getLayoutInflater();
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

        if (activeModes[position + 4] == 1) {
            ((ListView) parent).setItemChecked(position, true);
        }
        else {
            ((ListView) parent).setItemChecked(position, false);
        }
        return convertView;
    }

    //    @NonNull
//    @Override
//    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
//
//        CheckedTextView checkbox;
//        View tempView = convertView;
//        if(tempView == null) {
//            LayoutInflater inflater = context.getLayoutInflater();
//            tempView = inflater.inflate(android.R.layout.simple_list_item_multiple_choice, null, true);
//            checkbox = tempView.findViewById(android.R.id.text1);
//            checkbox.setText(chars[position]);
//            tempView.setTag(checkbox);
//
//        } else {
//            checkbox = (CheckedTextView) tempView.getTag();
//        }
//
//        if(ControlActivity.data != null) {
//            Log.d(TAG, "MyArrayAdapter position: " + position);
//            if (ControlActivity.data[position + 4] == 1) {
//                // checkbox.setChecked(true);
//                ((ListView)parent).setItemChecked((position + 4), true);
//            } else {
//                // checkbox.setChecked(false);
//                ((ListView)parent).setItemChecked((position + 4), false);
//            }
//        }
//
//        return tempView;
//    }
}
