#pragma once
#ifndef _TARGETFINDER_H_INCLUDED
#define _TARGETFINDER_H_INCLUDED

#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/features2d.hpp>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <map>
#include <vector>
#include <string>
#include <chrono>
#include <math.h>

namespace tf
{
    struct CFoundTargets
    {
		CFoundTargets():
            angle(0.f), distance(0.f), height(0.f), center(){}

        double angle;
        double distance;
		double height;
        cv::Point2f center;
    };

    typedef std::vector<CFoundTargets> targetList_t;

    class CTargetFinder
    {
        public:
            CTargetFinder();
            int getMaxAreaContourId(std::vector< std::vector<cv::Point> > contours);
			void work(cv::Mat& f_imgIn, std::vector<std::vector<cv::Point> > & f_contours, targetList_t& f_listOfTargets);
			void calcPerspective(cv::RotatedRect f_rectIn, tf::CFoundTargets f_targetOut);

        private:
            cv::Mat m_hsvImage;
            cv::Mat m_maskImage;
            cv::Mat m_blurredImage;    

    };

}

#endif