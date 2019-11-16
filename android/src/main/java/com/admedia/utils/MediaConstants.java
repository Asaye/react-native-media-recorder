package com.admedia.utils;

import android.media.MediaRecorder;
import android.media.CamcorderProfile;
import android.util.SparseIntArray;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class MediaConstants {

	private static final Map<String, Object> constants = new HashMap<>();
    private static String[] params_video = { "audioSource", "outputFormat", 
                                           "audioEncoder", "videoEncoder" };
    private static String[] params_audio = { "audioSource", "outputFormat", "audioEncoder" };
    private static String[] params_num = { "maxDuration", "maxFileSize" };
    private static String[] params_num_img = { "width", "height" };

    public static final int AUDIO_SOURCE = 10010;
    public static final int OUTPUT_FORMAT = 10011;
    public static final int AUDIO_ENCODER = 10012;
    public static final int VIDEO_ENCODER = 10013;
    public static final int MAX_DURATION = 10014;
    public static final int MAX_FILE_SIZE = 10015;
    public static final int FILE_EXTENSION = 10016;
    public static final int WIDTH = 10017;
    public static final int HEIGHT = 10020;

    private static final Map<String, Integer> PARAMETER_TYPE = new HashMap<String, Integer>();
    static {
    	PARAMETER_TYPE.put("audioSource", new Integer(AUDIO_SOURCE));
    	PARAMETER_TYPE.put("outputFormat", new Integer(OUTPUT_FORMAT));
    	PARAMETER_TYPE.put("audioEncoder", new Integer(AUDIO_ENCODER));
    	PARAMETER_TYPE.put("videoEncoder", new Integer(VIDEO_ENCODER));
    	PARAMETER_TYPE.put("maxDuration", new Integer(MAX_DURATION));
    	PARAMETER_TYPE.put("maxFileSize", new Integer(MAX_FILE_SIZE));
    	PARAMETER_TYPE.put("width", new Integer(WIDTH));
    	PARAMETER_TYPE.put("height", new Integer(HEIGHT));
    }

	static {
		constants.put("MIC", MediaRecorder.AudioSource.MIC);
		constants.put("CAMCORDER", MediaRecorder.AudioSource.CAMCORDER);
		constants.put("VOICE_RECOGNITION", MediaRecorder.AudioSource.VOICE_RECOGNITION);
		constants.put("DEFAULT", MediaRecorder.AudioSource.DEFAULT);
		constants.put("VOICE_DOWNLINK", MediaRecorder.AudioSource.VOICE_DOWNLINK);
		constants.put("VOICE_UPLINK", MediaRecorder.AudioSource.VOICE_UPLINK);
		constants.put("VOICE_CALL", MediaRecorder.AudioSource.VOICE_CALL);
		constants.put("VOICE_COMMUNICATION", MediaRecorder.AudioSource.VOICE_COMMUNICATION);

		constants.put("MPEG_4", MediaRecorder.OutputFormat.MPEG_4);
		constants.put("THREE_GPP", MediaRecorder.OutputFormat.THREE_GPP);
		constants.put("WEBM", MediaRecorder.OutputFormat.WEBM);   

		constants.put("AAC", MediaRecorder.AudioEncoder.AAC);  
		constants.put("AAC_ELD", MediaRecorder.AudioEncoder.AAC_ELD);  
		constants.put("AMR_NB", MediaRecorder.AudioEncoder.AMR_NB); 
		constants.put("DEFAULT_EN", MediaRecorder.AudioEncoder.DEFAULT); 

		constants.put("DEFAULT_V_EN", MediaRecorder.VideoEncoder.DEFAULT);  
		constants.put("H264", MediaRecorder.VideoEncoder.H264);  
		constants.put("HEVC", MediaRecorder.VideoEncoder.HEVC); 
		constants.put("MPEG_4_SP", MediaRecorder.VideoEncoder.MPEG_4_SP); 
		constants.put("VP8", MediaRecorder.VideoEncoder.VP8); 

		// constants.put("QUALITY_HIGH", CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));  
		// constants.put("QUALITY_LOW", CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));  
		// constants.put("QUALITY_2160P", CamcorderProfile.get(CamcorderProfile.QUALITY_2160P)); 
		// constants.put("QUALITY_1080P", CamcorderProfile.get(CamcorderProfile.QUALITY_1080P)); 
		// constants.put("QUALITY_720P", CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
		// constants.put("QUALITY_480P", CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
		
	}

	public static Map getConstants() {
		return constants;
	}

	public static SparseIntArray getParams(HashMap mRequestData, String type) {

        final SparseIntArray array = new SparseIntArray();

        array.append(AUDIO_SOURCE, (int) constants.get("MIC"));
        array.append(AUDIO_ENCODER, (int) constants.get("AMR_NB"));
        array.append(OUTPUT_FORMAT, (int) constants.get("MPEG_4"));

        if (type.equals("video")) {        	
            array.append(VIDEO_ENCODER, (int) constants.get("MPEG_4_SP"));
        }

        if (mRequestData == null) {
        	return array;
        }

        if (type.equals("video")) {
            for (String item: params_video) {
                if (mRequestData.get(item) != null) {
                    array.append(PARAMETER_TYPE.get(item).intValue(), (int) constants.get(mRequestData.get(item)));
                }
            }
        } else {
            for (String item: params_audio) {
                if (mRequestData.get(item) != null) {
                    array.append(PARAMETER_TYPE.get(item).intValue(), (int) constants.get(mRequestData.get(item)));
                }
            }
        }

        for (String item: params_num) {
            if (mRequestData.get(item) != null && mRequestData.get(item) instanceof Double) {
                Double data = (Double) mRequestData.get(item);
                array.append(PARAMETER_TYPE.get(item).intValue(), data.intValue());
            } else {
                array.append(PARAMETER_TYPE.get(item).intValue(), -1);
            } 
        }
       
        return array;
    }

    public static SparseIntArray getImageParams(HashMap mRequestData) {

    	if (mRequestData == null) {
        	return null;
        }

        final SparseIntArray array = new SparseIntArray();  

        for (String item: params_num) {
            if (mRequestData.get(item) != null && mRequestData.get(item) instanceof Double) {
                Double data = (Double) mRequestData.get(item);
                array.append(PARAMETER_TYPE.get(item).intValue(), data.intValue());
            } else {
                array.append(PARAMETER_TYPE.get(item).intValue(), -1);
            }   
        }       

        return array;
    }

    public static String getFileExtension(int outputFormat) {
    	switch (outputFormat) {
        	case MediaRecorder.OutputFormat.THREE_GPP: {
        		return ".3gpp";
        	}
        	case MediaRecorder.OutputFormat.WEBM: {
        		return ".webm";
        	}
        	default: {        		
        		return ".mp4";
        	}
        }
    }
}