package com.example.user.cameraapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import wseemann.media.FFmpegMediaMetadataRetriever;


public class CaptureHighSpeedVideoMode  extends Fragment
        implements View.OnClickListener, FragmentCompat.OnRequestPermissionsResultCallback {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private static final String TAG = "CaptureVideoMode";
    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    private SensorEventListener mSensorEventListener;

    private float[] rotationMatrix;

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * Button to record video
     */
    private Button mRecButtonVideo;

    /**
     * A refernce to the opened {@link android.hardware.camera2.CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A reference to the current {@link CameraCaptureSession} for
     * preview.
     */
    private CameraConstrainedHighSpeedCaptureSession mPreviewSessionHighSpeed;
    private CameraCaptureSession mPreviewSession;
    public static File VideoData;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * The {@link android.util.Size} of video recording.
     */
    private Size mVideoSize;

    private Range<Integer>[] mVideoFps;

    /**
     * Camera preview.
     */
    private CaptureRequest.Builder mPreviewBuilder;


    /**
     * High Speed Camera Request-list
     */
//    private
    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;


    List<Surface> surfaces = new ArrayList<Surface>();

    /**
     * Whether the app is recording video now
     */
    private boolean mIsRecordingVideo = false;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    public static File videoFile;
    private String currentDateAndTime;
    private File myDir;

    private ImageView linesImageView;


    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.release();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    public static CaptureHighSpeedVideoMode newInstance() {
        return new CaptureHighSpeedVideoMode();
    }

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == 1280 && size.getHeight() <= 720) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() <= width && option.getHeight() <= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.max(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.camera, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        linesImageView = (ImageView) view.findViewById(R.id.linesImageView);
        mRecButtonVideo = (Button) view.findViewById(R.id.video_record);
        mRecButtonVideo.setOnClickListener(this);
        View decorView = getActivity().getWindow().getDecorView();
        getActivity().getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
//        setUpLineAndRect(720, 1280);
    }

    private void setUpLineAndRect(int width, int height) {
        Log.e(TAG, "setUpLines() called!");

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        linesImageView.setImageBitmap(bitmap);

        // Line and Rect paint
        Paint red = new Paint();
        red.setColor(Color.RED);
        red.setStrokeWidth(3);
        red.setStyle(Paint.Style.STROKE);

        Paint blue = new Paint();
        blue.setColor(Color.BLUE);
        blue.setStrokeWidth(3);

        Paint yellow = new Paint();
        yellow.setColor(Color.YELLOW);
        yellow.setStrokeWidth(3);

        Paint green = new Paint();
        green.setColor(Color.GREEN);
        green.setStrokeWidth(3);

        Paint cyan = new Paint();
        cyan.setColor(Color.CYAN);
        cyan.setStrokeWidth(3);

        Paint magenta = new Paint();
        magenta.setColor(Color.MAGENTA);
        magenta.setStrokeWidth(3);

        Paint white = new Paint();
        white.setColor(Color.WHITE);
        white.setStrokeWidth(3);

        Paint gray = new Paint();
        gray.setColor(Color.GRAY);
        gray.setStrokeWidth(3);

        // Line
        int startx = width / 2;
        int endx = width / 2;
        int starty = 0;
        int endy = height - 1;

        canvas.drawLine(startx, starty, endx, endy, red);

        // Rect
        float left = 200;
        float top = 400;
        float right = width - left;
        //float bottom = height - top;
        float bottom = 1000;

        canvas.drawRect(left, top, right, bottom, red);

        // Calibration lines
        canvas.drawLine(0, 100, 719, 100, cyan);
        canvas.drawLine(0, 130, 719, 130, green);
        canvas.drawLine(0, 160, 719, 160, yellow);
        canvas.drawLine(0, 190, 719, 190, blue);
        canvas.drawLine(0, 220, 719, 220, magenta);
        canvas.drawLine(0, 250, 719, 250, white);
        canvas.drawLine(0, 280, 719, 280, gray);

        canvas.drawLine(0, 310, 719, 310, cyan);
        canvas.drawLine(0, 340, 719, 340, green);
        canvas.drawLine(0, 370, 719, 370, yellow);
        canvas.drawLine(0, 430, 719, 430, blue);
        canvas.drawLine(0, 460, 719, 460, magenta);
        canvas.drawLine(0, 490, 719, 490, white);
        canvas.drawLine(0, 520, 719, 520, gray);

        canvas.drawLine(0, 550, 719, 550, cyan);
        canvas.drawLine(0, 580, 719, 580, green);
        canvas.drawLine(0, 610, 719, 610, yellow);
        canvas.drawLine(0, 640, 719, 640, blue);
        canvas.drawLine(0, 670, 719, 670, magenta);
        canvas.drawLine(0, 700, 719, 700, white);
        canvas.drawLine(0, 730, 719, 730, gray);

        canvas.drawLine(0, 760, 719, 760, cyan);
        canvas.drawLine(0, 790, 719, 790, green);
        canvas.drawLine(0, 820, 719, 820, yellow);
        canvas.drawLine(0, 850, 719, 850, blue);
        canvas.drawLine(0, 880, 719, 880, magenta);
        canvas.drawLine(0, 910, 719, 910, white);
        canvas.drawLine(0, 940, 719, 940, gray);

        canvas.drawLine(0, 970, 719, 970, cyan);
        canvas.drawLine(0, 1030, 719, 1030, green);
        canvas.drawLine(0, 1060, 719, 1060, yellow);
        canvas.drawLine(0, 1090, 719, 1090, blue);
        canvas.drawLine(0, 1120, 719, 1120, magenta);
        canvas.drawLine(0, 1150, 719, 1150, white);
        canvas.drawLine(0, 1180, 719, 1180, gray);

        canvas.drawLine(0, 1210, 719, 1210, cyan);
        canvas.drawLine(0, 1240, 719, 1240, green);
        canvas.drawLine(0, 1270, 719, 1270, yellow);
        canvas.drawLine(0, 1300, 719, 1300, blue);
        canvas.drawLine(0, 1330, 719, 1330, magenta);
        canvas.drawLine(0, 1360, 719, 1360, white);
        canvas.drawLine(0, 1390, 719, 1390, gray);
    }

    @Override
    public void onResume() {
        surfaces.clear();
        super.onResume();
        startBackgroundThread();
        View decorView = getActivity().getWindow().getDecorView();
        getActivity().getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        if (mIsRecordingVideo) {
            stopRecordingVideoOnPause();
        }
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        View decorView = getActivity().getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        switch (view.getId()) {
            case R.id.video_record: {
                decorView.setSystemUiVisibility(uiOptions);
                if (mIsRecordingVideo) {
                    stopRecordingVideo();
                } else {
                    startRecordingVideo();
                }
                break;
            }
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets whether you should show UI with rationale for requesting permissions.
     *
     * @param permissions The permissions your app wants to request.
     * @return Whether you can show permission rationale UI.
     */
    private boolean shouldShowRequestPermissionRationale(String[] permissions) {
        for (String permission : permissions) {
            if (FragmentCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requests permissions needed for recording video.
     */
    private void requestVideoPermissions() {
        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            FragmentCompat.requestPermissions(this, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        ErrorDialog.newInstance(getString(Integer.parseInt("0")))
                                .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                        break;
                    }
                }
            } else {
                ErrorDialog.newInstance(getString(Integer.parseInt("0")))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Camcorder Profile
     */
    private Range<Integer>[] availableFpsRange;

    private void openCamera(int width, int height) {
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {
            requestVideoPermissions();
            return;
        }
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.d(TAG, "tryAcquire");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            String cameraId = manager.getCameraIdList()[0];

            for (String cameraid : manager.getCameraIdList()) {
                Log.e(TAG, "CameraId: " + cameraid);
            }

            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            /*
      Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            mVideoSize = chooseVideoSize(map.getHighSpeedVideoSizes());
            for (Size size : map.getHighSpeedVideoSizes()) {
                Log.d("RESOLUTION", size.toString());
            }
            mVideoFps = map.getHighSpeedVideoFpsRangesFor(mVideoSize);

//            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
//                    width, height, mVideoSize);

            mPreviewSize = new Size(1280, 720);

            // FPS
            availableFpsRange = map.getHighSpeedVideoFpsRangesFor(mVideoSize);
            int max = 0;
            int min;
            for (Range<Integer> r : availableFpsRange) {
                if (max < r.getUpper()) {
                    max = r.getUpper();
                }
            }
            min = max;

            for (Range<Integer> r : availableFpsRange) {
                if (min > r.getLower()) {
                    min = r.getUpper();
                }
            }
//            for(Range<Integer> r: availableFpsRange) {
//                if(min == r.getLower() && max == r.getUpper()) {
//                     mPreviewBuilder.set(CONTROL_AE_TARGET_FPS_RANGE,r);
//                    Log.d("RANGES", "[ " + r.getLower() + " , " + r.getUpper() + " ]");
//                }
//            }

            for (Range<Integer> r : availableFpsRange) {
                Log.d("RANGES", "[ " + r.getLower() + " , " + r.getUpper() + " ]");
            }
            Log.d("RANGE", "[ " + min + " , " + max + " ]");
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();
//            mMediaFormat = new MediaFormat();
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            manager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(Integer.parseInt("0")))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Start the camera preview.
     */
    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            surfaces.clear();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            HandlerThread thread = new HandlerThread("CameraHighSpeedPreview");
            thread.start();

            if (mIsRecordingVideo) {
                setUpCaptureRequestBuilder(mPreviewBuilder);
                List<CaptureRequest> mPreviewBuilderBurst = mPreviewSessionHighSpeed.createHighSpeedRequestList(mPreviewBuilder.build());
                mPreviewSessionHighSpeed.setRepeatingBurst(mPreviewBuilderBurst, null, mBackgroundHandler);
            } else {
                mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Finds the framerate-range with the highest capturing framerate, and the lowest
     * preview framerate.
     *
     * @param fpsRanges A list contains framerate ranges.
     * @return The best option available.
     */
    private Range<Integer> getHighestFpsRange(Range<Integer>[] fpsRanges) {
        Range<Integer> fpsRange = Range.create(fpsRanges[0].getLower(), fpsRanges[0].getUpper());
        for (Range<Integer> r : fpsRanges) {
            if (r.getUpper() > fpsRange.getUpper()) {
                fpsRange.extend(0, r.getUpper());
            }
        }

        for (Range<Integer> r : fpsRanges) {
            if (r.getUpper() == fpsRange.getUpper()) {
                if (r.getLower() < fpsRange.getLower()) {
                    fpsRange.extend(r.getLower(), fpsRange.getUpper());
                }
            }
        }
        return fpsRange;
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        Range<Integer> fpsRange = Range.create(240, 240);
//        Range<Integer> fpsRange = getHighestFpsRange(availableFpsRange);
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    //    private MediaFormat mMediaFormat;
    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM);
        videoFile = getVideoFile(activity);
        mMediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mMediaRecorder.setVideoEncodingBitRate(1000000000);
        mMediaRecorder.setVideoFrameRate(240);
        mMediaRecorder.setCaptureRate(240);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.VP8);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = ORIENTATIONS.get(rotation);
        mMediaRecorder.setOrientationHint(orientation);
        mMediaRecorder.prepare();
    }

    /**
     * This method chooses where to save the video and what the name of the video file is
     *
     * @param context where the camera activity is
     * @return path + filename
     */
    private File getVideoFile(Context context) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //            File dir = new File(Environment.getExternalStorageDirectory()+ File.separator + "DCIM/Camera/")
//        return new File(context.getExternalFilesDir("DCIM"),
//                "TEST_VID_" + timeStamp + ".mp4");

        return new File(myDir,
                "video.webm");
    }

    private void startRecordingVideo() {
        try {
            // UI
            Toast.makeText(getContext(), "Recording!", Toast.LENGTH_SHORT).show();
            mIsRecordingVideo = true;
            mRecButtonVideo.setText("Stop");
            surfaces.clear();

            String root = Environment.getExternalStorageDirectory().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            currentDateAndTime = sdf.format(new Date());
            myDir = new File(root + "/BulletEffectVideoData/" + currentDateAndTime);
            myDir.mkdirs();

            VideoData = new File(myDir, "VideoData.txt");

            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
//            List<Surface> surfaces = new ArrayList<>();
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);


            Log.d("FPS", CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE.toString());

            mCameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    mPreviewSessionHighSpeed = (CameraConstrainedHighSpeedCaptureSession) mPreviewSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Log.d("ERROR", "COULD NOT START CAMERA");
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
            // Start recording
            mMediaRecorder.start();
            new TimeVideoStart(getActivity()).execute();

            mSensorEventListener = new SensorEventListener() {
                float[] mGravity;
                float[] mGeomagnetic;
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                        mGravity = sensorEvent.values;
                    if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                        mGeomagnetic = sensorEvent.values;
                    if (mGravity != null && mGeomagnetic != null) {
                        float R[] = new float[9];
                        float I[] = new float[9];
                        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                        if (success) {
                        }
                        rotationMatrix = R;
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {

                }
            };

            VideoHighFPSActivity.mSensorManager.registerListener(mSensorEventListener, VideoHighFPSActivity.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
            VideoHighFPSActivity.mSensorManager.registerListener(mSensorEventListener, VideoHighFPSActivity.mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    VideoHighFPSActivity.mSensorManager.unregisterListener(mSensorEventListener);
                }
            }, 1000);



        } catch (IllegalStateException | IOException | CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopRecordingVideo() {
        mRecButtonVideo.setEnabled(false);
        // UI
        mIsRecordingVideo = false;
        mRecButtonVideo.setText("Processing...");
        // Stop recording
        try {
            mPreviewSessionHighSpeed.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        new TimeVideoStop(getActivity()).execute();
//        new TimeServer(getActivity()).execute();

        mMediaRecorder.stop();
        mMediaRecorder.reset();
        Activity activity = getActivity();
        if (null != activity) {
//            Toast.makeText(activity, "Video saved, please wait for a file to process...",
//                    Toast.LENGTH_SHORT).show();
        }
        startPreview();

        File rotationMatrixFile = new File(myDir, "rotationMatrixFileCamera.txt");

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(rotationMatrixFile, true), 1024);
            String entry = "";
            for (int i = 0; i < rotationMatrix.length; i++) {
                entry += rotationMatrix[i] + " ";
            }
            out.write(entry);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(getContext(), NumberOfFoldersActivity.class);
        intent.putExtra("Activity", "Camera");
        startActivity(intent);

//        File path = getContext().getExternalFilesDir(null);
//        final File VideoData = new File(myDir, "VideoData.txt");
//
//        final String filePath = videoFile.getPath();

//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                int height, width;
//
//                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
//                mediaMetadataRetriever.setDataSource(filePath);
//
//                FFmpegMediaMetadataRetriever mediaRetriever = new FFmpegMediaMetadataRetriever();
//                mediaRetriever.setDataSource(filePath);
//
//                long duration = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
//
//                Bitmap sample = mediaRetriever.getFrameAtTime(0);
//
//                height = sample.getHeight();
//                width = sample.getWidth();
//
//                long diff = VideoActivity.frameTimeInMillis - (VideoActivity.videoStopTimeInMillis - duration);
//
//                Bitmap frame = mediaRetriever.getFrameAtTime(diff * 1000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
//
//                Matrix matrix = new Matrix();
//
//                matrix.postRotate(90);
//
//                Bitmap scaledBitmap = Bitmap.createScaledBitmap(frame,width,height,true);
//
//                Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
//
//                VideoHighFPSActivity.action = rotatedBitmap;
//
//                String savedImage = saveImage(rotatedBitmap);
//
//                try {
//                    BufferedWriter out = new BufferedWriter(new FileWriter(VideoData, true), 1024);
//                    String entry = "Start: " + VideoActivity.videoStartTimeInMillis + " Stop: " + VideoActivity.videoStopTimeInMillis + " Difference: " + (VideoActivity.videoStopTimeInMillis - VideoActivity.videoStartTimeInMillis) + " Duration: " + duration + " Delta: " + (VideoActivity.videoStopTimeInMillis-VideoActivity.videoStartTimeInMillis-duration);
//                    out.write(entry);
//                    out.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
////                int i = -50000;
////                int j = 0;
////                double a = 0;
////                Bitmap frameToSave = null;
////                while (i <= 50000) {
////                    Bitmap frame = mediaRetriever.getFrameAtTime(diff * 1000 + i, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
////
////                    Matrix matrix = new Matrix();
////
////                    matrix.postRotate(90);
////
////                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(frame,width,height,true);
////
////                    Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
////
////                            saveImage(rotatedBitmap);
////
////                    if (j == 0) {
////                        a = amountOfBluriness(rotatedBitmap);
////                        frameToSave = rotatedBitmap;
////                        j++;
////                    } else {
////                        if (a < amountOfBluriness(rotatedBitmap)) {
////                            a = amountOfBluriness(rotatedBitmap);
////                            frameToSave = rotatedBitmap;
////                        }
////                    }
////                    i += 10000;
////                }
////
////                saveImage(frameToSave);
//
//                Intent intent = new Intent(getActivity(), ImagePreviewActivity.class);
////                intent.putExtra("savedImage", savedImage);
//                startActivity(intent);
//            }
//        }, 2500);
    }

    private void stopRecordingVideoOnPause() {
        mIsRecordingVideo = false;
        try {
            mPreviewSessionHighSpeed.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mMediaRecorder.stop();

        mMediaRecorder.reset();
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }
    }

    public static class ConfirmationDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage("efddsf")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentCompat.requestPermissions(parent, VIDEO_PERMISSIONS,
                                    REQUEST_VIDEO_PERMISSIONS);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    parent.getActivity().finish();
                                }
                            })
                    .create();
        }
    }

    public static String saveImage(Bitmap finalBitmap) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateandTime = sdf.format(new Date());
        String fname = "Image_"+ currentDateandTime +".jpg";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fname;
    }

    private double amountOfBluriness(Bitmap bmp) {
        Mat destination = new Mat();
        Mat matGray = new Mat();

        Mat image = new Mat();
        Utils.bitmapToMat(bmp, image);
        Imgproc.cvtColor(image, matGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Laplacian(matGray, destination, 3);
        MatOfDouble median = new MatOfDouble();
        MatOfDouble std= new MatOfDouble();
        Core.meanStdDev(destination, median , std);

        return Math.pow(std.get(0,0)[0],2);
    }
}