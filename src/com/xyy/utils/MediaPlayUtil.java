package com.xyy.utils;

import java.io.File;
import java.util.LinkedList;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.webkit.URLUtil;

/**
 * 支持网络以及本地的媒体加载工具(可以缓存在本地)
 */
public class MediaPlayUtil implements MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener {
	private MediaPlayer mPlayer;
	private MediaDownload mMediaDownload;
	private String tempPath;

	private boolean isStop = true;
	// 出错时的播放进度
	private int errorPos;
	private LinkedList<String> currentDownloads;
	// 视频显示载体
	private Surface surface;
	private SurfaceHolder holder;

	public MediaPlayUtil(Context context) {
		tempPath = context.getExternalCacheDir().toString();
		currentDownloads = new LinkedList<String>();
	}

	// public void start() {
	// if (null == mPlayer) {
	// play(playUrl);
	// } else {
	// if (!mPlayer.isPlaying()) {
	// mPlayer.start();
	// }
	// }
	// }

	public void pause() {
		if (null != mPlayer && mPlayer.isPlaying()) {
			mPlayer.pause();
		}
	}

	public void play(String url) {
		initMediaPlayer();
		if (URLUtil.isNetworkUrl(url)) {
			// 检测本地是否有该完整视频，有则直接播放，没有则启动线程下载视频，然后播放
			String localPath = tempPath + "/" + getNameFromHttpUrl(url);
			File localAudio = new File(localPath);
			if (localAudio.exists()) {
				setDataAndPrepare(localPath);
			} else {
				if (!currentDownloads.contains(url)) {
					currentDownloads.add(url);
					new Thread(new DownloadRunnable(url)).start();
				}
			}
		} else {
			setDataAndPrepare(url);
		}
	}

	class DownloadRunnable implements Runnable,
			MediaDownload.OnMediaLoadListener {
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
			// 下载视频
			if (null == mMediaDownload) {
				mMediaDownload = new MediaDownload();
			}
			localPath = tempPath + "/" + getNameFromHttpUrl(url);
			mMediaDownload.download(url, localPath, this);
		}

		@Override
		public void onStart(String savePath, int fileLenght) {
			if (isTempFileExist()) {
				localLenght = fileLenght;
			}
		}

		@Override
		public void onDownloading(long currentLenght) {
			// 到这里才能确保本地有文件（不够完整）
			int current = (int) (100 * currentLenght / localLenght); // 30 50
			if (current >= 20 && isStop) { // 大于1%之后开始播放
				isStop = false;
				setDataAndPrepare(localPath);
			}
		}

		@Override
		public void onDownloadError() {

		}

		@Override
		public void onDownloadComplete() {
			currentDownloads.remove(url);
		}
	}

	// 初始化MediaPlayer
	private void initMediaPlayer() {
		if (null == mPlayer) {
			mPlayer = new MediaPlayer();
			mPlayer.setOnPreparedListener(this);
			mPlayer.setOnErrorListener(this);
			if (null != onVideoSizeChangedListener) {
				mPlayer.setOnVideoSizeChangedListener(onVideoSizeChangedListener);
			}
			mPlayer.setOnCompletionListener(onCompletionListener);
			mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		} else {
			mPlayer.reset();
		}
		if (null != surface) {
			mPlayer.setSurface(surface);
		} else if (null != holder) {
			mPlayer.setDisplay(holder);
		}
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
			TipsUtil.log("exception : " + e.getMessage());
		}
	}

	private MediaPlayer.OnCompletionListener onCompletionListener;

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// 记录当前播放位置
		errorPos = mp.getCurrentPosition();
		mp.stop();
		mp.reset();
		// 记录它当前是被异常停止的
		isStop = true;
		return true;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		if (errorPos > 0) {
			mp.seekTo(errorPos);
		}
		mp.start();
	}

	private boolean isTempFileExist() {
		File f = new File(tempPath);
		if (!f.exists()) {
			return f.mkdirs();
		}
		return true;
	}

	public void stop() {
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}
	}

	private String getNameFromHttpUrl(String url) {
		int index = url.lastIndexOf("/");
		String name = url.substring(index);
		name = name.replaceAll("[^\\w]", ""); // download?filename=pic1.jpg -->
		// downloadfilenamepic1jpg
		return name;
	}

	private MediaPlayer.OnVideoSizeChangedListener onVideoSizeChangedListener;

	public MediaPlayer.OnCompletionListener getOnCompletionListener() {
		return onCompletionListener;
	}

	public void setOnCompletionListener(
			MediaPlayer.OnCompletionListener onCompletionListener) {
		this.onCompletionListener = onCompletionListener;
	}

	public Surface getSurface() {
		return surface;
	}

	public void setSurface(Surface surface) {
		this.surface = surface;
		holder = null;
	}

	public SurfaceHolder getHolder() {
		return holder;
	}

	public void setHolder(SurfaceHolder holder) {
		this.holder = holder;
		surface = null;
	}

	public MediaPlayer.OnVideoSizeChangedListener getOnVideoSizeChangedListener() {
		return onVideoSizeChangedListener;
	}

	public void setOnVideoSizeChangedListener(
			MediaPlayer.OnVideoSizeChangedListener onVideoSizeChangedListener) {
		this.onVideoSizeChangedListener = onVideoSizeChangedListener;
	}

}
