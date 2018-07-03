package com.wt.screenimage_lib.server.tcp.interf;

import com.wt.screenimage_lib.entity.ReceiveData;
import com.wt.screenimage_lib.server.tcp.AcceptMsgThread;

import java.io.OutputStream;

/**
 * Created by wt
 * 监听连接回调
 */

public abstract class OnAcceptTcpStateChangeListener {
    //接收到客户端的Tcp连接
    public abstract void acceptTcpConnect(AcceptMsgThread acceptMsgThread);

    /**
     * by wt
     * 接收到客户端的Tcp断开连接
     *
     * @param e               异常提示
     * @param acceptMsgThread 当前投屏线程
     */
    public abstract void acceptTcpDisConnect(Exception e, AcceptMsgThread acceptMsgThread);

    //接到逻辑消息
    public abstract void acceptLogicMsg(ReceiveData data);
}
