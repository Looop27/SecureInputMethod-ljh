package com.seu.SecureFingerMouse;

import org.opencv.core.Point;

public class RawPoint {
	Point p;
//	double x;
//	double y;
//	double d;
	double dy;
//	double dx;
//	double vy;
//	double vx;
//	double v;
	double t;
	
	RawPoint(Point currentpoint, double currenttime){
		p = currentpoint;
//		x = currentpoint.x;
//		y = currentpoint.y;
		t = currenttime;
	}
	
	double getdy(Point previouspoint){
		double dy = previouspoint.y - p.y;
		return dy;
	}
	
	double getdx(Point previouspoint){
		double dx = previouspoint.x - p.x;
		return dx;
	}
	
	double getd(Point previouspoint){
		double d = Math.hypot(getdy(previouspoint), getdx(previouspoint));
		return d;
	}
	
	double getvy(Point previouspoint, double previoustime){
		if (previoustime != 0){
			double vy = getdy(previouspoint)/(t-previoustime);
			return vy;
		}else{
			return 0;
		}
	}
	
	double getvx(Point previouspoint, double previoustime){
		if (previoustime != 0){
			double vx = getdy(previouspoint)/(t-previoustime);
			return vx;
		}else{
			return 0;
		}
	}
	
	double getv(Point previouspoint, double previoustime){
		if (previoustime != 0){
			double v = getd(previouspoint)/(t-previoustime);
			return v;
		}else{
			return 0;
		}
	}
	
	
}
