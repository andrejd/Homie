package com.kvajpoj.homie.common;

import android.content.Context;
import android.util.Base64;
import android.webkit.URLUtil;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.kvajpoj.homie.R;
import com.kvajpoj.homie.model.Node;

import org.apache.log4j.Logger;

/**
 * Created by Andrej on 23.4.2017.
 */

public class Utils {

    public interface LoadImageCallback {
        void onLoadImageError(Exception e);
        void onLoadImageSuccess();
    }


    public static boolean ClearImage( ImageView imagePlaceholder ){
        Glide.clear(imagePlaceholder);
        imagePlaceholder.setImageDrawable(null);
        return true;
    }

    public static  boolean LoadImage(Context ctx, ImageView imagePlaceholder, Node currentNode, final LoadImageCallback cb) {
        return LoadImage(ctx, imagePlaceholder, currentNode, cb, R.drawable.ic_broken_image_white_24dp);
    }

    public static  boolean LoadImage(Context ctx, ImageView imagePlaceholder, Node currentNode, final LoadImageCallback cb, int errorResourceId) {

        Logger LOG =  Logger.getLogger(Utils.class);
        String url = currentNode.getWebcamURL();
        String pass = currentNode.getWebcamPassword();
        String username = currentNode.getWebcamUsername();

        if(url == null) url = "";
        if(pass == null) pass = "";
        if(username == null) username = "";

        if (!url.contains("http://")) {
            url = "http://" + url;
        }

        if (URLUtil.isValidUrl(url)) {
            LOG.info("Loading image on address: " + url);
            //"http://kvajpoj.com:4500/image.jpg?t=" + (System.currentTimeMillis()/1000
            GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                    .addHeader("Authorization", "Basic " + Base64.encodeToString((username + ":" + pass).getBytes(), Base64.NO_WRAP))
                    .build());

            Glide.with(ctx)
                    .load(glideUrl)
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .error(errorResourceId)
                    .listener(new RequestListener<GlideUrl, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, GlideUrl model, Target<GlideDrawable> target, boolean isFirstResource) {
                            if(cb != null) cb.onLoadImageError(e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, GlideUrl model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            if(cb != null) cb.onLoadImageSuccess();
                            return false;
                        }
                    })
                    .placeholder(imagePlaceholder.getDrawable())
                    .crossFade()
                    .into(imagePlaceholder);

            return true;
        }
        else {
            LOG.debug("Current ip camera URL is not valid! '" +  url + "'");
            if(cb != null) {
                cb.onLoadImageError(new Exception("Current ip camera URL is not valid! '" +  url + "'" ));
            }
            return false;
        }
    }
}
