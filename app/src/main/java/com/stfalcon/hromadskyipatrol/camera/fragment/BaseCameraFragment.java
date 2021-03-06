/*
 * Copyright (c) 2015 - 2016. Stepan Tanasiychuk
 *
 *     This file is part of Gromadskyi Patrul is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Found ation, version 3 of the License, or any later version.
 *
 *     If you would like to use any part of this project for commercial purposes, please contact us
 *     for negotiating licensing terms and getting permission for commercial use.
 *     Our email address: info@stfalcon.com
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.stfalcon.hromadskyipatrol.camera.fragment;


import android.app.Fragment;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;

import com.stfalcon.hromadskyipatrol.camera.ICamera;
import com.stfalcon.hromadskyipatrol.models.ViolationItem;
import com.stfalcon.hromadskyipatrol.utils.FilesUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by alex on 08.11.15.
 */
public class BaseCameraFragment extends Fragment {

    private static final String TAG = BaseCameraFragment.class.getName();
    private long detectViolationTime;
    public String violationFileURI;
    public String previousFileURI;

    private int TIME_RECORD_AFTER_TAP = 12 * 1000; //10sec
    private int TIME_RECORD_SEGMENT = 1 * 60 * 1000;  //1 min
    //private int TIME_RECORD_SEGMENT = 30 * 1000;// 30sec

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public ICamera callback;

    public boolean violationRecording = false;
    public boolean mIsRecordingVideo = false;


    public void addCameraCallback(ICamera callback) {
        this.callback = callback;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        prepareTimer();
        onStopRecord();
    }

    protected void initCamera() {
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
        if (callback != null) {
            callback.onStopRecord();
        }
    }


    protected void createNewVideoFile() {
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

        recordSegmentCountDownTimer.cancel();
        violationCountDownTimer.start();
    }


    public void startRecordSegment() {
        onStartRecord();
        recordSegmentCountDownTimer.start();
    }

    private void prepareTimer() {
        try {
            recordSegmentCountDownTimer.cancel();
            violationCountDownTimer.cancel();
        } catch (IllegalStateException e) {
        }
    }


    private CountDownTimer violationCountDownTimer = new CountDownTimer(TIME_RECORD_AFTER_TAP, 1000) {
        @Override
        public void onFinish() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        Log.d(TAG, "run: " + violationFileURI);
                        Log.d(TAG, "prev run: " + previousFileURI);
                        if (previousFileURI != null) {
                            final File prevVideo = new File(FilesUtils.getOutputExternalMediaFile(FilesUtils.MEDIA_TYPE_VIDEO).getAbsolutePath());
                            try {
                                FilesUtils.copyFile(new File(previousFileURI), prevVideo);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onVideoPrepared(new ViolationItem(detectViolationTime, violationFileURI, prevVideo.getAbsolutePath()));
                                }
                            });
                        } else {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onVideoPrepared(new ViolationItem(detectViolationTime, violationFileURI));
                                }
                            });
                        }
                        previousFileURI = violationFileURI;
                    }
                    if (violationRecording && mIsRecordingVideo) {

                        onStopRecord();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                initCamera();
                            }
                        });
                    }
                }
            });
        }

        @Override
        public void onTick(long millisUntilFinished) {
            updateTimerView((int) (System.currentTimeMillis() - detectViolationTime) / 1000);
        }
    };


    private CountDownTimer recordSegmentCountDownTimer = new CountDownTimer(TIME_RECORD_SEGMENT, 1000) {
        @Override
        public void onFinish() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!violationRecording && mIsRecordingVideo) {
                        onStopRecord();
                        if (previousFileURI != null) {
                            new File(previousFileURI).delete();
                        }
                        previousFileURI = violationFileURI;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                initCamera();
                            }
                        });
                    }
                }
            });
        }

        @Override
        public void onTick(long millisUntilFinished) {
        }
    };

}
