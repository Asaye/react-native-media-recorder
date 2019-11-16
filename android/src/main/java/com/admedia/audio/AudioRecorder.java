package com.admedia.audio;

import com.admedia.utils.Messages;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;
import com.facebook.react.bridge.ReadableMap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Context;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Process;

import java.util.HashMap;

public class AudioRecorder extends ReactContextBaseJavaModule 
            implements PermissionListener {

    private static final String TAG = "AUDIO_RECORD_ERROR";
    private static final int PERMISSIONS_REQUEST_CODE = 10001; 

    private static final String[] PERMISSIONS = 
                        new String[] {   
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, 
                            Manifest.permission.RECORD_AUDIO 
                        };

    private boolean mIsPermissionGranted = false;
    private Promise mAudioRecordPromise;
    private HashMap mRequestData;
    private FragmentManager mFragmentManager;
    private AudioFragment mRecorderFragment;      

    public AudioRecorder(ReactApplicationContext reactContext) {
        super(reactContext);    
    }

    @Override
    public String getName() {
        return "Audio";
    }

    @Override
    public boolean onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE && 
            grantResults.length == PERMISSIONS.length) {
            mIsPermissionGranted = true;
            record();
        } else {
            mAudioRecordPromise.reject(TAG, Messages.PERMISSION_DENIED);
        }
        return true;
    }

    @ReactMethod
    public void prepare(ReadableMap map, Promise promise) {
        mRequestData = map.toHashMap();
        mAudioRecordPromise = promise;
        checkPermission();
    }

    @ReactMethod
    public void startRecording(Promise promise) {
        getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (mRecorderFragment != null && !mRecorderFragment.isRecordingAudio()) {
                    mRecorderFragment.startRecordingAudio();
                }
            }
        });
    }

    @ReactMethod
    public void stopRecording() {
        getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (mRecorderFragment != null && mRecorderFragment.isRecordingAudio()) {
                    mRecorderFragment.stopRecordingAudio();
                }
            }
        });
    }

    @ReactMethod
    public void isRecording(Promise promise) {
        if (mRecorderFragment == null) {
           promise.resolve(false);
        }
        promise.resolve(mRecorderFragment.isRecordingAudio());
    }  

    @ReactMethod
    public void exitRecording() {
        getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (mRecorderFragment != null) {
                    mRecorderFragment.exitRecordingAudio();
                }
            }
        });
    }

    private void checkPermission() {        
        int pid = Process.myPid();
        int uid = Process.myUid();
        int status_ok = PackageManager.PERMISSION_GRANTED;
        Activity activity = getCurrentActivity();
        Context context = getReactApplicationContext().getBaseContext();

        mIsPermissionGranted = true;
        for (String permission : PERMISSIONS) {
            if (context.checkPermission(permission, pid, uid) != status_ok) {
                mIsPermissionGranted = false;
                ((PermissionAwareActivity) activity)
                        .requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE, this);
                break;
            }
        } 

        if (mIsPermissionGranted) {
            record();
        }        
    }

    private void record() {
        try {
            Activity activity = getCurrentActivity();

            assert activity == null;

            if (mRequestData == null || mRequestData.get("outputFolder") == null) {
                mAudioRecordPromise.reject(TAG, Messages.NO_OUTPUT_FOLDER);
                return;
            }              

            mFragmentManager = activity.getFragmentManager();
            mRecorderFragment = AudioFragment.newInstance();                  
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            Bundle bundle = new Bundle(); 
            bundle.putSerializable("recorderData", mRequestData);  
            mRecorderFragment.setArguments(bundle);       
            transaction.replace(android.R.id.content, mRecorderFragment);
            transaction.addToBackStack(null);
            transaction.commitAllowingStateLoss();
            
        } catch(Exception ex)  {
            mAudioRecordPromise.reject(TAG, ex.getMessage());
        }
    }  
}
