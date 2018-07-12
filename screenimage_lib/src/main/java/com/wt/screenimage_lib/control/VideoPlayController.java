package com.wt.screenimage_lib.control;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.SurfaceHolder;

import com.wt.screenimage_lib.ScreenImageController;
import com.wt.screenimage_lib.decode.DecodeThread;
import com.wt.screenimage_lib.decode.play.VideoPlay;
import com.wt.screenimage_lib.entity.Frame;
import com.wt.screenimage_lib.mediacodec.VIdeoMediaCodec;
import com.wt.screenimage_lib.server.tcp.NormalPlayQueue;
import com.wt.screenimage_lib.server.tcp.interf.OnAcceptBuffListener;

/**
 * Created by wt
 * Date on  2018/7/3 18:59:52.
 *
 * @Desc
 */

public class VideoPlayController implements OnAcceptBuffListener {
    private static final String TAG = "VideoPlayController";
    private VIdeoMediaCodec videoMediaCodec;
    private DecodeThread mDecodeThread;
    private NormalPlayQueue mPlayqueue;

    public VideoPlayController() {
        mPlayqueue = new NormalPlayQueue();
        ScreenImageController.getInstance().setOnAcceptBuffListener(this);
    }

    // TODO: 2018/7/5 开始解码
    public void surfaceCreate(SurfaceHolder holder) {
        //初始化解码器
        Log.e(TAG, "initial play queue");
        videoMediaCodec = new VIdeoMediaCodec(holder);
        //开启解码线程
        mDecodeThread = new DecodeThread(videoMediaCodec.getCodec(), mPlayqueue);
        videoMediaCodec.start();
        mDecodeThread.start();
    }


    public void surfaceDestrory() {
        mPlayqueue.stop();
        mDecodeThread.shutdown();
    }

    public void stop() {
        mPlayqueue.stop();
        mDecodeThread.shutdown();
    }

    @Override
    public void acceptBuff(Frame frame) {
        mPlayqueue.putByte(frame);
    }
}
