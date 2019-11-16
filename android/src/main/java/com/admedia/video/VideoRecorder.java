package com.admedia.video;

import com.admedia.utils.Messages;
import com.admedia.utils.MediaConstants;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;

import android.content.Context;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class VideoRecorder extends ReactContextBaseJavaModule 
            implements PermissionListener {

    private static final String TAG = "VIDEO_RECORD_ERROR";
    private static final int PERMISSIONS_REQUEST_CODE = 10070;    
    
    private static final String[] PERMISSIONS = 
                        new String[] {   
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, 
                            Manifest.permission.RECORD_AUDIO, 
                            Manifest.permission.CAMERA 
                        };

    private boolean mIsPermissionGranted = false;

    private Promise mVideoRecordPromise;
    private HashMap mRequestData;
    private FragmentManager mFragmentManager;
    private VideoFragment mVideoFragment;       

    public VideoRecorder(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "Video";
    }

    @Override
    public boolean onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE && 
            grantResults.length == PERMISSIONS.length) {
            mIsPermissionGranted = true;
            record();
        } else {
            mVideoRecordPromise.reject(TAG, Messages.PERMISSION_DENIED);
        }
        return true;
    } 
     
    @ReactMethod
    public void prepare(ReadableMap map, Promise promise) {
        mRequestData = map.toHashMap();
        mVideoRecordPromise = promise;
        checkPermission();
    }

    @ReactMethod
    public void startRecording(Promise promise) {
        getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {                
                if (mVideoFragment != null && !mVideoFragment.isRecordingVideo()) {
                    mVideoFragment.startRecordingVideo();
                }
            }
        });
    }

    @ReactMethod
    public void stopRecording() {
        getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (mVideoFragment != null && mVideoFragment.isRecordingVideo()) {
                    mVideoFragment.stopRecordingVideo();
                }
            }
        });
    }

    @ReactMethod
    public void exitRecording() {
        getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (mVideoFragment != null) {
                    mVideoFragment.exitRecordingVideo();
                    mVideoFragment = null;
                }
            }
        });
    }

    @ReactMethod
    public void isRecording(Promise promise) {
        if (mVideoFragment == null) {
           promise.resolve(false);
        }
        promise.resolve(mVideoFragment.isRecordingVideo());
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
                mVideoRecordPromise.reject(TAG, Messages.NO_OUTPUT_FOLDER);
            }            
                                   
            mFragmentManager = activity.getFragmentManager();
            mVideoFragment = VideoFragment.newInstance();                  
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            Bundle bundle = new Bundle(); 
            bundle.putSerializable("recorderData", mRequestData);  
            mVideoFragment.setArguments(bundle);       
            transaction.replace(android.R.id.content, mVideoFragment);
            transaction.addToBackStack(null);
            transaction.commitAllowingStateLoss();  
            
        } catch(Exception ex)  {
            if (mVideoRecordPromise != null) {
                mVideoRecordPromise.reject(TAG, ex.getMessage());
            }            
        }
    }        
}
