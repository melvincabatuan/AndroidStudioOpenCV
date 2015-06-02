package io.github.melvincabatuan.androidstudioopencv;


import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;


public class MainActivity extends Activity {

    private CameraPreview camPreview;
    private ImageView MyCameraPreview = null;
    private RelativeLayout mainLayout;
    private int PreviewSizeWidth = 0;
    private int PreviewSizeHeight = 0;
    private int offset = 12;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_main);

        MyCameraPreview = new ImageView(this);
        SurfaceView camView = new SurfaceView(this);
        SurfaceHolder camHolder = camView.getHolder();
        mainLayout = (RelativeLayout) findViewById(R.id.container);

        getDisplaySize();
        camPreview = new CameraPreview(PreviewSizeWidth, PreviewSizeHeight, MyCameraPreview);
        camHolder.addCallback(camPreview);

        FrameLayout.LayoutParams parameters = new FrameLayout.LayoutParams(
                PreviewSizeWidth, PreviewSizeHeight);
        mainLayout.addView(camView, parameters);
        mainLayout.addView(MyCameraPreview, parameters);
    }

    private void getDisplaySize() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        PreviewSizeHeight = metrics.heightPixels - offset;
        PreviewSizeWidth = metrics.widthPixels - offset;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}