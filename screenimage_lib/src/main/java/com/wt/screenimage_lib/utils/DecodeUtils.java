package com.wt.screenimage_lib.utils;

import android.util.Log;

import com.wt.screenimage_lib.entity.Frame;

/**
 * 解析H264和AAC的Decoder
 * 解码工具类
 */
public class DecodeUtils {
    private static final String TAG = "H264AacDecoder";
    // Coded slice of a non-IDR picture slice_layer_without_partitioning_rbsp( )
    public final static int NonIDR = 1;
    // Coded slice of an IDR picture slice_layer_without_partitioning_rbsp( )
    public final static int IDR = 5;
    // Supplemental enhancement information (SEI) sei_rbsp( )
    public final static int SEI = 6;
    // Sequence parameter set seq_parameter_set_rbsp( )
    public final static int SPS = 7;
    // Picture parameter set pic_parameter_set_rbsp( )
    public final static int PPS = 8;
    // Access unit delimiter access_unit_delimiter_rbsp( )
    public final static int AccessUnitDelimiter = 9;

    //
    public final static int AUDIO = -2;

    private byte[] mPps;
    private byte[] mSps;

    // TODO: 2018/6/7 wt区分音视频
    public void isCategory(byte[] frame) {
        boolean isKeyFrame = false;
        if (frame == null) {
            Log.e(TAG, "annexb not match.");
            return;
        }
        // ignore the nalu type aud(9)
        if (isAccessUnitDelimiter(frame)) {
            return;
        }
        // for pps
        if (isPps(frame)) {
            mPps = frame;
            if (mPps != null && mSps != null) {
                mListener.onSpsPps(mSps, mPps);
            }
            return;
        }
        // for sps
        if (isSps(frame)) {
            mSps = frame;
            if (mPps != null && mSps != null) {
                mListener.onSpsPps(mSps, mPps);
            }
            return;
        }
        if (isAudio(frame)) {
            byte[] temp = new byte[frame.length - 4];
            System.arraycopy(frame, 4, temp, 0, frame.length - 4);
            mListener.onVideo(temp, Frame.AUDIO_FRAME);
            return;
        }
        // for IDR frame
        if (isKeyFrame(frame)) {
            isKeyFrame = true;
        } else {
            isKeyFrame = false;
        }
        mListener.onVideo(frame, isKeyFrame ? Frame.KEY_FRAME : Frame.NORMAL_FRAME);
    }

    private boolean isAudio(byte[] frame) {
        if (frame.length < 5) {
            return false;
        }
        return frame[4] == ((byte) 0xFF) && frame[5] == ((byte) 0xF9);
    }

    private boolean isSps(byte[] frame) {
        if (frame.length < 5) {
            return false;
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        int nal_unit_type = (frame[4] & 0x1f);
        return nal_unit_type == SPS;
    }

    private boolean isPps(byte[] frame) {
        if (frame.length < 5) {
            return false;
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        int nal_unit_type = (frame[4] & 0x1f);
        return nal_unit_type == PPS;
    }

    private boolean isKeyFrame(byte[] frame) {
        if (frame.length < 5) {
            return false;
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        int nal_unit_type = (frame[4] & 0x1f);
        return nal_unit_type == IDR;
    }

    private static boolean isAccessUnitDelimiter(byte[] frame) {
        if (frame.length < 5) {
            return false;
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        int nal_unit_type = (frame[4] & 0x1f);
        return nal_unit_type == AccessUnitDelimiter;
    }

    // TODO: 2018/6/7 自定义回调接口
    public DecodeUtils.OnVideoListener mListener;

    public void setOnVideoListener(DecodeUtils.OnVideoListener listener) {
        this.mListener = listener;
    }
    public interface OnVideoListener {
        void onSpsPps(byte[] sps, byte[] pps);

        void onVideo(byte[] video, int type);
    }
}
