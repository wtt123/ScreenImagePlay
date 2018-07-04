package com.wt.screenimage_lib.server.tcp;

import android.util.Log;


import com.wt.screenimage_lib.ScreenImageApi;
import com.wt.screenimage_lib.ScreenImageController;
import com.wt.screenimage_lib.entity.ReceiveData;
import com.wt.screenimage_lib.entity.ReceiveHeader;
import com.wt.screenimage_lib.server.tcp.interf.OnAcceptBuffListener;
import com.wt.screenimage_lib.server.tcp.interf.OnServerStateChangeListener;
import com.wt.screenimage_lib.utils.AnalyticDataUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created wt
 * Date on  2018/5/28
 *
 * @Desc 创建服务监听
 */

public class TcpServer implements AcceptMsgThread.OnTcpChangeListener {
    private static final String TAG = "TcpServer";
    private ServerSocket serverSocket;
    private int tcpPort = 11111;
    private boolean isAccept = true;
    private EncodeV1 mEncodeV1;
    private OnAcceptBuffListener mListener;
    //把线程给添加进来
    private List<AcceptMsgThread> acceptMsgThreadList;
    private AcceptMsgThread acceptMsgThread1;
    private AnalyticDataUtils mAnalyticUtils;

    public TcpServer() {
        this.acceptMsgThreadList = new ArrayList<>();
        mAnalyticUtils = new AnalyticDataUtils();
        init();
    }

    private void init() {
        mEncodeV1 = new EncodeV1(ScreenImageApi.SERVER.MAIN_CMD, ScreenImageApi.SERVER.INITIAL_SUCCESS, "初始化成功", new byte[0]);
    }

    public void startServer() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    // 创建一个ServerSocket对象，并设置监听端口
                    serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(true);
                    InetSocketAddress socketAddress = new InetSocketAddress(tcpPort);
                    serverSocket.bind(socketAddress);
                    acceptMsgThreadList.clear();
                    while (isAccept) {
                        //服务端接收客户端的连接请求
                        Socket socket = serverSocket.accept();
                        InputStream inputStream = socket.getInputStream();
                        byte[] temp = mAnalyticUtils.readByte(inputStream, 18);
                        ReceiveHeader receiveHeader = mAnalyticUtils.analysisHeader(temp);
                        if (receiveHeader.getMainCmd() == ScreenImageApi.LOGIC_REQUEST.MAIN_CMD) {  //业务逻辑请求
                            //接收逻辑消息线程
                            LogicThread logicThread = new LogicThread(socket, receiveHeader);
                            logicThread.setOnAcceptLogicMsgListener(new LogicThread.OnAcceptLogicMsgListener() {
                                @Override
                                public EncodeV1 onAcceptLogicMsg(ReceiveData data) {
                                    return acceptLogicMsg(data);
                                }
                            });
                            logicThread.start();
                        } else if (receiveHeader.getMainCmd() == ScreenImageApi.RECORD.MAIN_CMD) {  //投屏请求
                            //开启接收H264和Aac线程
                            AcceptMsgThread acceptMsgThread = new AcceptMsgThread(socket, mEncodeV1, mListener, TcpServer.this);
                            acceptMsgThread.start();
                            //把线程添加到集合中去
                            acceptMsgThreadList.add(acceptMsgThread);
                            showLog("run: " + acceptMsgThreadList.size());
                            if (acceptMsgThreadList.size() > 1) {
                                return;
                            }
                            //默认先发送成功标识给第一个客户端
                            acceptMsgThreadList.get(0).sendStartMessage();
                            //把第一个投屏的设备对象记录下来
                            acceptMsgThread1 = acceptMsgThreadList.get(0);
                        } else {
                            socket.close();
                        }
                    }
                } catch (Exception e) {
                    Log.e("TcpServer", "" + e.toString());
                }
            }
        }.start();
    }

    public void setOnAccepttBuffListener(OnAcceptBuffListener listener) {
        this.mListener = listener;
    }

    public void stopServer() {
        this.mListener = null;
        isAccept = false;
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    for (AcceptMsgThread thread : acceptMsgThreadList) {
                        if (thread == null) continue;
                        thread.shutdown();
                    }
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    public EncodeV1 acceptLogicMsg(ReceiveData data) {
        ArrayList<OnServerStateChangeListener> mList = ScreenImageController.getInstance().mList;
        if (mList == null) {
            return null;
        }
        for (OnServerStateChangeListener listener : mList) {
            EncodeV1 encodeV1 = listener.acceptLogicTcpMsg(data);
            if (encodeV1 == null) continue;
            return encodeV1;
        }
        return null;
    }

    public void connectListener() {
        ArrayList<OnServerStateChangeListener> mList = ScreenImageController.getInstance().mList;
        if (mList == null) return;
        for (OnServerStateChangeListener listener : mList) {
            listener.acceptH264TcpConnect(currentSize());
        }
    }

    /**
     * 所有listener执行uankai断开回调
     */
    public void disconnectListener(Exception e) {
        ArrayList<OnServerStateChangeListener> mList = ScreenImageController.getInstance().mList;
        if (mList == null) return;
        for (OnServerStateChangeListener listener : mList) {
            listener.acceptH264TcpDisConnect(e, currentSize());
        }
    }


    // TODO: 2018/6/27 返回当前设备集合数量
    public int currentSize() {
        if (acceptMsgThreadList == null) {
            return 0;
        }
        return acceptMsgThreadList.size();
    }

    private void showLog(String msg) {
        Log.d(TAG, msg);
    }

    @Override
    public void disconnect(Exception e, AcceptMsgThread thread) {
        boolean remove = acceptMsgThreadList.remove(thread);
        disconnectListener(e);
        Log.e("wtt", "移除成功" + remove + "acceptTcpDisConnect: 个数" + acceptMsgThreadList.size());
        if (acceptMsgThreadList == null || acceptMsgThreadList.size() == 0) {
            return;
        }
        //如果停止的不是正在投屏的线程，就不再去走下面的方法
        if (thread != acceptMsgThreadList.get(0) && thread != acceptMsgThread1) {
            Log.e("wt", "setacceptTcpDisConnect: zzz");
            return;
        }
        //开启第下一个投屏
        acceptMsgThreadList.get(0).sendStartMessage();
    }

    /**
     * 2018/6/15 wt连接断开逻辑
     *
     * @param thread
     */
    @Override
    public void connect(AcceptMsgThread thread) {
        connectListener();
    }
}
