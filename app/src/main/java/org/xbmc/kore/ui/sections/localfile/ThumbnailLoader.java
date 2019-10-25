package org.xbmc.kore.ui.sections.localfile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.lang.ref.WeakReference;



public class ThumbnailLoader extends AsyncTask<String, Void, Bitmap> {

    public int width, height;
    private final WeakReference<ImageView> imageViewReference;

    ThumbnailLoader(ImageView imageView, int width, int height) {
        this.imageViewReference = new WeakReference<ImageView>(imageView);
        this.width = width;
        this.height = height;
    }

    @Override
    public Bitmap doInBackground(String... params) {
        Bitmap bmp = BitmapFactory.decodeFile(params[0]);
        Bitmap icon_bmp = ThumbnailUtils.extractThumbnail(bmp, width, height);
        return icon_bmp;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            bitmap = null;
        }

        if (imageViewReference != null) {
            ImageView imageView = imageViewReference.get();
            if ((imageView != null) && (bitmap != null)) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}

