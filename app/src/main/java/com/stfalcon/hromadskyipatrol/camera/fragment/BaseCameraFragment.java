package com.stfalcon.hromadskyipatrol.camera.fragment;


import android.app.Fragment;
import android.os.Handler;
import android.util.SparseIntArray;
import android.view.Surface;

import com.stfalcon.hromadskyipatrol.camera.ICamera;
import com.stfalcon.hromadskyipatrol.models.ViolationItem;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by alex on 08.11.15.
 */
public class BaseCameraFragment extends Fragment {

    private long detectViolationTime;
    protected String violationFileURI;

    private int TIME_RECORD_AFTER_TAP = 10 * 1000; //10sec
    //private int TIME_RECORD_SEGMENT = 2 * 60 * 1000;  //2 min
    private int TIME_RECORD_SEGMENT = 30 * 1000;// 30sec

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public ICamera callback;
    private Handler handler;
    private Timer segmentTimer;
    private Timer violationTimer;
    private StopRecordSegmentTask stopRecordSegmentTask;
    private StopRecordViolationTask stopRecordViolationTask;


    private Runnable updateTimerRunnable = new Runnable() {
        public void run() {
            updateTimerView((int) (System.currentTimeMillis() - detectViolationTime) / 1000);
            if (violationRecording) {
                handler.postDelayed(updateTimerRunnable, 1000);
            }
        }
    };

    public boolean violationRecording = false;
    public boolean mIsRecordingVideo = false;


    public void addCameraCallback(ICamera callback) {
        this.callback = callback;
    }

    @Override
    public void onStart() {
        super.onStart();
        handler = new Handler();
        segmentTimer = new Timer();
        violationTimer = new Timer();
    }

    @Override
    public void onStop() {
        super.onStop();
        handler.removeCallbacks(updateTimerRunnable);
        prepareTimer();
        onStopRecord();
    }

    protected void initCamera(){
    }

    protected void onCameraPrepared() {
        if (callback != null) {
            callback.onCameraPrepared();
        }
    }

    protected void onStartRecord() {
        if (callback != null) {
            callback.onStartRecord();
        }
    }

    protected void onStopRecord() {
        violationRecording = false;
        handler.removeCallbacks(updateTimerRunnable);
        if (callback != null) {
            callback.onStopRecord();
        }
    }


    protected void createNewVideFile(){
    }

    private void updateTimerView(int sec) {
        if (callback != null && violationRecording) {
            callback.onTime(sec);
        }
    }

    protected void startViolationRecording() {
        if (callback != null) {
            callback.onViolationDetected();
        }
        violationRecording = true;
        detectViolationTime = System.currentTimeMillis();

        stopRecordSegmentTask.cancel();
        stopRecordViolationTask = new StopRecordViolationTask();
        violationTimer.schedule(stopRecordViolationTask, TIME_RECORD_AFTER_TAP);
        handler.postDelayed(updateTimerRunnable, 1000);
    }


    public void startRecordSegment() {
        onStartRecord();
        stopRecordSegmentTask = new StopRecordSegmentTask();
        segmentTimer.schedule(stopRecordSegmentTask, TIME_RECORD_SEGMENT);
    }

    private void prepareTimer() {
        try {
            segmentTimer.cancel();
            violationTimer.cancel();
        } catch (IllegalStateException e){}
    }


    class StopRecordSegmentTask extends TimerTask {

        @Override
        public void run() {
            if (!violationRecording && mIsRecordingVideo) {
                onStopRecord();
                new File(violationFileURI).delete();
                initCamera();
            }
        }
    }

    class StopRecordViolationTask extends TimerTask {

        @Override
        public void run() {
            if (violationRecording && mIsRecordingVideo) {
                onStopRecord();
                initCamera();
            }

            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (callback != null) {
                        callback.onVideoPrepared(new ViolationItem(detectViolationTime, violationFileURI));
                    }
                }
            });
        }
    }
}