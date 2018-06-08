package com.test.screenimageplay.server;

import android.util.Log;


import com.test.screenimageplay.entity.Frame;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by wt
 * Date on  2018/6/4
 * 缓存区
 * @Desc
 */

public class NormalPlayQueue {
    private ArrayBlockingQueue<Frame> mPlayQueue;
    private String TAG = "NormalPlayQueue";
    private static final int NORMAL_FRAME_BUFFER_SIZE = 800; //缓存区大小

    public NormalPlayQueue() {
        mPlayQueue = new ArrayBlockingQueue<Frame>(NORMAL_FRAME_BUFFER_SIZE, true);
    }


    public Frame takeByte() {
        try {
            if (mPlayQueue.size() >= NORMAL_FRAME_BUFFER_SIZE) {
                Log.e(TAG, "too much frame in NormalPlayQueue" + mPlayQueue.size());
            }
            return mPlayQueue.take();
        } catch (InterruptedException e) {
            Log.e(TAG, "take bytes exception" + e.toString());
            return null;
        }
    }

    public void putByte(Frame frame) {
        try {
            mPlayQueue.put(frame);
        } catch (InterruptedException e) {
            Log.e(TAG, "put bytes exception" + e.toString());
        }
    }

    public void stop() {
        if (mPlayQueue != null) {
            mPlayQueue.clear();
        }
    }

}
