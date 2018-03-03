package com.seu.SecureFingerMouse;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import android.os.Process;

/* @project 
 * 
 * License to access, copy or distribute this file.
 * This file or any portions of it, is Copyright (C) 2012, Radu Motisan ,  http://www.pocketmagic.net . All rights reserved.
 * @author Radu Motisan, radu.motisan@gmail.com
 * 
 * This file is protected by copyright law and international treaties. Unauthorized access, reproduction 
 * or distribution of this file or any portions of it may result in severe civil and criminal penalties.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * @purpose 
 * Cursor Overlay Sample
 * (C) 2012 Radu Motisan , all rights reserved.
 */
public class CursorService extends Service {
    OverlayView mView;
    
    private int m_nScreenW = 0, m_nScreenH = 0;
    
    /**
     * @param x
     * @param y
     * @param autoenable if set, it will automatically show the cursor when movement is detected
     */
    public void Update( int x,  int y, final boolean autoenable) {
    	
//    	//lingzhen.2014.07.04 
//    	if (m_nScreenW > x && m_nScreenH > y){
//    		x = m_nScreenW; y = m_nScreenH;
//    	}else if(m_nScreenW > x){
//    		x = m_nScreenW;
//    	}else if (m_nScreenH > y){
//    		y = m_nScreenH;
//    	}
    	
    	mView.Update(x,y);
    	if ((x!=0 || y!= 0) && autoenable && mView.isCursorShown() ) 
    		ShowCursor(true); //will also post invalidate
    	else
    		mView.postInvalidate();
    }
    public void ShowCursor(boolean status) {
    	mView.ShowCursor(status);
    	mView.postInvalidate();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Singleton.getInstance().m_CurService = this;

        Log.d("MyService", "CursorService thread id is " + Thread.currentThread().getId());  
        
        Log.d("MyService", "CursorService process id is " + Process.myPid());  
        Log.d("MyService", "CursorService thread id is " + Process.myTid());  

		Log.d("CursorService", "Service created");
		
        mView = new OverlayView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, //WRAP_CONTENT, //MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT, //WRAP_CONTENT, //FILL_PARENT
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,//TYPE_SYSTEM_ALERT,//TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, //will cover status bar as well!!!
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.LEFT | Gravity.TOP;
       //params.x = 100;
        //params.y = 100;
        params.setTitle("Cursor");
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mView, params);
        //wm.        
        
     // get screen size
	    DisplayMetrics metrics = new DisplayMetrics();
		try {
			WindowManager winMgr = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
	       	winMgr.getDefaultDisplay().getMetrics(metrics);
	       	m_nScreenW = winMgr.getDefaultDisplay().getWidth();
	       	m_nScreenH = winMgr.getDefaultDisplay().getHeight();
		}
		catch (Exception e) { //default to a HVGA 320x480 and let's hope for the best
			e.printStackTrace();
			m_nScreenW = 0;
			m_nScreenH = 0;
		} 
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("CursorService", "Service destroyed");
        Singleton.getInstance().m_CurService = null;
        if(mView != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mView);
            mView = null;
        }
    }
}

class OverlayView extends ViewGroup {
    private Paint mLoadPaint;
    boolean mShowCursor;
    
    Bitmap	cursor;
    public int x = 0,y = 0;
    
    
    public void Update(int nx, int ny) {
    		x = nx; y = ny;
    }
    public void ShowCursor(boolean status) {
    	mShowCursor = status;
	}
    public boolean isCursorShown() {
    	return mShowCursor;
    }
    
	public OverlayView(Context context) {
        super(context);
        cursor = BitmapFactory.decodeResource(context.getResources(), R.drawable.cursor);

        mLoadPaint = new Paint();
        mLoadPaint.setAntiAlias(true);
        mLoadPaint.setTextSize(10);
        mLoadPaint.setARGB(255, 255, 0, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawText("Hello World", 0, 0, mLoadPaint);
        if (mShowCursor) canvas.drawBitmap(cursor,x,y,null);
    }

    @Override
    protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
