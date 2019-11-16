package com.admedia.audio;

import com.admedia.utils.MediaConstants;
import com.admedia.R;

import android.content.Context;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.app.Fragment; 
import android.app.Activity;
import android.os.Bundle;
import android.media.MediaRecorder;
import android.util.SparseIntArray;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.text.SimpleDateFormat;

public class AudioFragment extends Fragment implements View.OnClickListener {
	
	private MediaRecorder mMediaRecorder = null;	
	private HashMap<String, Object> mRequestData;

	private static AudioFragment mInstance;
	private Button mButtonAudio;
	private TextView mTextAudio;

	private boolean mIsRecordingAudio = false;

	private static final Map<String, Object> constants = MediaConstants.getConstants();

	private MediaRecorder.OnInfoListener mRecorderListener = new MediaRecorder.OnInfoListener() { 
		@Override
		public void onInfo(MediaRecorder mr, int response, int extra) {
			if ( response == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
			     response == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED ) {
				stopRecordingAudio();
			}
		}
	};	

	public static AudioFragment newInstance() {
		mInstance = new AudioFragment();
		return mInstance;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Bundle bundle = getArguments();
        mRequestData = (HashMap) bundle.getSerializable("recorderData");
		return inflater.inflate(R.layout.admedia_fragment_audio, container, false);
	}

	@Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mButtonAudio = (Button) view.findViewById(R.id.audio);
        mTextAudio = (TextView) view.findViewById(R.id.audioText);
        mButtonAudio.setOnClickListener(this);
        mButtonAudio.setText(R.string.start);
        mTextAudio.setText(R.string.ready);
    }	

    @Override
    public void onClick(View view) {
    	if (mIsRecordingAudio) {    		
    		stopRecordingAudio();
    	} else {    		
    		startRecordingAudio();
    	}        
    }  

	@Override
	public void onDetach() {
		super.onDetach();
		exitRecordingAudio();
	} 

	@Override
	public void onStop() {
		super.onStop();
		exitRecordingAudio();
	}         
	
	void startRecordingAudio() {
		try {				
			mMediaRecorder = new MediaRecorder();
			
			SparseIntArray params = MediaConstants.getParams(mRequestData, "audio");
			int outputFormat = params.get(MediaConstants.OUTPUT_FORMAT);
	        mMediaRecorder.setAudioSource(params.get(MediaConstants.AUDIO_SOURCE));
	        mMediaRecorder.setOutputFormat(outputFormat);
	        mMediaRecorder.setAudioEncoder(params.get(MediaConstants.AUDIO_ENCODER));

	        String path = (String) mRequestData.get("outputFolder");
	        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
	        String extension = MediaConstants.getFileExtension(outputFormat);

	        mMediaRecorder.setOutputFile(path + timeStamp + extension);	        
	        
	        if (params.get(MediaConstants.MAX_DURATION) > 0) {
	        	mMediaRecorder.setMaxDuration(params.get(MediaConstants.MAX_DURATION));
	        }

	        if (params.get(MediaConstants.MAX_FILE_SIZE) > 0) {
	        	mMediaRecorder.setMaxDuration(params.get(MediaConstants.MAX_FILE_SIZE));
	        }

			mMediaRecorder.setOnInfoListener(mRecorderListener); 
			mMediaRecorder.prepare();
			mIsRecordingAudio = true;
			mButtonAudio.setText(R.string.stop);
			mTextAudio.setText(R.string.recording);
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					mMediaRecorder.start();
				}
			});
			
		} catch (Exception ex) {
			Activity activity = getActivity();
			if (activity != null) {
				Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
			}			
		 	stopRecordingAudio();
		}
	}

	void stopRecordingAudio() {
		if (mMediaRecorder != null) {
			mIsRecordingAudio = false;
			mButtonAudio.setText(R.string.start);
			mTextAudio.setText(R.string.ready);
			mMediaRecorder.stop();
			mMediaRecorder.reset();
			mMediaRecorder.release();
			mMediaRecorder = null;
		}
	}

	boolean isRecordingAudio() {
		return mIsRecordingAudio;
	}

	void exitRecordingAudio() {
		stopRecordingAudio();
		if (getFragmentManager() != null) {
			getFragmentManager().beginTransaction().remove(mInstance)
                				.commitAllowingStateLoss();
		}
	}
}
