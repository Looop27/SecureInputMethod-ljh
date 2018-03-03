#include "preProcessAndDetection.hpp"//由.hpp文件定义而来
#include <iomanip>
#include <math.h>
#include <stdlib.h>
#include <ctime>
#include <string>
#include <fstream>

#include <vector>
#include <numeric>//最多可存储 38 个数字，所有数字都能够放到小数点的右边。

//#include <system/camera.h>
//#include <camera/Camera.h>
//#include <camera/ICamera.h>
//#include <camera/CameraParameters.h>

using namespace std;
using namespace cv;

CascadeClassifier fingertip_cascade;//是Opencv中做人脸检测的时候的一个级联分类器，现在有两种选择：一是使用老版本的CvHaarClassifierCascade函数，一是使用新版本的CascadeClassifier类。

clock_t starttime;//记录开始时间，后面还有上次时间以及现在时间

int c = 1;
int ac = 1;
Mat firstsrc;//初矩阵
Point finalft(320/2,240/2);//和二維坐標的點一樣
int lastx = finalft.x, lasty = finalft.y;//記錄上一次的x和y
double lastarea;//上次的区域
Point mousedlt;//翻译为鼠标链路终端，但是某种意义上怎么看都是鼠标的点坐标。
clock_t lasttime, curtime;

queue<Point> ftpointq, deltaq;//队列，存放（手指指尖）和（增量）
queue<int> areaq;//队列（区域）
queue<double> velq, lenq;//（vel）和（长度）

int cx = 100;
int cy = 100;
int tlx = 0;
int tly = 0;
int brx = 0;
int bry = 0;
int FRAME = 50;//帧数
int BOUND = 80;

bool lastclkbj = false;
bool clkbj = false;
bool lastend = false;
bool end = false;
bool clkperiod = false;

Mat covarmat;//协方差矩阵
Mat meanmat;
Mat covarinv;
double r;

/*
//---1--- preprocessing the frame
void preSkinFilter(Mat& src) {
	Mat frame = src.clone();

	Mat tmp = Mat::zeros(src.rows,src.cols,CV_8UC3);
	tmp.copyTo(src);

	vector<Mat> bgr_plane(3);

	Mat bchannel;
	Mat gchannel;
	Mat rchannel;

	split(frame, bgr_plane);
	bchannel = bgr_plane[0];
	gchannel = bgr_plane[1];
	rchannel = bgr_plane[2];

//	///bgrthred is the result after checking all the channels separately
//
//	Mat bthred;
//	Mat gthred;
//	Mat rthred;
//	Mat bgrthred;
//
//	threshold(rchannel, rthred, 95, 255, THRESH_BINARY);
//	threshold(gchannel, gthred, 40, 255, THRESH_BINARY);
//	threshold(bchannel, bthred, 20, 255, THRESH_BINARY);
//
//	bitwise_and(rthred, gthred, bgrthred);
//	bitwise_and(bgrthred, bthred, bgrthred);
//
//	/// check the difference between the max and min value;
//	/// threshold result in MaxMinThred;
//	Mat MaxMinThred;
//	Mat maxBGR;
//	Mat minBGR;
//
//	max(rchannel, gchannel, maxBGR);
//	max(bchannel, maxBGR, maxBGR);
//
//	min(rchannel, gchannel, minBGR);
//	min(bchannel, minBGR, minBGR);
//
//	Mat shortMAX;
//	Mat shortMIN;
//
//	////////////////////////
//	minBGR.convertTo(shortMIN, CV_16SC1);
//	maxBGR.convertTo(shortMAX, CV_16SC1);
//
//	Mat max_min_ABSdiff;
//	absdiff(shortMIN, shortMAX, max_min_ABSdiff);
//
//	max_min_ABSdiff.convertTo(max_min_ABSdiff, CV_8UC1);
//
//	threshold(max_min_ABSdiff, MaxMinThred, 15, 255, THRESH_BINARY);
//	///////////////////
//
//	Mat FirstTwo;
//	bitwise_and(MaxMinThred, bgrthred, FirstTwo);

	///check the difference between R,G,B
	/// threshold result in RGBdistance;

	Mat RgbDistance_Thrd;
	Mat Bsigned;
	Mat Gsigned;
	Mat Rsigned;

	Mat Rg_Abs_Dist;
	Mat Rg_Abs_Dist_thred;

	Mat Rg_Dist;
	Mat Rb_Dist;

	Mat Rg_thred;
	Mat Rb_thred;

	bchannel.convertTo(Bsigned, CV_16SC1);
	gchannel.convertTo(Gsigned, CV_16SC1);
	rchannel.convertTo(Rsigned, CV_16SC1);

	absdiff(Rsigned, Gsigned, Rg_Abs_Dist);
	Rg_Abs_Dist.convertTo(Rg_Abs_Dist, CV_8UC1);

	threshold(Rg_Abs_Dist, Rg_Abs_Dist_thred, 15, 255, THRESH_BINARY);

	subtract(Rsigned, Gsigned, Rg_Dist);
	threshold(Rg_Dist, Rg_thred, 0, 255, THRESH_BINARY);
	Rg_thred.convertTo(Rg_thred, CV_8UC1);

	subtract(Rsigned, Bsigned, Rb_Dist);
	threshold(Rb_Dist, Rb_thred, 0, 255, THRESH_BINARY);
	Rb_thred.convertTo(Rb_thred, CV_8UC1);

	bitwise_and(Rg_Abs_Dist_thred, Rg_thred, RgbDistance_Thrd);
	bitwise_and(RgbDistance_Thrd, Rb_thred, RgbDistance_Thrd);

//	Mat RGB_Distance_Hand;
//	frame.copyTo(RGB_Distance_Hand,RgbDistance_Thrd);
//	frame.copyTo(src,RgbDistance_Thrd);

	Mat hsv_img;
	cvtColor(frame, hsv_img, CV_BGR2HSV_FULL);
	Mat tmp1, tmp2, maskhsv, mask2;
	inRange(hsv_img, Scalar(0, 30, 30), Scalar(40, 170, 255), tmp1);
//	inRange(hsv_img, Scalar(0, 0, 0), Scalar(50, 170, 255), tmp1);
//	inRange(hsv_img, Scalar(140, 0, 0), Scalar(180, 170, 255), tmp2);
	inRange(hsv_img, Scalar(156, 30, 30), Scalar(180, 170, 255), tmp2);
//	inRange(hsv_img,Scalar(0,22,0), Scalar(60,175,255),mask2);
	bitwise_or(tmp1, tmp2, maskhsv);

//	Mat YCrCb_IMG;
//	cvtColor(frame,YCrCb_IMG,CV_BGR2YCrCb);
//	Mat maskycc;
//	inRange(YCrCb_IMG,Scalar(20,133,77), Scalar(255,178,127),maskycc);

	Mat tmp3,sumThred;
//	bitwise_and(RgbDistance_Thrd,maskhsv,tmp3);
//	bitwise_and(maskycc,tmp3,sumThred);
	bitwise_and(maskhsv,RgbDistance_Thrd,sumThred);

	frame.copyTo(src,sumThred);

	//  namedWindow("RGB_Distance",CV_WINDOW_NORMAL);
	//  imshow("RGB_Distance",RgbDistance_Thrd);

	/////////////////////////////////////Show the result for the first condition

//	Mat ThreeThred;
//	bitwise_and(FirstTwo, RgbDistance_Thrd, ThreeThred);

//	Mat Handimage;
//	frame.copyTo(Handimage,ThreeThred);
//	frame.copyTo(src,ThreeThred);
//	frame.copyTo(src,RgbDistance_Thrd);

	//////////////calculate for the second condition
//	Mat Fbthred;
//	Mat Fgthred;
//	Mat Frthred;
//	Mat Fbgrthred;
//
//	threshold(rchannel, Frthred, 220, 255, THRESH_BINARY);
//	threshold(gchannel, Fgthred, 210, 255, THRESH_BINARY);
//	threshold(bchannel, Fbthred, 170, 255, THRESH_BINARY);
//
//	bitwise_and(Frthred, Fgthred, Fbgrthred);
//	bitwise_and(Fbgrthred, Fbthred, Fbgrthred);
//
//	Mat FRg_Abs_Dist_thred;
//	threshold(Rg_Abs_Dist, FRg_Abs_Dist_thred, 15, 255, THRESH_BINARY_INV);
//
//	Mat Gb_Dist;
//	Mat Gb_thred;
//	subtract(Gsigned, Bsigned, Gb_Dist);
//	threshold(Gb_Dist, Gb_thred, 0, 255, THRESH_BINARY);
//	Gb_thred.convertTo(Gb_thred, CV_8UC1);
//
//	Mat SecCondThred;
//	bitwise_and(Fbgrthred, FRg_Abs_Dist_thred, SecCondThred);
//	bitwise_and(SecCondThred, Rb_thred, SecCondThred);
//	bitwise_and(SecCondThred, Gb_thred, SecCondThred);
//
////	Mat result;
////	frame.copyTo(result,SecCondThred);
//	frame.copyTo(src,SecCondThred);
//


//	Mat ProcessedB;
//	Mat ProcessedG;
//	Mat ProcessedR;
//
//	bitwise_and(bchannel, ThreeThred, ProcessedB);
//	bitwise_and(gchannel, ThreeThred, ProcessedG);
//	bitwise_and(rchannel, ThreeThred, ProcessedR);
//
//	vector<Mat> channels;
//	channels.push_back(ProcessedB);
//	channels.push_back(ProcessedG);
//	channels.push_back(ProcessedR);
//
//	merge(channels, src);

	//  namedWindow("Hand",CV_WINDOW_NORMAL);
	//  imshow("Hand",src);
}*/


//---1--- preprocessing the frame预处理边框
void preSkinFilter(Mat& src) {//单次皮肤滤波（也就是背景过滤）
	Mat frame = src.clone();//复制矩阵src给frame

	Mat tmp = Mat::zeros(src.rows,src.cols,CV_8UC3);//CV_8UC3---则可以创建----8位无符号的三通道---RGB彩色图像,据我推测，该条语句是为了创建一个临时的矩阵。
	//此处主要还与构建轮廓有关
	tmp.copyTo(src);//复制矩阵src给临时矩阵tmp

	vector<Mat> bgr_plane(3);//由3个mat组成的向量

	Mat bchannel;
	Mat gchannel;
	Mat rchannel;

	split(frame, bgr_plane);
	bchannel = bgr_plane[0];//将r、g、b三种矩阵存进向量
	gchannel = bgr_plane[1];
	rchannel = bgr_plane[2];

	///bgrthred is the result after checking all the channels separately，对所有信道进行分割

	Mat bthred;
	Mat gthred;
	Mat rthred;
	Mat bgrthred;


	threshold(rchannel, rthred, 95, 255, THRESH_BINARY);//二值化,隔离图像上像素的边缘，下面函数将大于95像素的值置为0,小于的置为255  
	threshold(gchannel, gthred, 40, 255, THRESH_BINARY);
	threshold(bchannel, bthred, 20, 255, THRESH_BINARY);

	bitwise_and(rthred, gthred, bgrthred);//对图像（灰度图像或彩色图像均可）每个像素值进行二进制“与”操作,用于提取边界
	bitwise_and(bgrthred, bthred, bgrthred);//对图像（灰度图像或彩色图像均可）每个像素值进行二进制“与”操作，用于提取边界


	/// check the difference between the max and min value;
	/// threshold result in MaxMinThred;
	//  得到原始图像帧后，对其所有像素的 RGB 值进行不同通道的分割，依次扫描，按照公式（3.1）进行过滤，将判断为人体皮肤的像素点保留，其他像素点则置为黑即可。
	Mat MaxMinThred;
	Mat maxBGR;
	Mat minBGR;
    //求每个像素点R，G，B的最大值
	max(rchannel, gchannel, maxBGR);
	max(bchannel, maxBGR, maxBGR);
    //求每个像素点R ,G,B的最小值
	min(rchannel, gchannel, minBGR);
	min(bchannel, minBGR, minBGR);

	Mat shortMAX;
	Mat shortMIN;

	////////////////////////
	//将其转化为16为有符号数，为什么这么做目前不清楚
	minBGR.convertTo(shortMIN, CV_16SC1);
	maxBGR.convertTo(shortMAX, CV_16SC1);

	Mat max_min_ABSdiff;
	absdiff(shortMIN, shortMAX, max_min_ABSdiff);//OpenCV 中计算两个数组差的绝对值的函数
    //将其转化为8位无符号数
	max_min_ABSdiff.convertTo(max_min_ABSdiff, CV_8UC1);
    //设置阈值进行筛选，结果放在MaxMinThred中
	threshold(max_min_ABSdiff, MaxMinThred, 15, 255, THRESH_BINARY);
	///////////////////
	Mat FirstTwo;
	bitwise_and(MaxMinThred, bgrthred, FirstTwo);

	///check the difference between R,G,B
	/// threshold result in RGBdistance;

	Mat RgbDistance_Thrd;
	Mat Bsigned;
	Mat Gsigned;
	Mat Rsigned;

	Mat Rg_Abs_Dist;
	Mat Rg_Abs_Dist_thred;

	Mat Rg_Dist;
	Mat Rb_Dist;

	Mat Rg_thred;
	Mat Rb_thred;
    //数据转换
	bchannel.convertTo(Bsigned, CV_16SC1);
	gchannel.convertTo(Gsigned, CV_16SC1);
	rchannel.convertTo(Rsigned, CV_16SC1);
    //把R通道减去G通道
	absdiff(Rsigned, Gsigned, Rg_Abs_Dist);

	Rg_Abs_Dist.convertTo(Rg_Abs_Dist, CV_8UC1);

	threshold(Rg_Abs_Dist, Rg_Abs_Dist_thred, 15, 255, THRESH_BINARY);
    //以R>G的条件筛选
	subtract(Rsigned, Gsigned, Rg_Dist);
	threshold(Rg_Dist, Rg_thred, 0, 255, THRESH_BINARY);
	Rg_thred.convertTo(Rg_thred, CV_8UC1);
    //以R>B的条件去筛选
	subtract(Rsigned, Bsigned, Rb_Dist);
	threshold(Rb_Dist, Rb_thred, 0, 255, THRESH_BINARY);
	Rb_thred.convertTo(Rb_thred, CV_8UC1);

	bitwise_and(Rg_Abs_Dist_thred, Rg_thred, RgbDistance_Thrd);
	bitwise_and(RgbDistance_Thrd, Rb_thred, RgbDistance_Thrd);

	Mat sumThred;
//	bitwise_and(RgbDistance_Thrd,maskhsv,tmp3);
//	bitwise_and(maskycc,tmp3,sumThred);
	bitwise_and(FirstTwo,RgbDistance_Thrd,sumThred);

//	frame.copyTo(src,sumThred);

	//  namedWindow("RGB_Distance",CV_WINDOW_NORMAL);
	//  imshow("RGB_Distance",RgbDistance_Thrd);

	/////////////////////////////////////Show the result for the first condition

//	Mat ThreeThred;
//	bitwise_and(FirstTwo, RgbDistance_Thrd, ThreeThred);

//	Mat Handimage;
//	frame.copyTo(Handimage,ThreeThred);
//	frame.copyTo(src,ThreeThred);
//	frame.copyTo(src,RgbDistance_Thrd);

	//////////////calculate for the second condition
//	Mat Fbthred;
//	Mat Fgthred;
//	Mat Frthred;
//	Mat Fbgrthred;
//
//	threshold(rchannel, Frthred, 220, 255, THRESH_BINARY);
//	threshold(gchannel, Fgthred, 210, 255, THRESH_BINARY);
//	threshold(bchannel, Fbthred, 170, 255, THRESH_BINARY);
//
//	bitwise_and(Frthred, Fgthred, Fbgrthred);
//	bitwise_and(Fbgrthred, Fbthred, Fbgrthred);
//
//	Mat FRg_Abs_Dist_thred;
//	threshold(Rg_Abs_Dist, FRg_Abs_Dist_thred, 15, 255, THRESH_BINARY_INV);
//
//	Mat Gb_Dist;
//	Mat Gb_thred;
//	subtract(Gsigned, Bsigned, Gb_Dist);
//	threshold(Gb_Dist, Gb_thred, 0, 255, THRESH_BINARY);
//	Gb_thred.convertTo(Gb_thred, CV_8UC1);
//
//	Mat SecCondThred;
//	bitwise_and(Fbgrthred, FRg_Abs_Dist_thred, SecCondThred);
//	bitwise_and(SecCondThred, Rb_thred, SecCondThred);
//	bitwise_and(SecCondThred, Gb_thred, SecCondThred);
//
//	Mat result;
//	bitwise_or(sumThred,SecCondThred,result);

	frame.copyTo(src,sumThred);
//
////	Mat result;
////	frame.copyTo(result,SecCondThred);
//	frame.copyTo(src,SecCondThred);
//


//	Mat ProcessedB;
//	Mat ProcessedG;
//	Mat ProcessedR;
//
//	bitwise_and(bchannel, ThreeThred, ProcessedB);
//	bitwise_and(gchannel, ThreeThred, ProcessedG);
//	bitwise_and(rchannel, ThreeThred, ProcessedR);
//
//	vector<Mat> channels;
//	channels.push_back(ProcessedB);
//	channels.push_back(ProcessedG);
//	channels.push_back(ProcessedR);
//
//	merge(channels, src);

	//  namedWindow("Hand",CV_WINDOW_NORMAL);
	//  imshow("Hand",src);
}

void Hsv_SkinFilter(Mat& src){//不同的滤波技术         RGB色彩空间 – R代表单色红，G代表单色绿，B代表单色蓝
  //                                               HSV色彩空间 – H 代表色彩， S代表饱和度，V代表强度值
  //                                              YCbCr色彩空间 – 是数字电视的色彩空间

	Mat image=src.clone();

	Mat tmp = Mat::zeros(src.rows,src.cols,CV_8UC3);
	tmp.copyTo(src);

	Mat hsv_img;
	cvtColor(image,hsv_img,CV_BGR2HSV_FULL);
//	inRange(hsv_img,Scalar(0,22,0), Scalar(60,175,255),mask);
	Mat tmp1,tmp2;
	inRange(hsv_img,Scalar(0,30,30), Scalar(40,170,255),tmp1);//实现二值化功能
	inRange(hsv_img,Scalar(156,30,30), Scalar(180,170,255),tmp2);//函数将分别比较rgb的B.G.R三个通道的每个元素是否位于[156,180][30,170][30,255]区间，如果是，mask的相应位设为255，否则为0。

	Mat mask;
	bitwise_or(tmp1,tmp2,mask);//用选定的图像、图形或物体，对处理的图像（全部或局部）进行遮挡，来控制图像处理的区域或处理过程，比如反转图像创建掩码
	image.copyTo(src,mask);//得到一个淹膜MASK的矩阵
}

void YCbCr_SkinFilter(Mat& src){//解释如上面

	Mat image=src.clone();

	Mat tmp = Mat::zeros(src.rows,src.cols,CV_8UC3);
	tmp.copyTo(src);

	Mat YCrCb_IMG;
	cvtColor(image,YCrCb_IMG,CV_BGR2YCrCb);
	Mat mask;
	inRange(YCrCb_IMG,Scalar(20,133,77), Scalar(255,178,127),mask);

	image.copyTo(src,mask);
}

//---2--- finding the fingertip point, control the mouse

int findlongestcontour(vector<vector<Point> > & contours) {
//函数，寻找最长的那条轮廓线

	int index = 0;
	for (int i = 0; i < contours.size(); i++) {
		if (contours[i].size() > contours[index].size())
			index = i;
	}
	return index;
}

int minyvalue(vector<Point> a) {//用向量中的一组点找到y的最小值
	int value = a[0].y;
	for (int i = 0; i < a.size(); i++) {
		if (a[i].y < value) {
			value = a[i].y;
		}
	}
	return value;
}
int maxyvalue(vector<Point> a) {//用向量中的一组点找到x的最大值
	int value = a[0].y;
	for (int i = 0; i < a.size(); i++) {
		if (a[i].y > value) {
			value = a[i].y;
		}
	}
	return value;
}
int minxvalue(vector<Point> a) {//用向量中的一组点找到x的最小值
  int value = a[0].x;
  for (int i = 0; i < a.size(); i++) {
    if (a[i].x < value) {
      value = a[i].x;
    }
  }
  return value;
}
int maxxvalue(vector<Point> a) {//用向量中的一组点找到y的最大值
	int value = a[0].x;
	for (int i = 0; i < a.size(); i++) {
		if (a[i].x > value) {
			value = a[i].x;
		}
	}
	return value;
}


void findpoint(vector<Point> contour, int boundindex[]) {
//找到y坐标最大的点的索引值存入boundindex[0],找到x坐标最大的点的索引值存入boundindex[1].

	int ymax = maxyvalue(contour);
	int xmax = maxxvalue(contour);
	for (int i = 0; i < contour.size(); i++) {
		if (contour[i].y == ymax) {
			boundindex[0] = i;
			break;
		}
	}
	for (int i = contour.size() - 1; i > 0; i--) {
		if (contour[i].x == xmax) {
			boundindex[1] = i;
			break;
		}
	}
}

void findfingertip_simple(vector<vector<Point> >& fingertipROIContour, int& xcoor, int& ycoor) {//这里我初步推断是简单手势识别，并且引入了ROI
	int index = findlongestcontour(fingertipROIContour);
	ycoor = minyvalue(fingertipROIContour[index]);
	int sum = 0;
	int count = 0;
	for (int i = 0; i < fingertipROIContour[index].size(); i++) {
		if (fingertipROIContour[index][i].y == ycoor){
			sum = sum + fingertipROIContour[index][i].x;
			count++;
		}
	}
	xcoor = sum / count;
}

inline bool sortPointY(Point pt1,Point pt2){//比较y值？
    return pt1.y<pt2.y;
}
void generatepointvec(vector<Point> contour, int boundindex[], vector<Point>& rawData){//计算最近点对
	vector<Point> equaly,leftp;
	for (int i=0; i<contour.size(); i++)
	{
		if (contour[i].y <= contour[boundindex[1]].y)
			equaly.push_back(contour[i]);
		else if(contour[i].x <= contour[boundindex[0]].x)
			leftp.push_back(contour[i]);
	}

	sort(equaly.begin(), equaly.end(), sortPointY);

	int i=0;
	while (i < equaly.size()){
		int counter=1;
		Point2f center=equaly[i], nextPt;
		while (((i + 1) < equaly.size()) && (equaly[i].y == equaly[i + 1].y)) {
			counter++; // how many points in the line
			i++; // get to the next point
			nextPt = equaly[i];
			center += nextPt; // add the next point
			// the body exits, i points to last point of the same y
		}
		if(counter == 2){
			center.x = (int) (center.x / counter + 0.5);
			center.y = (int) (center.y / counter + 0.5);
			rawData.push_back(center); // collecting centers of lines
		}
		i++; // next unaccounted pixel
	}

	for(int k=0; k<leftp.size(); k++){
		Point center;
		center.x = (int)((double)(leftp[k].x+contour[boundindex[1]].x)/(double)2+0.5);
		center.y = (int)((double)(leftp[k].y+contour[boundindex[1]].y)/(double)2+0.5);
		rawData.push_back(center);
	}
}
int findfingertip(vector< vector<Point> >& fingertipROIContour, Mat& src, int& xcoor, int& ycoor) {
	int index = findlongestcontour(fingertipROIContour);

	int boundindex[2] = { 0, 0 };
	findpoint(fingertipROIContour[index], boundindex);

	vector<Point> contour=fingertipROIContour[index], rawData;
	Vec4f lines;

	// get the center point of line from left to right in the contour
	generatepointvec(contour,boundindex,rawData);

	if(rawData.size()==0)
	    return -1;

	// fit a line over those line centers
	fitLine(Mat(rawData), // Input vector of 2D or 3D points
			lines, // Output line parameters. In case of 2D fitting, it should be a vector of 4 elements (like Vec4f) - (vx, vy, x0, y0), where (vx, vy) is a normalized vector collinear to the line and (x0, y0) is a point on the line.
			CV_DIST_L2, // euclidean distance used by the M-estimator such as least square
			0, // Numerical parameter ( C ) for some types of distances. If it is 0, an optimal value is chosen.
			0.01, // reps - Sufficient accuracy for the radius (distance between the coordinate origin and the line)
			0.01); // aeps - Sufficient accuracy for the angle. 0.01 would be a good default value for reps and aeps.
	// (vx, vy, x0, y0)
	// lines[0]=vx, lines[1]=vy, lines[2]=x0, lines[3]=y0, row

	float k = lines[0] / lines[1];
	float b = lines[2]- k * lines[3];

	Point c1((int)b,0);
	Point c2((int)(k*(src.rows-1)+b),src.rows-1);
//	line(src, c1, c2, Scalar(255,0,0), 3);

	double posdif = fingertipROIContour[index][boundindex[0]].y * k + b - fingertipROIContour[index][boundindex[0]].x;
	double negdif = fingertipROIContour[index][boundindex[1]].y * k + b - fingertipROIContour[index][boundindex[1]].x;
	int posid,negid;
	for (int i = 0; i < fingertipROIContour[index].size(); i++) {
		if (i >= boundindex[0] && i <= boundindex[1])
			continue;
		double cdif = fingertipROIContour[index][i].y * k + b - fingertipROIContour[index][i].x;
		if (cdif >= 0 && cdif < posdif){
			posid = i;
			posdif = cdif;
		}
		else if (cdif < 0 && cdif > negdif){
			negid = i;
			negdif = cdif;
		}
	}

//	LOGI("***************** before xcoor/ycoor **************************");
//	LOGI("posid:%d; negid:%d",posid,negid);

	if (posid < fingertipROIContour[index].size() && negid < fingertipROIContour[index].size()) {
		xcoor = 0.5 * (fingertipROIContour[index][posid].x + fingertipROIContour[index][negid].x);
		ycoor = 0.5 * (fingertipROIContour[index][posid].y + fingertipROIContour[index][negid].y);
		return posid;
	}
	return -1;
}

//double caltheta(vector<Point> lcontour, int ftindex, int xmin, int xmax, int ymin, int goaly){
//  vector<int> xlefttmp, xrighttmp;
//  for(int i = 0; i < lcontour.size(); i++){
//    if(lcontour[i].x >= xmin && lcontour[i].x <= lcontour[ftindex].x && lcontour[i].y != goaly)
//      xlefttmp.push_back(lcontour[i].x);
//    else if(lcontour[i].x > lcontour[ftindex].x && lcontour[i].x <= xmax && lcontour[i].y != goaly)
//      xrighttmp.push_back(lcontour[i].x);
//  }
//
//  double leftedge = accumulate(xlefttmp.begin(), xlefttmp.end(), 0.0) / xlefttmp.size();
//  double rightedge = accumulate(xrighttmp.begin(), xrighttmp.end(), 0.0) / xrighttmp.size();
//
////  return (double)contourArea(lcontour)/((double)(rightedge - leftedge)*(double)(goaly-ymin));
//  return 1000*(double)hypot((rightedge - leftedge),(double)(goaly-ymin))/(double)contourArea(lcontour);
//}

int calfingerwidth(vector<Point> lcontour, int goaly){
  int leftx = 0, rightx = 0;

  for (int i = 0; i < lcontour.size(); i++){
    if (lcontour[i].y == goaly) {
      leftx = lcontour[i].x;
      break;
    }
  }
  for (int j = lcontour.size()-1; j >= 0; j--){
    if (lcontour[j].y == goaly) {
    	rightx = lcontour[j].x;
      break;
    }
  }
  return (rightx - leftx);
}
//int calfingerwidth(vector<Point> lcontour, int goaly, int xmin, int xmax, Point tl, Point br){
//  Point lt(0,0);
//  Point lb(0,0);
//  Point rb(0,0);
//  Point rt(0,0);
//  int leftlen = 0, cenlen = 0, rightlen = 0;
//
//  for (int i = 0; i < lcontour.size(); i++){
//    if (lcontour[i].y == goaly) {
//      lb = lcontour[i];
//      break;
//    }
//  }
//  for (int j = lcontour.size()-1; j >= 0; j--){
//    if (lcontour[j].y == goaly) {
//      rb = lcontour[j];
//      break;
//    }
//  }
//  cenlen = rb.x - lb.x + 1;
//
//  if (xmin == (tl.x+2)) {
//    for (int i = 0; i < lcontour.size(); i++){
//      if(lcontour[i].x == xmin && lcontour[i].y != goaly) {
//        lt = lcontour[i];
//        break;
//      }
//    }
//    if (lt.x != 0 && lt.y != 0)
//      leftlen = lb.y - lt.y + 1;
//  }
//  if (xmax == (rb.x-2)) {
//    for (int j = lcontour.size()-1; j >= 0; j--){
//      if(lcontour[j].x == xmax && lcontour[j].y != goaly) {
//        rt = lcontour[j];
//        break;
//      }
//    }
//    if (rt.x !=0 && rt.y != 0)
//      rightlen = rb.y - rt.y + 1;
//  }

//  ofstream testf;
//  testf.open("testdata.txt",ios::app);
//
//  testf << goaly <<';'<< xmin <<';'<< xmax << endl;
//  testf << lt <<';'<< lb <<';'<< rt <<';'<< rb << endl;
//
//  testf << leftlen <<';'<< cenlen <<';'<< rightlen <<';'<< (leftlen+cenlen+rightlen) << endl;
//  testf << endl;

//  return (leftlen+cenlen+rightlen);
//}

//---3--- detecting the click action according to the velocity on the y axis and the fingertip's degree
//void clickdetection(bool& clkbj, queue<double> velq, queue<double> dgrq) {
void clickdetection(bool& clkbj, queue<double> velq, double deltaarea) {
	if (clkbj == true) {
		if (end == true)
			clkbj = false;
		return;
	}
	queue<double> tmp = velq;
	double m = 0;
	int s = tmp.size();
	for (int a = 0; a < s; a++) {
		m = m + tmp.front();
		tmp.pop();
	}
	double meanvy = m / (double)s;
//	if (meanvy >= 80 && deltawidth >= 10) {
//	if (meanvy >= 100 ) {
	if (meanvy >= 78 && deltaarea > 300) {
//	if (meanvy >= 70 && deltaarea < -500) {
		clkbj = true;
	} else
		clkbj = false;
}
void enddetection(bool& end, bool clk, queue<int> areaq, int s, queue<Point>& deltaq) {
	if (clk == true)
		end = false;
	else {
		int ds1 = areaq.back() - areaq.front();
		int ds2 = s - areaq.back();

		if (ds1 < 150 && ds1 > -150 && ds2 < 150 && ds2 > -150){
//		if (ds1 <= 50 && ds1 > -150 && ds2 > ds1 && ds2 < 150){
			end = true;
			deltaq.back() = Point(0,0);
			LOGE("ds1:%d; ds2:%d. click successful!", ds1, ds2);
		}
		else
			end = false;
	}
}
void enddetection(bool& end, queue<Point> pq, int cury, bool clk, queue<Point>& deltaq) {
	if (clk == true)
		end = false;
	else {
		int dy1 = pq.back().y - pq.front().y;
		int dy2 =cury - pq.back().y;
		if (dy1 <= 0 && dy1 > -10 && dy2 > dy1 && dy2 < 10){
			end = true;
//			deltaq.front() = Point(0,0);
			deltaq.back() = Point(0,0);
		}
		else
			end = false;
	}
}

void morphOps(Mat &thresh){

  //create structuring element that will be used to "dilate" and "erode" image.
  //the element chosen here is a 3px by 3px rectangle
  Mat erodeElement = getStructuringElement( MORPH_RECT,Size(3,3) );
  //dilate with larger element so make sure object is nicely visible
  Mat dilateElement = getStructuringElement( MORPH_RECT,Size(3,3) );

  dilate(thresh,thresh,dilateElement);
  dilate(thresh,thresh,dilateElement);
  dilate(thresh,thresh,dilateElement);
  erode(thresh,thresh,erodeElement);
  erode(thresh,thresh,erodeElement);
  erode(thresh,thresh,erodeElement);

}

double calibration(Mat fingeregion)//校准
{
  preSkinFilter(fingeregion);
  Mat fg;
  cvtColor(fingeregion, fg, COLOR_BGR2GRAY );
  Mat nonZeroCoordinates;
  findNonZero(fg, nonZeroCoordinates);

  if (nonZeroCoordinates.total() == 0)
      return 0;

  Mat samplemat(2,nonZeroCoordinates.total(),CV_64FC1);

  for (int i=0;i<nonZeroCoordinates.total();i++){
    Vec3b tmp = fingeregion.at<Vec3b>(nonZeroCoordinates.at<Point>(i).y,nonZeroCoordinates.at<Point>(i).x);
    float s = tmp[0]+tmp[1]+tmp[2];//B,G,R
    samplemat.at<double>(0,i) = (float)tmp[2]/(float)s;
    samplemat.at<double>(1,i) = (float)tmp[1]/(float)s;
  }
  // cv::calcCovarMatrix(X, Covar, Mu, CV_COVAR_NORMAL+CV_COVAR_COLS, CV_64F);
  cv::calcCovarMatrix(samplemat, covarmat, meanmat, CV_COVAR_NORMAL+CV_COVAR_COLS, CV_64F);//用来求取向量集的协方差矩阵
  covarmat = covarmat / nonZeroCoordinates.total();
  invert(covarmat, covarinv, DECOMP_CHOLESKY);

  return 1;
}

bool detectAndDisplay(string fnt, Point& mousedlt, int& frame_count, int& allframe_count, Mat& src, clock_t curtime, int& lastx, int& lasty, double& lastarea, Point& finalft, clock_t & lasttime, queue<double>& velq, queue<Point>& ftpointq, queue<Point>& deltaq, queue<int>& areaq)
 {
//	if (allframe_count <= 100){
//	    Mat ttt;
//	    src(Rect(Point(100,0), Point(220,src.rows-1))).copyTo(ttt);
//	    rectangle(src, Point(100,0), Point(220,src.rows-1), Scalar(0, 255, 255, 255), 3);
//	    r =  calibration(ttt);
//	    allframe_count++;
//	    return false;
//	}

	Point ntl, nbr;
	if (frame_count < FRAME) {
		ntl = Point(0, 0);
		nbr = Point(src.cols - 1, src.rows - 1);
	} else {
		tlx = cx - BOUND;
		if (tlx < 0){
			tlx = 0;
			brx = 2*BOUND;
		}else{
			brx = cx + BOUND;
			if(brx >= src.cols-1){
				brx = src.cols - 1;
				tlx = src.cols - 2*BOUND;
			}
		}

		tly = 0;
		bry = src.rows - 1;
		ntl = Point(tlx, tly);
		nbr = Point(brx, bry);
	}

	Mat frame_gray;
	///////////////////////////
//	cvtColor(src, frame_gray, COLOR_BGR2GRAY);
//	Mat resrc;
//	if(frame_count <= 1){
//		src.copyTo(firstsrc);
//		src.copyTo(resrc);
//	}
//	else{
//		Mat firstsrc_gray;
//		cvtColor(firstsrc,firstsrc_gray,COLOR_BGR2GRAY);
//		Mat twodiff;
//		absdiff(frame_gray, firstsrc_gray, twodiff);
//		blur(twodiff, twodiff, Size(5, 5), Point(-1,-1));
//		Mat diffm;
//		threshold(twodiff, diffm, 10, 255, CV_THRESH_BINARY);
//		src.copyTo(resrc,diffm);
//	}
//	src = resrc;
	///////////////////////////

	Mat ftROI = src(Rect(ntl, nbr));

	preSkinFilter(ftROI);
//	YCbCr_SkinFilter(ftROI);
//	Hsv_SkinFilter(ftROI);

	int dx, dy, deltax, deltay, deltaxy, fingerwidth;
	double vel, dt;
	double s;

//	ofstream dyvalue;
//	string k = "/sdcard/dyvalue_"+fnt+".txt";
//	dyvalue.open(k.data(), ios::app);
//	ofstream dx;
//	ofstream dy;
//	ofstream dxy;
//	ofstream v;
//	v.open("/sdcard/y_vel.txt", ios::app);
	stringstream fn;
    fn << time;
//    string fns = "/sdcard/rawdata_"+fnt+".txt";
    string fns = "/sdcard/rawdata.txt";
	ofstream rawdata;
	rawdata.open(fns.data(),ios::app);

	string fry = "/sdcard/rawy.txt";
	ofstream rawy;
	rawy.open(fry.data(),ios::app);
	string frs = "/sdcard/raws.txt";
	ofstream raws;
	raws.open(frs.data(),ios::app);
	string fdy = "/sdcard/deltay.txt";
	ofstream dlty;
	dlty.open(fdy.data(),ios::app);
	string fds = "/sdcard/deltas.txt";
	ofstream dlts;
	dlts.open(fds.data(),ios::app);
//	ofstream xy;
//	ofstream dxy;

//	dx.open("/sdcard/delta_x.txt", ios::app);
//	dy.open("/sdcard/delta_y.txt", ios::app);
//	dxy.open("/sdcard/delta_xy.txt", ios::app);
//	xy.open("/sdcard/xy_handvalue.txt", ios::app);
//	dxy.open("/sdcard/dxy_handvalue.txt", ios::app);

//	ofstream len;
//	len.open("/sdcard/dgrfile.txt", ios::app);
//	ofstream deltawid;
//	deltawid.open("/sdcard/deltawidfile.txt", ios::app);

	Mat IMpathbat,IMpath;
	Mat fingertipROIBinary;
	bool clk = false;

	cvtColor( src, frame_gray, COLOR_BGR2GRAY );
	IMpath = frame_gray(Rect(ntl, nbr));
//	IMpathbat = frame_gray(Rect(ntl, nbr));
//	IMpathbat.copyTo(IMpath);
//	cvtColor( ftROI, IMpath, COLOR_BGR2GRAY );
//
//	Mat nonZeroCoordinates;
//	findNonZero(IMpath, nonZeroCoordinates);
//
//	if (nonZeroCoordinates.total() != 0 && r != 0){
//		Mat mask = Mat::zeros(IMpath.rows,IMpath.cols,CV_8UC1);
//		for (int i = 0; i < nonZeroCoordinates.total(); i++ ) {
//			Vec3b tmp = ftROI.at<Vec3b>(nonZeroCoordinates.at<Point>(i).y,
//					nonZeroCoordinates.at<Point>(i).x);
//			int s = tmp[0] + tmp[1] + tmp[2];
//			Mat rg =
//					(Mat_<double>(2, 1) << (float) tmp[2] / (float) s, (float) tmp[1] / (float) s);
//			Mat rgmeansub(2, 1, CV_32FC1);
//			rgmeansub = rg - meanmat;
//			Mat tmp1;
//			gemm(rgmeansub, covarinv, 1, rgmeansub, 0, tmp1, GEMM_1_T);
//			Mat res;
//			gemm(tmp1, rgmeansub, 1, rgmeansub, 0, res, 0);
//			double p = exp(-0.5 * res.at<double>(0, 0));
//
//			if (p > 0.5)
//				mask.at <uchar> (nonZeroCoordinates.at<Point>(i).y, nonZeroCoordinates.at< Point>(i).x) = 255;
//		}
//		morphOps(mask);
//		IMpathbat.copyTo(ftROI,mask);
//	}

	int ht = 0;

//	Mat nonZeroCoordinates;
//	Point startp = ntl;
//	findNonZero(IMpath, nonZeroCoordinates);
//	if (nonZeroCoordinates.total() != 0){
//		ht = nonZeroCoordinates.at<Point>(0).y-20;
//		if (ht < 0) {
//			ht = 0;
//		}
//		startp = Point(ntl.x,ht);
//	}

//	Point startp = ntl;
//	for (int i = 0; i < IMpath.rows; i++ ) {
//		int j;
//		for ( j = 0; j < IMpath.cols; j++) {
//			if ((int)(IMpath.at<uchar>(i,j)) != 0)
//			{
//				ht = i-20;
//				if (ht < 0) {
//					ht = 0;
//				}
//				startp = Point(ntl.x,ht);
//				LOGE("i:%d;j:%d;ht:%d;g:%d", i, j, ht, (int)(IMpath.at<uchar>(i,j)));
//				break;
//			}
//		}
//		if (j != IMpath.cols)
//			break;
//	}
//
//	rectangle(src, startp, nbr, Scalar(0, 255, 255, 255), 3);
//	IMpath = frame_gray(Rect(startp,nbr));

	std::vector<Rect> fingertip;
//	fingertip_cascade.detectMultiScale(ftROI, fingertip, 1.1, 3, 0, Size(45, 27), Size(150, 120));
	//(Rect(Point(startp.x-50,startp.y-50), Point(IMpath.cols-1,IMpath.rows-1)))
	fingertip_cascade.detectMultiScale(IMpath, fingertip, 1.1, 3, 0, Size(45, 27), Size(150, 120));//在预处理后的 ROI 灰度图中检测到了多个可能是用户指尖的矩形 区域并保存在 objects 中，
//	fingertip_cascade.detectMultiScale(src(Rect(startp, nbr)), fingertip, 1.1, 3, 0, Size(45, 27), Size(150, 120));

	double tempY = frame_gray.rows;
	int smallestRE = -1;
	Point tl, br;

	for (size_t i = 0; i < fingertip.size(); i++) {
		tl = Point(fingertip[i].tl().x + ntl.x , fingertip[i].tl().y + ntl.y + ht);
		br = Point(fingertip[i].br().x + ntl.x , fingertip[i].br().y + ntl.y + ht);
//		tl = Point(fingertip[i].tl().x + ntl.x , fingertip[i].tl().y + ntl.y);
//		br = Point(fingertip[i].br().x + ntl.x , fingertip[i].br().y + ntl.y);
//		rectangle(src, tl, br, Scalar(0, 255, 0, 255), 3);

		double ys = tl.y;
		if (ys < tempY) {
			smallestRE = i;
			tempY = ys;
		}
	}

	if(ac == 1)
		starttime = curtime;

	if (smallestRE != -1) {
		tl = Point(fingertip[smallestRE].tl().x + ntl.x, fingertip[smallestRE].tl().y + ntl.y + ht);
		br = Point(fingertip[smallestRE].br().x + ntl.x, fingertip[smallestRE].br().y + ntl.y + ht);
//		tl = Point(fingertip[smallestRE].tl().x + ntl.x, fingertip[smallestRE].tl().y + ntl.y);
//		br = Point(fingertip[smallestRE].br().x + ntl.x, fingertip[smallestRE].br().y + ntl.y);
		rectangle(src, tl, br, Scalar(0, 255, 0, 255), 3);

		Mat fingertipROI = frame_gray(Rect(tl, br));

//		cv::threshold(fingertipROI, fingertipROIBinary, 10, 255, CV_THRESH_BINARY);
		cv::threshold(fingertipROI, fingertipROIBinary, 70, 255, CV_THRESH_BINARY|CV_THRESH_OTSU);

//		Mat element = getStructuringElement(MORPH_RECT,Size(5,5));
//		morphologyEx(fingertipROIBinary,fingertipROIBinary,MORPH_CLOSE,element);

		morphOps(fingertipROIBinary);//对图像进行膨胀、腐蚀操作

		vector<vector<Point> > fingertipROIContour;//存储检测到的所有轮廓
		vector<Vec4i> hierarchy;//存储轮廓之间的关系，一个轮廓的后一个、前一个、父轮廓、内嵌轮廓
		findContours(fingertipROIBinary, fingertipROIContour, hierarchy, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE);

		if (fingertipROIContour.size() == 0) {
			return clk;
		}

		for (int i = 0; i < fingertipROIContour.size(); i++) {
			for (int j = 0; j < fingertipROIContour[i].size(); j++) {
				fingertipROIContour[i][j].x += tl.x;
				fingertipROIContour[i][j].y += tl.y;
			}
//			drawContours(src, fingertipROIContour, i, Scalar(255, 251, 240), 1, 8, hierarchy, 0, Point());
		}
		drawContours(src, fingertipROIContour, findlongestcontour(fingertipROIContour), Scalar(255, 251, 240), 1, 8, hierarchy, 0, Point());

		vector<Point> lcontour = fingertipROIContour[findlongestcontour(fingertipROIContour)];
		int* ya = new int[lcontour.size()];
//		int* xta = new int[lcontour.size()];
		for (int i = 0; i < lcontour.size(); i++) {
			ya[i] = lcontour[i].y;
//			xta[i] = lcontour[i].x;
		}
//		int ymin = *min_element(ya, ya + lcontour.size());
		int goaly = *max_element(ya, ya + lcontour.size());
//		int xmax = *max_element(xta, xta + lcontour.size());
//		int xmin = *min_element(xta, xta + lcontour.size());

		fingerwidth = calfingerwidth(lcontour, goaly);
		double rate = (double)fingerwidth/(double)(br.x-tl.x);
//		if (rate < 0.85 && rate > 0.7){
//			stringstream sss;
//			sss<<"suitable to click";
//			putText(src,sss.str(),Point(10,300),CV_FONT_HERSHEY_COMPLEX, 0.7, Scalar(0, 0, 255));
//		}

//		findfingertip_simple(fingertipROIContour, ft.x, ft.y);
		int ftindex = findfingertip(fingertipROIContour, src, finalft.x, finalft.y);

		rawy << finalft.y << endl;

		Point curpoint = finalft;
		circle(src, finalft, 3, Scalar(0, 0, 255), -1);

//		double theta = caltheta(lcontour, ftindex, xmin, xmax, ymin, goaly);
        //s记录轮廓内连通区域的面积
		s = contourArea(fingertipROIContour[findlongestcontour(fingertipROIContour)]);

		raws << s << endl;

		if (frame_count == 1) {
			dx = 0;
			dy = 0;
			lastx = finalft.x;
			lasty = finalft.y;
			lasttime = curtime;
			mousedlt = Point(0,0);
//			lastfigwid = fingerwidth;
			lastarea = s;
//			mousecoor = finalft;
//			ftpointq.push(finalft);
//			yvalue << ft.y << endl;
//			xy<<'('<<finalft.x<<','<<finalft.y<<','<<((double)(curtime-starttime)/CLOCKS_PER_SEC)<<')'<<endl;

		} else {
//			yvalue << tl << ';' << br << ';' << (br.x-tl.x) << ';' << (br.y-tl.y) << endl;
//			xy<<'('<<finalft.x<<','<<finalft.y<<','<<((double)(curtime-starttime)/CLOCKS_PER_SEC)<<')'<<endl;
//			dxy<<'('<<(finalft.x-lastx)<<','<<(finalft.y-lasty)<<','<<((double)(curtime-starttime)/CLOCKS_PER_SEC)<<')'<<endl;
//			rawdata << setiosflags(ios::fixed) << setprecision(9) << (double)(curtime/CLOCKS_PER_SEC) << ';';
//			rawdata << setiosflags(ios::fixed) << setprecision(6) << (double)(curtime)/(double)(CLOCKS_PER_SEC) << ';';
//			rawdata.unsetf( ios::fixed );
//			rawdata << finalft.x << ';' << finalft.y << endl;

			dx = finalft.x - lastx;
			dy = finalft.y - lasty;

			deltax = (int)(((double)finalft.x / (double)frame_gray.cols) * 100 + 0.5) - (int)(((double)lastx / (double)frame_gray.cols) * 100 + 0.5);
			deltay = (int)(((double)finalft.y / (double)frame_gray.rows) * 100 + 0.5) - (int)(((double)lasty / (double)frame_gray.rows) * 100 + 0.5);
			deltaxy = abs(deltax) + abs(deltay);

			dt = (double) (curtime - lasttime) / CLOCKS_PER_SEC;
			vel = (double) deltay / dt;

			dlty << vel << endl;

			rawdata << ((double) (curtime) / CLOCKS_PER_SEC) << ';' << dt << ';';
			rawdata << dx << ';' << dy << ';' << abs(dx) + abs(dy) << ';' << vel << ';';

//			v << vel << endl;

//			dx << deltax << endl;
//			dy << deltay << endl;
//			dxy << deltaxy << endl;

//			dyvalue << dy << endl;

//			double deltaperi = perimeter-lastperi;
//			deltalen<<deltaperi<<endl;
//
			if (ftpointq.size() == 2) {
				int fx = ftpointq.front().x;
				int fy = ftpointq.front().y;
				int bx = ftpointq.back().x;
				int by = ftpointq.back().y;

				int formerdx = bx - fx ;
				int nextdx = finalft.x  - bx ;
				int formerdy = by  - fy ;
				int nextdy = finalft.y  - by ;
//				int formerdx = (int)(((double)bx / (double)frame_gray.cols) * 100 + 0.5) - (int)(((double)fx / (double)frame_gray.cols) * 100 + 0.5);
//				int nextdx = (int)(((double)finalft.x / (double)frame_gray.cols) * 100 + 0.5) - (int)(((double)bx / (double)frame_gray.cols) * 100 + 0.5);
//				int formerdy = (int)(((double)by / (double)frame_gray.rows) * 100 + 0.5) - (int)(((double)fy / (double)frame_gray.rows) * 100 + 0.5);
//				int nextdy = (int)(((double)finalft.y / (double)frame_gray.rows) * 100 + 0.5) - (int)(((double)by / (double)frame_gray.rows) * 100 + 0.5);
				if ((formerdx >= 0 && formerdx <= 2 && nextdx <= 0 && nextdx >= -2 && formerdy >= 0 && formerdy <= 2 && nextdy <= 0 && nextdy >= -2) ||
						(formerdx >= 0 && formerdx <= 2 && nextdx <= 0 && nextdx >= -2 && formerdy <= 0 && formerdy >= -2 && nextdy >= 0 && nextdy <= 2) ||
						(formerdx <= 0 && formerdx >= -2 && nextdx >= 0 && nextdx <= 2 && formerdy >= 0 && formerdy <= 2 && nextdy <= 0 && nextdy >= -2) ||
						(formerdx <= 0 && formerdx >= -2 && nextdx >= 0 && nextdx <= 2 && formerdy <= 0 && formerdy >= -2 && nextdy >= 0 && nextdy <= 2))
				{
//					LOGI("Juggle happened");
					ftpointq.back().x = fx;
					ftpointq.back().y = fy;
					deltaq.back() = Point(0,0);
				}
			}

			mousedlt = deltaq.front();
			LOGE("testtest: %d, %d", mousedlt.x, mousedlt.y);

			// push vel,deltadegree data into queue
			if (velq.size() < 3){
				velq.push(vel);
			}
			else {
				velq.pop();
				velq.push(vel);
				clickdetection(clkbj, velq, (s-areaq.back()));
				dlts << s-areaq.back() << endl;
//				clickdetection(clkbj, velq, (fingerwidth-figwidthq.front()));
//				clickdetection(clkbj, velq, (theta-lasttheta)/dt);
			}

			queue<double> tmp = velq;
			double m = 0;
			int si = tmp.size();
			for (int a = 0; a < si; a++) {
				m = m + tmp.front();
				tmp.pop();
			}
//			v << m / (double) s << endl;

//			deltawid << (s-areaq.front()) << endl;
//			LOGE("s:%d; lasts:%d; formers:%d.", s, areaq.back(), areaq.front());
//			LOGE("ds1:%d; ds2:%d.", (s-areaq.back()), (areaq.back()-areaq.front()));
//			LOGE("ds1:%d; ds2:%d.", (s-lastarea), (areaq.back()-areaq.front()));

			end = false;

			if (lastclkbj == false && clkbj == true){
				clk = true;
				clkperiod = true;
//				dyvalue << endl;
//				rawdata << endl;
//				v << endl;
//				deltawid << endl;

			}

			rawdata << (s-areaq.back()) << ';' << (s-areaq.front()) << ';' ;

			if (clk == 1)
				rawdata << "true" << ';';
			else
				rawdata << "false" << ';';

			if (clkperiod == true){
				mousedlt = Point(0,0);
//				enddetection(end, ftpointq, finalft.y, clk, deltaq);
				enddetection(end, clk, areaq, s, deltaq);
			}

			rawdata << mousedlt.x << ';' << mousedlt.y << endl;

			if (lastend == false && end == true){
				clkperiod = false;
//				yvalue << endl;
//				rawdata << endl;
//				v << endl;
//				deltawid << endl;
			}

			lastclkbj = clkbj;
			lastend = end;
			lastx = curpoint.x;
			lasty = curpoint.y;
			lasttime = curtime;
			lastarea = s;
//			formerfigwid = lastfigwid;
//			lastfigwid = fingerwidth;
		}

		frame_count++;
	}
	else {
		mousedlt = Point(0,0);
		dx = 0;
		dy = 0;
	}

	if (ftpointq.size() >= 2){ // queue length: 2
		ftpointq.pop();
		deltaq.pop();
		areaq.pop();
	}
	ftpointq.push(finalft);
	deltaq.push(Point(dx,dy));
	areaq.push(s);

	cx = finalft.x;
	cy = finalft.y;

	ac++;

//	dyvalue.close();
	rawdata.close();

	rawy.close();
	raws.close();
	dlty.close();
	dlts.close();
//	dx.close();
//	dxy.close();
//	dy.close();
//	v.close();
//	len.close();
//	deltawid.close();
//	xy.close();
//	dxy.close();

	return clk;
}


extern "C" {
JNIEXPORT void JNICALL Java_com_seu_SecureFingerMouse_OpenCVWorker_CreateCascadeClassifier(
		JNIEnv *env, jobject obj, jstring jCascadeFilePath) {
	const char* jpathstr = env->GetStringUTFChars(jCascadeFilePath, NULL);
	string CascadeFilePath(jpathstr);
	fingertip_cascade.load(CascadeFilePath);
	env->ReleaseStringUTFChars(jCascadeFilePath,jpathstr);
}

JNIEXPORT jintArray JNICALL Java_com_seu_SecureFingerMouse_OpenCVWorker_preProcessAndDetection(
		JNIEnv *env, jobject obj, jstring fnt, jlong imgAddr, jstring jCascadeFilePath) {
	Mat& src = *(Mat*) imgAddr;
	jintArray result;
	jint * resultptr;
	string filename = env->GetStringUTFChars(fnt, NULL);

	try
	{
		result = env->NewIntArray(3);
	    resultptr = env->GetIntArrayElements(result, NULL);
	    //preprocess
//	    preSkinFilter(src);
//	    Hsv_SkinFilter(src);
//	    YCbCr_SkinFilter(src);
	}
	catch (cv::Exception& e) {
		LOGI("nativeCreateObject caught cv::Exception: %s", e.what());
	} catch (...) {
		LOGD("nativeCreateObject caught unknown exception");
	}

	// rotate 90 degree
	transpose(src, src);
	flip(src, src, 1);

	bool click;
	try
	{
		curtime = clock();
//		LOGD("%u",curtime);
		click = detectAndDisplay(filename, mousedlt, c, ac, src, curtime, lastx, lasty, lastarea, finalft, lasttime, velq, ftpointq, deltaq, areaq);
	}
	catch (cv::Exception& e) {
		LOGI("nativeCreateObject caught cv::Exception: %s", e.what());
	} catch (...) {
		LOGD("nativeCreateObject caught unknown exception");
	}

	if (click)
		resultptr[0] = 1;
	else
		resultptr[0] = 0;
	resultptr[1] = mousedlt.x;
	resultptr[2] = mousedlt.y;

	env->ReleaseIntArrayElements(result, resultptr, NULL);

	return result;
}
}

