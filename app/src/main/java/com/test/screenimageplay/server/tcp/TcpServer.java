package com.test.screenimageplay.server.tcp;

import android.util.Log;


import com.test.screenimageplay.server.tcp.AcceptMsgThread;
import com.test.screenimageplay.server.tcp.EncodeV1;
import com.test.screenimageplay.server.tcp.interf.OnAcceptBuffListener;
import com.test.screenimageplay.server.tcp.interf.OnAcceptTcpStateChangeListener;

import java.io.IOException;
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

public class TcpServer {
    private ServerSocket serverSocket;
    private int tcpPort = 11111;
    private boolean isAccept = true;
    private EncodeV1 mEncodeV1;
    private OnAcceptBuffListener mListener;
    private OnAcceptTcpStateChangeListener mConnectListener;
    private AcceptMsgThread acceptMsgThread;
    //把线程给添加进来
    private List<AcceptMsgThread> acceptMsgThreadList;
    //目前连接状态
    private boolean isConnect = false;
    //是否需要去更新界面
    private boolean isUpdateUI = false;
    private AcceptMsgThread acceptMsgThread1;

    public TcpServer() {
        this.acceptMsgThreadList = new ArrayList<>();
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
                        //开启接收消息线程
                        acceptMsgThread = new AcceptMsgThread(socket.getInputStream(),
                                socket.getOutputStream(), mEncodeV1, mListener, mConnectListener);
                        acceptMsgThread.start();
                        //把线程添加到集合中去
                        acceptMsgThreadList.add(acceptMsgThread);
                        Log.e("wt", "run: " + acceptMsgThreadList.size());
                        if (acceptMsgThreadList.size() > 1) {
                            isConnect = true;
                            continue;
                        }
                        //默认先发送成功标识给第一个客户端
                        acceptMsgThreadList.get(0).sendStartMessage();
                        //把第一个投屏的设备对象记录下来
                        acceptMsgThread1 = acceptMsgThreadList.get(0);
                    }
                } catch (Exception e) {
                    Log.e("TcpServer", "" + e.toString());
                }
            }
        }.start();
    }

    /**
     * TODO: 2018/6/12 wt像客户端回传消息
     *
     * @param mainCmd    主指令
     * @param subCmd     子指令
     * @param sendBody   文本内容
     * @param sendBuffer byte内容
     */
    public void setBackpassBody(int mainCmd, int subCmd, String sendBody,
                                byte[] sendBuffer) {
        mEncodeV1 = new EncodeV1(mainCmd, subCmd, sendBody, sendBuffer);
    }

    public void setOnAccepttBuffListener(OnAcceptBuffListener listener) {
        this.mListener = listener;
    }

    public void setOnTcpConnectListener(OnAcceptTcpStateChangeListener listener) {
        this.mConnectListener = listener;
    }

    public void stopServer() {
        this.mListener = null;
        isAccept = false;
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    if (acceptMsgThread != null) {
                        acceptMsgThread.shutdown();
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

    // TODO: 2018/6/15 wt 连接中逻辑
    public void setacceptTcpConnect(AcceptMsgThread acceptMsgThread) {
        //投屏正在连接
        Log.e("123", "acceptTcpConnect: zzz");
//        isConnect = true;
    }

    // TODO: 2018/6/15 wt连接断开逻辑
    public void setacceptTcpDisConnect(AcceptMsgThread acceptMsgThread) {
        //连接断开
        boolean remove = acceptMsgThreadList.remove(acceptMsgThread);
        Log.e("wtt", "移除成功" + remove + "acceptTcpDisConnect: 个数" + acceptMsgThreadList.size());
        if (acceptMsgThreadList == null || acceptMsgThreadList.size() == 0) {
            return;
        }
        //如果停止的不是正在投屏的线程，就不再去走下面的方法
        if (acceptMsgThread != acceptMsgThreadList.get(0) && acceptMsgThread != acceptMsgThread1) {
            Log.e("wt", "setacceptTcpDisConnect: zzz");
            return;
        }
        //开启第下一个投屏
        acceptMsgThreadList.get(0).sendStartMessage();
    }


    // TODO: 2018/6/27 返回当前设备集合数量
    public int currentSize() {
        if (acceptMsgThreadList == null) {
            return 0;
        }
        return acceptMsgThreadList.size();
    }

}
