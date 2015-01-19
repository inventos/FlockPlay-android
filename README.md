FlockPlay Android
=========

- [Description of the technology](#description-of-the-technology)
- [Requirements](#requirements)
- [Video player connection](#version-of-the-compiler)
- [Setup](#setup)
- [Editing manifest](#editing-manifest)
- [Usage](#usage)
- [Traffic](#traffic)
- [Example](#example)
- [Possible problems and their solutions](#solutions)

<a name="description-of-the-technology"></a>Description of the technology
-------------------------------------------------------------------------

FlockPlay technology for the Android platform is a proxy server and P2P module. 

<a name="requirements"></a>Requirements
---------------------------------------

Before you use the library, make sure that you have the correct version of the compiler JAVA - 1.6. or higher.

- The library has been developed for the platform Android 4.0 or higher.
- Currently it supports processor architectures - armeabi-v7a (armv7), ia32 (x86) and arm64-v8a (armv8).

<a name="description-of-the-technology"></a>Version of the compiler
-------------------------------------------------------------------

Before you use the library, make sure that you have the correct version of the compiler JAVA - 1.6. or higher.

![android_java_version](https://peer-control.megacdn.ru/images/setup_android_java_version.png)

<a name="setup"></a>Setup
-------------------------
Next, you need to download the following library files.
***To avoid errors, use the recommended libs!***

| Name                     | Recommended version | JAR                              | armeabi | armeabi-v7a                        | x86                                | x86_64 | arm64-v8a |
|:------------------------:|:-------------------:|:--------------------------------:|:-------:|:----------------------------------:|:----------------------------------:|:------:|:----:|
|android-async-http        | 1.4.4               | [android-async-http-1.4.4.jar](https://github.com/loopj/android-async-http/raw/master/releases/android-async-http-1.4.4.jar) |    -    |      -                             |  -                                 |   -    |  -   |
|autobahn-ws 	           | 0.5.0               | [autobahn-0.5.0.jar](https://autobahn.s3.amazonaws.com/android/autobahn-0.5.0.jar)           |    -    |      -                             |  -                                 |   -    |  -   |
|libjingle_peerconnection  | -                | [libjingle_peerconnection.jar](https://github.com/inventos/FlockPlay-android/blob/master/libs/android-flockplay.jar?raw=true) |    -    | [libjingle_peerconnection_so.so](https://github.com/inventos/FlockPlay-android/blob/master/libs/armeabi-v7a/libjingle_peerconnection_so.so?raw=true) | [libjingle_peerconnection_so.so](https://github.com/inventos/FlockPlay-android/blob/master/libs/x86/libjingle_peerconnection_so.so?raw=true) |   -    |  [libjingle_peerconnection_so.so](https://github.com/inventos/FlockPlay-android/blob/master/libs/arm64-v8a/libjingle_peerconnection_so.so?raw=true)   |
|android-flockplay 	       |   -                 | [android-flockplay.jar](https://github.com/inventos/FlockPlay-android/blob/master/libs/android-flockplay.jar?raw=true)        |    -    |      -                             |  -                                 |   -    |  -   |

Now add the JAR files to the _libs_ project and SO files in the folders _libs/armeabi-v7a_, _libs/x86_ and _libs/arm64-v8a_.

Next, add files to project build path.

<a name="editing-manifest"></a>Editing manifest
-----------------------------------------------

Add in _AndroidManifest.xml_ file the following permissions: 

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/> 
```

and information about the current SDK: 

```xml
<uses-sdk android:minSdkVersion="14" android:targetSdkVersion="21"/> 
```

<a name="usage"></a>Usage
-------------------------

First, fill the structure `ru.inventos.flockplay.p2p.Options`.
It has several fields: 
- `Options.tag` - translation identifier.
- `Options.key` - key.

Next you have to implement interface and send object to server constructor.

```java
public interface AbstractMediaPlayer {
    public int getCurrentPosition ();
    public boolean isPlaying();
}
```
Where:

```java
public int getCurrentPosition();
```

returns current playback position in milliseconds.

Constructor interface:

```java
public ProxyServer (Options o, Context c,AbstractMediaPlayer p);
```

Example could look like this:

```java
VideoView player = ...;
AbstractMediaPlayer abstractMediaPlayer = new AbstractMediaPlayer() {
    @Override
    public int getCurrentPosition() {
        return player.getCurrentPosition();
    }
    @Override
    public boolean isPlaying() {
        return player.isPlaying();
    }
};
ProxyServer server = new ProxyServer(ops,getContext(),abstractMediaPlayer);
```

After creating the server does not automatically start. Therefore, run it by: 

```java
public boolean start (int p)
```

The only argument is the port number.
If the call returns _false_ - then launch failed.
Now the server is running is the last step - prepare URL playlist.
For this purpose the method: 

```java
public android.net.Uri preparePlaylist (java.lang.String u)
```

The resulting object is passed to the video player.

In order to permanently shut down the server, use this method: 

```java
public void destroy (android.content.Context c)
```

<a name="traffic"></a>Traffic
-----------------------------

Using the information about the current internet connection (Wi-Fi, 4G or 3G / 2G) plugin can automatically disable the ability to serve the files.
In order to globally block the uploading of files (data about the internet connection will not be affected by this flag), you must call the class `ru.inventos.flockplay.p2p.ProxyServer`: 

```java
public void setSendingDisabled (boolean f)
```

It should be noted that the possibility of a forced enable file uploading not provided.


<a name="example"></a>Example
-----------------------------

Using the server class with `VideoView`:

***ExampleActivity.java***
```java
public class ExampleActivity extends Activity {

    private ProxyServer mProxyServer;
    private VideoView mVideoView;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_layout);
        mVideoView = (VideoView)findViewById(R.id.video_view);
        Options options = new Options();
        options.tag = "github";
        options.key = "demo";
        AbstractMediaPlayer abstractMediaPlayer = new AbstractMediaPlayer() {
            @Override
            public int getCurrentPosition() {
                return mVideoView.getCurrentPosition();
            }

            @Override
            public boolean isPlaying() {
                return mVideoView.isPlaying();
            }
        };
        mProxyServer = new ProxyServer(options,this,abstractMediaPlayer);        
    }

    @Override    
    public void onStop () {
        mProxyServer.pause();
        super.onStop();
    }

    @Override
    public void onStart () {
        super.onStart();
        if (mProxyServer.start(8089)) {
            mVideoView.setVideoURI(mProxyServer.preparePlaylist("http://flockplay.com/test/playlist.m3u8"));
            mVideoView.requestFocus();
            mVideoView.start();
        }        
    }

    @Override
    protected void onDestroy() {
        mProxyServer.destroy(this);
        super.onDestroy();
    }

}
```

<a name="solutions"></a>Possible problems and their solutions
-------------------------------------------------------------

- An error occurred while building the project in Android Studio: "Unable to execute DX"

***Solution:*** Go to the _"Project Structure"_ -> _"Facets"_ -> _"Packaging"_ -> Uncheck _"Pre-dex external jars and Android library dependencies"_ 
