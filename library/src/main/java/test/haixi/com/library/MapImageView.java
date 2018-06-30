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
import android.widget.Toast;

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
    private Bitmap originBitmap, bitmap, basicBitmap;
    private ViewTreeObserver observer;
    private IntentFilter intentFilter;
    private MapImageViewBroadcast mapImageViewBroadcast;

    private static int mapImageViewBroadcastCode = 0;//account the number of  all mapImageViews
    private int windowsWidth, windowsHeight, originBitmapWidth, originBitmapHeight, x = 0, y = 0;
    private float certificationScale = 0.66f, minCertificationScale, maxCertificationScale = 2;
    private boolean isMeasureWH = false, isBroadcastRegister = false;
    private String intentActionInitImage, intentActionDestroyImage, intentActionMoveImage,
            intentActionLoadOver = "test.haixi.com.library.MapImageView.loadOver",
            intentAction = "test.haixi.com.library.MapImageView";

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
        }else {
            if (isBroadcastRegister == false){
                isBroadcastRegister = true;
                registerMapImageViewBroadcast();
            }
        }
    }

    //initMapImageView get the width and height
    private void initImageView(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                basicBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.earth);//图片
                Intent intent = new Intent(intentActionLoadOver);
                getContext().sendBroadcast(intent);
            }
        }).start();
        intentActionDestroyImage = intentAction + "." + mapImageViewBroadcastCode + ".destroyImage";
        intentActionInitImage = intentAction + "." + mapImageViewBroadcastCode + ".initImage";
        intentActionMoveImage = intentAction + "." + mapImageViewBroadcastCode + ".moveImage";
        mapImageViewBroadcastCode++;

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
                isMeasureWH = true;
                windowsWidth = getMeasuredWidth();
                windowsHeight = getMeasuredHeight();
//                System.out.println("width = " + getMeasuredWidth() + ", height = " + getMeasuredHeight());
            }
        });
    }

    int curX = 0, curY = 0, dX = 0, dY = 0, pointNumber = 0;
    float centerX = -1, centerY = -1, changeX, changeY, averageDistance;
    /*
        bitmapX/Y != windowX/Y , the function calculateX/Y will change them to the right type
        point = 1
        dx; x before the movement;
        dy; x before the movement;
        curX; x after the movement;
        curY; x after the movement;

        point >= 2
        centerX: many focus in the screen X
        centerY: many focus in the screen Y
        changeX: after the center changed X
        changeY: after the center changed Y
    */
    private void setAction(){
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        pointNumber = 1;
                        dX = (int) motionEvent.getX();
                        dY = (int) motionEvent.getY();
                        curX = dX;
                        curY = dY;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int pNumber;
                        pNumber = motionEvent.getPointerCount();
                        if (pNumber == 1){
                            curX = (int) motionEvent.getX();
                            curY = (int) motionEvent.getY();
                            if (Math.abs(curX - dX) <= 1 && Math.abs(curY - dY) <= 1){//极小x,y忽略
                                pointNumber = pNumber;
                                break;
                            }

                            if (centerX >= 0 && centerY >= 0){//两个手指一个离开情况屏幕不动
                                dX = (int) motionEvent.getX();
                                dY = (int) motionEvent.getY();
                                centerX = -1;
                                centerY = -1;
                                pointNumber = pNumber;
                                break;
                            }

                            calculateWidth(dX - curX);
                            calculateHeight(dY - curY);
                            Intent intent = new Intent(intentActionMoveImage);
                            getContext().sendBroadcast(intent);

                        }else if (pNumber >= 2){
                            calculateChangeXY(motionEvent);
                            if (centerX < 0 || centerY < 0 || pNumber < pointNumber){
                                calculateChangeXY(motionEvent);
                                centerX = changeX;
                                centerY = changeY;
                                changeX = -1;
                                changeY = -1;
                                pointNumber = pNumber;
                                break;
                            }

                            if (Math.abs(centerX - changeX) <= 1 && Math.abs(centerY - changeY) <= 1){
                                pointNumber = pNumber;
                                break;
                            }

                            calculateWidth((int) (centerX - changeX));
                            calculateHeight((int) (centerY - changeY));
                            Intent intent = new Intent(intentActionMoveImage);
                            getContext().sendBroadcast(intent);
                        }
                        pointNumber = pNumber;
                        break;
                    case MotionEvent.ACTION_UP:
                        pointNumber = 0;
                        centerX = -1;
                        centerY = -1;
                        changeX = -1;
                        changeY = -1;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        System.out.println("action_Pointer_down");
                        pointNumber = motionEvent.getPointerCount();
                        calculateChangeXY(motionEvent);
                        centerX = changeX;
                        centerY = changeY;
                        changeX = -1;
                        changeY = -1;
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        pointNumber = motionEvent.getPointerCount();
                        if (pointNumber <= 1){
                            centerX = -1;
                            centerY = -1;
                            changeX = -1;
                            changeY = -1;
                            break;
                        }

                        calculateChangeXY(motionEvent);
                        centerX = changeX;
                        centerY = changeY;
                        changeX = -1;
                        changeY = -1;
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
    }

    //--------------------------------calculate---------------------------------------------
    private void calculateChangeXY(MotionEvent event){
        changeX = 0; changeY = 0;
        int pNumber = event.getPointerCount();
        for (int i = 0; i < pNumber; i++){
            changeX = event.getX(i) + changeX;
            changeY = event.getY(i) + changeY;
        }
        changeX = changeX / pNumber;
        changeY = changeY / pNumber;
    }

    private float calculateAverageDistance(MotionEvent event){
        float midX, midY, midDistance = 0f;
        int pNumber = event.getPointerCount();
        for (int i = 0; i < pNumber; i++){
            midX = event.getX(i) - centerX;
            midY = event.getY(i) - centerY;
            midDistance = (float) Math.sqrt(midX * midX + midY * midY) + midDistance;
        }
        midDistance = midDistance / event.getPointerCount();
        return midDistance;
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

    //------------------------bitmap action------------------------------------------
    //init the originBitmap's width and height( will use the windowWidth)
    private void initOriginBitmap(){
        originBitmapWidth = basicBitmap.getWidth();
        originBitmapHeight = basicBitmap.getHeight();
        if (originBitmapWidth < windowsWidth || originBitmapHeight < windowsHeight){
            Toast.makeText(getContext(), "图片太小无法加载", Toast.LENGTH_SHORT).show();
            return;
        }

        minCertificationScale = windowsWidth / originBitmapWidth;
        float min2 = windowsHeight / originBitmapHeight;
        if (min2 > minCertificationScale){
            minCertificationScale = min2;
        }

        maxCertificationScale = 2f;
        System.out.println("min = " + minCertificationScale);

        originBitmap = Bitmap.createBitmap((int) (windowsWidth / certificationScale  + originBitmapWidth),
                originBitmapHeight,
                Bitmap.Config.ARGB_8888);
        Bitmap windowBitmap = Bitmap.createBitmap(basicBitmap, 0, 0, (int) (windowsWidth / certificationScale), originBitmapHeight);

        Canvas canvas = new Canvas(originBitmap);
        canvas.drawBitmap(basicBitmap, 0, 0, null);
        canvas.drawBitmap(windowBitmap, basicBitmap.getWidth(), 0, null);
//        System.out.println("width = " + originBitmap.getWidth() + ", height = " + originBitmap.getHeight());

        Matrix matrix = new Matrix();
        matrix.setScale(certificationScale, certificationScale);
        originBitmap = Bitmap.createBitmap(originBitmap,
                0, 0, originBitmap.getWidth(), originBitmap.getHeight(), matrix, true);
        originBitmapWidth = originBitmap.getWidth();
        originBitmapHeight = originBitmap.getHeight();
//        System.out.println("originBitmapWidth = " + originBitmapWidth + ", originBitmapHeight = " + originBitmapHeight);
    }

    //change the right bitmap, only used by setAction()
    private void setImage(int x, int y){
//        System.out.println("x = " + x + ", y = " + y);
        bitmap = Bitmap.createBitmap(originBitmap, x, y, windowsWidth, windowsHeight);
        this.setImageBitmap(bitmap);
    }



    private void registerMapImageViewBroadcast(){
        intentFilter = new IntentFilter();
        mapImageViewBroadcast = new MapImageViewBroadcast();
        intentFilter.addAction(intentActionDestroyImage);
        intentFilter.addAction(intentActionInitImage);
        intentFilter.addAction(intentActionLoadOver);
        intentFilter.addAction(intentActionMoveImage);
        getContext().registerReceiver(mapImageViewBroadcast, intentFilter);
    }

    private class MapImageViewBroadcast extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(intentActionInitImage)){
                initOriginBitmap();
                setImage(x, y);
                dX = curX;
                dY = curY;
                setAction();
            }else if (action.equals(intentActionDestroyImage)){
                getContext().unregisterReceiver(this);
                isBroadcastRegister = false;
            }else if (action.equals(intentActionLoadOver)){
                Intent intent1 = new Intent(intentActionInitImage);
                getContext().sendBroadcast(intent1);
            }else if (action.equals(intentActionMoveImage)){
                setImage(x, y);
                dX = curX;
                dY = curY;
                centerX = changeX;
                centerY = changeY;
            }
        }
    }
}
