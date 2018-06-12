package com.test.screenimageplay.constant;

/**
 * Created by wt on 2018/6/11.
 * 设备常用配置
 */
public class ScreenImageApi {
    public static final byte encodeVersion1 = 0x00;       //版本号1
    public class RECORD {   //录屏指令
        public static final int MAIN_CMD = 0xA2; //主指令
        public static final int INITIAL_SUCCESS= 0x01;//服务器初始化成功
    }
}
