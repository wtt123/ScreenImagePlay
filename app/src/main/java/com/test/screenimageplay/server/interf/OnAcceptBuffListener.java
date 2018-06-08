package com.test.screenimageplay.server.interf;


import com.test.screenimageplay.entity.Frame;

/**
 * Created by wt
 * 关于帧类型回调
 */

public interface OnAcceptBuffListener {
    void acceptBuff(Frame frame);
}
