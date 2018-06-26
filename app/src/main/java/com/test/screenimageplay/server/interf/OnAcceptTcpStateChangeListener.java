package com.test.screenimageplay.server.interf;

import com.test.screenimageplay.server.AcceptMsgThread;

import java.util.List;

/**
 * Created by wt
 * 监听连接回调
 */

public interface OnAcceptTcpStateChangeListener {
    //接收到客户端的Tcp连接
    void acceptTcpConnect(AcceptMsgThread acceptMsgThread);

    /**
     * by wt
     * 接收到客户端的Tcp断开连接
     * @param e 异常提示
     * @param acceptMsgThread 当前投屏线程
     * @param updateUI 是否更新界面(显示二维码)
     */
    void acceptTcpDisConnect(Exception e, AcceptMsgThread acceptMsgThread,boolean updateUI);
}
