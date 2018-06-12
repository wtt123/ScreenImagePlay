package com.test.screenimageplay.server;

import android.util.Log;


import com.test.screenimageplay.server.interf.OnAcceptBuffListener;
import com.test.screenimageplay.server.interf.OnAcceptTcpStateChangeListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

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
                    while (isAccept) {
                        //服务端接收客户端的连接请求
                        Socket socket = serverSocket.accept();
                        //开启接收消息线程
                        acceptMsgThread = new AcceptMsgThread(socket.getInputStream(),
                                socket.getOutputStream(),mEncodeV1,mListener, mConnectListener);
                        acceptMsgThread.start();
                    }
                } catch (Exception e) {
                    Log.e("TcpServer", "" + e.toString());
                }

            }
        }.start();
    }

    /**
     * TODO: 2018/6/12 wt像客户端回传消息
     * @param mainCmd 主指令
     * @param subCmd 子指令
     * @param sendBody 文本内容
     * @param sendBuffer byte内容
     */
    public void setBackpassBody(int mainCmd, int subCmd, String sendBody,
                                byte[] sendBuffer){
        mEncodeV1=new EncodeV1(mainCmd,subCmd,sendBody,sendBuffer);
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

}
