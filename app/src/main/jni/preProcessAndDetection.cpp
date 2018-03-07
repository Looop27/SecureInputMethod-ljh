#include "preProcessAndDetection.hpp"//由.hpp文件定义而来
#include <iomanip>
#include <math.h>
#include <stdlib.h>
#include <ctime>
#include <string>
#include <fstream>

#include <vector>
#include <numeric>//最多可存储 38 个数字，所有数字都能够放到小数点的右边。


using namespace std;
using namespace cv;

CascadeClassifier fingertip_cascade;//是Opencv中做人脸检测的时候的一个级联分类器，现在有两种选择：一是使用老版本的CvHaarClassifierCascade函数，一是使用新版本的CascadeClassifier类。

clock_t starttime;//记录开始时间，后面还有上次时间以及现在时间

int c = 1;        //目前进入detectAndDisplay（）的有效次数
int ac = 1;		  //目前进入detectAndDisplay（）的次数
Mat firstsrc;//无用变量
Point finalft(320/2,240/2);//本次指尖坐标
int lastx = finalft.x, lasty = finalft.y;//記錄上一次指尖的x和y
double lastarea;//上次的区域
Point mousedlt;//鼠标的移动距离。
clock_t lasttime, curtime;//上次和本次的函数运行到现在的计算机时间

queue<Point> ftpointq, deltaq;//队列，存放（手指指尖）和 鼠标移动距离
queue<int> areaq;//指尖面积队列（区域）
queue<double> velq, lenq;//（速度）和（长度）

//上次指尖所在横纵坐标，用于判断黑色区域位置
int cx = 100;
int cy = 100;

//分别表示黑布的右上横纵坐标和左下横纵坐标
int tlx = 0;
int tly = 0;
int brx = 0;
int bry = 0;

int FRAME = 50;//帧数
int BOUND = 80;//黑色区域宽度

bool lastclkbj = false;  //上次点击过程
bool clkbj = false;      //点击过程
bool lastend = false;    //上次点击结束结果
bool end = false;        //点击结束结果
bool clkperiod = false;  //点击时段，参照算法3.5

//以下变量均为被注释或没被用到且不属于算法的变量
Mat covarmat;//协方差矩阵
Mat meanmat;
Mat covarinv;
double r;


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

	//以下操作为获取R > 95 and G > 40 and B > 20

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
//函数，寻找最长的那条轮廓线(比较包含的像素点)

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

inline bool sortPointY(Point pt1,Point pt2){//比较y值
    return pt1.y<pt2.y;
}
//此函数为五步操作的前三步：
//1、扫描轮廓上的所有点，将底部的指尖区域分界线排除，保留用户指尖的弧形轮廓 上的所有点
//2、对这些点按照其纵坐标值进行排序
//3、将纵坐标相同的每两个点作为一组，计算其中点坐标并保存下来
void generatepointvec(vector<Point> contour, int boundindex[], vector<Point>& rawData){
	//将第一步获得的点存在equaly
	vector<Point> equaly,leftp;
	for (int i=0; i<contour.size(); i++)
	{
		if (contour[i].y <= contour[boundindex[1]].y)
			equaly.push_back(contour[i]);
		else if(contour[i].x <= contour[boundindex[0]].x)
			leftp.push_back(contour[i]);
	}

	//根据纵坐标排序
	sort(equaly.begin(), equaly.end(), sortPointY);

	//将纵坐标相同的每两个点作为一组，计算其中点坐标并保存到rawData
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
		//实际上如果有两个以上的点纵坐标相同则不给予考虑
		if(counter == 2){
			//求中点时去上整
			center.x = (int) (center.x / counter + 0.5);
			center.y = (int) (center.y / counter + 0.5);
			rawData.push_back(center); // collecting centers of lines
		}
		i++; // next unaccounted pixel
	}

	//另外一部分求中点的方法，与横坐标最远的点连线求中点
	for(int k=0; k<leftp.size(); k++){
		Point center;
		center.x = (int)((double)(leftp[k].x+contour[boundindex[1]].x)/(double)2+0.5);
		center.y = (int)((double)(leftp[k].y+contour[boundindex[1]].y)/(double)2+0.5);
		rawData.push_back(center);
	}
}
//本函数获取的int值无用，但本函数获取了本次指尖的坐标
//算法部分参照图3.11上5个步骤
int findfingertip(vector< vector<Point> >& fingertipROIContour, Mat& src, int& xcoor, int& ycoor) {
	int index = findlongestcontour(fingertipROIContour);

	int boundindex[2] = { 0, 0 };
	findpoint(fingertipROIContour[index], boundindex);

	vector<Point> contour=fingertipROIContour[index], rawData;
	Vec4f lines;

	// get the center point of line from left to right in the contour
	//前三步骤函数
	generatepointvec(contour,boundindex,rawData);

	//无中点情况
	if(rawData.size()==0)
	    return -1;

	//第四步操作
	// fit a line over those line centers
	fitLine(Mat(rawData), // Input vector of 2D or 3D points
			lines, // Output line parameters. In case of 2D fitting, it should be a vector of 4 elements (like Vec4f) - (vx, vy, x0, y0), where (vx, vy) is a normalized vector collinear to the line and (x0, y0) is a point on the line.
			CV_DIST_L2, // euclidean distance used by the M-estimator such as least square，CV_DIST_L2为最简单快速的最小二乘法，推荐使用
			0, // Numerical parameter ( C ) for some types of distances. If it is 0, an optimal value is chosen.这个参数和以下2个均为默认最优
			0.01, // reps - Sufficient accuracy for the radius (distance between the coordinate origin and the line)
			0.01); // aeps - Sufficient accuracy for the angle. 0.01 would be a good default value for reps and aeps.
	// (vx, vy, x0, y0)
	// lines[0]=vx, lines[1]=vy, lines[2]=x0, lines[3]=y0, row

	//(line[0],line[1])表示直线的方向向量，(line[2],line[3])表示直线上的一个点。
	//k为斜率，b为常数
	//求出所需直线方程
	float k = lines[0] / lines[1];
	float b = lines[2]- k * lines[3];

	//论文中出现，但c1,c2并没被用到
	Point c1((int)b,0);
	Point c2((int)(k*(src.rows-1)+b),src.rows-1);
//	line(src, c1, c2, Scalar(255,0,0), 3);

	//以下是最后一步，求指尖坐标，也就是xcoor，ycoor
	//以下算法是根据程序推断，并不在论文中
	//首先学姐求出，纵坐标最大点和水平位置直线的△x以及横坐标最大的点和水平位置直线的△x
	//这里学界似乎肯定拟合直线会在两点之间
	//然后找出轮廓中直线两边到直线水平位置△x最小的各一个点
	//这两个点的中点为指尖
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

//手指宽度，默认为半圆，实际求得宽度为直径
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
//判断点击事件函数，三个元素为全局变量——是否点击、速度队列、本次与上次面积差
//判断过程参照算法3.4
void clickdetection(bool& clkbj, queue<double> velq, double deltaarea) {
	if (clkbj == true) {
		//如果点击刚结束，则本次未点击
		if (end == true)
			clkbj = false;
		return;
	}
	queue<double> tmp = velq;
	//m为速度合
	double m = 0;
	int s = tmp.size();
	for (int a = 0; a < s; a++) {
		m = m + tmp.front();
		tmp.pop();
	}
	//求平均速度
	double meanvy = m / (double)s;
//	if (meanvy >= 80 && deltawidth >= 10) {
//	if (meanvy >= 100 ) {
	//平局速度大于等于78，面积变化大于300，则判断未点击
	if (meanvy >= 78 && deltaarea > 300) {
//	if (meanvy >= 70 && deltaarea < -500) {
		clkbj = true;
	} else
		clkbj = false;
}
//学姐用该方法判断是否点击结束
void enddetection(bool& end, bool clk, queue<int> areaq, int s, queue<Point>& deltaq) {
	//如果已经判断本次操作就是点击，则必定不会结束
	if (clk == true)
		end = false;
	else {
		int ds1 = areaq.back() - areaq.front();
		int ds2 = s - areaq.back();
		//通过算法3.6，得到是否点击时段结束
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
//此判断方法已被上方法代替
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

//对图像进行腐蚀和膨胀，使获得的指尖部分更完美
void morphOps(Mat &thresh){

  //create structuring element that will be used to "dilate" and "erode" image.
  //the element chosen here is a 3px by 3px rectangle
  //腐蚀——获取3*3的矩形，和下面的膨胀矩形为内核
  Mat erodeElement = getStructuringElement( MORPH_RECT,Size(3,3) );
  //dilate with larger element so make sure object is nicely visible
  //膨胀-获取3*3的矩形
  Mat dilateElement = getStructuringElement( MORPH_RECT,Size(3,3) );

  //三次腐蚀，三次膨胀，参数分别为操作图像，输出图像，内核（可以理解为膨胀腐蚀的程度）
  dilate(thresh,thresh,dilateElement);
  dilate(thresh,thresh,dilateElement);
  dilate(thresh,thresh,dilateElement);
  erode(thresh,thresh,erodeElement);
  erode(thresh,thresh,erodeElement);
  erode(thresh,thresh,erodeElement);

}

//没有被用到
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

//依次带入opencvwork传来系统时间、鼠标点坐标、1、1、旋转后的图像、程序调用到现在的时间、上次记录x、上次记录y、上次的面积、、上次时间、、手指指间、增量、
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

//控制黑色区域范围，在论文图3.4上面部分
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

	//处理选出来的黑色区域，再其中通过滤波函数，通过3.1算法，找出手指区域
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
	bool clk = false;        //点击结果，本函数返回值

//转成灰度图像
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

//下面加减发用到，认为是无用变量，可能和被注释部分有关
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
	//在预处理后的 ROI 灰度图中检测到了多个可能是用户指尖的矩形
	//image--待检测图片，一般为灰度图像加快检测速度；
	//objects--被检测物体的矩形框向量组；
	//scaleFactor--表示在前后两次相继的扫描中，搜索窗口的比例系数。默认为1.1即每次搜索窗口依次扩大10%;
	//minNeighbors--表示构成检测目标的相邻矩形的最小个数(默认为3个)。
	//flags--要么使用默认值，要么使用CV_HAAR_DO_CANNY_PRUNING，如果设置为CV_HAAR_DO_CANNY_PRUNING，那么函数将会使用Canny边缘检测来排除边缘过多或过少的区域，因此这些区域通常不会是人脸所在区域
	//minSize和maxSize用来限制得到的目标区域的范围。
	fingertip_cascade.detectMultiScale(IMpath, fingertip, 1.1, 3, 0, Size(45, 27), Size(150, 120));
//	fingertip_cascade.detectMultiScale(src(Rect(startp, nbr)), fingertip, 1.1, 3, 0, Size(45, 27), Size(150, 120));

	double tempY = frame_gray.rows;
	int smallestRE = -1;
	Point tl, br;

//找到纵坐标最小的矩形，就是最上方的矩形
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

    //存在矩形，则进入
	if (smallestRE != -1) {
		tl = Point(fingertip[smallestRE].tl().x + ntl.x, fingertip[smallestRE].tl().y + ntl.y + ht);
		br = Point(fingertip[smallestRE].br().x + ntl.x, fingertip[smallestRE].br().y + ntl.y + ht);
//		tl = Point(fingertip[smallestRE].tl().x + ntl.x, fingertip[smallestRE].tl().y + ntl.y);
//		br = Point(fingertip[smallestRE].br().x + ntl.x, fingertip[smallestRE].br().y + ntl.y);
        //想选出来的部分用宽度3的绿色矩形框圈起
		rectangle(src, tl, br, Scalar(0, 255, 0, 255), 3);

        //选出灰度图像中对应矩形
		Mat fingertipROI = frame_gray(Rect(tl, br));

        //通过算法3.2，以及CV_THRESH_OTSU自适应阙值，分出指尖和背景
//		cv::threshold(fingertipROI, fingertipROIBinary, 10, 255, CV_THRESH_BINARY);
		cv::threshold(fingertipROI, fingertipROIBinary, 70, 255, CV_THRESH_BINARY|CV_THRESH_OTSU);

//		Mat element = getStructuringElement(MORPH_RECT,Size(5,5));
//		morphologyEx(fingertipROIBinary,fingertipROIBinary,MORPH_CLOSE,element);

		morphOps(fingertipROIBinary);//对图像进行膨胀、腐蚀操作

		vector<vector<Point> > fingertipROIContour;//存储检测到的所有轮廓
		vector<Vec4i> hierarchy;//存储轮廓之间的关系，一个轮廓的后一个、前一个、父轮廓、内嵌轮廓
		//CV_RETR_EXTERNAL:只检测外轮廓——hierarchy无意义。忽略轮廓内部的洞;CV_CHAIN_APPROX_NONE：把轮廓上所有的点存储,
		findContours(fingertipROIBinary, fingertipROIContour, hierarchy, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE);

		//无指间情况
		if (fingertipROIContour.size() == 0) {
			return clk;
		}

		//将所有轮廓的像素坐标恢复成原图坐标
		for (int i = 0; i < fingertipROIContour.size(); i++) {
			for (int j = 0; j < fingertipROIContour[i].size(); j++) {
				fingertipROIContour[i][j].x += tl.x;
				fingertipROIContour[i][j].y += tl.y;
			}
//			drawContours(src, fingertipROIContour, i, Scalar(255, 251, 240), 1, 8, hierarchy, 0, Point());
		}
		//绘制轮廓
		//元素依次为绘制图像，所有输入的轮廓，指定要绘制轮廓的编号，绘制轮廓所用的颜色，绘制粗细默认1，绘制连通性默认8，关于层级的可选参数（本算法用不到），maxLevel=0——绘制与输入轮廓属于同一等级的所有轮廓即输入轮廓和与其相邻的轮廓，最后一个固定point()
		drawContours(src, fingertipROIContour, findlongestcontour(fingertipROIContour), Scalar(255, 251, 240), 1, 8, hierarchy, 0, Point());

        //找出最长轮廓
		vector<Point> lcontour = fingertipROIContour[findlongestcontour(fingertipROIContour)];
		int* ya = new int[lcontour.size()];
//		int* xta = new int[lcontour.size()];
		for (int i = 0; i < lcontour.size(); i++) {
			ya[i] = lcontour[i].y;
//			xta[i] = lcontour[i].x;
		}
//		int ymin = *min_element(ya, ya + lcontour.size());
        //找到y最大值,也就是底部坐标
		int goaly = *max_element(ya, ya + lcontour.size());
//		int xmax = *max_element(xta, xta + lcontour.size());
//		int xmin = *min_element(xta, xta + lcontour.size());

        //手指宽度，默认为半圆，实际求得宽度为直径
		fingerwidth = calfingerwidth(lcontour, goaly);
		//rate 是一个比率，但是并没有被用到，所以上述一些值也是无用的
		double rate = (double)fingerwidth/(double)(br.x-tl.x);
//		if (rate < 0.85 && rate > 0.7){
//			stringstream sss;
//			sss<<"suitable to click";
//			putText(src,sss.str(),Point(10,300),CV_FONT_HERSHEY_COMPLEX, 0.7, Scalar(0, 0, 255));
//		}

//		findfingertip_simple(fingertipROIContour, ft.x, ft.y);
        //ftindex为无用变量，但是这个函数却是确定指间位置的函数——整个算法参照图3.11上5个步骤
		int ftindex = findfingertip(fingertipROIContour, src, finalft.x, finalft.y);

        //指间的纵坐标进文件
		rawy << finalft.y << endl;

        //当前指间
		Point curpoint = finalft;

        //以finalft为圆心画圆
		circle(src, finalft, 3, Scalar(0, 0, 255), -1);

//		double theta = caltheta(lcontour, ftindex, xmin, xmax, ymin, goaly);
        //s记录轮廓内连通区域的面积
		s = contourArea(fingertipROIContour[findlongestcontour(fingertipROIContour)]);

        //记录本次面积进文件
		raws << s << endl;

		if (frame_count == 1) {
		//第一次调用该程序时进入这里
			dx = 0;
			dy = 0;
			lastx = finalft.x;
			lasty = finalft.y;
			//记录本次函数计算机时间
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

            //如果不是第一次进入这里

			dx = finalft.x - lastx;
			dy = finalft.y - lasty;

			//deltaxy、deltax没用到；deltay计算：按照算法应该是计算了y移动量——像素（但是学姐是算的指尖移动的百分比）。（+0.5代表小数总是取上整）
			deltax = (int)(((double)finalft.x / (double)frame_gray.cols) * 100 + 0.5) - (int)(((double)lastx / (double)frame_gray.cols) * 100 + 0.5);
			deltay = (int)(((double)finalft.y / (double)frame_gray.rows) * 100 + 0.5) - (int)(((double)lasty / (double)frame_gray.rows) * 100 + 0.5);
			deltaxy = abs(deltax) + abs(deltay);

			//除以CLOCKS_PER_SEC代表秒数
			dt = (double) (curtime - lasttime) / CLOCKS_PER_SEC;
			//移动的速度，像素/秒（学姐算的百分比/每秒）
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
            //判断指尖抖动
            //ftpointq为指尖存储队列，只要这个函数运行两次以上，其大小必定为2，其储存的是本次前两次的指尖
			if (ftpointq.size() == 2) {
			//以下是为了获取本次和前一次，以及前一次和前两次的移动坐标△x和△y
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
				//根据算法3.3进行判断
				if ((formerdx >= 0 && formerdx <= 2 && nextdx <= 0 && nextdx >= -2 && formerdy >= 0 && formerdy <= 2 && nextdy <= 0 && nextdy >= -2) ||
						(formerdx >= 0 && formerdx <= 2 && nextdx <= 0 && nextdx >= -2 && formerdy <= 0 && formerdy >= -2 && nextdy >= 0 && nextdy <= 2) ||
						(formerdx <= 0 && formerdx >= -2 && nextdx >= 0 && nextdx <= 2 && formerdy >= 0 && formerdy <= 2 && nextdy <= 0 && nextdy >= -2) ||
						(formerdx <= 0 && formerdx >= -2 && nextdx >= 0 && nextdx <= 2 && formerdy <= 0 && formerdy >= -2 && nextdy >= 0 && nextdy <= 2))
				{
                //如果为抖动，开始重新记录坐标，矫正
//					LOGI("Juggle happened");
					ftpointq.back().x = fx;
					ftpointq.back().y = fy;
					deltaq.back() = Point(0,0);
				}
			}

            //记录鼠标坐标
			mousedlt = deltaq.front();
			LOGE("testtest: %d, %d", mousedlt.x, mousedlt.y);

			// push vel,deltadegree data into queue
			//如果速度队列不足3个，就放入，否则开始进行判断点击
			//此处为算法3.4
			if (velq.size() < 3){
				velq.push(vel);
			}
			else {
			//加入最新速度
				velq.pop();
				velq.push(vel);
			//判断点击事件
				clickdetection(clkbj, velq, (s-areaq.back()));
			//将本次面积与上次面积差写入文件
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

			//判断点击和点击时段，参照算法3.5
			//对于四种点击量，在这里做一下解释
			//clkbj主要判断本次与前两次动作是否构成点击
			//clkperiod判断目前是否是还在某次点击过程中
			//clk判断该次操作是否返回给前端点击指令
			//end判断本次点击时段是否结束
			if (lastclkbj == false && clkbj == true){
				clk = true;
				clkperiod = true;
//				dyvalue << endl;
//				rawdata << endl;
//				v << endl;
//				deltawid << endl;

			}

			//将本次与上次和上上次的面积差写入文件
			rawdata << (s-areaq.back()) << ';' << (s-areaq.front()) << ';' ;

			//将是否点击写入文件
			if (clk == 1)
				rawdata << "true" << ';';
			else
				rawdata << "false" << ';';

			//如果是点击时段，判断是否点击结束
			if (clkperiod == true){
				mousedlt = Point(0,0);
//				enddetection(end, ftpointq, finalft.y, clk, deltaq);
				enddetection(end, clk, areaq, s, deltaq);
			}

			rawdata << mousedlt.x << ';' << mousedlt.y << endl;

			//上次点击时段没结束，本次结束，则点击时段结束
			if (lastend == false && end == true){
				clkperiod = false;
//				yvalue << endl;
//				rawdata << endl;
//				v << endl;
//				deltawid << endl;
			}

			//记录下本次操作的点击情况、时间以及面积，为下次操作做准备
			lastclkbj = clkbj;
			lastend = end;
			lastx = curpoint.x;
			lasty = curpoint.y;
			lasttime = curtime;
			lastarea = s;
//			formerfigwid = lastfigwid;
//			lastfigwid = fingerwidth;
		}

		frame_count++; //有效帧数++，在被注释掉的地方似乎还有别的用处，但目前只有判断是否是第一次进入这个用处
	}
	else {
	//目前所示图像中，找不到矩形部分
		mousedlt = Point(0,0);
		dx = 0;
		dy = 0;
	}

	//指尖队列大于等于2的话要进行清除，保证加入目前量后队列数量为2，除了程序前两次进入本方法，之后都会执行
	if (ftpointq.size() >= 2){ // queue length: 2
		ftpointq.pop();
		deltaq.pop();
		areaq.pop();
	}
	ftpointq.push(finalft);
	deltaq.push(Point(dx,dy));
	areaq.push(s);

	//记录本次指尖坐标
	cx = finalft.x;
	cy = finalft.y;

	ac++;   //帧数（进入函数次数）++，似乎没有别的用处

	//关闭文件
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

//返回是否点击
	return clk;
}


extern "C" {
//这个函数是用来将opencvwork中获得的训练材料.xml文件的地址获取过来，加载到fingertip_cascade。
JNIEXPORT void JNICALL Java_com_seu_SecureFingerMouse_OpenCVWorker_CreateCascadeClassifier(
		JNIEnv *env, jobject obj, jstring jCascadeFilePath) {
	const char* jpathstr = env->GetStringUTFChars(jCascadeFilePath, NULL);
	string CascadeFilePath(jpathstr);
	fingertip_cascade.load(CascadeFilePath);
	env->ReleaseStringUTFChars(jCascadeFilePath,jpathstr);
}

//分析opencvwork获取的每一帧图像，并返回结果，包括是否点击，和指间横纵坐标
JNIEXPORT jintArray JNICALL Java_com_seu_SecureFingerMouse_OpenCVWorker_preProcessAndDetection(
		JNIEnv *env, jobject obj, jstring fnt, jlong imgAddr, jstring jCascadeFilePath) {
		//将图像的绝对地址转换成Mat类型图像
	Mat& src = *(Mat*) imgAddr;
	//储存最终结果
	jintArray result;
	jint * resultptr;
	//fnt为当前时间
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
	//opencv没有直接封装旋转任意角度的函数，所以先用transport转置，在用filp旋转90度
	transpose(src, src);
	flip(src, src, 1);

	bool click;
	try
	{
	//程序调用到现在的时间
		curtime = clock();
//		LOGD("%u",curtime);
//依次带入opencvwork传来系统时间、鼠标点坐标、1、1、旋转后的图像、程序调用到现在的时间、上次记录x、上次记录y、上次的面积、上次指尖、上次时间、速度队列、手指指间、增量、面积队列
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
	//鼠标的移动增量
	resultptr[1] = mousedlt.x;
	resultptr[2] = mousedlt.y;

	env->ReleaseIntArrayElements(result, resultptr, NULL);

	return result;
}
}

