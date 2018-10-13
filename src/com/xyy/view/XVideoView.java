package com.xyy.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.SeekBar;

import com.xyy.utils.DbUtil;
import com.xyy.utils.MediaDownload;
import com.xyy.utils.TipsUtil;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by admin on 2016/10/17.
 */
public class XVideoView extends TextureView implements TextureView.SurfaceTextureListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer
                .OnCompletionListener,MediaPlayer.OnVideoSizeChangedListener, SeekBar.OnSeekBarChangeListener {
    //播放进度消息
    private static final int MSG_PLAY_PROGRESS = 1;
    //缓冲消息
    private static final int MSG_BUFFER_UPDATE = 2;
    private MediaPlayer mPlayer;

//    private int surfaceWidth, surfaceHeight;
    private Context context;
    private MediaDownload mMediaDownload;
    private String tampPath;

    private boolean isStop = true;
    //出错时的播放进度
    private int errorPos;

    private String playUrl;

    //播放进度以及缓冲进度
    private SeekBar mSeekBar;
    //用于播放内容的Surface
    private Surface mSurface;
    private int seekToPos;
    private Handler mHandler;
    private int mVideoWidth;
    private int mVideoHeight;

    public XVideoView(Context context) {
        super(context);
        init(context);
    }

    public XVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public XVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        //TextureView使用时需要硬件加速
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        //添加Surface监听
        setSurfaceTextureListener(this);

        //tampPath = Environment.getExternalStorageDirectory().getPath()
        //        + "/Android/data/" + context.getApplicationInfo().packageName
        //        + "/video";
        tampPath = context.getExternalCacheDir().getParent() + "/video";
//        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            @Override
//            public boolean onPreDraw() {
//                surfaceWidth = getMeasuredWidth();
//                surfaceHeight = getMeasuredHeight();
//                getViewTreeObserver().removeOnPreDrawListener(this);
//                return true;
//            }
//        });
        mHandler = new MyHandler(this);
    }

    public void start() {
        if (null == mPlayer) {
            play(playUrl);
        } else {
            if (!mPlayer.isPlaying()) {
                mPlayer.start();
                mHandler.sendEmptyMessage(MSG_PLAY_PROGRESS);
            }
        }
    }

    public void pause() {
        if (null != mPlayer && mPlayer.isPlaying()) {
            mHandler.removeMessages(MSG_PLAY_PROGRESS);
            mPlayer.pause();
        }
    }

    private void play(String url) {
        initMediaPlayer();
        if (URLUtil.isNetworkUrl(url)) {
            //检测本地是否有该完整视频，有则直接播放，没有则启动线程下载视频，然后播放
            String localPath = DbUtil.getInstance(context).getLocalPath(url);
            if (localPath != null) {
                setDataAndPrepare(localPath);
            } else {
                new Thread(new DownloadRunnable(url)).start();
            }
        } else {
            setDataAndPrepare(url);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurface = new Surface(surface);
        if (playUrl != null) {
            play(playUrl);
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Log.i("@@@@", "onMeasure(" + MeasureSpec.toString(widthMeasureSpec) + ", "
        //        + MeasureSpec.toString(heightMeasureSpec) + ")");

        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        if (mVideoWidth > 0 && mVideoHeight > 0) {

            int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
                // the size is fixed
                width = widthSpecSize;
                height = heightSpecSize;

                // for compatibility, we adjust size based on aspect ratio
                if ( mVideoWidth * height  < width * mVideoHeight ) {
                    //Log.i("@@@", "image too wide, correcting");
                    width = height * mVideoWidth / mVideoHeight;
                } else if ( mVideoWidth * height  > width * mVideoHeight ) {
                    //Log.i("@@@", "image too tall, correcting");
                    height = width * mVideoHeight / mVideoWidth;
                }
            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
                // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize;
                height = width * mVideoHeight / mVideoWidth;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize;
                }
            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize;
                width = height * mVideoWidth / mVideoHeight;
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize;
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = mVideoWidth;
                height = mVideoHeight;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize;
                    width = height * mVideoWidth / mVideoHeight;
                }
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize;
                    height = width * mVideoHeight / mVideoWidth;
                }
            }
        } else {
            // no size yet, just adopt the given spec sizes
        }
        setMeasuredDimension(width, height);
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        playUrl = null;
        if (null != mPlayer) {
            mHandler.removeMessages(MSG_PLAY_PROGRESS);
            mPlayer.stop();
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
            seekToPos = 0;
            if (null != mSeekBar) {
                mSeekBar.setProgress(0);
            }
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (null != mPlayer) {
            if (mPlayer.isPlaying()) {
                mHandler.removeMessages(MSG_PLAY_PROGRESS);
            }
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (null != mPlayer) {
            int progress = seekBar.getProgress(); // max --> 100
            int duration = (Integer) seekBar.getTag();
            int currentPos = duration * progress / 100;
            mPlayer.seekTo(currentPos);
            if (mPlayer.isPlaying()) {
                mHandler.sendEmptyMessage(MSG_PLAY_PROGRESS);
            }
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if(mVideoWidth>0 && mVideoHeight>0){
            requestLayout();
        }
    }

    class DownloadRunnable implements Runnable, MediaDownload.OnMediaLoadListener {
        // 当前要播放的媒体文件的本地地址
        private String localPath;
        // 当前要播放的文件的总大小
        private int localLenght;

        private String url;

        DownloadRunnable(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            //下载视频
            if (null == mMediaDownload) {
                mMediaDownload = new MediaDownload();
            }
            localPath = tampPath + "/" + System.currentTimeMillis();
            mMediaDownload.download(url, localPath, this);
        }

        @Override
        public void onStart(String savePath, int fileLenght) {
            if (isTempFileExist()) {
                localLenght = fileLenght;
                //开始下载，可以在数据库中记录状态（未下载完毕的状态）
                DbUtil.getInstance(context).saveVideoDownloadState(url, localPath, 0);
            }
        }

        @Override
        public void onDownloading(long currentLenght) {
            // 到这里才能确保本地有文件（不够完整）
            int current = (int) (100 * currentLenght / localLenght); // 30  50
            if (current > 5) { //大于1%之后开始播放
                if (isStop) {
                    isStop = false;
                    setDataAndPrepare(localPath);
                }
            }
            // 更新缓冲进度（第二进度）--->得到下载的百分比
            mHandler.obtainMessage(MSG_BUFFER_UPDATE, current).sendToTarget();
        }

        @Override
        public void onDownloadError() {

        }

        @Override
        public void onDownloadComplete() {
            //更新下载状态（下载完毕）
            DbUtil.getInstance(context).saveVideoDownloadState(url, localPath, 1);
        }
    }

    private boolean isTempFileExist() {
        File f = new File(tampPath);
        if (!f.exists()) {
            return f.mkdirs();
        }
        return true;
    }

    // 初始化MediaPlayer
    private void initMediaPlayer() {
        if (null == mPlayer) {
            mPlayer = new MediaPlayer();
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnVideoSizeChangedListener(this);
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setScreenOnWhilePlaying(true);
        } else {
            mPlayer.reset();
        }
        //设置视频播放的画面显示
        mPlayer.setSurface(mSurface);
    }

    // 设置数据源并且准备播放
    private void setDataAndPrepare(String path) {
        try {
            mPlayer.setDataSource(path);
            mPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            isStop = true;
            mPlayer.reset();
            TipsUtil.log("exception : "+e.getMessage());
        }
    }

    public boolean isPlaying() {
        return null != mPlayer && mPlayer.isPlaying();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (null != onPlayerListener) {
            onPlayerListener.onPlayComplete(playUrl);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // 记录当前播放位置
        errorPos = mp.getCurrentPosition();
//        mHandler.removeMessages(MSG_PLAY_PROGRESS);
        mp.stop();
        mp.reset();
        // 记录它当前是被异常停止的
        isStop = true;
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // 如果出错的位置非0
        if (errorPos > 0) {
            mp.seekTo(errorPos);
        }
        if (seekToPos > 0) {
            mp.seekTo(seekToPos);
        }
        mp.start();
        if (mSeekBar != null) {
            // 获取播放的音乐的总时间
            int max = mp.getDuration();
            //将最大值标记到SeekBar上方便百分比的转换
            mSeekBar.setTag(max);
            // 需要更新播放进度
            mHandler.sendEmptyMessage(MSG_PLAY_PROGRESS);
        }
    }

    static class MyHandler extends Handler {
        private WeakReference<XVideoView> thisView;

        public MyHandler(XVideoView view) {
            thisView = new WeakReference<XVideoView>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_BUFFER_UPDATE:
                    if (null != thisView.get().mSeekBar) {
                        int bufferProgress = (Integer) msg.obj;
                        thisView.get().mSeekBar.setSecondaryProgress(bufferProgress);
                    }
                    break;
                case MSG_PLAY_PROGRESS:
                    // 计算播放进度
                    if (null != thisView.get().mSeekBar && null != thisView.get().mPlayer) {
                        if (null != thisView.get().mSeekBar.getTag()) {
                            int duration = (Integer) thisView.get().mSeekBar.getTag();
                            if (duration > 0) {
                                int currentPos = thisView.get().mPlayer.getCurrentPosition();
                                int progress = currentPos * 100 / duration;  //  20   3 3*100/20
                                thisView.get().mSeekBar.setProgress(progress);
                                sendEmptyMessageDelayed(MSG_PLAY_PROGRESS, 1000);
                            }
                        }
                    }
                    break;

            }
        }
    }


    public String getPlayUrl() {
        return playUrl;
    }

    public void setPlayUrl(String playUrl) {
        this.playUrl = playUrl;
    }

    public SeekBar getSeekBar() {
        return mSeekBar;
    }

    public void setSeekBar(SeekBar mSeekBar) {
        this.mSeekBar = mSeekBar;
        this.mSeekBar.setOnSeekBarChangeListener(this);
    }

    public int getCurrentPosition() {
        if (null != mPlayer) {
            return mPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void setSeekToPos(int seekToPos) {
        this.seekToPos = seekToPos;
    }

    public interface OnPlayerListener {
        void onPlayComplete(String url);
    }

    private OnPlayerListener onPlayerListener;

    public OnPlayerListener getOnPlayerListener() {
        return onPlayerListener;
    }

    public void setOnPlayerListener(OnPlayerListener onPlayerListener) {
        this.onPlayerListener = onPlayerListener;
    }
}
