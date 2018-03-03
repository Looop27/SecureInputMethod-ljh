/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//package com.seu.android.softkeyboard;
package com.seu.SecureFingerMouse;

//package com.random.android.randomkeyboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Date;

import org.opencv.android.BaseLoaderCallback;
//import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.*;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.view.MotionEventCompat;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.*;

/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class SoftKeyboard extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener, OpenCVWorker.ResultCallback,
        SurfaceHolder.Callback, View.OnTouchListener, GestureDetector.OnDoubleTapListener {
    
    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on 
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
  static final boolean PROCESS_HARD_KEYS = true; //是否在用硬键盘，这里默认的是总可以使用,费柴变量

    private InputMethodManager mInputMethodManager; //输入管理器

    private LatinKeyboardView mInputView;          //输入视图
    private CandidateView mCandidateView;          //候选区视图
    private CompletionInfo[] mCompletions;         //候选串之串
    
    private StringBuilder mComposing = new StringBuilder();  //一个字符串，候选区有关
    private boolean mPredictionOn;                 //这东西是决定能不能有候选条
    private boolean mCompletionOn;                 //决定auto是否需要显示在候选栏
    private int mLastDisplayWidth;                 //上次显示大小
    private boolean mCapsLock;                     //是否大写
    private long mLastShiftTime;                   //上次shift键的时间
    private long mMetaState;                       //matakey的按下状态，猜测是每种组合对应一个此值
    
    private LatinKeyboard mSymbolsKeyboard;         //特殊键盘
    private LatinKeyboard mSymbolsShiftedKeyboard;  //按下shift界面
    private LatinKeyboard mQwertyKeyboard;          //字母键盘

    private LatinKeyboard mKeypad;                  //某种键盘？
    private LatinKeyboard mRandomSymbolsKeyboard;   //之下三种 是上面3种的随机版，不过，选择项被设置成false,不会被选择
    private LatinKeyboard mRandomSymbolsShiftedKeyboard;
    private LatinKeyboard mRandomQwertyKeyboard;
    
    private LatinKeyboard mCurKeyboard;             //当前键盘
    
    private String mWordSeparators;                 //特殊字母表，默认使得输入中断的字符
    
    private static final String    TAG = "OCVSample::Activity";


     private View newView;                          //对于一个没有被载入或者想要动态载入的界面，都需要使用LayoutInflater.inflate()来载入，这个变量存储这个界面的信息
    //以下为布局视图
    private FrameLayout newLinearLayout;
    private FrameLayout m_panel_view;
    private LinearLayout LinearLayout_text;
    public static TextView text_x;
    public static TextView text_y;
    public static TextView text_dx;
    public static TextView text_dy;
    public static TextView text_d;
    public static TextView text_dt;


	int m_nScreenW = 0, m_nScreenH = 0;         //屏幕宽高
	boolean m_bRunning = false;                 //是否正在运行（未关闭或隐藏）
	int inital = 0;                             //某一函数用到的统计量，只在=0时有判断价值
		
	public static Handler tHandler;             //处理上个处理器mHandler发来的消息

    //电源和时间锁
	public PowerManager pm;
	public PowerManager.WakeLock mWakeLock; 
	
	// the size of each key ->键的大小
    // Sabrina
	private static final int KEY_HEIGHT = 60;// 60 dip
	private static final int ROWNUM = 4;

    //一些定值
    public static final int DRAW_RESULT_BITMAP = 10;
    public static final int DRAW_CURSOR_POINT = 11;
    public static final int RAW_POINT = 12;

    private Handler mSurfaceViewHandler;          //黑色视图处理器
    private SurfaceView mSurfaceView;             //黑色展示相机图像部分
    private SurfaceHolder mSurfaceHolder;         //显示一个surface的抽象接口，使你可以控制surface的大小和格式， 以及在surface上编辑像素，和监视surace的改变。
    private RectF mSurfaceSize;                   //黑色区域大小
    private OpenCVWorker mWorker;                 //opencv线程
    private double mFpsResult;                    //FPS返回结果
    private Paint mFpsPaint;                      //FPS画图工具
    private GestureDetector mGestureDetector;     //手势处理对象
    private boolean drawCanvas = true;            //是否可以绘制界面
	public Point getPoint = new Point(0,0);       //获取鼠标点信息
	
	//Sabrina
    //和鼠标随机移动有关
	private int[] acc = {16,18,20,22,24,26};
	private int[] thre = {2,3,4,5,6,8};
	private int randomacc = 0;
	private int numerator = 14;
	private int denominator = 2;
	private int threshold = 3;

    //Sabrina
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    //dxy存储的文件
	public File dxyinfo = new File(Environment.getExternalStorageDirectory(),"deltadata"+".txt");
	
   
	
	public boolean inputstate = true;    //输入状态，但是未被用到
    public int row = 5;

    //不同键盘的字符表
    private String [] keylista = {"q","w","e","r","t","y","u","i","o","p","a","s","d","f","g","h","j","k","l","shift","z","x","c","v","b","n","m","del",
   			"done","123","space","point","next"};
    private String [] keylistb = {"Q","W","E","R","T","Y","U","I","O","P","A","S","D","F","G","H","J","K","L","shift","Z","X","C","V","B","N","M","del",
   			"done","123","space","point","next"};
    private String [] keylistc = {"1","2","3","4","5","6","7","8","9","0","@","#","$","%","&","*","-","=","(",")","shift","!","\"","'",":",";","/","?","del",
   			"done","ABC","space","comma","next"};
    private String [] keylistd = {"~","±","×","÷","•","°","`","´","{","}","©","£","€","^","®","¥","_","+","[","]","shift","¡","&lt;","&gt;","¢","|","\\","¿",
    		"del","done","ABC","space","apostrophe","next"};
    private String [] keylistp = {"7","8","9","4","5","6","1","2","3","del","0","next"};

    //存储所有键位信息
	private String[][] keyinfoa = new String[keylista.length][row];
    private String[][] keyinfob = new String[keylistb.length][row];
    private String[][] keyinfoc = new String[keylistc.length][row];
    private String[][] keyinfod = new String[keylistd.length][row];
    private String[][] keyinfop = new String[keylistp.length][row];
    private String[][] subkeyinfo;

    public long init;

    //存储字母对应的ACSII
    public Map<String, Integer> keymap = new HashMap<String, Integer>();
    
    //a h j k del shift A h shift shift A H 123 5 # ! del ABC A 123 " " , shift + [ … shift & ABC A shift shift a h
    public int[][] c = {{100,1500},{600,1600},{800,1550},{900,1500},{950,1700},{100,1700},{100,1500},{600,1600},
    		//a h j k del shift A h
    		{100,1700},{100,1700},{100,1500},{600,1600},{300,1800},{500,1400},{200,1500},{200,1700},{1000,1700},
    		//shift shift A H 123 5 # ! del
    		{300,1800},{100,1500},{300,1800},{500,1900},{800,1800},{100,1700},{800,1600},{900,1500},{800,1900},
    		//ABC A 123 " " , shift + [ …
    		{100,1700},{500,1600},{300,1800},{100,1500},{100,1700},{100,1700},{100,1500},{600,1600}};
     		//shift & ABC A shift shift a h

    //消息处理器，用于处理消息
    //msg通过下文的onMousePoint（）获取信息
    private Handler mHandler = new Handler() {  
        @Override
        //这个线程的处理消息，写进dxyinfo
        //msg.obj存储point信息和click信息
        //除此之外，这个函数将点信息发给tHandler函数，使其改变鼠标坐标
        //并将click发送给判断点击值函数（其实可以进行一次是否点击在选择发不发送，但这里是在判断点击值函数顺便判断是否点击）
        public void handleMessage(Message msg) {
            if (msg.what == RAW_POINT) {  
//            	Point handpoint = (Point) msg.obj;
            	Point handpoint = new Point(((double[])msg.obj)[0],((double[])msg.obj)[1]);
            	int click = (int)((double[])msg.obj)[2];
            	
                String c;            	
            	if (click == 1)
            		c = "true";
            	else
            		c = "false";
            	
            	try {
    				FileOutputStream fos = new FileOutputStream(dxyinfo, true);
    				fos.write((Integer.toString((int)handpoint.x)+","+Integer.toString((int)handpoint.y)+","+c+"\n").getBytes());
    				fos.close();
    			} catch (Exception e) {
    				e.printStackTrace();
    			}

            	if(tHandler != null){
                                                         
                	tHandler.obtainMessage(DRAW_CURSOR_POINT, handpoint).sendToTarget();
                	
                	keytovalue(click);
            	}
            }
        }  
    };

    /**
     * This class will receive a callback once the OpenCV library is loaded.
     */

    //opencv的一个抽象类，，为了支持opencv与app之间的相互作用，
    //该类声明了一个callback，在opencv  manager之后执行，这个回调是为了使opencv在合适的地方进行初始化
    private static final class OpenCVLoaderCallback extends BaseLoaderCallback {
        private Context mContext;

        public OpenCVLoaderCallback(Context context) {
            super(context);
            mContext = context;
        }

        //初始相机视图
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                	
        	       try{
        	    	   System.loadLibrary("HandGestureApp");
        	           //To do - add your static code
        	       }
        	       catch(UnsatisfiedLinkError e) {
        	            Log.v(TAG, "Native code library failed to load.\n" + e);
        	       }         
        	       catch(Exception e) {
        	            Log.v(TAG, "Exception: " + e);
        	       }
        	   
                	//如果callback success，初始化相机视图
                    ((SoftKeyboard) mContext).initCameraView();
        	       //把context传递给sigleton
                    Singleton.getInstance().m_context = (SoftKeyboard) mContext;
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }

    }
    
    //初始opencv相机视图，再次打开，或者新建
    private void initCameraView() {
    	if( mWorker == null){
    		mWorker = new OpenCVWorker(OpenCVWorker.FIRST_CAMERA);
    		mWorker.addResultCallback(this);
    		new Thread(mWorker).start();
    	}else{
    		mWorker.addResultCallback(this);
    		mSurfaceView.setVisibility(View.VISIBLE);
    		drawCanvas = true;
    		mWorker.drawCanvas = true;
    	}
                
    }

    //少数几个完全不清楚作用的函数
    //给 有关鼠标随机移动的参数赋值
    private void setThr_Acc(String setfile){
        //访问指定的文件，逐行读取，以—分隔
    	File sf = new File(Environment.getExternalStorageDirectory(),setfile);
    	BufferedReader reader = null;
    	try {
    		reader = new BufferedReader(new FileReader(sf));
    		String tempString = null;
    		String para[];
			while ((tempString = reader.readLine()) != null) {
				para = tempString.split("_");
				randomacc = Integer.parseInt(para[0]);
				if (randomacc == 0) {
				    //给这些参数赋值
					numerator = Integer.parseInt(para[1]);
					denominator = Integer.parseInt(para[2]);
					threshold = Integer.parseInt(para[3]);
				}
			} 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
    }
    
    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    //该service生成运行时，首先执行该函数，，也就是选择成为默认输入时
    @Override public void onCreate() {
        super.onCreate();
        Log.e("start","testoncreate");
        Log.d("MyService", "MainActivity thread id is " + Thread.currentThread().getId()); 
        Log.d("MyService", "MainActivity process id is " + Process.myPid()); 
        Log.d("MyService", "MainActivity thread id is " + Process.myTid()); 
        
        Log.e("thre",Integer.toString(numerator) + ';' + Integer.toString(denominator) + ';' + Integer.toString(threshold));
        setThr_Acc("setfile.txt");//赋值一些参数
        Log.e("thre",Integer.toString(numerator) + ';' + Integer.toString(denominator) + ';' + Integer.toString(threshold));

        Log.e("lingzhen","onCreate()");
        //生成光标服务，并运行
        cmdTurnCursorServiceOn();
        //初始化输入管理器
        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        //初始化特殊符号数组
        mWordSeparators = getResources().getString(R.string.word_separators);

        //初始视图
    /*    getViewById和getLayoutInflater().inflate得用法比较
             LayoutInflater是什么?
             This class is used to instantiate layout XML file into its corresponding View objects
             这个类是代码形势, 将未载入的xml文件加载成一个相应的view对象
             getviewbyid的功能是用id参数去寻找一个已经加载了的对象上的某个控件!
     */
        newView = getLayoutInflater().inflate(
                R.layout.linear_layout, null);
        newLinearLayout = (FrameLayout)newView.findViewById(R.id.FrameLayout_Layout1);
        m_panel_view = (FrameLayout)newView.findViewById(R.id.FrameLayout_Layout2);
        LinearLayout_text = (LinearLayout)newView.findViewById(R.id.LinearLayout2);
        
      //SCREEN_DIM_WAKE_LOCK
        //电源和mWakeLock初始化
        //这部分在API17就已经被弃用了
        //powermanager+wakelock实现保证屏幕全亮，同时键盘背光也亮。
      	pm = (PowerManager) getSystemService(Context.POWER_SERVICE); 
      	mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");


        //手势监听（点击等）
        //GestureDetector是sdk里的类
	    mGestureDetector = new GestureDetector(new MyOnGestureListener());
        mGestureDetector.setOnDoubleTapListener(this);
        mGestureDetector.setIsLongpressEnabled(false);

        //FPS绘制
        mFpsPaint = new Paint();
        mFpsPaint.setColor(Color.GREEN);
        mFpsPaint.setDither(true);
        mFpsPaint.setFlags(Paint.SUBPIXEL_TEXT_FLAG);
        mFpsPaint.setTextSize(28);
        mFpsPaint.setTypeface(Typeface.SANS_SERIF);

        //获取屏幕尺寸
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager winMgr = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
       	winMgr.getDefaultDisplay().getMetrics(dm); //创建一个新窗口函数
       	
       	double screenheight = dm.heightPixels*0.3;
       	double screenwidth = dm.widthPixels*0.3;
       	double mymargin = dm.widthPixels/2;
       	FrameLayout.LayoutParams layoutParams=  
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins((int)(mymargin), 0, 0, 0); 
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;//放置在surfaceview的左上角

        //初始界面绘图工具
       	mSurfaceView = new SurfaceView(this);

       	mSurfaceView.setLayoutParams(layoutParams);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceView.getHolder().setFixedSize((int)screenwidth, (int)screenheight);

        //绘图处理器
        //getMainLooper()要在主线程中刷新ui
        //传递数据handle
        mSurfaceViewHandler = new Handler(getMainLooper(), new SurfaceViewUiCallback());

        text_x = new TextView(this);
        text_y = new TextView(this);
        text_dx = new TextView(this);
        text_dy = new TextView(this);
        text_d = new TextView(this);
        text_dt = new TextView(this);
    }
    
    
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    //初始化各个键盘，系统按顺序调用之一，也是选择成为默认输入法时调用
    @Override public void onInitializeInterface() {
    	
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        //初始化键盘
        mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
        //lingzhen 
        mKeypad = new LatinKeyboard(this, R.xml.keypad);
        mQwertyKeyboard.setParameter(1);
        mRandomQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
        mQwertyKeyboard.setParameter(2);
        mRandomSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mQwertyKeyboard.setParameter(3);
        mRandomSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
        //lingzhen
    }
    
    // create keys' coordinates information file
    // Sabrina 14-07-10
    //初始不同键盘每个键的位置信息
    public void createKeyfile(){     
    	Log.e("chenqi","createKeyfile() run.");
        DisplayMetrics dm = new DisplayMetrics();
        
        File keyinfo = new File(Environment.getExternalStorageDirectory(),"keyfile.txt");
        Log.e("chenqi",Environment.getExternalStorageDirectory().getAbsolutePath()+'/'+"keyfile.txt");

       	
		try {
			WindowManager winMgr = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
	       	winMgr.getDefaultDisplay().getMetrics(dm);
	       	
	       	int screenheight = dm.heightPixels;
	       	int screenwidth = dm.widthPixels;
	       	int keyheight = (int)(KEY_HEIGHT * dm.density + (float)0.5);// dip --> pixel
	       	int ystart = screenheight - keyheight * ROWNUM;
	       	int xstart = 0;
	        	       	
	       	FileOutputStream writer = new FileOutputStream(keyinfo);
	       	
	       	int i = 0;
	       	int len = keylista.length;
	       	Log.e("length", Integer.toString(len));
	       	double keywidth,gap;
	       	// row 1
			for (; i < 10; i++) {
				keywidth = 0.1 * (double) screenwidth;
                keyinfoa[i][0] = keylista[i];
                keyinfoa[i][1] = Integer.toString((int) (xstart + i * keywidth));
                keyinfoa[i][2] = Integer.toString(ystart);
                keyinfoa[i][3] = Integer.toString((int) (xstart + (1 + i) * keywidth));
                keyinfoa[i][4] = Integer.toString(ystart + keyheight);
	       	}
			//row 2
			for (; i < 19; i++){
				gap = 0.05 * (double) screenwidth;
				keywidth = 0.1 * (double) screenwidth;
                keyinfoa[i][0] = keylista[i];
                keyinfoa[i][1] = Integer.toString((int) (gap + xstart + (i-10) * keywidth));
                keyinfoa[i][2] = Integer.toString(ystart + keyheight);
                keyinfoa[i][3] = Integer.toString((int) (gap + xstart + (i-9) * keywidth));
                keyinfoa[i][4] = Integer.toString(ystart + 2*keyheight);
			}
			//row 3 i=19
			double[] kwa = {0.15 * (double) screenwidth,0.1 * (double) screenwidth,0.1 * (double) screenwidth,
					0.1 * (double) screenwidth,0.1 * (double) screenwidth,0.1 * (double) screenwidth,
					0.1 * (double) screenwidth,0.1 * (double) screenwidth,0.15 * (double) screenwidth};
			for (; i < 28; i++) {
				keywidth = kwa[i - 19];
                keyinfoa[i][0] = keylista[i];
                keyinfoa[i][1] = Integer.toString(xstart);
                keyinfoa[i][2] = Integer.toString(ystart + 2 * keyheight);
                keyinfoa[i][3] = Integer.toString((int) (xstart + keywidth));
                keyinfoa[i][4] = Integer.toString((int) (ystart + 3 * keyheight));
				xstart = (int) (xstart + keywidth);
			}
			//row 4   i=28
			double[] kw = {0.2 * (double) screenwidth,0.15 * (double) screenwidth,0.3 * (double) screenwidth,
					0.15 * (double) screenwidth,0.2 * (double) screenwidth};
			xstart = 0;
			for (; i < len; i++) {
				keywidth = kw[i - 28];
                keyinfoa[i][0] = keylista[i];
                keyinfoa[i][1] = Integer.toString(xstart);
                keyinfoa[i][2] = Integer.toString(ystart + 3 * keyheight);
                keyinfoa[i][3] = Integer.toString((int) (xstart + keywidth));
                keyinfoa[i][4] = Integer.toString((int) (ystart + 4 * keyheight));
				xstart = (int) (xstart + keywidth);
			}

            for(int j=0;j<len;j++){
                for(int m=0;m<row;m++){
                    writer.write((keyinfoa[j][m]).getBytes());
                    if(m<row-1)
                        writer.write(',');
                }
                writer.write('\n');
            }
			writer.write('\n');
			
			i=0;
			len = keylistb.length;
			xstart = 0;
			// row 1
			for (; i < 10; i++) {
				keywidth = 0.1 * (double) screenwidth;
                keyinfob[i][0] = keylistb[i];
                keyinfob[i][1] = Integer.toString((int) (xstart + i * keywidth));
                keyinfob[i][2] = Integer.toString(ystart);
                keyinfob[i][3] = Integer.toString((int) (xstart + (1 + i) * keywidth));
                keyinfob[i][4] = Integer.toString(ystart + keyheight);
			}
			// row 2
			for (; i < 19; i++) {
				gap = 0.05 * (double) screenwidth;
				keywidth = 0.1 * (double) screenwidth;
                keyinfob[i][0] = keylistb[i];
                keyinfob[i][1] = Integer.toString((int) (gap + xstart + (i - 10) * keywidth));
                keyinfob[i][2] = Integer.toString(ystart + keyheight);
                keyinfob[i][3] = Integer.toString((int) (gap + xstart + (i - 9) * keywidth));
                keyinfob[i][4] = Integer.toString(ystart + 2 * keyheight);
			}
			//row 3 i=19
			for (; i < 28; i++) {
				keywidth = kwa[i - 19];
                keyinfob[i][0] = keylistb[i];
                keyinfob[i][1] = Integer.toString(xstart);
                keyinfob[i][2] = Integer.toString(ystart + 2 * keyheight);
                keyinfob[i][3] = Integer.toString((int) (xstart + keywidth));
                keyinfob[i][4] = Integer.toString((int) (ystart + 3 * keyheight));
				xstart = (int) (xstart + keywidth);
			}
			// row 4 i=28
			xstart = 0;
			for (; i < len; i++) {
				keywidth = kw[i - 28];
                keyinfob[i][0] = keylistb[i];
                keyinfob[i][1] = Integer.toString(xstart);
                keyinfob[i][2] = Integer.toString(ystart + 3 * keyheight);
                keyinfob[i][3] = Integer.toString((int) (xstart + keywidth));
                keyinfob[i][4] = Integer.toString((int) (ystart + 4 * keyheight));
				xstart = (int) (xstart + keywidth);
			}

            for(int j=0;j<len;j++){
                for(int m=0;m<row;m++){
                    writer.write((keyinfob[j][m]).getBytes());
                    if(m<row-1)
                        writer.write(',');
                }
                writer.write('\n');
            }
			writer.write('\n');
			
			i = 0;
			len = keylistc.length;
			Log.e("length", Integer.toString(len));  //34
			xstart = 0;
			// row 1
			for (; i < 10; i++) {
				keywidth = 0.1 * (double) screenwidth;
                keyinfoc[i][0] = keylistc[i];
                keyinfoc[i][1] = Integer.toString((int) (xstart + i * keywidth));
                keyinfoc[i][2] = Integer.toString(ystart);
                keyinfoc[i][3] = Integer.toString((int) (xstart + (1 + i) * keywidth));
                keyinfoc[i][4] = Integer.toString(ystart + keyheight);
			}
			// row 2
			for (; i < 20; i++) {
				keywidth = 0.1 * (double) screenwidth;
                keyinfoc[i][0] = keylistc[i];
                keyinfoc[i][1] = Integer.toString((int) (xstart + (i - 10) * keywidth));
                keyinfoc[i][2] = Integer.toString(ystart + keyheight);
                keyinfoc[i][3] = Integer.toString((int) ( xstart + (i - 9) * keywidth));
                keyinfoc[i][4] = Integer.toString(ystart + 2 * keyheight);
			}
			//row 3 i=20
			for (; i < 29; i++) {
				keywidth = kwa[i - 20];
                keyinfoc[i][0] = keylistc[i];
                keyinfoc[i][1] = Integer.toString(xstart);
                keyinfoc[i][2] = Integer.toString(ystart + 2 * keyheight);
                keyinfoc[i][3] = Integer.toString((int) (xstart + keywidth));
                keyinfoc[i][4] = Integer.toString((int) (ystart + 3 * keyheight));
				xstart = (int) (xstart + keywidth);
			}
			// row 4 i=29
			xstart = 0;
			for (; i < len; i++) {
				keywidth = kw[i - 29];
                keyinfoc[i][0] = keylistc[i];
                keyinfoc[i][1] = Integer.toString(xstart);
                keyinfoc[i][2] = Integer.toString(ystart + 3 * keyheight);
                keyinfoc[i][3] = Integer.toString((int) (xstart + keywidth));
                keyinfoc[i][4] = Integer.toString((int) (ystart + 4 * keyheight));
				xstart = (int) (xstart + keywidth);
			}

            for(int j=0;j<len;j++){
                for(int m=0;m<row;m++){
                    writer.write((keyinfoc[j][m]).getBytes());
                    if(m<row-1)
                        writer.write(',');
                }
                writer.write('\n');
            }

			writer.write('\n');

			i = 0;
			len = keylistd.length;
			xstart = 0;
			// row 1
			for (; i < 10; i++) {
				keywidth = 0.1 * (double) screenwidth;
                keyinfod[i][0] = keylistd[i];
                keyinfod[i][1] = Integer.toString((int) (xstart + i * keywidth));
                keyinfod[i][2] = Integer.toString(ystart);
                keyinfod[i][3] = Integer.toString((int) (xstart + (1 + i) * keywidth));
                keyinfod[i][4] = Integer.toString(ystart + keyheight);
			}
			// row 2
			for (; i < 20; i++) {
				keywidth = 0.1 * (double) screenwidth;
                keyinfod[i][0] = keylistd[i];
                keyinfod[i][1] = Integer.toString((int) (xstart + (i - 10) * keywidth));
                keyinfod[i][2] = Integer.toString(ystart + keyheight);
                keyinfod[i][3] = Integer.toString((int) ( xstart + (i - 9) * keywidth));
                keyinfod[i][4] = Integer.toString(ystart + 2 * keyheight);
			}
			//row 3 i=20
			for (; i < 29; i++) {
				keywidth = kwa[i - 20];
                keyinfod[i][0] = keylistd[i];
                keyinfod[i][1] = Integer.toString(xstart);
                keyinfod[i][2] = Integer.toString(ystart + 2 * keyheight);
                keyinfod[i][3] = Integer.toString((int) (xstart + keywidth));
                keyinfod[i][4] = Integer.toString((int) (ystart + 3 * keyheight));
				xstart = (int) (xstart + keywidth);
			}
			// row 4 i=29
			xstart = 0;
			for (; i < len; i++) {
				keywidth = kw[i - 29];
                keyinfod[i][0] = keylistd[i];
                keyinfod[i][1] = Integer.toString(xstart);
                keyinfod[i][2] = Integer.toString(ystart + 3 * keyheight);
                keyinfod[i][3] = Integer.toString((int) (xstart + keywidth));
                keyinfod[i][4] = Integer.toString((int) (ystart + 4 * keyheight));
				xstart = (int) (xstart + keywidth);
			}

			Log.e("length", Integer.toString(len));
            for(int j=0;j<len;j++){
                for(int m=0;m<row;m++){
                    writer.write((keyinfod[j][m]).getBytes());
                    if(m<row-1)
                        writer.write(',');
                }
                writer.write('\n');
            }
            
            writer.write('\n');

			i = 0;
			len = keylistp.length;
			xstart = 0;
			// row 1
			for (; i < 3; i++) {
				keywidth = 0.33 * (double) screenwidth;
				keyinfop[i][0] = keylistp[i];
				keyinfop[i][1] = Integer.toString((int) (xstart + i * keywidth));
				keyinfop[i][2] = Integer.toString(ystart);
				keyinfop[i][3] = Integer.toString((int) (xstart + (1 + i) * keywidth));
				keyinfop[i][4] = Integer.toString(ystart + keyheight);
			}
			// row 2
			for (; i < 6; i++) {
				keywidth = 0.33 * (double) screenwidth;
				keyinfop[i][0] = keylistp[i];
				keyinfop[i][1] = Integer.toString((int) (xstart + (i - 3) * keywidth));
				keyinfop[i][2] = Integer.toString(ystart + keyheight);
				keyinfop[i][3] = Integer.toString((int) (xstart + (i - 2) * keywidth));
				keyinfop[i][4] = Integer.toString(ystart + 2 * keyheight);
			}
			// row 3 i=20
			for (; i < 9; i++) {
				keywidth = 0.33 * (double) screenwidth;
				keyinfop[i][0] = keylistp[i];
				keyinfop[i][1] = Integer.toString((int)(xstart + (i - 6) * keywidth));
				keyinfop[i][2] = Integer.toString(ystart + 2 * keyheight);
				keyinfop[i][3] = Integer.toString((int)(xstart + (i - 5) * keywidth));
				keyinfop[i][4] = Integer.toString((int) (ystart + 3 * keyheight));
			}
			// row 4 i=29
			for (; i < len; i++) {
				keywidth = 0.33 * (double) screenwidth;
				keyinfop[i][0] = keylistp[i];
				keyinfop[i][1] = Integer.toString((int)(xstart + (i - 9) * keywidth));
				keyinfop[i][2] = Integer.toString(ystart + 3 * keyheight);
				keyinfop[i][3] = Integer.toString((int)(xstart + (i - 8) * keywidth));
				keyinfop[i][4] = Integer.toString((int) (ystart + 4 * keyheight));
			}

			Log.e("length", Integer.toString(len));
			for (int j = 0; j < len; j++) {
				for (int m = 0; m < row; m++) {
					writer.write((keyinfop[j][m]).getBytes());
					if (m < row - 1)
						writer.write(',');
				}
				writer.write('\n');
			}
			
			writer.flush(); 
		    writer.close();  
		}
		catch (IOException e){
			Log.e("length","IOException");
			e.printStackTrace();
		}
		catch (Exception e) { //default to a HVGA 320x480 and let's hope for the best
			Log.e("length","Exception");
			e.printStackTrace();
		}    
		
		Log.e("length","createKeyfile() over.");
  	}
    
    ////////////////////////////////////
    //Sabrina    2014-7-18
    //初始键盘的字母和编号
    public void createKeyMap(){
    	keymap.put("q",113);
    	keymap.put("w",119);
    	keymap.put("e",101);
    	keymap.put("r",114);
    	keymap.put("t",116);
    	keymap.put("y",121);
    	keymap.put("u",117);
    	keymap.put("i",105);
    	keymap.put("o",111);
    	keymap.put("p",112);
    	keymap.put("a",97);
    	keymap.put("s",115);
    	keymap.put("d",100);
    	keymap.put("f",102);
    	keymap.put("g",103);
    	keymap.put("h",104);
    	keymap.put("j",106);
    	keymap.put("k",107);
    	keymap.put("l",108);
    	keymap.put("shift",-1);
    	keymap.put("z",122);
    	keymap.put("x",120);
    	keymap.put("c",99);
    	keymap.put("v",118);
    	keymap.put("b",98);
    	keymap.put("n",110);
    	keymap.put("m",109);
    	keymap.put("del",-5);
    	keymap.put("done",-3);
    	keymap.put("123",-2);
    	keymap.put("space",32);
    	keymap.put("point",46);
    	keymap.put("next",10);
    	
    	keymap.put("0", 48);
    	keymap.put("1", 49);
    	keymap.put("2", 50);
    	keymap.put("3", 51);
    	keymap.put("4", 52);
    	keymap.put("5", 53);
    	keymap.put("6", 54);
    	keymap.put("7", 55);
    	keymap.put("8", 56);
    	keymap.put("9", 57);    	
    	keymap.put("@", 64);
    	keymap.put("#", 35);
    	keymap.put("$", 36);
    	keymap.put("%", 37);
    	keymap.put("&", 38);
    	keymap.put("*", 42);
    	keymap.put("-", 45);
    	keymap.put("=", 61);
    	keymap.put("(", 40);
    	keymap.put(")", 41);
    	keymap.put("!", 33);
    	keymap.put("\"", 34);
    	keymap.put("'", 39);
    	keymap.put(":", 58);
    	keymap.put(";", 59);
    	keymap.put("/", 47);
    	keymap.put("?", 63);  
    	keymap.put("ABC",-2);

//    	"~","±","×","÷","•","°","`","´","{","}","©","£","€","^","®","¥","_","+","[","]","shift","¡","&lt;","&gt;","¢","|","\\","¿",
//		"del","done","ABC","space","apostrophe","next"
    	keymap.put("~", 126);
    	keymap.put("±", 177);
    	keymap.put("×", 215);
    	keymap.put("÷", 247);
    	keymap.put("•", 8226);
    	keymap.put("°", 176);
    	keymap.put("`", 96);
    	keymap.put("´", 180);
    	keymap.put("{", 123);
    	keymap.put("}", 125);    	
    	keymap.put("©", 169);
    	keymap.put("£", 163);
    	keymap.put("€", 8364);
    	keymap.put("^", 94);
    	keymap.put("®", 174);
    	keymap.put("¥", 165);
    	keymap.put("_", 95);
    	keymap.put("+", 43);
    	keymap.put("[", 91);
    	keymap.put("]", 93);
    	keymap.put("¡", 161);
    	keymap.put("&lt;", 60);
    	keymap.put("&gt;", 62);
    	keymap.put("¢", 162);
    	keymap.put("|", 124);
    	keymap.put("\\", 92);
    	keymap.put("¿", 191); 
    	
    	keymap.put("comma",44);
    	keymap.put("apostrophe", 8230);
    }
    
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    //初始创建键盘的视图
    //当输入法首次展现时，系统调用onCreateInputView()回调函数
    @Override public View onCreateInputView() {
    	Log.e("lingzhen","onCreateInputView");
    	
        //输入视图
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(mQwertyKeyboard);

		// ///////////////////////
        //创建每个键的信息和创建每个键所对的值
		createKeyfile();
		createKeyMap();
		// ////////////////////////

        try{
        	drawCanvas = false;
        	Log.e("lingzhen", "Hello0");
        	newLinearLayout.removeAllViews();
        	Log.e("lingzhen", "Hello");
        	newLinearLayout.addView(mInputView);
        	Log.e("lingzhen", "Hello1");
        	m_panel_view.addView(mSurfaceView);
        	LinearLayout_text.addView(text_x);
        	LinearLayout_text.addView(text_y);
        	LinearLayout_text.addView(text_dx);
        	LinearLayout_text.addView(text_dy);
        	LinearLayout_text.addView(text_d);
        	LinearLayout_text.addView(text_dt);
        	
        	Log.e("lingzhen", "Hello2");
        	drawCanvas = true;
        	
        }catch(Exception e){
        	Log.e("lingzhen", ""+e.getMessage());
        	e.printStackTrace();
        }
        return (View)newView;
    }
    
    //lingzhen.2014.05.10
    //隐藏键盘请求，主要隐藏鼠标和mwork的绘制
    @Override public void requestHideSelf(int flags) {
        super.requestHideSelf(flags);
        Log.e("lingzhen","requestHideSelf:"+flags);
        if(flags == 0){
        	cmdHideCursor();
            m_bRunning = false;
            mWorker.drawCanvas = false;
            drawCanvas = false;
        }
    }

    //光标服务开启
    private void cmdTurnCursorServiceOn() {	
       	Intent i = new Intent();
        i.setAction("com.seu.SecureFingerMouse.CursorService");
        startService(i);
	}
	//关闭光标服务
	private void cmdTurnCursorServiceOff() {
		Intent i = new Intent();
        i.setAction("com.seu.SecureFingerMouse.CursorService");
        stopService(i);
	}
    //展示光标
	private void cmdShowCursor() {
		if (Singleton.getInstance().m_CurService != null)
			Singleton.getInstance().m_CurService.ShowCursor(true);
	}
	//隐藏光标
	private void cmdHideCursor() {
		if (Singleton.getInstance().m_CurService != null)
			Singleton.getInstance().m_CurService.ShowCursor(false);
	}
    
    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    //初始化候选区域视图，本程序无候选区
    @Override public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        return mCandidateView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    //系统开始调用的函数之一，IMF框架
    //每次打开新界面自动调用
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        //初始化候选区，暂无用
        mComposing.setLength(0);
        updateCandidates();
        setCandidatesViewShown(false);

        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }

        //候选区有关
        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;

        boolean random_flag = false;
        
        
        // 现在我们将根据正在编辑的文本类型初始化我们的状态。
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                // 数字和日期默认为符号键盘，没有额外的特征。
            	mCurKeyboard = mSymbolsKeyboard;
            	int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD || 
                		variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                    mCurKeyboard = mSymbolsKeyboard;
                    //lingzhen
                    //初始化mRandom
                    if (random_flag){
	                    mRandomSymbolsKeyboard.setParameter(2);
	    	    		mRandomSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
	    	    		mCurKeyboard = mRandomSymbolsKeyboard;
	    	    		mRandomQwertyKeyboard.setParameter(1);
        	    		mRandomQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
	    	    		mRandomSymbolsShiftedKeyboard.setParameter(3);
	    	            mRandomSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
                    }
                }
                break;
                
            case InputType.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
//                mCurKeyboard = mSymbolsKeyboard;
            	mCurKeyboard = mKeypad;
                break;
                
            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mQwertyKeyboard;
                mPredictionOn = true;
                
                // We now look for a few special variations of text that will
                // modify our behavior.
                variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                    mCurKeyboard = mSymbolsKeyboard;
                    if (random_flag){
        	    		mRandomQwertyKeyboard.setParameter(1);
        	    		mRandomQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
        	    		mCurKeyboard = mRandomQwertyKeyboard;
        	    		mRandomSymbolsKeyboard.setParameter(2);
			        	mRandomSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
			        	mRandomSymbolsShiftedKeyboard.setParameter(3);
	    	            mRandomSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
                    }
                    //lingzhen                    
                }
                
                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }
                
                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }
                
                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;
            
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
                updateShiftKeyState(attribute);
        }
        
        // Update the label on the enter key, depending on what the application
        // says it will do.
        //lingzhen
        attribute.imeOptions |= EditorInfo.IME_FLAG_NO_FULLSCREEN;
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
        
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    //结束输出后系统默认调用
    //清空关闭候选区、电源锁等
    //关闭视图，初始键盘格式（恢复成小写字母键盘）
    //每次打开一个新界面，自动调用
    @Override public void onFinishInput() {
        super.onFinishInput();
        cmdHideCursor();
        Log.e("lingzhen", "onFinishInput()");
        if (mWorker != null)
            mWorker.mIsEnabled = false;
        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates();
        
        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);
        if (mWakeLock != null){
        	Log.v("lingzhen", "Releasing mWakeLock");
        	try {
            	mWakeLock.release();          		
        	}catch (Throwable th) {
        		
        	}
        } else {
        	Log.e("lingzhen", "mWakeLock reference is null");
        }

        
        mCurKeyboard = mQwertyKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }

    //关闭输入法，系统默认调用
    @Override public void onDestroy() {
    	super.onDestroy();
    	Log.e("lingzhen", "onDestroy()");
        cmdHideCursor();
//		m_bRunning = false;
		// close our cursor service
		cmdTurnCursorServiceOff();
		mWorker.drawCanvas = false;
		drawCanvas = false;
		mSurfaceView.setVisibility(SurfaceView.GONE);
		mWorker.stopProcessing();
        mWorker.removeResultCallback(this);
    }

    //系统初始化默认调用函数之一，在onStartInput（）之后并且确认为输入模式后调用，开始输入时最开始调用
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        if(inital == 0){
        	SystemClock.sleep(350);
        }
        if (mWorker != null)
              mWorker.mIsEnabled = true;
        
        mWakeLock.acquire(); 
        
		// ///////////////////////
		// 2014-07-11 Sabrina

		inputstate = true;


		Log.e("StartInputView", "onStartInputView()");
        
     // get screen size
        DisplayMetrics metrics = new DisplayMetrics();
		try {
			WindowManager winMgr = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
	       	winMgr.getDefaultDisplay().getMetrics(metrics);
	       	m_nScreenW = winMgr.getDefaultDisplay().getWidth();
	       	m_nScreenH = winMgr.getDefaultDisplay().getHeight();
	       	Log.e("lingzhen", Integer.toString(m_nScreenW)+" "+Integer.toString(m_nScreenH)
	       			+" "+Integer.toString(metrics.heightPixels)+" "+Integer.toString(metrics.widthPixels));
	       	Log.e("lingzhen",Integer.toString(DisplayMetrics.DENSITY_DEFAULT)+" "+Float.toString(metrics.densityDpi)+" "+Float.toString(metrics.ydpi));
		}
		catch (Exception e) { //default to a HVGA 320x480 and let's hope for the best
			e.printStackTrace();
			m_nScreenW = 0;
			m_nScreenH = 0;
		} 
        
        Log.e("lingzhen", "onStartInputView: m_nScreenW:"+m_nScreenW+" m_nScreenW:"+m_nScreenH);
        
        //start SurfaceView
        mSurfaceHolder.addCallback(this);
        mSurfaceView.setOnTouchListener(this);                 
        
        // Apply the selected keyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
        Log.e("lingzhen123", "Hello "+Integer.toString(mInputView.getWidth())+" "+Integer.toString(mInputView.getHeight()));
        mInputView.closing();
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        mInputView.setSubtypeOnSpaceKey(subtype);

        if(OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, new OpenCVLoaderCallback(this))){
        	Log.i(TAG, "Loaded OpenCV");
        }else{
        	Log.i(TAG, "Couldn't load OpenCV");
        }
        
        if(Singleton.getInstance().m_CurService != null){
	        cmdShowCursor();
	        m_bRunning = true;
			// start a thread to move cursor arround
	        if(inital == 0){
	        	inital = inital + 1;
                //鼠标坐标线程
	        	Thread t = new Thread() {

	        		int cursor_x = 0, cursor_y = 0;
	        		int dx = 0, dy = 0, px = 0, py = 0;
	        		float xremainder = 0, yremainder = 0;
	        		int ini = 0;
	        		
	        		public void run(){
	        			Looper.prepare();
		        		Log.d("MyService", "Accelerating thread id is " + Thread.currentThread().getId()); 
		        		Log.d("MyService", "Accelerating process id is " + Process.myPid());
		        		Log.d("MyService", "Accelerating thread id is " + Process.myTid());

                        //该处理器主要处理上个处理器mHandler发来的消息
                        //该处理器的作用是更新或者定义鼠标的坐标，根据inti来确定,inti初始为0
	        			tHandler = new Handler() {
	                        public void handleMessage(Message msg) {

	                        	if (msg.what == DRAW_CURSOR_POINT) {
                                    
	                                Point resultMouseDelta = (Point) msg.obj;
									
									if (ini != 0) {
			                    		
			                    		if (randomacc == 1){
			                    			Random rand = new Random();
			                				numerator = acc[rand.nextInt(6)];
			                				denominator = 2;
			                				threshold = thre[rand.nextInt(6)];
			                    		}

										dx = (int)resultMouseDelta.x;
										dy = (int)resultMouseDelta.y;
										if (threshold != 0) {
											if ((Math.abs(dx) + Math.abs(dy)) >= threshold) {
												xremainder = ((float) dx * (float) numerator) / (float) (denominator) + xremainder;
												px = (int) xremainder;
												xremainder = xremainder - (float) (px);

												yremainder = ((float) dy * (float) numerator) / (float) (denominator) + yremainder;
												py = (int) yremainder;
												yremainder = yremainder - (float) (py);

												cursor_x = cursor_x + px;
												cursor_y = cursor_y + py;
											} 
											else {
												cursor_x = cursor_x + dx;
												cursor_y = cursor_y + dy;
											}
										}

									} else {

										init = Core.getTickCount();
										ini = ini + 1;
										getPoint = new Point(m_nScreenW/2,m_nScreenH - 0.5*(double)mInputView.getHeight()); 
										cursor_x = (int)getPoint.x;
										cursor_y = (int)getPoint.y;
									}			

		                    		//limit the scope of the cursor

		                    		if(cursor_x >= m_nScreenW-20)
		                    			cursor_x = m_nScreenW-20;
		                    		else if(cursor_x <= 0)
		                    			cursor_x = 0;
		                    		
		                    		if(cursor_y >= m_nScreenH-20)
		                    			cursor_y = m_nScreenH-20;
		                    		else if(cursor_y <= (m_nScreenH-mInputView.getHeight()))
		                    			cursor_y = m_nScreenH-mInputView.getHeight()-1;

		                    		getPoint = new Point(cursor_x,cursor_y);

		                    		Log.e("sumocf", Integer.toString(m_nScreenW)+","+Integer.toString(m_nScreenH)+","+Integer.toString(m_nScreenH-mInputView.getHeight()));
		                    		Log.e("sumo", Integer.toString((int)cursor_x)+","+Integer.toString((int)cursor_y));
		                    		Log.e("sumo", Integer.toString((int)getPoint.x)+","+Integer.toString((int)getPoint.y));
		                    				                			
		                    		Singleton.getInstance().m_CurService.Update((int)getPoint.x, (int)getPoint.y, true);
		                    				                    		
									try {
										Thread.sleep(5);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}

                            }
	                        	
	                        }
	        			};
	        	        Looper.loop();
	        		}
	        	};
	        	t.start();
	        }
        }
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        mInputView.setSubtypeOnSpaceKey(subtype);
        Log.i("lingzhen", "onCurrentInputMethodSubtypeChanged()");
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    //当应用报告新的文本选择区域时回调该函数，无论输入法是否请求更新extracted text，onUpdateSelection都会被调用，自动调用函数
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {

        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            Log.e("lingzhen","lingzhen onUpdateSelection");
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }
    
    //lingzhen start
    @Override public void onExtractedCursorMovement(int dx, int dy) {
        super.onExtractedCursorMovement(dx, dy);
    }

    @Override public void onExtractedSelectionChanged(int start, int end) {
        super.onExtractedSelectionChanged(start, end);
    }
    @Override public void onUpdateExtractedText(int token, ExtractedText text) {
    	text.selectionStart = text.selectionEnd;
    	super.onUpdateExtractedText(token, text);
    }
    
    @Override public void onUpdateExtractingViews(EditorInfo ei) {
    	super.onUpdateExtractingViews(ei);
    }
    
    @Override public void onExtractedTextClicked() {
    	super.onExtractedTextClicked();
    }
    
    @Override public void onExtractingInputChanged(EditorInfo ei) {
    	super.onExtractingInputChanged(ei);
    }
    //lingzhen end
    
    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    //展示候选栏，很多时候自动调用
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }
            
            List<String> stringList = new ArrayList<String>();
            for (int i = 0; i < completions.length; i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }
    
    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    //用于下两个函数，与候选区有关
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
    	Log.e("chq","translateKeyDown() run.");
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }


        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }
        
        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() -1 );
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length()-1);
            }
        }
        
        onKey(c, null);
        
        return true;
    }
    
    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    //onkeydown 事件会在用户按下一个键盘按键时发生
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    	Log.e("chq","onKeyDown() run.");
        switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			// /////////////////////////
			// 2014-07-11 Sabrina
			inputstate = false;
			// The InputMethodService already takes care of the back
			// key for us, to dismiss the input method if it is shown.
			// However, our keyboard could be showing a pop-up window
			// that back should dismiss, so we first allow it to do that.
			if (event.getRepeatCount() == 0 && mInputView != null) {
				if (mInputView.handleBack()) {
					// lingzhen.2014.05.10
					Log.e("lingzhen", "onKeyDown KeyEvent.KEYCODE_BACK");
					cmdHideCursor();
					return true;
				}
			}
			break;

		case KeyEvent.KEYCODE_DEL:
			// Special handling of the delete key: if we currently are
			// composing text for the user, we want to modify that instead
			// of let the application to the delete itself.
			if (mComposing.length() > 0) {
				onKey(Keyboard.KEYCODE_DELETE, null);
				return true;
			}
			break;

		case KeyEvent.KEYCODE_ENTER:
			Log.e("cfws","KeyEvent.KEYCODE_ENTER go!" + Integer.toString(keyCode));
			// Let the underlying text editor always handle these.
			return false;

		default:
			// For all other keys, if we want to do transformations on
			// text being entered with a hard keyboard, we need to process
			// it and do the appropriate action.
			if (PROCESS_HARD_KEYS) {
				if (keyCode == KeyEvent.KEYCODE_SPACE
						&& (event.getMetaState() & KeyEvent.META_ALT_ON) != 0) {
					// A silly example: in our input method, Alt+Space
					// is a shortcut for 'android' in lower case.
					InputConnection ic = getCurrentInputConnection();
					if (ic != null) {
						// First, tell the editor that it is no longer in the
						// shift state, since we are consuming this.
						ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
						keyDownUp(KeyEvent.KEYCODE_A);
						keyDownUp(KeyEvent.KEYCODE_N);
						keyDownUp(KeyEvent.KEYCODE_D);
						keyDownUp(KeyEvent.KEYCODE_R);
						keyDownUp(KeyEvent.KEYCODE_O);
						keyDownUp(KeyEvent.KEYCODE_I);
						keyDownUp(KeyEvent.KEYCODE_D);
						// And we consume this event.
						return true;
					}
				}
				if (mPredictionOn && translateKeyDown(keyCode, event)) {
					return true;
				}
			}
		}

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    //onkeydown 事件会在用户按下一个键盘按键时发生
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
    	Log.e("chq","onKeyUp() run.");
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }
        
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    //inputConnection.commitText（）输入候选区内容
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, 1);
            mComposing.setLength(0);
            updateCandidates();
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    //更新shift键的状态
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
        }
    }
    
    /**
     * Helper to determine if a given character code is alphabetic.
     */
    //判断是不是字母
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    //换行、删除等一系列键的实现
    private void keyDownUp(int keyEventCode) {
    	Log.e("cfws","keyDownUp() run.");
        //长按设置
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        //双击设置
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }
    
    /**
     * Helper to send a character to the editor as raw key events.
     */
    //确定具体的特殊符号，并根据不同的符号执行不同的操作
    private void sendKey(int keyCode) {
    	Log.e("chq","sendKey() run.");
        switch (keyCode) {
            //如果是换行符，则KeyEvent为KEYCODE_ENTER
            case '\n':
            	Log.e("cfws","sendKey '\\n' go!" + Integer.toString(keyCode));
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                //如果是数字（实际上数字键不会进入）
                //如果不是，其余特殊符号则直接输出
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    //////////////////////////////////
    //Sabrina 2013-07-18
    //获取目前点击位置——>给我键盘型号，键盘键的个数，点坐标
    public String transferkeycoordinate(String[][] keyinfo, int n, Point lastpoint){
    	Log.e("chqiqi",Double.toString(lastpoint.x)+","+Double.toString(lastpoint.y));
    	if(lastpoint.x < 0 || lastpoint.y < (m_nScreenH - mInputView.getHeight()) || lastpoint.x > m_nScreenW || lastpoint.y > m_nScreenH )
    		return null;
    	for(int i=0; i<n; i++)
    	{
    		if (lastpoint.x > Integer.parseInt(keyinfo[i][1]) && lastpoint.x < Integer.parseInt(keyinfo[i][3])
    				&& lastpoint.y > Integer.parseInt(keyinfo[i][2]) && lastpoint.y < Integer.parseInt(keyinfo[i][4])){
    			Log.e("chqiq",Integer.parseInt(keyinfo[i][1])+","+Integer.parseInt(keyinfo[i][3])+","+Integer.parseInt(keyinfo[i][2])+","+Integer.parseInt(keyinfo[i][4]));
    			Log.e("chqiq",keyinfo[i][0]);
    			return keyinfo[i][0];
    		}
    	}
        return null;
    }

    //获取到读的键位，返回值该建所对应的数字值
    public int getKeycode(String s){
    	if(s == null)
    		return KeyEvent.KEYCODE_UNKNOWN;
    	return keymap.get(s);
    }

    // Implementation of KeyboardViewListener

    //onKey重写能判断输入的键值，根据不同的值有不同的操作
	public void onKey(int primaryCode, int[] keyCodes) {
		Log.e("chq", "onKey() run.");
		Log.e("chq", Integer.toString(primaryCode));
		Log.e("chqi", String.valueOf((char) primaryCode));

        //判断是否是特殊字符
		if (isWordSeparator(primaryCode)) {
			// Handle separator
            //是特殊字符，那么如果候选区存在内容则输出
			if (mComposing.length() > 0) {
				commitTyped(getCurrentInputConnection());
			}
            //输出特殊字符函数
			sendKey(primaryCode);
            //后台查看输出
			int f = (getCurrentInputEditorInfo().inputType & InputType.TYPE_MASK_VARIATION);
			Log.e("cfws","sendKey go!" + Integer.toString(f));
			//更新shift键
			updateShiftKeyState(getCurrentInputEditorInfo());
		} else if (primaryCode == Keyboard.KEYCODE_DELETE) {
			handleBackspace();
		} else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
			handleShift();
		} else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
			handleClose();
			return;
		} else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
			// Show a menu or somethin'
		} else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
				&& mInputView != null) {
			// 这个模块是用来更换键盘的
			EditorInfo ei = getCurrentInputEditorInfo();
			Keyboard current = mInputView.getKeyboard();
			boolean random_flag = false;
			int variation = ei.inputType & InputType.TYPE_MASK_VARIATION;
			if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
					|| variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
					|| variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
				// Do not display predictions / what the user is typing
				// when they are entering a password.
				mPredictionOn = false;

				if (random_flag) {
					if (current == mRandomSymbolsKeyboard
							|| current == mRandomSymbolsShiftedKeyboard) {
						current = mRandomQwertyKeyboard;
					} else {

						current = mRandomSymbolsKeyboard;
					}
				} else {
					if (current == mSymbolsKeyboard
							|| current == mSymbolsShiftedKeyboard) {
						current = mQwertyKeyboard;
					} else {
						current = mSymbolsKeyboard;
					}
				}
				mInputView.setKeyboard(current);
				if (current == mRandomSymbolsKeyboard) {
					current.setShifted(false);
				}
				// lingzhen
			} else {
				if (current == mSymbolsKeyboard
						|| current == mSymbolsShiftedKeyboard) {
					current = mQwertyKeyboard;
				} else {
					current = mSymbolsKeyboard;
				}
				mInputView.setKeyboard(current);
				if (current == mSymbolsKeyboard) {
					current.setShifted(false);
				}
			}
			
		} else if (primaryCode == LatinKeyboard.KEYCODE_RND) {
            // 并无此按钮，进不来此模块
			EditorInfo ei = getCurrentInputEditorInfo();
			Keyboard current1 = mInputView.getKeyboard();
			int variation = ei.inputType & InputType.TYPE_MASK_VARIATION;
			if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
					|| variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
					|| variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
				mPredictionOn = false;
				mQwertyKeyboard.setParameter(1);
				mRandomQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
				current1 = mRandomQwertyKeyboard;
				mRandomSymbolsKeyboard.setParameter(2);
				mRandomSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
				mRandomSymbolsShiftedKeyboard.setParameter(3);
				mRandomSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
			}
			
			mInputView.setKeyboard(current1);
		} else {
			Log.e("cfws","handleCharacter go!" + Integer.toString(primaryCode));
			handleCharacter(primaryCode, keyCodes);
		}
	}

    //把一个CharSequence类型的字符串提交给文本域，并设置新的光标位置。非函数调用
    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    //更新候选框
    private void updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(mComposing.toString());
                setSuggestions(list, true, true);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }

    //逐步设更新输入位置的字符
    public void setSuggestions(List<String> suggestions, boolean completions,
            boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
    }

    //处理退格键
    private void handleBackspace() {
    	//Log.e("lingzhen", "lingzhen handleBackspace");
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    //解决按下shift键
    private void handleShift() {
        if (mInputView == null) {
            return;
        }
        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertyKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            mInputView.setKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        } else if (mRandomQwertyKeyboard == currentKeyboard){ //lingzhen
        	checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
        } else if (currentKeyboard == mRandomSymbolsKeyboard){ //lingzhen
        	mRandomSymbolsKeyboard.setShifted(true);
            mInputView.setKeyboard(mRandomSymbolsShiftedKeyboard);
            mRandomSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mRandomSymbolsShiftedKeyboard) { //lingzhen
            mRandomSymbolsShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mRandomSymbolsKeyboard);
            mRandomSymbolsKeyboard.setShifted(false);
        }
    }

    //打印字符或数字
    private void handleCharacter(int primaryCode, int[] keyCodes) {
    	Log.e("chq","handleCharacter() run.");
        if (isInputViewShown()) {
            //判断是否按下shift
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        //这个判断是基于候选框的
        //mPredictionOn暂时未知，但根据全局赋值情况来说，基本不会进入
        if (isAlphabet(primaryCode) && mPredictionOn) {
            mComposing.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateShiftKeyState(getCurrentInputEditorInfo());
            updateCandidates();
        } else {
            //根据候选框，因为无候选框，只会进入else
        	Log.e("cfws","handleCharacter else go!" + Integer.toString(primaryCode));
        	if(mComposing.length() > 0){ //lingzhen
        		mComposing.append((char) primaryCode);
        		getCurrentInputConnection().setComposingText(mComposing, 1);
        	}else{ //lingzhen
        		getCurrentInputConnection().commitText(
                    String.valueOf((char) primaryCode), 1);
        	}
        }
    }

    //隐藏键盘操作
    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();//关闭视图
    }

    //检查大写
    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }

    //配合下个函数检查输入的是不是特殊字符
    private String getWordSeparators() {
        return mWordSeparators;
    }
    
    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char)code));
    }

    //与下面两个函数解决 DefaultCandidate 问题
    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }

    //在其他类中也有调用
    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {
            // If we were generating candidate suggestions for the current
            // text, we would commit one of them here.  But for this sample,
            // we will just commit the current text.
            commitTyped(getCurrentInputConnection());
        }
    }
    //在上述一个方法中调用，似乎是候选区满了调用
    public void swipeRight() {
        if (mCompletionOn) {
            pickDefaultCandidate();
        }
    }
    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }
    
    public void onPress(int primaryCode) {
    }
    
    public void onRelease(int primaryCode) {
    }

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		// TODO Auto-generated method stub
    	
        return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		// TODO Auto-generated method stub
		mWorker.drawCanvas = false;
		drawCanvas = false;
		mSurfaceView.setVisibility(View.GONE);
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
    //触摸事件
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		return mGestureDetector.onTouchEvent(event);
	}

    //下面几个serface初始化，系统自动调用
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
	}

    //系统自动调用，在初始化之后
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		mSurfaceSize = new RectF(0, 0, width, height);
	}

	@Override
    //隐藏界面时自动调用，但不关闭mWork
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mWorker.drawCanvas = false;
		drawCanvas = false;
	}

	@Override
    //送message，通过OpenCV类，绘制有关消息
	public void onResultMatrixReady(Bitmap resultBitmap) {
		// TODO Auto-generated method stub		
		if(drawCanvas == true){
			mSurfaceViewHandler.obtainMessage(DRAW_RESULT_BITMAP, resultBitmap).sendToTarget();
		}
	}

    //首先这个函数是用来判断是否进行了点击，未点击则函数结束
    //如果点击，那么在判断目前的键盘种类是哪一种
    //然后通过key()函数，获取目前按入的字母
    //然后再调用onKey(getKeycode(key), null)函数，这两个函数的作用请见这两个函数的注释
	public void keytovalue(int click){

        //mInputView是键盘视图，通过其为重写的getKeyboard可获得键盘信息
        //目标是为了获取LatinKeyboard，由于get函数是返回Keyboard（LatinKeyboard的父类），所以这里进行了一步强制转换
		Keyboard currentKeyboard = mInputView.getKeyboard();
		mCurKeyboard = (LatinKeyboard) currentKeyboard;

        //获取目前的输入框
		InputConnection ic = getCurrentInputConnection();
		String key;
		if (ic != null) {
			if (click == 1) {
				int len = 0;
				if (currentKeyboard == mQwertyKeyboard) {
					subkeyinfo = keyinfoa;
					len = keylista.length;
				} else if (currentKeyboard == mSymbolsKeyboard) {
					subkeyinfo = keyinfoc;
					len = keylistc.length;
				} else if (currentKeyboard == mSymbolsShiftedKeyboard) {
					subkeyinfo = keyinfod;
					len = keylistd.length;
				}else if (currentKeyboard == mKeypad) {
					subkeyinfo = keyinfop;
					len = keylistp.length;
				}

                //subkeyinfo是对应的键盘信息，len是改键盘键的个数，pint是目前点的信息
				key = transferkeycoordinate(subkeyinfo, len, getPoint);
				onKey(getKeycode(key), null);
				
			}
		}
			
	}

	@Override
    //用在OpenCVwork里面，currenttime没有用上，主要传送handpoint和click数据
    //该函数将获取到的point信息和click信息存到result中
    //通过obtainMessage(RAW_POINT, result).sendToTarget()将msg传给自己
    //该函数在mWork中一直在执行，所以次函数持续发送消息给mHandler
	public void onMousePoint(Point handpoint, int click, long currenttime) {
		// TODO Auto-generated method stub
		if(mHandler != null){
			double result[] = {handpoint.x,handpoint.y,click};
			mHandler.obtainMessage(RAW_POINT, result).sendToTarget();

		}
	}
	
	@Override
    //展示fps
	public void onFpsUpdate(double fps) {
		// TODO Auto-generated method stub
		mFpsResult = fps;
	}
	
	 /**
     * This Handler callback is used to draw a bitmap to our SurfaceView.
     */
    private class SurfaceViewUiCallback implements Handler.Callback {
        @Override
        //接收绘图消息的handler
        public boolean handleMessage(Message message) {
            if (message.what == DRAW_RESULT_BITMAP) {
                Bitmap resultBitmap = (Bitmap) message.obj;
                Canvas canvas = null;
                try {
                    canvas = mSurfaceHolder.lockCanvas();

                    canvas.drawBitmap(resultBitmap, null, mSurfaceSize, null);
                    canvas.drawText(String.format("FPS: %.2f", mFpsResult), 35, 45, mFpsPaint);
                }catch (Exception e) { //default to a HVGA 320x480 and let's hope for the best
        			Log.e("chqiq","Exception");
        			e.printStackTrace();
                } finally {
                    if (canvas != null) {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                    // Tell the worker that the bitmap is ready to be reused
                    mWorker.releaseResultBitmap(resultBitmap);
                }
            }
            return true;
        }
    }
	
    private class MyOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }
    }
}
