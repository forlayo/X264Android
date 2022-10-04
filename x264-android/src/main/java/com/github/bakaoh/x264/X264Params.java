package com.github.bakaoh.x264;

/**
 * Created by taitt on 10/01/2017.
 */

public class X264Params {

    public static final int CSP_I420 = 0x0002; // yuv 4:2:0 planar
    public static final int CSP_YV12 = 0x0003; // yvu 4:2:0 planar
    public static final int CSP_NV12 = 0x0004; // yuv 4:2:0, with one y plane and one packed u+v
    public static final int CSP_NV21 = 0x0005; // yuv 4:2:0, with one y plane and one packed v+u
    public static final int CSP_BGR  = 0x000e; // packed bgr 24bits
    public static final int CSP_RGBA = 0x000f; // packed bgr 32bits
    public static final int CSP_RGB  = 0x0010; // packed rgb 24bits

    public int width = 1280;
    public int height = 720;
    public int bitrate = 500;
    public int fps = 24;
    public int gop = 48;
    public boolean IDRWithSPSPPS = true;
    public String profile = "baseline";
    public String preset = "ultrafast";
}
