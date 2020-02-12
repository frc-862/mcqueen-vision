#include "targetfinder.h"


const int iRows = 480;
const int iCols = 640;

const double dTargetHeight = 17; //INCHES
const double dTargetWidth = 39.25; //INCHES

const double dTargetAspectRatioWoH = dTargetWidth / dTargetHeight; // WIDTH over HEIGHT
const double dTollerance = 0.3; // TODO tune!!

const float fMinDetectedRadius = 20.f;
const float fMaxDetectedRadius = 200.f;


using namespace cv;
using namespace std;

namespace tf
{

	CTargetFinder::CTargetFinder()
    {
        m_hsvImage = cv::Mat(iRows, iCols, CV_8UC3);
        m_blurredImage = cv::Mat(iRows, iCols, CV_8UC3);
        m_maskImage  = cv::Mat(iRows, iCols, CV_8UC1);
		namedWindow("targetInterMed", 1);

    }

    int 
	CTargetFinder::getMaxAreaContourId(std::vector< std::vector<cv::Point> > contours) 
	{
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
	CTargetFinder::calcPerspective(cv::RotatedRect f_rectIn, tf::CFoundTargets f_targetOut) 
	{
		double ratio = f_rectIn.size.width / f_rectIn.size.height;
		if (std::abs(ratio) < dTollerance) 
		{
			//INSERT MATH HERE
			f_targetOut.center = f_rectIn.center;
		}
		else
		{
			f_targetOut.angle = 0.f;
			f_targetOut.distance = 0.f;
			f_targetOut.height = 0.f;
			cv::Point2f center;
			center.x = 0.f;
			center.y = 0.f;
			f_targetOut.center = center;
		}
		
		// target.angle = 0.f;
		// target.distance = 0.f;
		// target.height = 0.f;
		// target.center = center;
	}

    void
	CTargetFinder::work(cv::Mat & f_imgIn, std::vector<std::vector<cv::Point> > & f_contours, targetList_t & f_listOfTargets)
    {
		imshow("targetInterMed", f_imgIn);

		std::vector<cv::Point> contour;
		std::cout << "Me Got " << f_contours.size() << " target contours" << std::endl;

		if (f_contours.size() > 0)
		{
			int id;
			while (f_contours.size() > 0)
			{
				id = getMaxAreaContourId(f_contours);
				if (!(id < 0))
				{
					contour = f_contours.at(id);
					cv::RotatedRect currRect = minAreaRect(contour);

					tf::CFoundTargets target;

					calcPerspective(currRect, target);

					// target.angle = 0.f;
					// target.distance = 0.f;
					// target.height = 0.f;
					// target.center = center;

					f_listOfTargets.push_back(target);
					
				}
				f_contours.erase(f_contours.begin() + id);
			}
		}
		else
		{
			f_listOfTargets.clear(); // Target Not Found
		}


    }

}
