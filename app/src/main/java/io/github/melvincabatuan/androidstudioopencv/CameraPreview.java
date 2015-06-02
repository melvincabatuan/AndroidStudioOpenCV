package io.github.melvincabatuan.androidstudioopencv;

/**
 * Created by root on 6/2/15.
 */

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.ImageView;

import java.io.IOException;
import java.util.List;

public class CameraPreview implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private Camera mCamera = null;
    private ImageView MyCameraPreview = null;
    private Bitmap bitmap = null;
    private int[] pixels = null;
    private byte[] FrameData = null;
    private int imageFormat;
    private int PreviewSizeWidth;
    private int PreviewSizeHeight;
    private boolean bProcessing = false;
    public Parameters parameters;


    Handler mHandler = new Handler(Looper.getMainLooper());

    public CameraPreview(int PreviewlayoutWidth, int PreviewlayoutHeight,
                         ImageView CameraPreview) {
        PreviewSizeWidth = PreviewlayoutWidth;
        PreviewSizeHeight = PreviewlayoutHeight;
        MyCameraPreview = CameraPreview;
        bitmap = Bitmap.createBitmap(PreviewSizeWidth, PreviewSizeHeight, Bitmap.Config.ARGB_8888);
        pixels = new int[PreviewSizeWidth * PreviewSizeHeight];
    }



    int i = 0;
    long now, oldnow, count = 0;

    @Override
    public void onPreviewFrame(byte[] arg0, Camera arg1) {
        // Log.d("CameraPreview:", "ON Preview frame");

        i++;
        now = System.nanoTime()/1000;
        if (i>3) {
            Log.d("onPreviewFrame: ", "Measured: " + 1000000L / (now - oldnow) + " fps.");
            count++;
        }
        oldnow = now;


        /// NV21 Format ONLY

        if (imageFormat == ImageFormat.NV21) {

            if (!bProcessing) {
                FrameData = arg0;

                mHandler.post(DoImageProcessing);
            }
        }
    }


    public void onPause() {
        mCamera.stopPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        Camera.Size result = getOptimalPreviewSize(sizes, PreviewSizeWidth, PreviewSizeHeight);
        PreviewSizeWidth = result.width;
        PreviewSizeHeight = result.height;
        parameters.setPreviewSize(PreviewSizeWidth,PreviewSizeHeight);

        bitmap = Bitmap.createBitmap(PreviewSizeWidth, PreviewSizeHeight,
                Bitmap.Config.ARGB_8888);
        MyCameraPreview.setImageBitmap(bitmap);
        pixels = new int[PreviewSizeWidth * PreviewSizeHeight];
        imageFormat = parameters.getPreviewFormat();

        try {
            mCamera.setParameters(parameters);
            mCamera.startPreview();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        mCamera = Camera.open();

        try {
            mCamera.setPreviewDisplay(arg0);
            mCamera.setPreviewCallback(this);
        } catch (IOException e) {
            mCamera.release();
            mCamera = null;
        }
        parameters = mCamera.getParameters();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }


    private Runnable DoImageProcessing = new Runnable() {
        public void run() {
            bProcessing = true;

            // Converts YUV420 NV21 to Y888 (RGB8888) (GrayScale)
            //applyGrayScale(PreviewSizeWidth, PreviewSizeHeight, FrameData, pixels);

            pixels = convertYUV420_NV21toRGB8888(PreviewSizeWidth, PreviewSizeHeight, FrameData);

            bitmap.setPixels(pixels, 0, PreviewSizeWidth, 0, 0, PreviewSizeWidth, PreviewSizeHeight);
            MyCameraPreview.setImageBitmap(bitmap);
            bProcessing = false;
        }
    };


    /**
     * Converts YUV420 NV21 to Y888 (RGB8888). The grayscale image still holds 3 bytes on the pixel.
     *
     * @param pixels output array with the converted array of grayscale pixels
     * @param data byte array on YUV420 NV21 format.
     * @param width pixels width
     * @param height pixels height
     */
    public static void applyGrayScale(int width, int height, byte [] data, int [] pixels) {
        int p;
        int size = width*height;
        for(int i = 0; i < size; i++) {
            p = data[i] & 0xFF;
            pixels[i] = 0xff000000 | p<<16 | p<<8 | p;
        }
    }



    /**
     * Converts YUV420 NV21 to RGB8888
     *
     * @param data byte array on YUV420 NV21 format.
     * @param width pixels width
     * @param height pixels height
     * @return a RGB8888 pixels int array. Where each int is a pixels ARGB.
     */
    public static int[] convertYUV420_NV21toRGB8888(int width, int height, byte [] data) {
        int size = width*height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        // i percorre os Y and the final pixels
        // k percorre os pixles U e V
        for(int i=0, k=0; i < size; i+=2, k+=2) {
            y1 = data[i  ]&0xff;
            y2 = data[i+1]&0xff;
            y3 = data[width+i  ]&0xff;
            y4 = data[width+i+1]&0xff;

            u = data[offset+k  ]&0xff;
            v = data[offset+k+1]&0xff;
            u = u-128;
            v = v-128;

            pixels[i  ] = convertYUVtoRGB(y1, u, v);
            pixels[i+1] = convertYUVtoRGB(y2, u, v);
            pixels[width+i  ] = convertYUVtoRGB(y3, u, v);
            pixels[width+i+1] = convertYUVtoRGB(y4, u, v);

            if (i!=0 && (i+2)%width==0)
                i+=width;
        }

        return pixels;
    }

    private static int convertYUVtoRGB(int y, int u, int v) {
        int r,g,b;

        r = y + (int)1.402f*v;
        g = y - (int)(0.344f*u +0.714f*v);
        b = y + (int)1.772f*u;
        r = r>255? 255 : r<0 ? 0 : r;
        g = g>255? 255 : g<0 ? 0 : g;
        b = b>255? 255 : b<0 ? 0 : b;
        return 0xff000000 | (b<<16) | (g<<8) | r;
    }
}