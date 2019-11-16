# react-native-media-recorder

<p style = "text-align: justify">A simple react-native module which is used to record audio and video with android phones.

# Getting Started

### Installation

Using npm

```	$ npm install react-native-media-recorder --save```

Using yarn

```	$ yarn add react-native-media-recorder```

### Linking
There are two options for linking:

##### 1. Automatic

```	react-native link react-native-media-recorder```

##### 2. Manual

If the automatic linking fails for some reason, you can do the linking manually as follows:
 * add the following to <code>yourAppName/android/settings.gradle</code> file:
 
 ```
 	include ':react-native-media-recorder'
 	project(':react-native-media-recorder').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-media-recorder/android')
 ```

 * add the following inside the dependencies closure of  <code>yourAppName/android/app/build.gradle</code> file:
 ```
 	implementation project(':react-native-media-recorder')
```

* add the following to your <code>MainApplication.java</code> file:
 ```
 	import com.admedia.MediaPackage;
 ```
 and also,
 ```
	@Override
	protected List<ReactPackage> getPackages() {
		return Arrays.<ReactPackage>asList(
			new MainReactPackage(),
			....
			new MediaPackage()                    <== Add this
		);
	}
 ```

### Usage
To record audio or video, first import the module to your app like so:

```   import { Audio, Video } from 'react-native-media-recorder';```

After this, you can call any of the functions stated below for the recording activity. You can call the same
functions on both <code>Audio</code> and <code>Video</code> objects to perform similar tasks. However, 
there are some differences in the parameters passed to <code>prepare(options:object)</code> function. The differences
are clearly stated below.

## Functions
<p style = "text-align: justify">The following functions can be called on both <code>Audio</code> 
and <code>Video</code> objects.</p>

``` 
    prepare(Object options)
    startRecording()
    stopRecording()
    isRecording()
    exitRecording()
```

### Permissions
<p style = "text-align: justify">The following permissions are common for both audio and video recording activities and 
they should be included in the AndroidManifest.xml file.</p>

```     
        <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
        <uses-permission android:name="android.permission.RECORD_AUDIO" /
```

In addition, for video recording the following permissions are essential.

```     
        <uses-permission android:name="android.permission.CAMERA"/>
        <uses-feature android:name="android.hardware.camera" android:required="true" />
```

### Description
<p style = "text-align: justify">The above functions are used to perform the following activities.</p>

#### prepare(Object options): 

<p style = "text-align: justify">is used to prepare an audio/video recroding environment. The <code>options</code> parameter can have the properties shown below. 

   #### Properties common to both Audio and Video Recording:    
<table>
<tr><th>Prop</th><th>Required</th><th>Default</th><th style =  "width: 150px">Type</th><th>Description</th></tr>
<tr><td>audioSource </td><td> false</td><td>"DEFAULT" </td><td>enum( "MIC" ,  "DEFAULT" , "CAMCORDER" ,  "VOICE_UPLINK",  "VOICE_CALL", "VOICE_DOWNLINK", "VOICE_COMMUNICATION",  "VOICE_RECOGNITION") </td><td style = "text-align: justify">An audio source which will be used for the audio recoding activity.</td></tr>
<tr><td>outputFormat  </td><td> false</td><td>"MPEG_4"</td><td>enum( "MPEG_4" , "3GPP", "WEBM" )</td><td style = "text-align: justify">An output format for the type of the media to be used for the output file.</td></tr>
<tr><td>audioEncoder</td><td> false</td><td>"AMR_NB" </td><td>enum( "AMR_NB" , "DEFAULT_EN",  "AAC",  "AAC_ELD" )</td><td style = "text-align: justify">An audio encoder used to process the audio data.</td></tr>
<tr><td>outputFolder </td><td>true</td><td>-</td><td>String</td><td style = "text-align: justify">The path to the folder where the recorded file will be saved.</td></tr>
<tr><td>maxDuration</td><td> false</td><td>-</td><td>int</td><td style = "text-align: justify">The maximum duration (in milliseconds) of the recording.</td></tr>
<tr><td>maxFileSize</td><td> false</td><td>-</td><td>int</td><td style = "text-align: justify">The maximum file size (in bytes) of the recoded file.</td></tr>
</table>

All the above properties except the <code>outputFolder</code> are optional. If you  don't specify them, default values ,as described above, will be assigned to those properties during runtime.

#### Properties peculiar to Video Recording:

<table>
<tr><th>Prop</th><th>Required</th><th>Default</th><th style =  "width: 150px">Type</th><th>Description</th></tr>
<tr><td>videoEncoder</td><td> false</td><td> "MPEG_4_SP"</td><td> enum( "MPEG_4_SP" , "VP8",  "HEVC",  "H264", "DEFAULT_V_EN" )</td><td style = "text-align: justify">An encoder used to process video data.</td></tr>
<tr><td>cameraType</td><td> false</td><td>"BACK"</td><td>enum( "BACK", "FRONT")</td><td style = "text-align: justify">The type of camera to be used for the recording.</td></tr>
</table>


##### Sample code snippet
``` 
        import { Audio, Video } from 'react-native-media-recorder';
        .....
        .....

        _prepareMediaRecorder = () => {
            const params = { "outputFolder": "Music/" };
            Audio.prepare(params);  // to prepare an audio recording environment
            /* Video.prepare(params);   use this instead to prepare a video recording environment */
        } 
```

<p style = "text-align: justify">Invoking the <code>_prepareMediaRecorder()</code> function prepares the media recorder for the  recording activity so that the output will be saved in <code>Music</code> folder (Don't forget the last backslash). This makes the media recorder to be ready for recording and a user interface with a START/STOP button appears on the screen of the device. After this, there are two options to start recording. The first one is to call the <code>startRecording()</code> function programmatically. And the second option is to manually press the START button on the created user interface.</p>

#### startRecording(): 

<p style = "text-align: justify">is used to start audio/video recroding activity. Calling this function is equivalent to pressing the START button. After calling the <code>prepare()</code> function and before calling this one, I recommend to provide a few milli seconds gap to make sure that the recording environment is prepared and ready to record. Once the recording is started, it can be terminated by:</p>

* explicitly calling <code>stopRecording()</code> function.
* pressing the <code>STOP</code> button which appears on the recording view.
* setting <code>maxDuration</code> property in the arguments passed to <code>prepare()</code> function.
* setting <code>maxFileSize</code> property in the arguments passed to <code>prepare()</code> function.
* explicitly calling <code>exitRecording()</code> function.
* pressing the back button.

<p style = "text-align: justify">Note that if the recording is terminated by the first four ways, another session of recording can be started again by calling the <code>startRecording()</code> method or by pressing the START button and the loop continues until the recording is terminated by pressing the back button or calling the <code>exitRecording()</code> function.</p>
    
##### Sample code snippet
``` 
        import { Audio, Video } from 'react-native-media-recorder';
        .....
        .....

        _startMediaRecording = () => {
          setTimeout(() => {
            Audio.startRecording(); // to start audio recording. 
            /*  Video.startRecording(); use this instead to start video recording */
          }, 1000);         
        } 
```

<p style = "text-align: justify">Invoking the <code>_startMediaRecording()</code> function starts recording after 1000ms.</p>

#### stopRecording(): 

<p style = "text-align: justify">is used to terminate an ongoing media recording activity. Calling this function is equivalent to pressing the STOP button. After calling this function, the media recording evnvironment is still set up and another session of audio/video recording can be started by calling <code>startRecording()</code> function or by pressing the START button.</p>

##### Sample code snippet
``` 
            import { Audio, Video } from 'react-native-media-recorder';
            .....
            .....

            _stopMediaRecording = () => {
                Audio.stopRecording(); // stops audio recording activity
                /* Video.stopRecording();  use this instead to stop an ongoing video recording. */
            } 
```
<p style = "text-align: justify">Invoking the <code>_stopMediaRecording()</code> function terminates an ongoing recording activity.</p>

#### isRecording(): 

<p style = "text-align: justify">is used to check whether there is an ongoing recording activity or not. This might be useful if you want to call <code>startRecording()</code> function while there is no any ongoing recording activity or to call <code>stopRecording()</code> function while there is a confirmed ongoing recording activity.</p>

##### Sample code snippet
``` 
            import { Audio, Video } from 'react-native-media-recorder';
            .....
            .....

            _checkMediaRecording = async () => {
                      Audio.isRecording().then((res) => {
                          if (res) Audio.stopRecording();
                      }).catch((err) => {
                          console.log(err);
                      });
                      
              /* the video counter part is: 
                      Video.isRecording().then((res) => {
                          if (res) Video.stopRecording();
                      }).catch((err) => {
                          console.log(err);
                      });  
              */
            } 
```
<p style = "text-align: justify">Invoking the <code>_checkMediaRecording()</code> checks if there is an ongoing recording activity and the <code>stopRecording()</code> function is called if the response is <code>true</code>.</p>

#### exitRecording(): 

<p style = "text-align: justify">is used to exit the recording activity. Calling this function is equivalent to pressing the back button. No further media recording session can be started after calling this funtion.</p>

##### Sample code snippet
```         
            import { Audio, Video } from 'react-native-media-recorder';
            .....
            .....

            _exitMediaRecording = () => {
                Audio.exitRecording();
                /*  Video.exitRecording();  to exit video recording */
            } 
```

<p style = "text-align: justify">Invoking the <code>_exitMediaRecording()</code> function aborts an ongoing recording activity and closes the recording view.</p>

## Issues or suggestions?
If you have any issues or if you want to suggest something , you can write it [here](https://github.com/Asaye/react-native-media-recorder/issues).

## Additional info
This is a component of a more comprehensive [react-native-system-applications](https://www.npmjs.com/package/react-native-system-applications) module developed by the same author.
