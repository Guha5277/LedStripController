package com.guhafun.ws2812bcontroller;

import android.app.Activity;
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
    private final Activity context;
    // private byte[] mData = null;
    private String[] names;

    public CustomArrayAdapter(Activity context, byte[] data, String[] names) {
        super(context, android.R.layout.simple_list_item_multiple_choice, names);
        this.context = context;
//        if (data != null) {
//            mData = data;
//        }
        this.names = names;
    }

    public CustomArrayAdapter(Activity context, String[] names) {
        super(context, android.R.layout.simple_list_item_multiple_choice, names);
        //ArrayAdapter.createFromResource(context, R.array.mode_list, android.R.layout.simple_list_item_multiple_choice);
        this.context = context;
        this.names = names;
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        CheckedTextView checkbox;
        View tempView = convertView;
        if(tempView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            tempView = inflater.inflate(android.R.layout.simple_list_item_multiple_choice, null, true);
            checkbox = tempView.findViewById(android.R.id.text1);
            CharSequence[] chars = names;
            checkbox.setText(chars[position]);
            tempView.setTag(checkbox);

        } else {
            checkbox = (CheckedTextView) tempView.getTag();
        }

        if(ControlActivity.data != null) {
            Log.d(TAG, "MyArrayAdapter position: " + position);
            if (ControlActivity.data[position + 4] == 1) {
                // checkbox.setChecked(true);
                ((ListView)parent).setItemChecked((position + 4), true);
            } else {
                // checkbox.setChecked(false);
                ((ListView)parent).setItemChecked((position + 4), false);
            }
        }

        return tempView;
    }
}
