package com.frame.camera.activity;

import android.os.Bundle;
import android.widget.Toast;

import com.frame.camera.R;
import androidx.navigation.Navigation;
import com.frame.camera.databinding.ActivityCameraBinding;
import com.frame.camera.utils.PermissionsUtils;

public class CameraActivity extends BaseActivity {
    private static final String TAG = "CameraActivity:CAMERA";
    private ActivityCameraBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (PermissionsUtils.checkCameraPermission(this)
                && PermissionsUtils.checkVideoRecordPermission(this)) {
            Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        } else {
            Toast.makeText(this, R.string.without_camera_permission, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}