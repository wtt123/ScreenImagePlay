package com.test.screenimageplay;

import android.app.Application;

import com.test.screenimageplay.utils.SupportMultipleScreensUtil;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;
import com.wt.screenimage_lib.ScreenImageController;


/**
 * Created by wt on 2018/5/28.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SupportMultipleScreensUtil.init(this);
        ZXingLibrary.initDisplayOpinion(this);
        initScreenImage();
    }

    private void initScreenImage() {

    }
}
