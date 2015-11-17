package com.stfalcon.hromadskyipatrol.ui.fragment;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.stfalcon.hromadskyipatrol.R;
import com.stfalcon.hromadskyipatrol.camera.CameraHelper;

import java.io.IOException;
import java.util.List;

/**
 * Created by alex on 08.11.15.
 */


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class CameraVideoFragment extends BaseCameraFragment implements View.OnClickListener {

    private TextureView mPreview;
    private boolean isRecording = false;
    private static final String TAG = "Recorder";
    private Camera mCamera;

    public static CameraVideoFragment newInstance() {
        return new CameraVideoFragment();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.camera_screen, container, false);
        initViews(rootView);
        return rootView;
    }

    private void initViews(View rootView) {
        mPreview = (TextureView) rootView.findViewById(R.id.texture);
        mPreview.setOnClickListener(this);
        new MediaPrepareTask().execute(null, null, null);
    }


    public void onCaptureClick() {
        if (isRecording) {
            releaseMRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder
            isRecording = false;
            releaseCamera();
            // END_INCLUDE(stop_release_media_recorder)
        } else {
            new MediaPrepareTask().execute(null, null, null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // if we are using MediaRecorder, release it first
        releaseMRecorder();
        // release the camera immediately on pause event
        releaseCamera();
    }

    private void releaseMRecorder() {
        mCamera.lock();

    }

    private void releaseCamera() {
        if (mCamera != null) {
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private boolean prepareVideoRecorder() {

        // BEGIN_INCLUDE (configure_preview)
        mCamera = CameraHelper.getDefaultCameraInstance();

        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes,
                mPreview.getWidth(), mPreview.getHeight());

        // Use the same size for recording profile.
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;

        // likewise for the camera object itself.
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mCamera.setParameters(parameters);
        try {
            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
            // with {@link SurfaceView}
            mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
        } catch (IOException e) {
            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
            return false;
        }

        mCamera.unlock();
        try {

        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMRecorder();
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.texture:
                onCaptureClick();
                break;
        }
    }

    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                onStartRecord();

                isRecording = true;
            } else {
                // prepare didn't work, release the camera
                releaseMRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                getActivity().finish();
            }
            // inform the user that recording has started
            onStartRecord();

        }
    }
}
