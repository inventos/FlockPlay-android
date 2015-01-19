package ru.inventos.flockplay.example;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.VideoView;

import ru.inventos.flockplay.p2p.AbstractMediaPlayer;
import ru.inventos.flockplay.p2p.Options;
import ru.inventos.flockplay.p2p.ProxyServer;

public class MainActivity extends Activity {

    private VideoController mController;
    private ProxyServer mProxyServer;
    private VideoView mVideoView;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.example_layout);
        getActionBar().setLogo(R.drawable.ic_launcher);
        mController = (VideoController)findViewById(R.id.controller);
        mVideoView = (VideoView)findViewById(R.id.video_view);
        mVideoView.setOnPreparedListener(onPreparedListener);
        mController.setMediaPlayer(mVideoView);
        mController.setOnControllerShowListener(controllerShowListener);
        Options options = new Options();
        options.tag = "default";
        options.key = "test_key";
        mProxyServer = new ProxyServer(options,this,abstractMediaPlayer);
        findViewById(R.id.frame).setOnTouchListener(controllerToggler);
    }

    private final AbstractMediaPlayer abstractMediaPlayer = new AbstractMediaPlayer() {
        @Override
        public int getCurrentPosition() {
            return mVideoView != null ? mVideoView.getCurrentPosition() : 0;
        }

        @Override
        public boolean isPlaying() {
            return mVideoView != null && mVideoView.isPlaying();
        }
    };

    private final MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.start();
            mController.update();
            mController.hide();
        }
    };

    private final View.OnTouchListener controllerToggler = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mController.isShowing()) {
                mController.hide();
            } else {
                mController.show();
            }
            return false;
        }
    };

    private final VideoController.OnControllerShowListener controllerShowListener = new VideoController.OnControllerShowListener() {
        @Override
        public void onShow() {
            getActionBar().show();
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }

        @Override
        public void onHide() {
            getActionBar().hide();
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    };

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
        }
    }

    @Override
    protected void onDestroy() {
        mProxyServer.destroy(this);
        super.onDestroy();
    }

}
