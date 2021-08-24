package com.frame.camera.activity;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.frame.camera.utils.SystemBarUtils;

/**
 * Created by ${liangcw} on 2021/08/12.
 */

public  abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSystemUIChangeListener();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //焦点改变的时候，当Home键退出，重新从新进入等情况的处理。
        SystemBarUtils.setStickyStyle(getWindow());
    }

    /**
     * 监听系统UI的显示，进行特殊处理
     */
    private void setSystemUIChangeListener() {
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            //当系统UI显示的时候（例如输入法显示的时候），再次隐藏
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                SystemBarUtils.setStickyStyle(getWindow());
            }
        });
    }
}
