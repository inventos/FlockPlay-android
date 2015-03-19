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

Before you use the library, make sure that you have the correct version of the compiler JAVA - 1.7. or higher.

- The library has been developed for the platform Android 4.0 or higher.
- Currently it supports processor architectures - armeabi-v7a (armv7) and ia32 (x86).

<a name="description-of-the-technology"></a>Version of the compiler
-------------------------------------------------------------------

Before you use the library, make sure that you have the correct version of the compiler JAVA - 1.7. or higher.

<a name="setup"></a>Setup
-------------------------
Next, you need to download the following library files.
***To avoid errors, use the recommended libs!***

| Name                     | Recommended version | JAR                              | armeabi | armeabi-v7a                        | x86                                | x86_64 | arm64-v8a |
|:------------------------:|:-------------------:|:--------------------------------:|:-------:|:----------------------------------:|:----------------------------------:|:------:|:----:|
|android-async-http        | 1.4.6               | [android-async-http-1.4.6.jar](https://github.com/loopj/android-async-http/raw/master/releases/android-async-http-1.4.6.jar) |    -    |      -                             |  -                                 |   -    |  -   |
|tyrus-standalone-client 	           | 1.10               | [tyrus-standalone-client-1.10.jar](http://repo1.maven.org/maven2/org/glassfish/tyrus/bundles/tyrus-standalone-client/1.10/tyrus-standalone-client-1.10.jar)           |    -    |      -                             |  -                                 |   -    |  -   |
|libjingle_peerconnection  | -                | [libjingle_peerconnection.jar](https://github.com/inventos/FlockPlay-android/blob/master/libs/android-flockplay.jar?raw=true) |    -    | [libjingle_peerconnection_so.so](https://github.com/inventos/FlockPlay-android/blob/master/libs/armeabi-v7a/libjingle_peerconnection_so.so?raw=true) | [libjingle_peerconnection_so.so](https://github.com/inventos/FlockPlay-android/blob/master/libs/x86/libjingle_peerconnection_so.so?raw=true) |   -    |  -  |
|android-flockplay 	       |   2                 | [android-flockplay-2.jar](https://github.com/inventos/FlockPlay-android/blob/master/libs/android-flockplay-2.jar?raw=true)        |    -    |      -                             |  -                                 |   -    |  -   |

Now add the JAR files to the _libs_ project and SO files in the folders _libs/armeabi-v7a_ and _libs/x86_.

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
    public int getCurrentPositionMs ();
}
```
Where:

```java
public int getCurrentPositionMs();
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
    public int getCurrentPositionMs() {
        return player.getCurrentPosition();
    }
};
ProxyServer server = new ProxyServer(ops,getContext(),abstractMediaPlayer);
```

After creating the server does not automatically start. Therefore, run it by: 

```java
public boolean open ()
```

If the call returns _false_ - then launch failed.
Now the server is running is the last step - prepare URL playlist.
For this purpose the method: 

```java
public android.net.Uri preparePlaylist (java.lang.String u)
```

The resulting object is passed to the video player.

In order to permanently shut down the server, use this method: 

```java
public void shutdown (android.content.Context c)
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
            public int getCurrentPositionMs() {
                return mVideoView.getCurrentPosition();
            }
        };
        mProxyServer = new ProxyServer(options,this,abstractMediaPlayer);
        mProxyServer.open()
    }

    @Override    
    public void onStop () {
        mProxyServer.pause();
        super.onStop();
    }

    @Override
    public void onStart () {
        super.onStart();
        mProxyServer.resume();
        mVideoView.setVideoURI(mProxyServer.preparePlaylist("http://flockplay.com/test/playlist.m3u8"));
        mVideoView.requestFocus();
        mVideoView.start();
    }

    @Override
    protected void onDestroy() {
        mProxyServer.shutdown(this);
        super.onDestroy();
    }

}
```

<a name="solutions"></a>Possible problems and their solutions
-------------------------------------------------------------

- An error occurred while building the project in Android Studio: "Unable to execute DX"

***Solution:*** Go to the _"Project Structure"_ -> _"Facets"_ -> _"Packaging"_ -> Uncheck _"Pre-dex external jars and Android library dependencies"_ 
