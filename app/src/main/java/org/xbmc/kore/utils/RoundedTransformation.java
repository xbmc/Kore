package org.xbmc.kore.utils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Rounded corner transformation for {@link com.squareup.picasso}.
 *
 * <p>
 * Modified code taken from https://gist.github.com/aprock/6213395
 * <p>
 * enables hardware accelerated rounded corners <br/>
 * original idea here :
 * http://www.curious-creature.org/2012/12/11/android-recipe-1-image-with-rounded-corners/
 */
public class RoundedTransformation implements com.squareup.picasso.Transformation {
    private final int radius;  // dp
    private final int margin;  // dp
    private final String KEY;
    private boolean topCorners = true;
    private boolean bottomCorners = true;

    /**
     * Creates rounded transformation for all corners.
     *
     * @param radius radius is corner radii in dp
     * @param margin margin is the board in dp
     */
    public RoundedTransformation(final int radius, final int margin) {
        this.radius = radius;
        this.margin = margin;
        KEY = "rounded_" + radius + margin;
    }


    /**
     * Creates rounded transformation for top or bottom corners.
     *
     * @param radius radius is corner radii in dp
     * @param margin margin is the board in dp
     * @param topCornersOnly Rounded corner for top corners only.
     * @param bottomCornersOnly Rounded corner for bottom corners only.
     */
    public RoundedTransformation(final int radius, final int margin, boolean topCornersOnly,
            boolean bottomCornersOnly) {
        this(radius, margin);
        topCorners = topCornersOnly;
        bottomCorners = bottomCornersOnly;
    }

    @Override
    public Bitmap transform(final Bitmap source) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

        Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        if(topCorners && bottomCorners) {
            // Uses native method to draw symmetric rounded corners
            canvas.drawRoundRect(new RectF(margin, margin, source.getWidth() - margin,
                    source.getHeight() - margin), radius, radius, paint);
        } else {
            // Uses custom path to generate rounded corner individually
            canvas.drawPath(RoundedRect(margin, margin, source.getWidth() - margin,
                    source.getHeight() - margin, radius, radius, topCorners, topCorners,
                    bottomCorners, bottomCorners), paint);
        }


        if (source != output) {
            source.recycle();
        }

        return output;
    }

    @Override
    public String key() {
        return KEY;
    }

    /**
     * Prepares a path for rounded corner selectively.
     * <p>
     * Source taken from http://stackoverflow.com/a/35668889/6635889 <br/>
     * Usage:
     * <pre>
     *     Path path = RoundedRect(0, 0, fwidth , fheight , 5,5, false, true, true, false);
     *     canvas.drawPath(path, myPaint);
     * </pre>
     *
     * @param leftX The X coordinate of the left side of the rectangle
     * @param topY The Y coordinate of the top of the rectangle
     * @param rightX The X coordinate of the right side of the rectangle
     * @param bottomY The Y coordinate of the bottom of the rectangle
     * @param rx The x-radius of the oval used to round the corners
     * @param ry The y-radius of the oval used to round the corners
     * @param topLeft
     * @param topRight
     * @param bottomRight
     * @param bottomLeft
     * @return
     */
    public static Path RoundedRect(float leftX, float topY, float rightX, float bottomY, float rx,
            float ry, boolean topLeft, boolean topRight, boolean
            bottomRight, boolean bottomLeft) {
        Path path = new Path();
        if (rx < 0) rx = 0;
        if (ry < 0) ry = 0;
        float width = rightX - leftX;
        float height = bottomY - topY;
        if (rx > width / 2) rx = width / 2;
        if (ry > height / 2) ry = height / 2;
        float widthMinusCorners = (width - (2 * rx));
        float heightMinusCorners = (height - (2 * ry));

        path.moveTo(rightX, topY + ry);
        if (topRight)
            path.rQuadTo(0, -ry, -rx, -ry);//top-right corner
        else{
            path.rLineTo(0, -ry);
            path.rLineTo(-rx,0);
        }
        path.rLineTo(-widthMinusCorners, 0);
        if (topLeft)
            path.rQuadTo(-rx, 0, -rx, ry); //top-left corner
        else{
            path.rLineTo(-rx, 0);
            path.rLineTo(0,ry);
        }
        path.rLineTo(0, heightMinusCorners);

        if (bottomLeft)
            path.rQuadTo(0, ry, rx, ry);//bottom-left corner
        else{
            path.rLineTo(0, ry);
            path.rLineTo(rx,0);
        }

        path.rLineTo(widthMinusCorners, 0);
        if (bottomRight)
            path.rQuadTo(rx, 0, rx, -ry); //bottom-right corner
        else{
            path.rLineTo(rx,0);
            path.rLineTo(0, -ry);
        }

        path.rLineTo(0, -heightMinusCorners);

        path.close();//Given close, last lineto can be removed.

        return path;
    }

}
