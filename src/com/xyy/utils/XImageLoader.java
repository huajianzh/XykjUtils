package com.xyy.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.LruCache;
import android.webkit.URLUtil;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 缩略图加载工具
 */
public class XImageLoader {

	private static XImageLoader instance;
	// 内存缓存类
	private LruCache<String, Bitmap> caches;
	// 本地缓存的文件夹
	private String localCachePath;
	// 处理缩略图生成的线程池
	private ExecutorService executorService;
	// 记录当前加载的任务情况(一个地址，关联一个视图)
	private Map<String, ImageView> currentTask;

	private XImageLoader(Context context) {
		// 获取系统分配给应用的运行空间的1/8来作为缓存的最大空间
		int size = (int) (Runtime.getRuntime().maxMemory() / 8);
		// 初始化内存缓存对象
		caches = new LruCache<String, Bitmap>(size) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				// 处理缓存的图片的大小计算
				return value.getRowBytes() * value.getHeight();
			}
		};
		// 获取缓存文件夹 /sdcard/Android/data/应用包名/cache
		localCachePath = context.getApplicationContext().getExternalCacheDir()
				.toString();
		// 初始化线程池
		executorService = Executors.newCachedThreadPool();
		currentTask = new HashMap<String, ImageView>();
	}

	public static XImageLoader getInstance(Context context) {
		if (instance == null) {
			instance = new XImageLoader(context);
		}
		return instance;
	}

	public void showImage(String path, ImageView iv, int defIcon) {
		showImage(path, iv, 100, 100, defIcon, false);
	}

	/**
	 * 显示图像缩略图
	 * 
	 * @param path
	 *            假如 ：/mnt/sdcard/123.jpg_100-100
	 *            -->/sdcard/Android/data/应用包名/cache/mntsdcard123jpg100100
	 * @param iv
	 */
	public void showImage(String path, ImageView iv, int maxWidth,
			int maxHeight, int defIcon, boolean isResize) {
		String key = path;
		if (isResize) {
			key = path + "_" + maxWidth + "-" + maxHeight;
		}
		// 针对该路径检测内存中是否有对应的缩略图
		if (caches.get(key) != null) {
			iv.setImageBitmap(caches.get(key));
		} else {
			// 没有缩略图，检测本地是否有缩略图
			String thumbPath = getLocalCachePath(key);
			File f = new File(thumbPath);
			if (f.exists()) {
				// 有缩略图,从本地缓存文件夹中加载缩略图
				Bitmap thumb = BitmapUtil.decodBitmap(thumbPath, maxWidth,
						maxHeight);
				if (null != thumb) {
					// 保存到内存中，方便下次可以从内存中快速的读取
					caches.put(key, thumb);
					// 显示
					iv.setImageBitmap(thumb);
				} else {
					// 显示默认图标
					iv.setImageResource(defIcon);
				}
			} else {
				// 检测当前路径是否有任务在生成中
				if (currentTask.containsKey(key)) {
					// 改变该路径对应的新视图
					currentTask.put(key, iv);
					return;
				}
				// 记录当前要加载的路径对应的显示视图
				currentTask.put(key, iv);
				if (URLUtil.isNetworkUrl(path)) {
					// 下载图片，生成缩略图
					executorService.execute(new DownloadRunnable(path, key,
							maxWidth, maxHeight, defIcon));
				} else {
					// 生成缩略图
					executorService.execute(new CreateThumbRunnable(path, key,
							maxWidth, maxHeight, defIcon));
				}
			}

		}
	}

	class CreateThumbRunnable extends Handler implements Runnable {

		protected String path; // 源图片地址
		protected int width;
		protected int height;
		protected String key; // 按尺寸记录在内存中的key
		protected int defIcon;

		public CreateThumbRunnable(String path, String key, int width,
				int height, int defIcon) {
			this.path = path;
			this.width = width;
			this.height = height;
			this.key = key;
			this.defIcon = defIcon;
		}

		@Override
		public void run() {
			// 从原图上生成缩略图
			Bitmap thumb = BitmapUtil.decodBitmap(path, width, height);
			if (thumb != null) {
				// 保存到本地缓存文件夹中
				String thumbPath = getLocalCachePath(key);
				BitmapUtil.saveBitmap(thumb, thumbPath);
				// 保存到内存中
				caches.put(key, thumb);
				// 显示
				obtainMessage(1, thumb).sendToTarget();
			} else {
				sendEmptyMessage(0);
			}
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				ImageView iv = currentTask.remove(key);
				if (iv.isShown()) {
					// 生成成功
					Bitmap b = (Bitmap) msg.obj;
					iv.setImageBitmap(b);
				}
				break;
			case 0:
				// 生成失败，显示默认图标
				ImageView iv1 = currentTask.remove(key);
				iv1.setImageResource(defIcon);
				break;
			}
		}
	}

	class DownloadRunnable extends CreateThumbRunnable {

		public DownloadRunnable(String path, String key, int width, int height,
				int defIcon) {
			super(path, key, width, height, defIcon);
		}

		@Override
		public void run() {
			try {
				// 1、创建URL(地址)
				URL url = new URL(path);
				// 2、打开连接
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				// 检测是否成功
				int code = conn.getResponseCode();
				if (code == 200) {
					Bitmap thumb;
					// 3、从服务器中获取内容
					InputStream in = conn.getInputStream();
					if (conn.getContentLength() >= 1048576) {
						// 如果是大图，建议先保存到磁盘再做
						String thumbPath = getLocalCachePath(key);
						FileOutputStream out = new FileOutputStream(thumbPath);
						byte[] buf = new byte[2048];
						int num;
						while ((num = in.read(buf)) != -1) {
							out.write(buf, 0, num);
						}
						out.flush();
						out.close();
						// 生成缩略图
						thumb = BitmapUtil
								.decodBitmap(thumbPath, width, height);

					} else {
						// 如果是小图可以转为byte[]然后在转
						thumb = BitmapUtil.decodBitmap(inputStrem2ByteAry(in),
								width, height);
						String thumbPath = getLocalCachePath(key);
						BitmapUtil.saveBitmap(thumb, thumbPath);
					}
					// 保存到内存中
					caches.put(key, thumb);
					// 显示
					obtainMessage(1, thumb).sendToTarget();
					in.close();
				}else{
					sendEmptyMessage(0);
				}
				conn.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
				sendEmptyMessage(0);
			}

		}
	}

	private byte[] inputStrem2ByteAry(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		byte[] result = null;
		int num;
		while ((num = in.read(buf)) != -1) {
			out.write(buf, 0, num);
		}
		result = out.toByteArray();
		out.close();
		return result;
	}

	/**
	 * 检测本地是否有缩略图
	 * 
	 * @param path
	 * @return
	 */
	public boolean isHasLocalCache(String path) {
public class XImageLoader {
    private Context context;
    private static XImageLoader instance;
    // 内存缓存类
    private LruCache<String, Bitmap> caches;
    // 本地缓存的文件夹
    private String localCachePath;
    // 处理缩略图生成的线程池
    private ExecutorService executorService;
    private ExecutorService executorVideoService;
    // 记录当前加载的任务情况(一个地址，关联一个视图)
    private Map<String, Set<ImageView>> currentTask;
    //生成缩略图的视频地址
    private List<String> videoLoadingPaths;
    private Map<String, Future> futures;

    private XImageLoader(Context c) {
        // 获取系统分配给应用的运行空间的1/8来作为缓存的最大空间
        int size = (int) (Runtime.getRuntime().maxMemory() / 8);
        // 初始化内存缓存对象
        caches = new LruCache<String, Bitmap>(size) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                // 处理缓存的图片的大小计算
                return value.getRowBytes() * value.getHeight();
            }

        };
        context = c.getApplicationContext();
        // 获取缓存文件夹 /sdcard/Android/data/应用包名/cache
        localCachePath = context.getExternalCacheDir()
                .toString();
        // 初始化线程池
        executorService = Executors.newCachedThreadPool();
        executorVideoService = Executors.newFixedThreadPool(3);
        currentTask = new HashMap<String, Set<ImageView>>();
        futures = new HashMap<>();
        videoLoadingPaths = new LinkedList<>();
    }

    public static XImageLoader getInstance(Context context) {
        if (instance == null) {
            instance = new XImageLoader(context);
        }
        return instance;
    }

    public void showImage(String path, ImageView iv, int defIcon) {
        showImage(path, iv, defIcon, 0, null);
    }

    public void showImage(String path, ImageView iv, int defIcon, float blurRadius) {
        showImage(path, iv, defIcon, blurRadius, null);
    }

    public void showImage(String path, ImageView iv, int defIcon, float blurRadius, Bitmap.Config config) {
        showImage(path, iv, 500, 500, defIcon, false, blurRadius, config);
    }

    /**
     * 显示图像缩略图
     *
     * @param path       假如 ：/mnt/sdcard/123.jpg_100-100
     *                   -->/sdcard/Android/data/应用包名/cache/mntsdcard123jpg100100
     * @param iv
     * @param maxWidth   显示的最大宽度
     * @param maxHeight  显示的转达高度
     * @param defIcon    加载失败的默认图标
     * @param isResize   是否是固定大小，是则找到对应大小的缩略图才可以找不到则重新生成，否则只要找到缩略图即可
     * @param blurRadius 模糊的程度0-25，干参数要配合Bitmap.Config.ARBG_8888或者Bitmap.Config.ARBG_4444使用效果更好
     * @param config     缩略图加载配置
     */
    public void showImage(String path, ImageView iv, int maxWidth,
                          int maxHeight, int defIcon, boolean isResize, float blurRadius, Bitmap.Config config) {
        if (null == path) {
            return;
        }
        String key = path;
        if (isResize) {
            String temp = path;
            int index = temp.lastIndexOf(".");
            if (index > 0) {
                key = temp.substring(0, index) + maxWidth + maxHeight + temp.substring(index);
            } else {
                key = path + maxWidth + maxHeight;
            }
        }
        // 针对该路径检测内存中是否有对应的缩略图
        Bitmap thumb = caches.get(key);
        if (null != thumb && !thumb.isRecycled()) {
            iv.setImageBitmap(thumb);
        } else {
            // 没有缩略图，检测本地是否有缩略图
            String thumbPath = getLocalCachePath(key);
            File f = new File(thumbPath);
            //本地存在同时没有加载任务则显示本地（有些网络加载较慢，第二次来加载时发现本地有部分文件另外的内容还在写入中，直接显示本地会出现半张图而已）
            if (f.exists() && !currentTask.containsKey(key)) {
                // 有缩略图,从本地缓存文件夹中加载缩略图
                Bitmap b = BitmapUtil.decodeBitmap(thumbPath, maxWidth, maxHeight, config);
                if (null != b) {
                    if (blurRadius > 0) {
                        thumb = BitmapUtil.blurBitmap(context, b, blurRadius, true);
                    } else {
                        thumb = b;
                    }
                }
                if (null != thumb) {
                    // 保存到内存中，方便下次可以从内存中快速的读取
                    caches.put(key, thumb);
                    // 显示
                    iv.setImageBitmap(thumb);
                } else {
                    // 显示默认图标
                    iv.setImageResource(defIcon);
                }
            } else {
                //检测当前视图是否有正在加载的地址
                if (!currentTask.isEmpty()) {
                    removeImageViewInTask(key, iv);
                    // 检测当前路径是否有任务在生成中
                    if (currentTask.containsKey(key)) {
                        // 改变该路径对应的新视图
//                    currentTask.put(key, iv);
                        TipsUtil.log("is current task loading : " + key);
                        currentTask.get(key).add(iv);
                        return;
                    }
                }
                // 记录当前要加载的路径对应的显示视图
                Set<ImageView> set = new HashSet<>(2);
                set.add(iv);
                TipsUtil.log("load thumb : " + key);
                currentTask.put(key, set);
                Future future;
                if (URLUtil.isNetworkUrl(path)) {
                    // 下载图片，生成缩略图
                    future = executorService.submit(new DownloadRunnable(path, key,
                            maxWidth, maxHeight, defIcon, blurRadius, config));
                } else {
                    //图像路径不是一个网址（说明是本地文件）
                    String fileType = FileTypeUtil.getFileType(path);
                    if (FileTypeUtil.isVideo(fileType)) {
                        //本地视频文件(单任务加载视频)
                        videoLoadingPaths.add(path);
                        future = executorVideoService.submit(new CreateVideoThumbRunnable(path, key,
                                maxWidth, maxHeight, defIcon, blurRadius, config));
//                            future = executorService.submit(new CreateVideoThumbRunnable(path, key,
//                                    maxWidth, maxHeight, defIcon));
                    } else {
                        // 本地直接生成缩略图
                        future = executorService.submit(new CreateThumbRunnable(path, key,
                                maxWidth, maxHeight, defIcon, blurRadius, config));
                    }
                }
                futures.put(path, future);
            }

        }
    }

    private void removeImageViewInTask(String url, ImageView iv) {
        Iterator<Map.Entry<String, Set<ImageView>>> it = currentTask.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Set<ImageView>> entry = it.next();
            String key = entry.getKey();
            for (ImageView iview : entry.getValue()) {
                if (!url.equals(key) && iv.equals(iview)) {
                    entry.getValue().remove(iv);
                    return;
                }
            }
        }
    }

    /**
     * 根据图像自身的角度从本地加载图像
     *
     * @param path
     * @param maxWidth
     * @param maxHeight
     * @param config
     * @return
     */
    public Bitmap decodeBitmapFromLocal(String path, int maxWidth, int maxHeight, Bitmap.Config config) {
        Bitmap thumb = BitmapUtil.decodeBitmap(path, maxWidth, maxHeight, config);
        if (null == thumb) {
            return null;
        }
        ExifInterface exif;
        try {
            exif = new ExifInterface(path);
            //获取图像的角度（摄像头拍照时会对图像做旋转）
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                int degree = 0;
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                }
                Matrix m = new Matrix();
                m.setRotate(degree);
                Bitmap bitmap = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), m, true);
                thumb = null;
                thumb = bitmap;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return thumb;
    }

    //获取缩略图显示到桌面Widget上(针对RemoteViews使用来加载图像)
    public Bitmap getThumbBitmap(String path, int maxWidth, int maxHeight, boolean saveToCache) {
        String key = path;
        Bitmap thumb = null;
        String thumbPath = null;
        if (saveToCache) {
            key = path + maxWidth + maxHeight;
            if (caches.get(key) != null && !caches.get(key).isRecycled()) {
                return caches.get(key);
            } else {
                thumbPath = getLocalCachePath(key);
                File f = new File(thumbPath);
                if (f.exists()) {
                    // 有缩略图,从本地缓存文件夹中加载缩略图
                    thumb = BitmapUtil.decodeBitmap(thumbPath, maxWidth,
                            maxHeight, Bitmap.Config.RGB_565);
                    if (null != thumb && saveToCache) {
                        caches.put(key, thumb);
                    }
                    return thumb;
                }
            }
        }
        //无本地缓存则从原图上生成
        String fileType = FileTypeUtil.getFileType(path);
        if (FileTypeUtil.isVideo(fileType)) {
            MediaMetadataRetriever metadata = new MediaMetadataRetriever();
            try {
                metadata.setDataSource(path);
                thumb = metadata.getFrameAtTime();
                //缩小缩略图
                int w = maxWidth, h = maxHeight;
                int bitmapWidth = thumb.getWidth();
                int bitmapHeight = thumb.getHeight();
                if (bitmapWidth > w || bitmapHeight > h) {
                    float wScale = (float) w / bitmapWidth;
                    float hScale = (float) h / bitmapHeight;
                    //如果宽方向出现的比例更小则按宽的比例计算对应的高
                    if (wScale < hScale) {
                        h = (int) (bitmapHeight * wScale);
                    } else {
                        w = (int) (bitmapWidth * hScale);
                    }
                    Bitmap b = Bitmap.createScaledBitmap(thumb, w, h, true);
                    thumb.recycle();
                    thumb = b;
                }
            } catch (Exception e) {
            } finally {
                metadata.release();
            }
        } else {
            thumb = decodeBitmapFromLocal(path, maxWidth, maxHeight, Bitmap.Config.RGB_565);
        }
        if (null != thumb) {
            // 保存到内存中，方便下次可以从内存中快速的读取
            if (saveToCache) {
                caches.put(key, thumb);
                BitmapUtil.saveBitmap(thumb, thumbPath, Bitmap.Config.RGB_565);
            }
            // 显示
            return thumb;
        }
        return null;
    }

    private class CreateThumbRunnable extends Handler implements Runnable {
        protected float blurRadius;
        protected String path; // 源图片地址
        protected int width;
        protected int height;
        protected String key; // 按尺寸记录在内存中的key
        protected int defIcon;
        protected Bitmap.Config config;

        public CreateThumbRunnable(String path, String key, int width,
                                   int height, int defIcon, float blurRadius, Bitmap.Config config) {
            this.path = path;
            this.width = width;
            this.height = height;
            this.key = key;
            this.defIcon = defIcon;
            this.blurRadius = blurRadius;
            this.config = config;
        }

        @Override
        public void run() {
            // 从原图上生成缩略图
            Bitmap thumb = null, b;
            b = decodeBitmapFromLocal(path, width, height, config);
            if (null != b) {
                if (blurRadius > 0) {
                    thumb = BitmapUtil.blurBitmap(context, b, blurRadius, true);
                } else {
                    thumb = b;
                }
            }
            if (thumb != null) {
                // 保存到内存中
                caches.put(key, thumb);
                // 显示
                obtainMessage(1, thumb).sendToTarget();
                //缩略图比显示区域小则保存缩略图，否则下次直接加载原图即可
                if (thumb.getWidth() < width || thumb.getHeight() < height) {
                    // 保存到本地缓存文件夹中
                    String thumbPath = getLocalCachePath(key);
                    BitmapUtil.saveBitmap(thumb, thumbPath, config);
                }
            } else {
                sendEmptyMessage(0);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            //release之后才收到消息时的异常处理
            if (null == currentTask) {
                return;
            }
            Set<ImageView> set = currentTask.remove(key);
            futures.remove(path);
            if (set == null) {
                return;
            }
            Iterator<ImageView> it = set.iterator();
            ImageView iv;
            while (it.hasNext()) {
                iv = it.next();
                switch (msg.what) {
                    case 1:
                        if (null != iv && iv.getVisibility() == View.VISIBLE) {
                            // 生成成功
                            Bitmap b = (Bitmap) msg.obj;
                            iv.setImageBitmap(b);
                            if ((path.endsWith("gif") || path.endsWith("GIF"))) {
                                if (null != onLoaderListener) {
                                    onLoaderListener.onLoadGif(iv, key);
                                }
                            }
                        }
                        break;
                    case 0:
                        if (null != iv && iv.getVisibility() == View.VISIBLE) {
                            // 生成失败，显示默认图标
                            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            iv.setImageResource(defIcon);
                        }
                        break;
                }
            }
        }
    }

    private class CreateVideoThumbRunnable extends CreateThumbRunnable {

        public CreateVideoThumbRunnable(String path, String key, int width, int height, int defIcon, float radius, Bitmap.Config config) {
            super(path, key, width, height, defIcon, radius, config);
        }

        @Override
        public void run() {
            Bitmap thumb = null;
            MediaMetadataRetriever metadata = new MediaMetadataRetriever();
            try (FileInputStream is = new FileInputStream(path)) {
                FileDescriptor fd = is.getFD();
                metadata.setDataSource(fd);
                thumb = metadata.getFrameAtTime();
            } catch (FileNotFoundException fileEx) {
                fileEx.printStackTrace();
            } catch (IOException ioEx) {
                ioEx.printStackTrace();
            } finally {
                metadata.release();
            }
            if (null != thumb) {
                int w = width, h = height;
                int bitmapWidth = thumb.getWidth();
                int bitmapHeight = thumb.getHeight();
                if (bitmapWidth > w || bitmapHeight > h) {
                    float wScale = (float) w / bitmapWidth;
                    float hScale = (float) h / bitmapHeight;
                    //如果宽方向出现的比例更小则按宽的比例计算对应的高
                    if (wScale < hScale) {
                        h = (int) (bitmapHeight * wScale);
                    } else {
                        w = (int) (bitmapWidth * hScale);
                    }
                    Bitmap b = Bitmap.createScaledBitmap(thumb, w, h, true);
                    thumb.recycle();
                    if (blurRadius > 0) {
                        thumb = BitmapUtil.blurBitmap(context, b, blurRadius, true);
                    } else {
                        thumb = b;
                    }
                }
                // 保存到内存中，方便下次可以从内存中快速的读取
                caches.put(key, thumb);
                obtainMessage(1, thumb).sendToTarget();
                Log.e("m_tag", path + " create thumb ok");
                String thumbPath = getLocalCachePath(key);
                BitmapUtil.saveBitmap(thumb, thumbPath, Bitmap.Config.RGB_565);
            } else {
                Log.e("m_tag", path + " create thumb fail");
                sendEmptyMessage(0);
            }
            videoLoadingPaths.remove(path);
        }
    }

    //从网络下载图像并生成缩略图
    private class DownloadRunnable extends CreateThumbRunnable {

        public DownloadRunnable(String path, String key, int width, int height,
                                int defIcon, float radius, Bitmap.Config config) {
            super(path, key, width, height, defIcon, radius, config);
        }

        @Override
        public void run() {
            try {
                // 1、创建URL(地址)
                URL url = new URL(path);
                // 2、打开连接
                HttpURLConnection conn = (HttpURLConnection) url
                        .openConnection();
                // 检测是否成功
                int code = conn.getResponseCode();
                if (code == 200) {
                    Bitmap thumb = null;
                    // 3、从服务器中获取内容
                    InputStream in = conn.getInputStream();
                    String thumbPath = getLocalCachePath(key);
                    if (conn.getContentLength() >= 1048576 || path.endsWith("gif") || path.endsWith("GIF")) {
                        // 如果是大图，先保存到磁盘再做缩略图
                        FileOutputStream out = new FileOutputStream(thumbPath);
                        byte[] buf = new byte[2048];
                        int num;
                        while ((num = in.read(buf)) != -1) {
                            out.write(buf, 0, num);
                        }
                        out.flush();
                        out.close();
                        TipsUtil.log("download and save finish : " + thumbPath);
                        // 生成缩略图
                        Bitmap b = BitmapUtil.decodeBitmap(thumbPath, width, height, config);
                        if (null != b) {
                            if (blurRadius > 0) {
                                thumb = BitmapUtil.blurBitmap(context, b, blurRadius, true);
                            } else {
                                thumb = b;
                            }
                        }
                    } else {
                        // 如果是小图可以转为byte[]然后在转
                        Bitmap b = BitmapUtil.decodeBitmap(inputStrem2ByteAry(in),
                                width, height, config);
                        if (null != b) {
                            if (blurRadius > 0) {
                                thumb = BitmapUtil.blurBitmap(context, b, blurRadius, true);
                            } else {
                                thumb = b;
                            }
                        }
                        TipsUtil.log("download and create thumb finish : "+thumbPath +" w:"+thumb.getWidth()+" h:"+thumb.getHeight()+" maxWidth:"+width+" maxHeight:"+height);
                        BitmapUtil.saveBitmap(thumb, thumbPath, config);
                    }
                    if (null != thumb) {
                        // 保存到内存中
                        caches.put(key, thumb);
                        // 显示
                        obtainMessage(1, thumb).sendToTarget();
                    } else {
                        sendEmptyMessage(0);
                    }
                    in.close();
                } else {
                    sendEmptyMessage(0);
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                sendEmptyMessage(0);
            }

        }
    }

    private byte[] inputStrem2ByteAry(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        byte[] result = null;
        int num;
        while ((num = in.read(buf)) != -1) {
            out.write(buf, 0, num);
        }
        result = out.toByteArray();
        out.close();
        return result;
    }

    public String getLocalCachePath(String path, int maxWidth, int maxHeight, boolean isResize) {
        String key = path;
        if (isResize) {
            String temp = path;
            int index = temp.lastIndexOf(".");
            if (index > 0) {
                key = temp.substring(0, index) + maxWidth + maxHeight + temp.substring(index);
            } else {
                key = path + maxWidth + maxHeight;
            }
        }
        String cachePath = getLocalCachePath(key);
        if (new File(cachePath).exists()) {
            return cachePath;
        }
        return null;
    }

    /**
     * 检测本地是否有缩略图
     *
     * @param path
     * @return
     */
    public boolean isHasLocalCache(String path) {
        String cachePath = getLocalCachePath(path);
        return new File(cachePath).exists();
    }

    public void deleteCacheThumb(String path) {
        if (caches.get(path) != null) {
            caches.remove(path);
        }
        if (futures.containsKey(path)) {
            Future f = futures.get(path);
            if (!f.isDone() && !f.isCancelled()) {
                f.cancel(true);
            }
            futures.remove(path);
        }
        // 没有缩略图，检测本地是否有缩略图
        final String thumbPath = getLocalCachePath(path);
        File folder = new File(thumbPath).getParentFile();
        File[] fs = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getPath().startsWith(thumbPath);
            }
        });
        if (null != fs && fs.length > 0) {
            for (int i = 0; i < fs.length; i++) {
                fs[i].delete();
            }
        }
    }

    /**
     * 保存本地缩略图
     *
     * @param path
     * @param thumb
     */
    public void saveThumbInLocal(String path, Bitmap thumb) {
        // 保存到本地缓存文件夹中
        String thumbPath = getLocalCachePath(path);
        BitmapUtil.saveBitmap(thumb, thumbPath, null);
        // 保存到内存中
        caches.put(path, thumb);
    }

    //网址域名正则表达式 http://www.abc.com:8080/abc1/addd.jpg -> 取出域名http://www.abc.com:8080/ -> 剩下abc1/addd.jpg
    private static final String WEB_HOST_REGEX = "^(?=^.{3,255}$)(http(s)?://)?(www.)?[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(.[a-zA-Z]{2,6})+(:\\d+)*(/)";

    /**
     * 基于源文件获取所对应的缩略图的路径
     *
     * @param path
     * @return
     */
    public String getLocalCachePath(String path) {
        String name;
        if (URLUtil.isNetworkUrl(path)) {
            // 该地址是一个网址
            int index = path.lastIndexOf("/");
            if (index > -1) {
                index++;
                name = path.substring(index);
            }else{
                name = path;
            }
            //gif图像保持后缀名以便于显示图像时区分
            //png缩略图加载方式需要ARGB模式，否则会有黑色背景
            if (!name.endsWith(".gif") && !name.endsWith(".png") && !name.endsWith(".GIF") && !name.endsWith(".PNG")) {
                name = path.replaceAll(WEB_HOST_REGEX,"");
                name = name.replaceAll("[^\\w]", "");
            }
        } else {
            if (new File(path).getParent().equals(localCachePath)) {
                return path;
            }
            // 将路径中的特殊字符去掉当做缩略图的文件名
            if (path.endsWith(".png") || path.endsWith(".PNG") || path.endsWith(".gif") || path.endsWith(".GIF")) {
                int index = path.lastIndexOf(".");
                String ext = path.substring(index);
                name = path.substring(0, index).replaceAll("[^\\w]", "") + ext;
            } else {
                name = path.replaceAll("[^\\w]", "");
            }
        }
        return localCachePath + File.separator + name;
    }

    public void release() {
        executorService.shutdownNow();
        executorVideoService.shutdownNow();
        caches.evictAll();
        currentTask.clear();
        caches = null;
        currentTask = null;
        instance = null;
    }

    public void resetVideoCreateTask() {
        executorVideoService.shutdownNow();
        Iterator<String> it = videoLoadingPaths.iterator();
        while (it.hasNext()) {
            currentTask.remove(it.next());
            it.remove();
        }
        executorVideoService = Executors.newFixedThreadPool(3);
    }

    public interface OnLoaderListener {
        void onLoadGif(ImageView iv, String url);
    }

    private OnLoaderListener onLoaderListener;

    public void setOnLoaderListener(OnLoaderListener onLoaderListener) {
        this.onLoaderListener = onLoaderListener;
    }
}
