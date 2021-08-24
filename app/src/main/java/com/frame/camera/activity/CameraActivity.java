package com.frame.camera.activity;

import android.os.Bundle;

import com.frame.camera.R;
import androidx.navigation.Navigation;
import com.frame.camera.databinding.ActivityCameraBinding;

public class CameraActivity extends BaseActivity {
    private static final String TAG = "CameraActivity:CAMERA";
    private ActivityCameraBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}