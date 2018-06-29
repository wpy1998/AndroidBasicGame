package test.haixi.com.library;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

@SuppressLint("AppCompatCustomView")
public class MapImageView extends ImageView{
    /*
    * the design of the component
    * MapImageView()->initImageView();
    * across the broadcast
    * initOriginBitmap()->setAction();
    *
    * my thinking is first load a big bitmap to originBitmap
    * by touching the screen get the X and Y
    * then change the window's X and Y to the current bitmapX and bitmapY
    * the by the bitmapX/Y cut the bitmap from originBitmap
    *
    * my aim is to avoid the oom, but there still many problem remain to be solved
    * now only support the move of the big image
    * then will come true the scale of the image
    * */
    private Bitmap originBitmap, bitmap;
    private ViewTreeObserver observer;
    private IntentFilter intentFilter;
    private MapImageViewBroadcast mapImageViewBroadcast;

    private static int mapImageViewBroadcastCode = 0;//account the number of  all mapImageViews
    private int windowsWidth, windowsHeight, originBitmapWidth, originBitmapHeight, x = 0, y = 0;
    private float certificationScale = 0.66f;
    private boolean isMeasureWH = false;
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
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus == false){//lose the focus from the window
            Intent intent = new Intent(intentActionDestroyImage);
            getContext().sendBroadcast(intent);
        }
    }

    //initMapImageView get the width and height
    private void initImageView(){
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
    /*
        bitmapX/Y != windowX/Y , the function calculateX/Y will change them to the right type
        dx; x before the movement;
        dy; x before the movement;
        curX; x after the movement;
        curY; x after the movement;
    */
    private void setAction(){
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

    //init the originBitmap's width and height( will use the windowWidth)
    private void initOriginBitmap(){
        Bitmap bitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.earth);//图片
        originBitmapWidth = bitmap1.getWidth();
        originBitmapHeight = bitmap1.getHeight();
        originBitmap = Bitmap.createBitmap((int) (windowsWidth / certificationScale  + originBitmapWidth),
                originBitmapHeight,
                Bitmap.Config.ARGB_8888);
        Bitmap windowBitmap = Bitmap.createBitmap(bitmap1, 0, 0, (int) (windowsWidth / certificationScale), originBitmapHeight);

        Canvas canvas = new Canvas(originBitmap);
        canvas.drawBitmap(bitmap1, 0, 0, null);
        canvas.drawBitmap(windowBitmap, bitmap1.getWidth(), 0, null);
//        System.out.println("width = " + originBitmap.getWidth() + ", height = " + originBitmap.getHeight());

        Matrix matrix = new Matrix();
        matrix.setScale(certificationScale, certificationScale);
        originBitmap = Bitmap.createBitmap(originBitmap,
                0, 0, originBitmap.getWidth(), originBitmap.getHeight(), matrix, true);
        originBitmapWidth = originBitmap.getWidth();
        originBitmapHeight = originBitmap.getHeight();
//        System.out.println("originBitmapWidth = " + originBitmapWidth + ", originBitmapHeight = " + originBitmapHeight);
    }

    //calculate the right x of the image, only used by setAction()
    private void calculateWidth(int curX){
        x = x + curX;
        if (x < 0){
            x = x + (originBitmapWidth - windowsWidth);
        }else if (x >= (originBitmapWidth - windowsWidth)){
            x = x - (originBitmapWidth - windowsWidth);
        }
    }

    //calculate the right y of the image, only used by setAction()
    private void calculateHeight(int curY){
        y = y + curY;
        if (y < 0){
            y = 0;
        }else if ((y + windowsHeight) > originBitmapHeight){
            y = originBitmapHeight - windowsHeight;
        }else {
        }
    }

    //change the right bitmap, only used by setAction()
    private void setImage(int x, int y){
//        System.out.println("x = " + x + ", y = " + y);
        bitmap = Bitmap.createBitmap(originBitmap, x, y, windowsWidth, windowsHeight);
        this.setImageBitmap(bitmap);
    }

    private class MapImageViewBroadcast extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(intentActionLoadImage)){
                initOriginBitmap();
                setImage(x, y);
                setAction();
            }else if (action.equals(intentActionDestroyImage)){
                getContext().unregisterReceiver(this);
            }
        }
    }
}
