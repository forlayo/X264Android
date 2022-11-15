package com.forlayo.mediaprojection2yuv;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.forlayo.mediaprojection2yuv.gl.EglBase;
import com.forlayo.mediaprojection2yuv.gl.GlUtil;

import java.util.concurrent.Callable;

public class VideoTextureHelper {
    private final static String TAG = VideoTextureHelper.class.getSimpleName();

    private final Handler handler;

    private EglBase eglBase;
    private int oesTextureId;
    private SurfaceTexture surfaceTexture;
    private boolean hasPendingTexture;
    private volatile boolean isTextureInUse;
    private int textureWidth;
    private int textureHeight;

    private com.forlayo.mediaprojection2yuv.VideoSink listener;
    private com.forlayo.mediaprojection2yuv.VideoSink pendingListener;
    final Runnable setListenerRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Setting listener to " + pendingListener);
            listener = pendingListener;
            pendingListener = null;
            // May have a pending frame from the previous capture session - drop it.
            if (hasPendingTexture) {
                // Calling updateTexImage() is neccessary in order to receive new frames.
                updateTexImage();
                hasPendingTexture = false;
            }
        }
    };

    public static VideoTextureHelper create(EglBase.Context sharedContext)
    {
        final HandlerThread thread = new HandlerThread("ChapuzaTexture");
        thread.start();
        final Handler handler = new Handler(thread.getLooper());

        return ThreadUtils.invokeAtFrontUninterruptibly(handler, new Callable<VideoTextureHelper>() {
            @Override
            public VideoTextureHelper call() {
                try {
                    return new VideoTextureHelper(sharedContext, handler);
                } catch (RuntimeException e) {
                    Log.e(TAG, " create failure", e);
                    return null;
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VideoTextureHelper(EglBase.Context sharedContext, Handler handler)
    {
        this.handler = handler;
        eglBase = EglBase.create(sharedContext, EglBase.CONFIG_PIXEL_BUFFER);
        try {
            // Both these statements have been observed to fail on rare occasions, see BUG=webrtc:5682.
            eglBase.createDummyPbufferSurface();
            eglBase.makeCurrent();
        } catch (RuntimeException e) {
            // Clean up before rethrowing the exception.
            eglBase.release();
            //handler.getLooper().quit();
            throw e;
        }
        oesTextureId = GlUtil.generateTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        surfaceTexture = new SurfaceTexture(oesTextureId);

        surfaceTexture.setOnFrameAvailableListener((SurfaceTexture st) -> {
            if (hasPendingTexture) {
                Log.d(TAG, "A frame is already pending, dropping frame.");
            }
            hasPendingTexture = true;
            tryDeliverTextureFrame();
        }, handler);
    }

    public void startListening(final com.forlayo.mediaprojection2yuv.VideoSink listener) {
        if (this.listener != null || this.pendingListener != null) {
            throw new IllegalStateException("SurfaceTextureHelper listener has already been set.");
        }
        this.pendingListener = listener;
        handler.post(setListenerRunnable);
    }

    private void updateTexImage() {
        // SurfaceTexture.updateTexImage apparently can compete and deadlock with eglSwapBuffers,
        // as observed on Nexus 5. Therefore, synchronize it with the EGL functions.
        // See https://bugs.chromium.org/p/webrtc/issues/detail?id=5702 for more info.
        synchronized (EglBase.lock) {
            surfaceTexture.updateTexImage();
        }
    }

    public void setTextureSize(int textureWidth, int textureHeight) {
        if (textureWidth <= 0) {
            throw new IllegalArgumentException("Texture width must be positive, but was " + textureWidth);
        }
        if (textureHeight <= 0) {
            throw new IllegalArgumentException(
                    "Texture height must be positive, but was " + textureHeight);
        }
        surfaceTexture.setDefaultBufferSize(textureWidth, textureHeight);
        handler.post(() -> {
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            //tryDeliverTextureFrame();
        });
    }

    private YuvConverter converter = new YuvConverter();
    private void tryDeliverTextureFrame() {
        if (handler.getLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Wrong thread.");
        }
        if ( !hasPendingTexture || isTextureInUse || listener == null) {
            return;
        }
        if (textureWidth == 0 || textureHeight == 0) {
            // Information about the resolution needs to be provided by a call to setTextureSize() before
            // frames are produced.
            Log.w(TAG, "Texture size has not been set.");
            return;
        }
        isTextureInUse = true;
        hasPendingTexture = false;

        updateTexImage();
        final float[] transformMatrix = new float[16];
        surfaceTexture.getTransformMatrix(transformMatrix);
        long timestampNs = surfaceTexture.getTimestamp();

        com.forlayo.mediaprojection2yuv.VideoFrame videoFrame = converter.convert(oesTextureId, transformMatrix, textureWidth, textureHeight);


        Log.d(TAG,"Timestamp -> "+timestampNs);

        videoFrame.timestampNs = timestampNs;
        listener.onFrame(videoFrame);

        isTextureInUse = false;
    }

    /**
     * Retrieve the underlying SurfaceTexture. The SurfaceTexture should be passed in to a video
     * producer such as a camera or decoder.
     */
    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

}
