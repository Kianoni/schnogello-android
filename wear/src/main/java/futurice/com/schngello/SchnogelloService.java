package futurice.com.schngello;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by kilp on 30/01/15.
 */
public class SchnogelloService extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        /* provide your watch face implementation */
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        static final int MSG_UPDATE_TIME = 0;
        private final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

        /* a time object */
        Time time;

        /* device features */
        boolean lowBitAmbient;

        /* graphic objects */
        Bitmap backgroundBitmap;
        Bitmap hourBitmap;
        Bitmap minuteBitmap;

        /* precomputed translation and rotate origins*/
        float hourX, hourY,minuteX, minuteY;
        float hourRotX, hourRotY, minuteRotX, minuteRotY;

        Paint clockHandPaint;

        boolean registeredReceiver;

        final Handler mUpdateTimeHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_UPDATE_TIME:
                    invalidate();
                    if (shouldTimerBeRunning()) {
                        long timeMs = System.currentTimeMillis();
                        long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                        mUpdateTimeHandler
                                .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                    }
                    break;
            }
            }
        };

        /* receiver to update the time zone */
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                time.clear(intent.getStringExtra("time-zone"));
                time.setToNow();
            }
        };

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            Resources resources = SchnogelloService.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.clock_face);
            Drawable hourDrawable = resources.getDrawable(R.drawable.hour_hand);
            Drawable minuteDrawable = resources.getDrawable(R.drawable.minute_hand);
            Bitmap originalBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
            Bitmap originalHourBitmap = ((BitmapDrawable) hourDrawable).getBitmap();
            Bitmap originalMinuteBitmap = ((BitmapDrawable) minuteDrawable).getBitmap();
            // Scale the drawables here
            backgroundBitmap = Bitmap.createScaledBitmap(originalBackgroundBitmap, width, height, true);
            float ratio = ((float)backgroundBitmap.getWidth() / originalBackgroundBitmap.getWidth());
            minuteBitmap = Bitmap.createScaledBitmap(originalMinuteBitmap,
                    (int) (originalMinuteBitmap.getWidth() * ratio), (int) (originalMinuteBitmap.getHeight() * ratio), true);
            hourBitmap = Bitmap.createScaledBitmap(originalHourBitmap,
                    (int) (originalHourBitmap.getWidth() * ratio), (int) (originalHourBitmap.getHeight() * ratio), true);

            hourRotX = width / 2;
            hourRotY = height / 2;
            hourX = ((float)width / 2) - ratio * 97f;
            hourY = ((float)height / 2) - ratio * 232f;

            minuteRotX = width / 2;
            minuteRotY = height / 2;
            minuteX = ((float) width / 2) - ratio * 60f;
            minuteY = ((float) height / 2) - ratio * 250f;
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            /* initialize your watch face */
            clockHandPaint = new Paint();
            clockHandPaint.setFilterBitmap(true);
            time = new Time();
            setWatchFaceStyle(new WatchFaceStyle.Builder(SchnogelloService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /* the time changed */
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /* the wearable switched between modes */
            lowBitAmbient = !inAmbientMode;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */
            time.setToNow();

            canvas.drawBitmap(backgroundBitmap, 0, 0, null);

            // minute and hour variables for easy testing (switch to seconds and minutes)
            long minute = time.minute;
            int hour = time.hour;

            float minRot = minute * 6.0f;
            float hourRot = hour * 30.0f + (0.5f * minute);

            Matrix minuteMatrix = new Matrix();
            minuteMatrix.preTranslate(minuteX, minuteY);
            minuteMatrix.postRotate(minRot, minuteRotX, minuteRotY);

            Matrix hourMatrix = new Matrix();
            hourMatrix.preTranslate(hourX, hourY);
            hourMatrix.postRotate(hourRot, hourRotX, hourRotY);

            canvas.drawBitmap(hourBitmap, hourMatrix, clockHandPaint);
            canvas.drawBitmap(minuteBitmap, minuteMatrix, clockHandPaint);
        }

        private void registerReceiver() {
            if (registeredReceiver) {
                return;
            }
            registeredReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SchnogelloService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!registeredReceiver) {
                return;
            }
            registeredReceiver = false;
            SchnogelloService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                time.clear(TimeZone.getDefault().getID());
                time.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible and
            // whether we're in ambient mode), so we may need to start or stop the timer
            updateTimer();

        }
    }

}
