package com.guhafun.ws2812bcontroller;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.ws2812bcontroller.R;

public class AppPreference extends PreferenceActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferenceFragment()).commit();

    }

    public static class PreferenceFragment extends android.preference.PreferenceFragment {
        CheckBoxPreference autoMode, autoSave;
        ListPreference autoDuration, saveDuration;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference);
//
//            autoMode = (CheckBoxPreference) findPreference("auto_state");
//            autoSave = (CheckBoxPreference) findPreference("autosave_state");
//
//            autoDuration = (ListPreference) findPreference("auto_duration");
//            saveDuration = (ListPreference) findPreference("autosave_duration");
//
//            boolean autoModeSate = getPreferenceScreen().getSharedPreferences().getBoolean("auto_state", false);
//            boolean autoSaveState = getPreferenceScreen().getSharedPreferences().getBoolean("autosave_state", false);
//
//            autoDuration.setEnabled(autoModeSate);
//            saveDuration.setEnabled(autoSaveState);
//
//            autoMode.setOnPreferenceChangeListener(changeListener);
//            autoSave.setOnPreferenceChangeListener(changeListener);
          }

//        Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//               if ( preference.getKey().equals("auto_state") ){
//                   autoDuration.setEnabled(preference.isEnabled());
//               }
//               else if ( preference.getKey().equals("autosave_state") ){
//                   autoDuration.setEnabled(preference.isEnabled());
//               }
//
//                return true;
//            }
//        };

        }
    }





