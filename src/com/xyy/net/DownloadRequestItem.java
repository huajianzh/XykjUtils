package com.xyy.net;

import java.util.Map;

/**
 * Created by Administrator on 2016/12/27.
 */
public class DownloadRequestItem extends RequestItem {
    int from;
    int to;
    String savePath;

    public static class Builder extends RequestItem.Builder {
        DownloadRequestItem request;

        public Builder() {
            request = new DownloadRequestItem();
        }

        public Builder from(int from) {
            request.from = from;
            return this;
        }

        public Builder to(int to) {
            request.to = to;
            return this;
        }

        public Builder savePath(String savePath) {
            request.savePath = savePath;
            return this;
        }

        @Override
        public Builder url(String url) {
            request.url = url;
            return this;
        }

        @Override
        public Builder method(String method) {
            request.method = method;
            return this;
        }

        @Override
        public Builder heads(Map<String, String> heads) {
            request.heads = heads;
            return this;
        }

        @Override
        public Builder addHead(String key, String value) {
            super.addHead(key, value);
            return this;
        }

        @Override
        public DownloadRequestItem build() {
            return request;
        }
    }

}
