package com.wt.screenimage_lib.server.tcp.interf;


import com.wt.screenimage_lib.entity.Frame;

/**
 * Created by wt
 * 关于帧类型回调
 */

public interface OnAcceptBuffListener {
    void acceptBuff(Frame frame);
}
