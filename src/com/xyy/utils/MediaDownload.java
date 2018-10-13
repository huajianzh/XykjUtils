package com.xyy.utils;

import java.io.BufferedInputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 媒体下载工具，功能就是可以将一个网络地址的媒体下载下来并且保存到某个具体位置(本地)，能够将保存位置传出去给MediaPlayer来播放
 *
 * @author admin
 */
public class MediaDownload {

    /**
     * 下载的方法
     *
     * @param url      下载地址
     * @param savePath 本地保存的路径
     * @param listener 下载监听
     */
    public void download(String url, String savePath,
                         OnMediaLoadListener listener) {
        try {
            URL link = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) link.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            // 获取返回码
            int code = conn.getResponseCode();
            if (code == 200) {
//				String ext = isAudio ? ".mp3" : ".mp4";
//				String savePath = "/mnt/sdcard/" + System.currentTimeMillis()
//						+ ext;
//                Log.e("m_tag","开始下载:"+url);
                int lenght = conn.getContentLength();
                // 将要保存的位置，以及要下载的大小返回
                if (null != listener) {
                    listener.onStart(savePath, lenght);
                }
                RandomAccessFile af = new RandomAccessFile(savePath, "rw");
                // 设置文件在系统总将要占据的大小
                af.setLength(lenght);
                BufferedInputStream in = new BufferedInputStream(
                        conn.getInputStream());
                byte[] buffer = new byte[1024];
                int count = 0;
                int num;
                while ((num = in.read(buffer)) != -1) {
                    af.write(buffer, 0, num);
                    //下载中，将目前下载的大小传回，以便于更新第二进度
                    if (null != listener) {
                        count += num;
                        listener.onDownloading(count);
                    }
                }
                af.close();
                in.close();
                if(null != listener){
                    listener.onDownloadComplete();
                }
            } else {
                if (null != listener) {
                    listener.onDownloadError();
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            if (null != listener) {
                listener.onDownloadError();
            }
        }
    }

    public interface OnMediaLoadListener {
        /**
         * 开始下载
         *
         * @param savePath   保存的位置
         * @param fileLenght 要下载的总大小
         */
        void onStart(String savePath, int fileLenght);

        /**
         * 正在下载中(方便在界面更新缓冲进度--第二进度)
         *
         * @param currentLenght
         */
        void onDownloading(long currentLenght);

        /**
         * 下载失败
         */
        void onDownloadError();

        /**
         * 下载完毕
         */
        void onDownloadComplete();
    }
}
