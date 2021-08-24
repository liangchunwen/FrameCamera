package com.frame.camera.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.frame.camera.databinding.FragmentCameraBinding;
import com.otaliastudios.cameraview.CameraView;

public class CameraFragment extends Fragment {
    private static final String TAG = "CameraFragment:CAMERA";
    private FragmentCameraBinding binding;
    private CameraView mCameraView;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentCameraBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCameraView = binding.cameraView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCameraView.open();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCameraView.close();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCameraView.destroy();
        binding = null;
    }

}