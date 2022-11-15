package com.forlayo.mediaprojection2yuv;

import java.nio.ByteBuffer;

public class VideoFrame {

    int width;
    int height;
    long timestampNs;

    ByteBuffer dataY;
    ByteBuffer dataU;
    ByteBuffer dataV;

    int strideY;
    int strideU;
    int strideV;

    private VideoFrame(int width, int height, ByteBuffer dataY, ByteBuffer dataU, ByteBuffer dataV, int strideY, int strideU, int strideV) {
        this.width = width;
        this.height = height;
        this.dataY = dataY;
        this.dataU = dataU;
        this.dataV = dataV;
        this.strideY = strideY;
        this.strideU = strideU;
        this.strideV = strideV;
    }

    /** Wraps existing ByteBuffers into VideoFrame object without copying the contents. */
    public static VideoFrame wrap(int width, int height, ByteBuffer dataY, ByteBuffer dataU, ByteBuffer dataV, int strideY, int strideU, int strideV) {
        if (dataY == null || dataU == null || dataV == null) {
            throw new IllegalArgumentException("Data buffers cannot be null.");
        }
        if (!dataY.isDirect() || !dataU.isDirect() || !dataV.isDirect()) {
            throw new IllegalArgumentException("Data buffers must be direct byte buffers.");
        }

        // Slice the buffers to prevent external modifications to the position / limit of the buffer.
        // Note that this doesn't protect the contents of the buffers from modifications.
        dataY = dataY.slice();
        dataU = dataU.slice();
        dataV = dataV.slice();

        return new VideoFrame(width, height, dataY, dataU, dataV, strideY, strideU, strideV);
    }
}
