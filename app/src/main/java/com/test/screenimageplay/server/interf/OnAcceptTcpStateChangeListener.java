package com.test.screenimageplay.server.interf;

/**
 * Created by wt
 * 监听连接回调
 */

public interface OnAcceptTcpStateChangeListener {
    //接收到客户端的Tcp连接
    void acceptTcpConnect();

    //接收到客户端的Tcp断开连接
    void acceptTcpDisConnect(Exception e);
}
