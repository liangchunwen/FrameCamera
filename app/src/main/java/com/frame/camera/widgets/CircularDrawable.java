package com.frame.camera.widgets;


import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class CircularDrawable extends Drawable {
    private final Paint mPaint;
    private Rect mRect;
    private int mLength;

    public CircularDrawable(Bitmap bitmap, ImageView mThumb) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        mLength = mThumb.getWidth();
        if (w > h) {
//                 mLength = h;
            bitmap = Bitmap.createBitmap(bitmap, (w - h) / 2, 0, h, h);
            w = h;
        } else if (w < h) {
//                 mLength = w;
            bitmap = Bitmap.createBitmap(bitmap, 0, (h - w) / 2, w, w);
            h = w;
        }
        float scaleWidth = ((float)mLength) / w;
        float scaleHeight = (float)mLength / h;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newBm = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);

        BitmapShader mBitmapShader = new BitmapShader(newBm, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setShader(mBitmapShader);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mRect = bounds;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRoundRect(new RectF(mRect), (float) ((mRect.right - mRect.left) / 2.0),
                (float) ((mRect.bottom - mRect.top) / 2.0), mPaint);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        mPaint.setColorFilter(filter);
    }

    @Override
    public int getIntrinsicWidth() {
        return mLength;
    }

    @Override
    public int getIntrinsicHeight() {
        return mLength;
    }
}