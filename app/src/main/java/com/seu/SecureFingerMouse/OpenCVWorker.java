/*
 * Copyright (c) 2010, Sony Ericsson Mobile Communication AB. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 *    * Redistributions of source code must retain the above copyright notice, this 
 *      list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 *    * Neither the name of the Sony Ericsson Mobile Communication AB nor the names
 *      of its contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.seu.SecureFingerMouse; //打包手指输入法

import org.opencv.android.Utils;  //引入opencv
import org.opencv.core.*;//opencv最基础的库。包含exception，point，rect，size，slice，vector，matrix，image等数据结构，和相应的操作函数，以及一些基础算法。
import org.opencv.highgui.Highgui;//HighHGUI只是用来建立快速软件原形或是试验用的。它的设计意图是为用户提供简单易用的图形用户接口。
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;//图像处理组件
import org.opencv.objdetect.CascadeClassifier;

import android.content.Context;//Context提供了关于应用环境全局信息的接口。它允许获取以应用为特征的资源和类型。同时启动应用级的操作，如启动Activity，broadcasting和接收intents。
import android.graphics.Bitmap;//Bitmap是Android系统中的图像处理的最重要类之一。用它可以获取图像文件信息，进行图像剪切、旋转、缩放等操作，并可以指定格式保存图像文件。

import java.math.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;//一个基于链接节点的无界线程安全队列。

import android.os.Environment;//提供访问环境变量.
import android.os.Handler;//在Android中提供了一种异步回调机制Handler,使用它，我们可以在完成一个很长时间的任务后做出相应的通知。
import android.os.Looper;//Handler:作用就是发送与处理信息,如果希望Handler正常工作,在当前线程中要有一个Looper对象
//Looper:每个线程只能够有一个Looper,管理MessageQueue,不断地从中取出Message分发给对应的Handler处理！
import android.os.Process;//各类组件元素的清单文件条目activity、service、receiver 和 provider均支持 android:process 属性，此属性可以指定该组件应在哪个进程运行。
import android.util.Log;//Android的Java程序通过android.util.Log类来输出Log
/**
 * @author Erik Hellman <erik.hellman@sonymobile.com>
 */
public class OpenCVWorker implements Runnable {//针对opencv的工作类
    public static final String TAG = "OpenCVWorker";



    public static final int FIRST_CAMERA = 0;
    public static final int SECOND_CAMERA = 1;

    public static final int RESULT_MATRIX_BUFFER_SIZE = 3;

    /**
     * Constant used to calculate FPS value (see measureFps())
     */
    public static final int FPS_STEPS = 20;//这里大概是在设置帧数

    // The threshold value for the lower and upper color limits
    public static final double THRESHOLD_LOW = 35;//大概是阈值
    public static final double THRESHOLD_HIGH = 35;

    /**
     * Boolean
     */
    private boolean mDoProcess;//判断是不是当前进程？
//    private int mCameraId = SECOND_CAMERA;
    private int mCameraId = FIRST_CAMERA;//这里是基于单摄像头的，赋予了一个ID
    private Size mPreviewSize;//OpenCv中尺寸的表示--------Size类
    //OpenCv中尺寸Size类与点Point类的表示十分类似,最主要的区别是,Size(尺寸)类的数据成员是width和  
    //height,而Point类的数据成员是坐标点
    private VideoCapture mCamera;//这是一个opencv中的类，作用是读取视频的
    private Set<ResultCallback> mResultCallbacks = Collections.synchronizedSet(new HashSet<ResultCallback>());//方法用于返回一个同步的(线程安全的)有序set由指定的有序set支持。
    private ConcurrentLinkedQueue<Bitmap> mResultBitmaps = new ConcurrentLinkedQueue<Bitmap>();//生成一个bmp的队列

    /**
     * Matrices used to hold the actual image data for each processing step
     */
    private Mat mCurrentFrame;//它可以用于存储实数或复数值的
    private Mat mInRangeResult;//向量和矩阵、灰度或彩色图像、体素、向量场、点云、张量、直方图 （尽管较高维的直方图存储在SparseMat可能更好）。
    private Mat mFilteredFrame;
    private Mat mCurrentFrameHsv;
    
    
	//Mode that presamples hand colors
	public final int SAMPLE_MODE = 0;
	
	//Mode that generates binary image
	public final int DETECTION_MODE = 1; 
	
	//Mode that displays color image together with contours, fingertips,
	//defect points and so on.
	public final int TRAIN_REC_MODE = 2;
	
	//Mode that presamples background colors
	public final int BACKGROUND_MODE = 3;
	
	//Initial mode is BACKGROUND_MODE to presample the colors of the hand
	public int mode = BACKGROUND_MODE;
	private static final int SAMPLE_NUM = 7;//一共7种样本数
	
	private int squareLen;
	private Scalar mColorsRGB[] = null;
	private Point[][] samplePoints = null;
	private double[][] avgColor = null;
	private double[][] avgBackColor = null;
    
	private HandGesture hg = null;
	private Mat rgbaMat = null;
	private Mat interMat = null;
	private Mat rgbMat = null;
	private Mat bgrMat = null;
	private Mat binMat = null;
	private Mat binTmpMat = null;
	private Mat binTmpMat2 = null;
	
	private Mat tmpMat = null;
	
	private Mat[] sampleMats = null;
	
	private double[][] cLower = new double[SAMPLE_NUM][3];
	private double[][] cUpper = new double[SAMPLE_NUM][3];
	private double[][] cBackLower = new double[SAMPLE_NUM][3];
	private double[][] cBackUpper = new double[SAMPLE_NUM][3];
	
	private Scalar lowerBound = new Scalar(0, 0, 0);
	private Scalar upperBound = new Scalar(0, 0, 0);
	
    //Color Space used for hand segmentation
	private static final int COLOR_SPACE = Imgproc.COLOR_RGB2Lab;

    private int mFpsCounter;
    private double mFpsFrequency;
    private long mPrevFrameTime;
    private double mPreviousFps;

    public boolean drawCanvas = true;
    public boolean mIsEnabled = true;
    private int curLabel = 0;
    public Point handpoint = new Point();
//    private Scalar mLowerColorLimit;
//    private Scalar mUpperColorLimit;
    private CascadeClassifier      FingertipDetector;//级联分类类型的指尖检测。
    private File                   mCascadeFile;
	private Mat                    mRgba = null;
    private Mat                    mGray = null;
    private Mat                    mBgr = null;
    private static final Scalar    FingerTip_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    String CascadeFilePath;//绝对路径文件地址
    ////////////////////////
    //Sabrina
//    private static int HIS = 7;
//    private int[] area = new int[HIS];
//    private double[] yvalue = new double[11];
    private double formerarea = 1;
    private double lastarea = 1;
    private double curarea = 1;
    
    private double lastx = 0;
    private double lasty = 0;
    private double curx = 0;
    private double cury = 0;
    
    private boolean lca = false;    

    private boolean needacc = true;

    private double curabsolutex = 0;
    private double formerabsolutex = 0;
    private double lastabsolutex = 0;
    private double curabsolutey = 0;
    private double formerabsolutey = 0;
    private double lastabsolutey = 0;
    
    private long curtime = 0;
    private long fntime = 0;
    
    private boolean flag = false;      
    private int count = 0;
    private int flipnum = 0;
    private int recflipnum = 0;
    private int lastclickflipnum = 0;
    
    private SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
	private String fileName;
    
    //left top point and right bottom point of the Region Of Interest
  	private Point ntl;
  	private Point nbr;
  	  	
  	private double cx=100;//xFayChen：这边两个参数干什么的？
    private double cy=100;
  	private double tlx=0;
  	private double tly=0;
  	private double brx=0;
  	private double bry=0;
  	
  	private final int FRAME=50;
  	
  	private int SECD=0;
          

    public OpenCVWorker(int cameraId) {//初始化这个类
        mCameraId = cameraId;
        // Default preview size
        mPreviewSize = new Size(480, 320);
        fileName = df.format(new Date());
    }

    public void releaseResultBitmap(Bitmap bitmap) {//添加bmp文件
        mResultBitmaps.offer(bitmap);
    }

    public void addResultCallback(ResultCallback resultCallback) {//添加ResultCallback
        mResultCallbacks.add(resultCallback);
    }

    public void removeResultCallback(ResultCallback resultCallback) {//删除一个线程resultcallback
        mResultCallbacks.remove(resultCallback);
    }

    public void stopProcessing() {
        mDoProcess = false;
    }//停止线程

    // Setup the camera
    private void setupCamera() {
    	Log.e("lingzhen","*********");//以后见到的所有Log.e都为红字输出。只是学姐用来检测断点的
        if (mCamera != null) {
//        	Log.e("lingzhen","if (mCamera != null)");
            VideoCapture camera = mCamera;
//            Log.e("lingzhen","if (mCamera != null) 1111");
            mCamera = null; // Make it null before releasing...
//            Log.e("lingzhen","if (mCamera != null) 222222");
            camera.release();//关闭视频文件
//            Log.e("lingzhen","if (mCamera != null) 33333");
        }

//        Log.e("lingzhen","if (mCamera != null) 444444");
//        mCamera = new VideoCapture(mCameraId);
        mCamera = new VideoCapture(0);//建立一个videocapture
        Log.e("lingzhen","************************");
        
        //////////////////////////////////
        //Sabrina 2014-7-17
//        for (int i = 0; i < HIS; i++){
//        	area[i] = 1;
//        	yvalue[i] = 0;
//        }
        
        // Figure out the most appropriate preview size that this camera supports.
        // We always need to do this as each device support different preview sizes for their cameras
        List<Size> previewSizes = mCamera.getSupportedPreviewSizes();//获取摄像头支持的各种分辨率
        
        double expectedWidth = 320,expectedHeight = 240;//期望高和宽
//        Log.e("cq","resolutionlist length:"+previewSizes.size());
//        for (Size previewSize : previewSizes) {
//        	Log.e("cq","width:"+previewSize.width+"height:"+previewSize.height);
////            if (previewSize.area() < largestPreviewSize && previewSize.width == expectedWidth) {
//            if (previewSize.width == expectedWidth && previewSize.height == expectedHeight) {
//                mPreviewSize = previewSize;
//            }
//        }
        mPreviewSize.width = expectedWidth;
        mPreviewSize.height = expectedHeight;
        
        Log.e("lingzhen","width:"+mPreviewSize.width+"height:"+mPreviewSize.height);
//        mPreviewSize = previewSizes.get(previewSizes.size()-2);
//        mPreviewSize = previewSizes.get(0);
//        mPreviewSize = previewSizes.get(previewSizes.size()-2);
        Log.e("cq","resolution:"+mPreviewSize.width+"*"+mPreviewSize.height);
        mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, mPreviewSize.width);
        mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, mPreviewSize.height);//这两句表示取出这个高和宽内的帧数


        try {
            // load cascade file from application resources
            InputStream is = Singleton.getInstance().m_context.getResources().openRawResource(R.raw.cascade_lbp_12);//用一个单例把m_context存储起来，context存储的是安卓端的资源。
            
            File cascadeDir = Singleton.getInstance().m_context.getDir("cascade", Context.MODE_PRIVATE);//获取手机里的特定文件夹
            mCascadeFile = new File(cascadeDir, "cascade_finger_tip.xml");//创建文件夹
            Log.e("cascade:",cascadeDir.getAbsolutePath());
            FileOutputStream os = new FileOutputStream(mCascadeFile);//变成流后写入文件

            byte[] buffer = new byte[4096];//多的话终止。
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            FingertipDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());//从安卓里拿去绝对路径，创建一个指尖检测器，该类是在C++里封装的
            if (FingertipDetector.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                FingertipDetector = null;
            } else{
                Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
                CascadeFilePath = mCascadeFile.getAbsolutePath();
                CreateCascadeClassifier(CascadeFilePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        } 	
    }

    /**
     * Initialize the matrices and the bitmaps we will use to draw the result
     */
    private void initMatrices() {//往主线程队列里添加一个bmp图片
        // Since drawing to screen occurs on a different thread than the processing,
        // we use a queue to handle the bitmaps we will draw to screen
        mResultBitmaps.clear();
        for (int i = 0; i < RESULT_MATRIX_BUFFER_SIZE; i++) {
            Bitmap resultBitmap = Bitmap.createBitmap((int) mPreviewSize.height, (int) mPreviewSize.width,
                    Bitmap.Config.ARGB_8888);
            mResultBitmaps.offer(resultBitmap);
        }
        
        if(mRgba == null){
        	mRgba = new Mat();
        }
        if(mGray == null){
        	mGray = new Mat();
        }
    }
    
//    public void caculatevalue()

    /**
     * The thread used to grab and process frames
     */
    @Override
    public void run() {
        mDoProcess = true;
        Rect previewRect = new Rect(0, 0, (int) mPreviewSize.width, (int) mPreviewSize.height);//显示图像的区域
        double fps;
        mFpsFrequency = Core.getTickFrequency();//返回每秒的计时周期数
        mPrevFrameTime = Core.getTickCount();//它返回从操作系统启动到当前所经的计时周期数

		Log.d("MyService", "OpenCVWorker thread id is " + Thread.currentThread().getId()); 
		Log.d("MyService", "OpenCVWorker process id is " + Process.myPid());
		Log.d("MyService", "OpenCVWorker thread id is " + Process.myTid());
        
        setupCamera();//开启摄像头
        
        double x=0,y=0;
        double areastandard = mPreviewSize.width * mPreviewSize.height;
        double xstandard = mPreviewSize.width;
        double ystandard = mPreviewSize.height;
        int gap = 0;

        int[] test;
        initMatrices();
        
        while (mDoProcess && mCamera != null) {

            if (!mIsEnabled) continue;
        	
            boolean grabbed = mCamera.grab();//获取一帧图片
            Point finalft;
            
            Log.e("lingzhen11","run");
            if (grabbed) {
            	
                flipnum = flipnum + 1;	
                count = count - 1;
                gap = gap + 1;
                boolean lastclick = false;
                
            	mCamera.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGB);//对该帧进行解码。
            	Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGB2BGR); //彩色图像空间转换
            	
            	test = preProcessAndDetection(fileName, mRgba.getNativeObjAddr(), CascadeFilePath);//C++中的一个函数
            	Log.e("lingzhen11","run2222:"+Integer.toString(test[0])+" "+Integer.toString(test[1])+" "+Integer.toString(test[2]));

            	cx = (double)test[1];
				cy = (double)test[2];//指尖坐标
				Log.e("lingzhen11","x:"+Integer.toString((int)cx)+" y:"+Integer.toString((int)cy));
				
				notifyMousePointResult(new Point(cx,cy),test[0],System.currentTimeMillis());//回调函数
				
			    Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_BGR2RGB);//彩色图像空间转换
        		notifyResultCallback(mRgba);//回调函数
        		
                fps = measureFps();
//                File fpsfile = new File(Environment.getExternalStorageDirectory(),"fpsrecord-"+fileName+".txt");
//                try {
//    				FileOutputStream fos = new FileOutputStream(fpsfile, true);
//    				fos.write((Double.toString(fps)+"\n").getBytes());
//    				fos.close();    				
//    			} catch (Exception e) {
//    				e.printStackTrace();
//    			}
                notifyFpsResult(fps);//回调函数
            }
        }

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

    }
        
    public double measureFps() {//测算当前fps
        mFpsCounter++;
        if (mFpsCounter % FPS_STEPS == 0) {
            long time = Core.getTickCount();
            double fps = FPS_STEPS * mFpsFrequency / (time - mPrevFrameTime);
            mPrevFrameTime = time;
            mPreviousFps = fps;
        }
        return mPreviousFps;
    }



    private void notifyFpsResult(double fps) {//回调函数获得fps结果
        for (ResultCallback resultCallback : mResultCallbacks) {
            resultCallback.onFpsUpdate(fps);
        }
    }
    private void notifyMousePointResult(Point handpoint,int click,long curtime){//回调函数
		for (ResultCallback resultCallback : mResultCallbacks) {
	        resultCallback.onMousePoint(handpoint,click,curtime);
	    }
	}
    //lingzhen 08.10
//    private void notifyMousePointResult(Point handpoint,long curtime,boolean click, boolean nacc){
//		for (ResultCallback resultCallback : mResultCallbacks) {
//	        resultCallback.onMousePoint(handpoint,curtime,click,nacc);
//	    }
//	}

    private void notifyResultCallback(Mat result) {
    	if(drawCanvas){
	        Bitmap resultBitmap = mResultBitmaps.poll();
	        if (resultBitmap != null) {
	            Utils.matToBitmap(result, resultBitmap, true);
	            for (ResultCallback resultCallback : mResultCallbacks) {
	                resultCallback.onResultMatrixReady(resultBitmap);
	            }
	        }
    	}
    }

//    public void setSelectedPoint(double x, double y) {
//        mLowerColorLimit = null;
//        mUpperColorLimit = null;
//        mSelectedPoint = new Point(x, y);
//    }
//
//    public void clearSelectedColor() {
//        mLowerColorLimit = null;
//        mUpperColorLimit = null;
//        mSelectedPoint = null;
//    }

    public Size getPreviewSize() {
        return mPreviewSize;
    }

    public interface ResultCallback {
        void onResultMatrixReady(Bitmap mat);

        void onFpsUpdate(double fps);
        
        void onMousePoint(Point handpoint, int click, long currenttime);
//lingzhen 08.10
        //        void onMousePoint(Point handpoint, long currenttime, boolean click, boolean needacc);
    }

    
    public native void preSkinFilter(long imgAddr);//C++函数。
//    public native int[] preProcessAndDetection(long imgAdd);
    //下面两个函数在OpenCVWorker类的内部被调用，没有在其它地方被调用
    public native int[] preProcessAndDetection(String fnt, long imgAddr, String CascadeFilePath);
    public native void CreateCascadeClassifier(String CascadeFilePath);
}