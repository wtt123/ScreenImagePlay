package com.wt.screenimage_lib;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import com.wt.screenimage_lib.server.tcp.TcpServer;
import com.wt.screenimage_lib.server.tcp.interf.OnAcceptBuffListener;
import com.wt.screenimage_lib.server.tcp.interf.OnServerStateChangeListener;

import java.util.ArrayList;


/**
 * Created by xu.wang
 * Date on  2018/7/3 18:49:43.
 *
 * @Desc 录屏控制类
 */

public class ScreenImageController {
    protected Handler mHandler;
    protected Context mContext;
    protected TcpServer tcpServer;
    private static ScreenImageController mController;
    public ArrayList<OnServerStateChangeListener> mList = new ArrayList<>();

    private ScreenImageController() {
    }

    // TODO: 2018/7/4 加锁,防止多线程同时操作单例对象,导致创建两次
    public static ScreenImageController getInstance() {
        synchronized (ScreenImageController.class) {
            if (mController == null) {
                mController = new ScreenImageController();
            }
        }
        return mController;
    }


    public ScreenImageController init(Application application) {
        mHandler = new Handler(application.getMainLooper());
        mContext = application;
        return mController;
    }

    // TODO: 2018/7/5 开启接收线程
    public ScreenImageController startServer() {
        if (tcpServer == null) {
            tcpServer = new TcpServer();
        }
        tcpServer.startServer();
        return mController;
    }

    public ScreenImageController stopServer() {
        if (tcpServer != null) tcpServer.stopServer();
        return mController;
    }

    public ScreenImageController setOnAcceptBuffListener(OnAcceptBuffListener listener) {
        if (tcpServer != null) tcpServer.setOnAccepttBuffListener(listener);
        return mController;
    }

    public void addOnAcceptTcpStateChangeListener(OnServerStateChangeListener listener) {
        if (mList.contains(listener)) {
            return;
        }
        mList.add(listener);
    }

    public void removeOnAcceptTcpStateChangeListener(OnServerStateChangeListener listener) {
        mList.remove(listener);
    }
}
