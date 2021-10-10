package com.frame.camera.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.Preference.OnPreferenceChangeListener;
import com.frame.camera.R;
import com.frame.camera.application.MyApplication;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity:CAMERA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //监听左上角的返回箭头
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements OnPreferenceChangeListener{
        private static final String KEY_PRETRAN = "pre_transcription";
        private static final String KEY_PROTEIN = "protein";
        private ListPreference preTranListPre;
        private ListPreference proteinListPre;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            preTranListPre = findPreference(KEY_PRETRAN);
            if (preTranListPre != null) {
                String val = MyApplication.mSharedPreferences.getString("pre_transcription_values", "0");
                Log.d(TAG, "pre-val: " + val);
                preTranListPre.setValue(val);
                preTranListPre.setOnPreferenceChangeListener(this);
            }
            proteinListPre = findPreference(KEY_PROTEIN);
            if (proteinListPre != null) {
                String val = MyApplication.mSharedPreferences.getString("protein_values", "0");
                Log.d(TAG, "pro-val: " + val);
                proteinListPre.setValue(val);
                proteinListPre.setOnPreferenceChangeListener(this);
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String value = (String) newValue;
            Log.d(TAG, "onPreferenceChange-value: " + value);
            if (preference.getKey().equals(KEY_PRETRAN)) {
                preTranListPre.setValue(value);
                MyApplication.mEditor.putString("pre_transcription_values", value).apply();
            } else if (preference.getKey().equals(KEY_PROTEIN)) {
                proteinListPre.setValue(value);
                MyApplication.mEditor.putString("protein_values", value).apply();
            }

            return false;
        }
    }
}