#pragma once

#include <cstdio>
#include <string>
#include <thread>
#include <vector>
#include <filesystem>

#include "InfiniteRecharge.h"

class Pipeline : public grip::InfiniteRecharge {
public:
    Pipeline() { }

    void Process(cv::Mat& mat) override {
      auto start = std::chrono::steady_clock::now();
      source = mat;
      grip::InfiniteRecharge::Process(mat);
      auto end = std::chrono::steady_clock::now();
      elapsed = std::chrono::duration_cast<std::chrono::microseconds>(end - start).count();
    }

	cv::Mat* GetSource() {
	    return &(this->source);
    }

    cv::Mat* GetMasked() {
      return GetCvErodeOutput();
    }

	std::vector<std::vector<cv::Point> >* GetContours() {
      return GetFilterContoursOutput();
    }


    // would rather use std::max_element; however we would recalc
    // boundingRect too often, mapping to boundingRect first would
    // waste memory and use extra ram, so we do it by hand
    //
    // TODO: Use height of bounding box, as a ratio to height of 
    // contour to filter
    bool findBestTarget(cv::Rect & target) {
        auto contours = GetContours();
        if(contours->size() == 0) return false;

        target = cv::Rect(0,0,0,0);
        
        for (const auto object : *contours) {
            auto rect = cv::boundingRect(object);
            if (rect.area() > target.area()) {
                target = rect;
            }
        }

        return true;
    }

    unsigned long GetDuration() { return elapsed; }
private:
		cv::Mat source;
    double elapsed;
};