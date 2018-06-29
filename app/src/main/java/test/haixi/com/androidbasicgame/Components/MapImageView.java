package test.haixi.com.androidbasicgame.Components;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import test.haixi.com.androidbasicgame.R;

@SuppressLint("AppCompatCustomView")
public class MapImageView extends ImageView{
    private Bitmap originBitmap, leftBitmap, rightBitmap, bitmap;
    private ViewTreeObserver observer;
    private IntentFilter intentFilter;
    private MapImageViewBroadcast mapImageViewBroadcast;

    private static int mapImageViewBroadcastCode = 0;//第几个mapImageView
    private int windowsWidth, windowsHeight, originBitmapWidth, originBitmapHeight, x = 0, y = 0;
    private boolean isLeft_Right = false, isMeasureWH = false;
    private String intentActionLoadImage, intentActionDestroyImage,
            intentAction = "test.haixi.com.androidbasicgame.Components.MapImageView";

    public MapImageView(Context context) {
        super(context);
        initImageView();
    }

    public MapImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initImageView();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Intent intent = new Intent(intentActionDestroyImage);
        getContext().sendBroadcast(intent);
    }

    public void initImageView(){
        intentActionDestroyImage = intentAction + "." + mapImageViewBroadcastCode + ".destroyImage";
        intentActionLoadImage = intentAction + "." + mapImageViewBroadcastCode + ".loadImage";
        mapImageViewBroadcastCode++;

        intentFilter = new IntentFilter();
        mapImageViewBroadcast = new MapImageViewBroadcast();
        intentFilter.addAction(intentActionDestroyImage);
        intentFilter.addAction(intentActionLoadImage);
        getContext().registerReceiver(mapImageViewBroadcast, intentFilter);

        observer = getRootView().getViewTreeObserver();

        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onGlobalLayout() {
                if (observer.isAlive()){
                    observer.removeOnGlobalLayoutListener(this);
                }
                if (isMeasureWH){
                    return;
                }
                isMeasureWH = false;
                windowsWidth = getMeasuredWidth();
                windowsHeight = getMeasuredHeight();
                System.out.println("width = " + getMeasuredWidth() + ", height = " + getMeasuredHeight());
                Intent intent = new Intent(intentActionLoadImage);
                getContext().sendBroadcast(intent);
            }
        });
    }

    int curX = 0, curY = 0, dX = 0, dY = 0;
    public void setAction(){
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        dX = (int) motionEvent.getX();
                        dY = (int) motionEvent.getY();
                        curX = dX;
                        curY = dY;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        curX = (int) motionEvent.getX();
                        curY = (int) motionEvent.getY();
                        calculateWidth(dX - curX);
                        calculateHeight(dY - curY);
//                        System.out.println("dX = " + dX + ", dY = " + dY + ", curX = " + curX + ", curY = " + curY);
                        setImage(x, y);
                        dX = curX; dY = curY;
                        break;
                    case MotionEvent.ACTION_UP:
                        curX = (int) motionEvent.getX();
                        curY = (int) motionEvent.getY();
                        calculateWidth(dX - curX);
                        calculateHeight(dY - curY);
                        setImage(x, y);
                        dX = curX; dY = curY;
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
    }

    public void calculateWidth(int curX){
//        System.out.println("curX = " + curX);
        x = x + curX;
        if (x >= 0 && x <= (originBitmapWidth - windowsWidth)){
            isLeft_Right = false;
        }else {
            x = (x + originBitmapWidth) % originBitmapWidth;
            isLeft_Right = true;
        }
//        System.out.println("x = " + x);
    }

    public void calculateHeight(int curY){
//        System.out.println("curY = " + curY);
        y = y + curY;
        if (y < 0){
            y = 0;
        }else if ((y + windowsHeight) > originBitmapHeight){
            y = originBitmapHeight - windowsHeight;
        }else {
        }
//        System.out.println("y = " + y);
    }

    private void setImage(int x, int y){
        System.out.println("width = " + getWidth() + ", height = " + getHeight());
//        System.out.println("x = " + x + ", y = " + y);
        if (isLeft_Right){
            int midX = (x + windowsWidth) % originBitmapWidth;
//            System.out.println("mid = " + midX);
            leftBitmap = Bitmap.createBitmap(originBitmap, x, y, (originBitmapWidth - x), windowsHeight);
            rightBitmap = Bitmap.createBitmap(originBitmap, 0, y, midX, windowsHeight);

            leftBitmap = Bitmap.createBitmap(originBitmap, x, y,
                    (originBitmapWidth - x), windowsHeight);
            rightBitmap = Bitmap.createBitmap(originBitmap, 0, y,
                    midX, windowsHeight);
            if (leftBitmap.getWidth() == windowsWidth){
                bitmap = leftBitmap;
            }else if (rightBitmap.getWidth() == windowsWidth){
                bitmap = rightBitmap;
            }else {
                bitmap = Bitmap.createBitmap((int) windowsWidth, windowsHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawBitmap(leftBitmap, 0, 0, null);
                canvas.drawBitmap(rightBitmap, leftBitmap.getWidth(), 0, null);
            }
        }else {
            bitmap = Bitmap.createBitmap(originBitmap, x, y, windowsWidth, windowsHeight);
        }
        this.setImageBitmap(bitmap);
    }

    private class MapImageViewBroadcast extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(intentActionLoadImage)){
                originBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.earth);//图片
                Matrix matrix = new Matrix();
                matrix.setScale(0.66f, 0.66f);
                originBitmap = Bitmap.createBitmap(BitmapFactory.decodeResource(getContext().getResources(), R.drawable.earth),
                        0, 0, originBitmap.getWidth(), originBitmap.getHeight(), matrix, true);
                originBitmapWidth = originBitmap.getWidth();
                originBitmapHeight = originBitmap.getHeight();

                setImage(x, y);
                setAction();
            }else if (action.equals(intentActionDestroyImage)){
                getContext().unregisterReceiver(this);
            }
        }
    }
}
