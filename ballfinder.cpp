#include "ballfinder.h"

const int iRows = 480;
const int iCols = 640;

const int iYellowThresholdsLower[] = {25, 100, 100};
const int iYellowThresholdUpper[] = {70, 255, 255};


using namespace cv;
using namespace std;

namespace bf
{


    CBallFinder::CBallFinder()
    {
        m_hsvImage = cv::Mat(iRows, iCols, CV_8UC3);
        m_blurredImage = cv::Mat(iRows, iCols, CV_8UC3);
        m_maskImage  = cv::Mat(iRows, iCols, CV_8UC1);
        // m_cnts = cv::Mat(iRows, iCols, CV_8UC3);

    }

    void
    CBallFinder::work(cv::Mat & f_imgIn, ballList_t & f_listOfBalls)
    {

        //here is the main part of the code
        GaussianBlur(f_imgIn, m_blurredImage, cv::Size(11,11),0);
        cvtColor(m_blurredImage, m_hsvImage, COLOR_BGR2HSV);

        inRange(m_hsvImage, (cv::InputArray) iYellowThresholdsLower, (cv::InputArray) iYellowThresholdUpper, m_maskImage);

        cv::Mat cvErodeKernel;
        cv::Point cvErodeAnchor(-1, -1);
        double cvErodeIterations = 1.0;  // default Double
        int cvErodeBordertype = cv::BORDER_CONSTANT;

        erode(m_maskImage, m_maskImage, cvErodeKernel, cvErodeAnchor, 5);
        dilate(m_maskImage, m_maskImage, cvErodeKernel, cvErodeAnchor, 5);

        std::vector< std::vector<cv::Point> >  cnts;
        findContours(m_maskImage.clone(), cnts, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);

        if(cnts.size() > 0) 
        {
            /*
            c = max(cnts, key=cv2.contourArea)
            ((x, y), radius) = cv2.minEnclosingCircle(c)
            M = cv2.moments(c)
            */ 
        } 
        else 
        {
            // Target Not Found
        }


    }

    std::vector< std::vector<cv::Point> >
    grab_contours(std::vector< std::vector<cv::Point> > f_cnts)
    {
        return f_cnts; // TODO implement 
    }






}