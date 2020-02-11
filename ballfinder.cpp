#include "ballfinder.h"


const int iRows = 480;
const int iCols = 640;


const cv::Scalar iYellowThresholdsLower = cv::Scalar(15, 100, 100);  // cv::Scalar(25, 100, 100);
const cv::Scalar iYellowThresholdsUpper = cv::Scalar(90, 255, 255);  // cv::Scalar(70, 255, 255);


const float fMinDetectedRadius = 20.f;
const float fMaxDetectedRadius = 200.f;


using namespace cv;
using namespace std;

namespace bf
{

    CBallFinder::CBallFinder()
    {
        m_hsvImage = cv::Mat(iRows, iCols, CV_8UC3);
        m_blurredImage = cv::Mat(iRows, iCols, CV_8UC3);
        m_maskImage  = cv::Mat(iRows, iCols, CV_8UC1);

    }

    int 
    CBallFinder::getMaxAreaContourId(std::vector< std::vector<cv::Point> > contours) {
        double maxArea = 0;
        int maxAreaContourId = -1;
        for (size_t j = 0; j < contours.size(); j++) {
            double newArea = cv::contourArea(contours.at(j));
            if (newArea > maxArea) {
                maxArea = newArea;
                maxAreaContourId = j;
            } // End if
        } // End for
        return maxAreaContourId;
    } // End function

    void
    CBallFinder::work(cv::Mat & f_imgIn, ballList_t & f_listOfBalls)
    {
        // std::cout << "work" << endl;

        //here is the main part of the code
        GaussianBlur(f_imgIn, m_blurredImage, cv::Size(11,11),0);
        cvtColor(m_blurredImage, m_hsvImage, COLOR_BGR2HSV);

        inRange(m_hsvImage, iYellowThresholdsLower, iYellowThresholdsUpper, m_maskImage);

        m_maskImage = 255 * m_maskImage;

        cv::Mat cvErodeKernel;
        cv::Point cvErodeAnchor(-1, -1);

        erode(m_maskImage, m_maskImage, cvErodeKernel, cvErodeAnchor, 5);
        dilate(m_maskImage, m_maskImage, cvErodeKernel, cvErodeAnchor, 5);

        std::vector< std::vector<cv::Point> >  contours;
        findContours(m_maskImage, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE); // m_maskImage.clone() ???

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
                        ball.angle = 0.f;
                        ball.distance = 0.f;
                        ball.center = center;
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
