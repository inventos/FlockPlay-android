package ru.inventos.flockplay.example;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.Locale;

public class VideoController extends FrameLayout {

    public interface OnControllerShowListener {
        public void onShow();
        public void onHide();
    }

    private static final int DEFAULT_TIMEOUT = 5000;
    private static final int ANIMATION_DURATION = 250;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;

    private MediaController.MediaPlayerControl player;

    private ImageButton pauseButton;
    private ImageButton forwardButton;
    private ImageButton rewindButton;
    private ImageButton nextButton;
    private ImageButton prevButton;
    private SeekBar progress;
    private TextView endTime;
    private TextView currentTime;

    private StringBuilder formatBuilder;
    private Formatter formatter;

    private boolean showing;
    private boolean dragging;

    private OnClickListener nextListener;
    private OnClickListener prevListener;
    private OnControllerShowListener controllerShowListener;

    public VideoController(Context c) {
        super (c);
        init (c);
    }

    public VideoController(Context c, AttributeSet attrs) {
        super (c,attrs);
        init(c);
    }

    public VideoController(Context c, AttributeSet attrs, int df) {
        super (c,attrs,df);
        init(c);
    }

    private void init (Context c) {
        View.inflate(c, R.layout.video_controller, this);
        initControllerView();
        showing = true;
        setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                show();
                return false;
            }

        });
    }

    public void setOnControllerShowListener (OnControllerShowListener c) {
        controllerShowListener = c;
    }

    public void setMediaPlayer (MediaController.MediaPlayerControl p) {
        player = p;
        updatePausePlay();
    }

    private void initControllerView () {
        pauseButton = (ImageButton)findViewById(R.id.pause);
        pauseButton.requestFocus();
        pauseButton.setOnClickListener(pauseListener);
        forwardButton = (ImageButton)findViewById(R.id.ffwd);
        forwardButton.setOnClickListener(forwListener);
        forwardButton.setVisibility(GONE);
        rewindButton = (ImageButton)findViewById(R.id.rew);
        rewindButton.setOnClickListener(rewListener);
        rewindButton.setVisibility(GONE);
        nextButton = (ImageButton)findViewById(R.id.next);
        prevButton = (ImageButton)findViewById(R.id.prev);

        progress = (SeekBar)findViewById(R.id.mediacontroller_progress);
        progress.setMax(1000);

        if (progress instanceof SeekBar) {
            SeekBar seeker = progress;
            seeker.setOnSeekBarChangeListener(seekListener);
        }

        endTime = (TextView)findViewById(R.id.time);
        currentTime = (TextView)findViewById(R.id.time_current);
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());

        installPrevNextListeners();
    }

    public void show () {
        if (player != null /*&& player.isPlaying()*/) {
            show(DEFAULT_TIMEOUT);
        }
    }

    public void setLive (boolean live) {
        if (live) {
            progress.setVisibility(GONE);
            currentTime.setVisibility(GONE);
            endTime.setVisibility(GONE);
            forwardButton.setVisibility(GONE);
            rewindButton.setVisibility(GONE);
        } else {
            progress.setVisibility(VISIBLE);
            currentTime.setVisibility(VISIBLE);
            endTime.setVisibility(VISIBLE);
            forwardButton.setVisibility(VISIBLE);
            rewindButton.setVisibility(VISIBLE);
        }
    }

    private void disableUnsupportedButtons() {
        try {
            if (pauseButton != null && !player.canPause()) {
                pauseButton.setEnabled(false);
            }
            if (rewindButton != null && !player.canSeekBackward()) {
                rewindButton.setEnabled(false);
            }
            if (forwardButton != null && !player.canSeekForward()) {
                forwardButton.setEnabled(false);
            }
        } catch (Exception ex) {}
    }

    private void show (int timeout) {
        if (!showing) {
            setProgress();
            if (pauseButton != null) {
                pauseButton.requestFocus();
            }
            disableUnsupportedButtons();
            showing = true;
            animate().y(getContext().getResources().getDisplayMetrics().heightPixels - getHeight()).setDuration(ANIMATION_DURATION).start();
            if (controllerShowListener != null) controllerShowListener.onShow();
        }
        updatePausePlay();
        handler.sendEmptyMessage(SHOW_PROGRESS);
        Message msg = handler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            handler.removeMessages(FADE_OUT);
            handler.sendMessageDelayed(msg, timeout);
        }
    }

    public boolean isShowing() {
        return showing;
    }

    public void hide () {
        if (showing && player != null && player.isPlaying()) {
            handler.removeMessages(SHOW_PROGRESS);
            animate().y(getContext().getResources().getDisplayMetrics().heightPixels + getHeight()).setDuration(ANIMATION_DURATION).start();
            showing = false;
            if (controllerShowListener != null) controllerShowListener.onHide();
        }
    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage (Message msg) {
            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    hide();
                    break;
                case SHOW_PROGRESS:
                    pos = setProgress();
                    if (!dragging && showing && player.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }

    };

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;
        formatBuilder.setLength(0);
        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    public void update () {
        updatePausePlay();
        setProgress();
    }

    private int setProgress() {
        if (player == null || dragging) {
            return 0;
        }
        int position = player.getCurrentPosition();
        int duration = player.getDuration();
        if (progress != null) {
            if (duration > 0) {
                long pos = 1000L * position / duration;
                progress.setProgress((int) pos);
            }
            int percent = player.getBufferPercentage();
            progress.setSecondaryProgress(percent * 10);
        }

        if (endTime != null) {
            endTime.setText(stringForTime(duration));
        }
        if (currentTime != null) {
            currentTime.setText(stringForTime(position));
        }
        return position;
    }

    @Override
    public boolean onTouchEvent (MotionEvent event) {
        show(DEFAULT_TIMEOUT);
        return true;
    }

    @Override
    public boolean onTrackballEvent (MotionEvent ev) {
        show(DEFAULT_TIMEOUT);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode ==  KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(DEFAULT_TIMEOUT);
                if (pauseButton != null) {
                    pauseButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !player.isPlaying()) {
                player.start();
                updatePausePlay();
                show(DEFAULT_TIMEOUT);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && player.isPlaying()) {
                player.pause();
                updatePausePlay();
                show(DEFAULT_TIMEOUT);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE || keyCode == KeyEvent.KEYCODE_CAMERA) {
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }
        show(DEFAULT_TIMEOUT);
        return super.dispatchKeyEvent(event);
    }

    private OnClickListener pauseListener = new OnClickListener() {
        public void onClick (View v) {
            doPauseResume();
            show(DEFAULT_TIMEOUT);
        }
    };

    public void updatePausePlay () {
        if (pauseButton == null) {
            return;
        }
        if (player != null && player.isPlaying()) {
            pauseButton.setImageResource(R.drawable.ic_action_av_pause);
        } else {
            pauseButton.setImageResource(R.drawable.ic_action_av_play);
        }
    }

    private void doPauseResume() {
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.start();
        }
        updatePausePlay();
    }

    private OnSeekBarChangeListener seekListener = new OnSeekBarChangeListener() {

        private int defaultProgress;
        private int newProgress;

        public void onStartTrackingTouch (SeekBar bar) {
            show(3600000);
            dragging = true;
            handler.removeMessages(SHOW_PROGRESS);
            defaultProgress = bar.getProgress();
        }

        public void onProgressChanged (SeekBar bar, int progress, boolean fromuser) {
            if (fromuser) {
                newProgress = progress;
                if (currentTime != null) {
                    long duration = player.getDuration();
                    long newposition = (duration * newProgress) / 1000L;
                    currentTime.setText(stringForTime( (int) newposition));
                }
            }
            disableUnsupportedButtons();
        }

        public void onStopTrackingTouch (SeekBar bar) {
            if (showing) {
                dragging = false;
                show(DEFAULT_TIMEOUT);
                handler.sendEmptyMessage(SHOW_PROGRESS);

                if (defaultProgress != newProgress) {
                    long duration = player.getDuration();
                    long newposition = (duration * newProgress) / 1000L;
                    player.seekTo( (int) newposition);

                    if (currentTime != null) {
                        currentTime.setText(stringForTime( (int) newposition));
                    }
                } else {
                    setProgress();
                }

                updatePausePlay();
            }
        }
    };

    @Override
    public void setEnabled (boolean enabled) {
        if (pauseButton != null) {
            pauseButton.setEnabled(enabled);
        }
        if (forwardButton != null) {
            forwardButton.setEnabled(enabled);
        }
        if (rewindButton != null) {
            rewindButton.setEnabled(enabled);
        }
        if (nextButton != null) {
            nextButton.setEnabled(enabled && nextListener != null);
        }
        if (prevButton != null) {
            prevButton.setEnabled(enabled && prevListener != null);
        }
        if (progress != null) {
            progress.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    @Override
    public void onInitializeAccessibilityEvent (AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(MediaController.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo (AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(MediaController.class.getName());
    }

    private OnClickListener rewListener = new OnClickListener() {

        public void onClick (View v) {
            int pos = player.getCurrentPosition();
            pos -= 5000; // milliseconds
            player.seekTo(pos);
            setProgress();
            show(DEFAULT_TIMEOUT);
        }

    };

    private OnClickListener forwListener = new OnClickListener() {

        public void onClick (View v) {
            int pos = player.getCurrentPosition();
            pos += 15000; // milliseconds
            player.seekTo(pos);
            setProgress();
            show(DEFAULT_TIMEOUT);
        }

    };

    private void installPrevNextListeners() {
        if (nextButton != null) {
            nextButton.setOnClickListener(nextListener);
            nextButton.setEnabled(nextListener != null);
        }
        if (prevButton != null) {
            prevButton.setOnClickListener(prevListener);
            prevButton.setEnabled(prevListener != null);
        }
    }

    public void setPrevNextListeners (OnClickListener next, OnClickListener prev) {
        nextListener = next;
        prevListener = prev;
        installPrevNextListeners();
        if (nextButton != null) {
            nextButton.setVisibility(View.VISIBLE);
        }
        if (prevButton != null) {
            prevButton.setVisibility(View.VISIBLE);
        }
    }

}