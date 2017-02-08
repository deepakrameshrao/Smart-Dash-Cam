package com.deepak.smartdashcam;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;


public class MainActivity extends Activity {

    private Camera mCamera;
    public static DashCamSurfaceView dashCameraSurfaceView;
    private MediaRecorder mediaRecorder;

    Button myButton;
    SurfaceHolder surfaceHolder;
    boolean recording;

    FrameLayout mainFrameLayout;
    RelativeLayout bottomLayout;
    TextView dateTimeView;
    TextView speedView;
    ImageView cameraSwitchButton;
    ImageButton videoControlButton;
    ImageButton settingsButton;
    ImageButton listButton;

    int surfaceHeight;
    int surfaceWeight;
    RecorderService mRecorderService = null;
    Intent startBgServiceIntent = null;

    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //mRecorderService = RecorderService.getInstance();
        //mCamera = mRecorderService.getCamera();

        Intent startServiceIntent = new Intent(this, RecorderService.class);
        startService(startServiceIntent);
        startBgServiceIntent = new Intent(this, BackgroundService.class);
/*
        Intent startServiceIntent = new Intent(this, BackgroundService.class);
        startService(startServiceIntent);
         Handler mhandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                Intent startServiceIntent = new Intent(mContext, BackgroundService.class);
                mContext.stopService(startServiceIntent);
                super.handleMessage(msg);
            }
        };

        mhandler.sendEmptyMessageDelayed(1, 10000);
        */
        mContext = this;


        //Get Camera for preview
        mCamera = getCameraInstance();
        if(mCamera == null){
            Toast.makeText(this,
                    "Fail to get Camera",
                    Toast.LENGTH_LONG).show();
        }

        dashCameraSurfaceView = new DashCamSurfaceView(this, mCamera);
        mainFrameLayout = (FrameLayout)findViewById(R.id.mainlayout);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        mainFrameLayout.addView(dashCameraSurfaceView, layoutParams);

        LayoutInflater inflater = getLayoutInflater();
        bottomLayout = (RelativeLayout)inflater.inflate((R.layout.bottom_layout), null);

        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80.0f, getResources().getDisplayMetrics());

        RelativeLayout.LayoutParams relativeLayoutParams = new RelativeLayout.LayoutParams(size, size);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        relativeLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);

        videoControlButton = (ImageButton) bottomLayout.findViewById(R.id.video_control);

        settingsButton = new ImageButton(this);
        settingsButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.settings5));
        relativeLayoutParams = new RelativeLayout.LayoutParams(size, size);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        listButton = new ImageButton(this);
        listButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.listview));
        relativeLayoutParams = new RelativeLayout.LayoutParams(size, size);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        FrameLayout.LayoutParams frameLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, size);
        frameLayoutParams.gravity = Gravity.BOTTOM;

        relativeLayoutParams = new RelativeLayout.LayoutParams(size, size);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        bottomLayout.setLayoutParams(frameLayoutParams);

        mainFrameLayout.addView(bottomLayout, frameLayoutParams);

        bottomLayout.setGravity(Gravity.BOTTOM);

        //Add date, time and speed information
        /*TextView dateTimeView = new TextView(mContext);
        dateTimeView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dateTimeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        dateTimeView.setText("15-AUG-2015 12:15:00 AM");
        dateTimeView.setBackgroundColor(getResources().getColor(R.color.background_material_dark));*/
        //dateTimeView.setBackgroundResource(R.drawable.date_time_background);

        //Inflate the date and time layout and then set text for date time
        LinearLayout dateTimeLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.date_time_layout, null);
        TextView dateTimeTextView = (TextView) dateTimeLayout.findViewById(R.id.date_time_textview);
        if(dateTimeTextView != null) {
            dateTimeTextView.setText("15-AUG-2015 12:15:00 AM");
        }


        frameLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, size);
        frameLayoutParams.gravity = Gravity.TOP;
        mainFrameLayout.addView(dateTimeLayout, frameLayoutParams);

        videoControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!recording) {
                    startRecording();
                } else {
                   stopRecording();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume. BackgroundService.mIsRecording: " + BackgroundService.mIsRecording);
        if(BackgroundService.mIsRecording) {
            stopService(startBgServiceIntent);
            startRecording();
        }

    }

    public void startRecording() {
        //Release Camera before MediaRecorder start
        releaseCamera();
        if (!prepareMediaRecorder()) {
            Toast.makeText(getApplicationContext(),
                    "Fail in prepareMediaRecorder()!\n - Ended -",
                    Toast.LENGTH_LONG).show();
            finish();
        }
        mediaRecorder.start();
        recording = true;
        videoControlButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.pause_3));
    }

    public void stopRecording() {

        try {
            // stop recording and release camera
            mediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            recording = false;
            //mRecorderService.stopRecording();
            videoControlButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.video_icon));
            releaseCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity", "onPause");

        // release the camera immediately on pause event
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event*/

        if(!BackgroundService.mIsRecording && recording) {
            stopRecording();
            startService(startBgServiceIntent);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        //releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseMediaRecorder(){
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private boolean prepareMediaRecorder(){
        mCamera = getCameraInstance();
        mediaRecorder = new MediaRecorder();
        if(mCamera != null) {
            mCamera.unlock();
            mediaRecorder.setCamera(mCamera);
        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        mediaRecorder.setOutputFile("/sdcard/myvideo.mp4");
        mediaRecorder.setMaxDuration(90000); // Set max duration 60 sec.
        //mediaRecorder.setMaxFileSize(5000000); // Set max file size 5M

        mediaRecorder.setPreviewDisplay(dashCameraSurfaceView.getHolder().getSurface());

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;

    }

    private Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


public class DashCamSurfaceView extends SurfaceView implements SurfaceHolder.Callback{

    private SurfaceHolder mHolder;
    private Camera mCamera;

    public DashCamSurfaceView(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int weight,
                               int height) {

        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // make any resize, rotate or reformatting changes here
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> supportSize = parameters
                .getSupportedPreviewSizes();
        Camera.Size previewSize = null;
        if (supportSize != null) {
            previewSize = getOptimalPreviewSize(supportSize, weight, height);
        }
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        parameters.set("orientation", "landscape");
        //mCamera.release();
        mCamera.setParameters(parameters);
        //mCamera.setDisplayOrientation(180);

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

        } catch (Exception e){
        }


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }
}

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
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


    /*//mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        //mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
       // mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        mediaRecorder.setMaxDuration(90000);
        //mediaRecorder.setOnInfoListener(oninfoLis);
        //mediaRecorder.setVideoSize(previewSize.width, previewSize.height);
        //mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mediaRecorder.setVideoFrameRate(30);

        mediaRecorder.setVideoEncodingBitRate(500);
        mediaRecorder.setAudioEncodingBitRate(128);
        mediaRecorder.setOutputFile("/mnt/sdcard/myfile"
                + SystemClock.elapsedRealtime() + ".mp4");
*/

}
