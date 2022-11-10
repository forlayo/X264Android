package com.github.bakaoh.x264;

import java.nio.ByteBuffer;

/**
 * Created by taitt on 09/01/2017.
 */

public class X264Encoder {

    static {
        System.loadLibrary("x264a");
    }
    private long ctx; //This holds the encoder context


    public native X264InitResult initEncoder(X264Params params);

    public native void releaseEncoder();

    public native X264EncodeResult encodeFrame(byte[] frame, int colorFormat, long pts);

    public native String getVersion();


    /** WebRTC I420Buffer support.
     * Helper method for copying I420 to tightly packed NV12 destination buffer.
     * */
    public static void I420ToNV12(ByteBuffer srcY, int srcStrideY, ByteBuffer srcU, int srcStrideU,
                                  ByteBuffer srcV, int srcStrideV, ByteBuffer dst, int width, int height) {
        final int chromaWidth = (width + 1) / 2;
        final int chromaHeight = (height + 1) / 2;
        final int minSize = width * height + chromaWidth * chromaHeight * 2;
        if (dst.capacity() < minSize) {
            throw new IllegalArgumentException("Expected destination buffer capacity to be at least "
                    + minSize + " was " + dst.capacity());
        }
        final int startY = 0;
        final int startUV = height * width;
        dst.position(startY);
        final ByteBuffer dstY = dst.slice();
        dst.position(startUV);
        final ByteBuffer dstUV = dst.slice();
        nativeI420ToNV12(srcY, srcStrideY, srcU, srcStrideU, srcV, srcStrideV, dstY, width, dstUV,
                chromaWidth * 2, width, height);
    }
    private static native void nativeI420ToNV12(ByteBuffer srcY, int srcStrideY, ByteBuffer srcU,
                                                int srcStrideU, ByteBuffer srcV, int srcStrideV, ByteBuffer dstY, int dstStrideY,
                                                ByteBuffer dstUV, int dstStrideUV, int width, int height);
}
