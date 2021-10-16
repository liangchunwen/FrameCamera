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
import com.frame.camera.utils.FileUtils;

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
        private static final String PRE_PRETRAN_KEY = "pre_transcription_key";
        private static final String PROTEIN_KEY = "protein_key";
        private static final String FILE_PATH_KEY = "file_path_key";
        private ListPreference preTranListPre;
        private ListPreference proteinListPre;
        private ListPreference filePathListPre;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            preTranListPre = findPreference(PRE_PRETRAN_KEY);
            if (preTranListPre != null) {
                String val = MyApplication.mSharedPreferences.getString("pre_transcription_values", "0");
                Log.d(TAG, "pre-val: " + val);
                preTranListPre.setValue(val);
                preTranListPre.setOnPreferenceChangeListener(this);
            }
            proteinListPre = findPreference(PROTEIN_KEY);
            if (proteinListPre != null) {
                String val = MyApplication.mSharedPreferences.getString("protein_values", "0");
                Log.d(TAG, "pro-val: " + val);
                proteinListPre.setValue(val);
                proteinListPre.setOnPreferenceChangeListener(this);
            }
            filePathListPre = findPreference(FILE_PATH_KEY);
            if (filePathListPre != null) {
                if (FileUtils.getRootStorageDir(1) == null) {
                    filePathListPre.setEnabled(false);
                    String val = MyApplication.mSharedPreferences.getString("file_path_values", "0");
                    Log.d(TAG, "path-val: " + val);
                    filePathListPre.setValue(val);
                } else {
                    String val = MyApplication.mSharedPreferences.getString("file_path_values", "0");
                    Log.d(TAG, "path-val: " + val);
                    filePathListPre.setValue(val);
                    filePathListPre.setOnPreferenceChangeListener(this);
                }
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String value = (String) newValue;
            Log.d(TAG, "onPreferenceChange-value: " + value);
            switch (preference.getKey()) {
                case PRE_PRETRAN_KEY:
                    preTranListPre.setValue(value);
                    MyApplication.mEditor.putString("pre_transcription_values", value).apply();
                    break;
                case PROTEIN_KEY:
                    proteinListPre.setValue(value);
                    MyApplication.mEditor.putString("protein_values", value).apply();
                    break;
                case FILE_PATH_KEY:
                    filePathListPre.setValue(value);
                    MyApplication.mEditor.putString("file_path_values", value).apply();
                    break;
            }

            return false;
        }
    }
}