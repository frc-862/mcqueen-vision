#define _USE_MATH_DEFINES

#include "ballfinder.h"
#include <cmath>


const int iCols = 640;
const int iRows = 480;

const cv::Scalar iYellowThresholdsLower = cv::Scalar(20, 170, 100);// cv::Scalar(15, 100, 100); 
const cv::Scalar iYellowThresholdsUpper = cv::Scalar(30, 255, 255);// cv::Scalar(90, 255, 255); 

const float fFieldOfViewVertRadians = 43.30 * (M_PI / 180.f);
const float fFieldOfViewHorizRadians = 70.42 * (M_PI / 180.f);

const float fBallHeight = 7.f; // 2.5 for small balls

const float fMinDetectedRadius = 20.f;
const float fMaxDetectedRadius = 200.f;

//! Camera Specific Values
const float focalLength = 3.67f;
const float pixelSize = 0.00398f;
const float focal = focalLength / pixelSize;


using namespace cv;
using namespace std;

namespace bf
{

	CBallFinder::CBallFinder() { }

    int 
    CBallFinder::getMaxAreaContourId(std::vector< std::vector<cv::Point> > contours) 
	{
        double maxArea = 0;
        int maxAreaContourId = -1;
        for (size_t j = 0; j < contours.size(); j++) {
            double newArea = cv::contourArea(contours.at(j));
            if (newArea > maxArea) {
                maxArea = newArea;
                maxAreaContourId = j;
            } 
        } 
        return maxAreaContourId;
    } 

    void
    CBallFinder::work(cv::Mat & f_imgIn, ballList_t & f_listOfBalls)
    {
		m_hsvImage = cv::Mat(f_imgIn.rows, f_imgIn.cols, CV_8UC3); //??
		m_blurredImage = cv::Mat(f_imgIn.rows, f_imgIn.cols, CV_8UC3); //??
		m_maskImage = cv::Mat(f_imgIn.rows, f_imgIn.cols, CV_8UC1); //??

        //GaussianBlur(f_imgIn, m_blurredImage, cv::Size(11,11),0);
		GaussianBlur(f_imgIn, m_blurredImage, cv::Size(13, 13), 0);
        cvtColor(m_blurredImage, m_hsvImage, COLOR_BGR2HSV);

        inRange(m_hsvImage, iYellowThresholdsLower, iYellowThresholdsUpper, m_maskImage);

        m_maskImage = 255 * m_maskImage;

        cv::Mat cvErodeKernel;
        cv::Point cvErodeAnchor(-1, -1);

        erode(m_maskImage, m_maskImage, cvErodeKernel, cvErodeAnchor, 5);
        dilate(m_maskImage, m_maskImage, cvErodeKernel, cvErodeAnchor, 5);

        imshow("ballBinary", m_maskImage);

        std::vector< std::vector<cv::Point> >  contours;
        findContours(m_maskImage, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE); 

        std::vector<cv::Point2f> centers;
        float radius;

        std::vector<cv::Point> contour;
        // std::cout << "Me Got " << contours.size() << " contours" << std::endl;

        if(contours.size() > 0) 
        {
            int id;
            while(contours.size() > 0)
            {
                id = getMaxAreaContourId(contours);
                if(!(id < 0))
                {
                    contour = contours.at(id);
                    cv::Point2f center;
                    minEnclosingCircle(contour, center, radius);
                    centers.push_back(center);
                    if((radius > fMinDetectedRadius) && (radius < fMaxDetectedRadius))
                    {
                        bf::CFoundBalls ball;

						ball.center = center;

						ball.distance = ((fBallHeight / (tan((2 * radius) * (fFieldOfViewVertRadians / m_maskImage.rows)))) / 12);

						float centerCamX = m_maskImage.cols / 2.f;
						
						float angleRad = atan((ball.center.x - centerCamX) / focal);
						ball.angle = (-(angleRad * (180.f / M_PI)));

						std::cout << "Distance-> " << ball.distance << "\nAngle-> " << ball.angle << std::endl;

                        f_listOfBalls.push_back(ball);
                    }
                }
                contours.erase(contours.begin() + id);
            }       
        } 
        else 
        {
            f_listOfBalls.clear(); // Target Not Found
        }


    }

}
