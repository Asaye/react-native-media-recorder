package com.admedia.video;

import com.admedia.utils.MediaConstants;
import com.admedia.R;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Surface;
import android.view.TextureView;
import android.app.Fragment; 
import android.app.Activity;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Handler;
import android.os.Looper;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.media.CamcorderProfile;
import android.support.annotation.NonNull;
import android.util.SparseIntArray;
import android.util.Size;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections; 
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.lang.Thread;
import java.lang.InterruptedException;
import java.text.SimpleDateFormat; 

public class VideoFragment extends Fragment implements TextureView.SurfaceTextureListener, 
                                                       View.OnClickListener  {

	private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    private static final Map<String, Object> constants = MediaConstants.getConstants();

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static VideoFragment mInstance;

	private Button mButtonVideo;	
	private AutoFitTextureView mTextureView = null;
	private SurfaceTexture mSurfaceTexture;
	private CameraDevice mCameraDevice;
	private MediaRecorder mMediaRecorder = null;
	private CameraManager mCameraManager = null;
	private CameraCaptureSession mPreviewSession = null;
	private CaptureRequest.Builder mPreviewBuilder = null;

	private Size mPreviewSize, mVideoSize;
	private Integer mSensorOrientation;
	private HashMap mRequestData;
	
	private HandlerThread mBackgroundThread;
	private Handler mBackgroundHandler;
	private Semaphore mCameraOpenCloseLock = new Semaphore(1);

	private boolean mIsRecordingVideo = false;	

	private MediaRecorder.OnInfoListener mRecorderListener = new MediaRecorder.OnInfoListener() { 
		@Override
		public void onInfo(MediaRecorder mr, int response, int extra) {
			if ( response == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
			     response == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED ) {
				stopRecordingVideo();
			}
		}
	};
	
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
            removeFragment();
        }
    };

     
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
    	mRequestData = (HashMap) getArguments().getSerializable("recorderData");
        return inflater.inflate(R.layout.admedia_fragment_video, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {        
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mButtonVideo = (Button) view.findViewById(R.id.video);
        mButtonVideo.setOnClickListener(this);
        mButtonVideo.setText(R.string.start);
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();        
    }

    @Override
    public void onDetach() {
    	super.onDetach();
    	exitRecordingVideo();
    }

    @Override
    public void onStop() {
    	super.onStop();
    	exitRecordingVideo();
    }    
    
    static VideoFragment newInstance() {
		mInstance = new VideoFragment();
		return mInstance;
	}
    
    @Override
    public void onClick(View view) {
    	if (mIsRecordingVideo) {    		
    		stopRecordingVideo();
    	} else {    		
    		startRecordingVideo();
    	}        
    }   

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

    private void openCamera(int width, int height) {
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            
            String cameraId = getCameraId();
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();
            mCameraManager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            removeFragment();
        } catch (NullPointerException e) {
            throw new RuntimeException("Camera2API not supported on the device.");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        } catch (Exception e) {            
        }        
    }

    private String getCameraId() throws CameraAccessException {
    	String cameraId = mCameraManager.getCameraIdList()[0];
    	String cameraType = "BACK";

    	if (mRequestData.get("cameraType") != null) {
    		cameraType = (String) mRequestData.get("cameraType");
    	}

    	CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);				
		Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
		
		if (facing != null) {
			if (facing == CameraCharacteristics.LENS_FACING_FRONT && cameraType.equals("BACK") || 
				facing == CameraCharacteristics.LENS_FACING_BACK && cameraType.equals("FRONT")) {
				cameraId = mCameraManager.getCameraIdList()[1];
			} 
		}
		return cameraId;
    }

    void startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();
            
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);
            
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                    Activity activity = getActivity();
                    if (activity != null) {
                    	activity.runOnUiThread(new Runnable() {
	                        @Override
	                        public void run() {
	                            mIsRecordingVideo = true;
			                    mButtonVideo.setText(R.string.stop);
			                    mMediaRecorder.start();
	                        }
	                    });
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException | IOException e) {
            
        }
    }
    
    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        SparseIntArray params = MediaConstants.getParams(mRequestData, "video");
		int outputFormat = params.get(MediaConstants.OUTPUT_FORMAT);
	        
        mMediaRecorder.setAudioSource(params.get(MediaConstants.AUDIO_SOURCE));
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(outputFormat);

        String path = (String) mRequestData.get("outputFolder");
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String extension = MediaConstants.getFileExtension(outputFormat);
        
        mMediaRecorder.setOutputFile(path + timeStamp + extension);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(params.get(MediaConstants.VIDEO_ENCODER));
        mMediaRecorder.setAudioEncoder(params.get(MediaConstants.AUDIO_ENCODER));
        
        if (params.get(MediaConstants.MAX_DURATION) > 0) {
        	mMediaRecorder.setMaxDuration(params.get(MediaConstants.MAX_DURATION));
        }

        if (params.get(MediaConstants.MAX_FILE_SIZE) > 0) {
        	mMediaRecorder.setMaxDuration(params.get(MediaConstants.MAX_FILE_SIZE));
        }
        
        mMediaRecorder.setOnInfoListener(mRecorderListener);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        mMediaRecorder.prepare();
    }
    
    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Activity activity = getActivity();
                            if (null != activity) {
                                Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {            
        }
    }

    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        return choices[choices.length - 1];
    }    

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

	void stopRecordingVideo() {
        mIsRecordingVideo = false;
        mButtonVideo.setText(R.string.start);
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        startPreview();
    }

    void exitRecordingVideo() {  
    	mCameraOpenCloseLock.release();      
        closeCamera();
        stopBackgroundThread();      
        removeFragment();
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

	private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

	private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
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

	private void stopBackgroundThread() {
		if (null != mBackgroundThread) {
			mBackgroundThread.quitSafely();
	        try {
	            mBackgroundThread.join();
	            mBackgroundThread = null;
	            mBackgroundHandler = null;
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
		}
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    boolean isRecordingVideo() {
		return mIsRecordingVideo;
	}

    private void removeFragment() {
        if (getFragmentManager() != null && mInstance != null) {
			getFragmentManager().beginTransaction().remove(mInstance)
                				.commitAllowingStateLoss();
		}
    }
}
