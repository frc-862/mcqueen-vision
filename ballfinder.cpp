#include "ballfinder.h"

const int iRows = 480;
const int iCols = 640;

const int iYellowThresholdsLower[] = {25, 100, 100};
const int iYellowThresholdUpper[] = {70, 255, 255};


using namespace cv;

namespace bf
{


CBallFinder::CBallFinder()
{
    m_hsvImage = cv::Mat(iRows, iCols, CV_8UC3);
    m_blurredImage = cv::Mat(iRows, iCols, CV_8UC3);
    m_maskImage  = cv::Mat(iRows, iCols, CV_8UC1);
    
}

void
CBallFinder::work(cv::Mat & f_imgIn, ballList_t & f_listOfBalls)
{

//here is the main part of the code
GaussianBlur(f_imgIn, m_blurredImage, cv::Size(11,11),0);
cvtColor(m_blurredImage, m_hsvImage, COLOR_BGR2HSV);

inRange(m_hsvImage, (cv::InputArray) iYellowThresholdsLower, (cv::InputArray) iYellowThresholdUpper, m_maskImage);




}






}