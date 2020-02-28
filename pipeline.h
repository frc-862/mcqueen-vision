#define _USE_MATH_DEFINES

#include <cstdio>
#include <string>
#include <thread>
#include <vector>
#include <filesystem>
#include <cmath>
#include "math.h"

float fFieldOfViewVertRad = 43.30 * (M_PI / 180.f);
float fFieldOfViewHorizRad = 70.42 * (M_PI / 180.f);

float fTargetHeightRatio = 1.f; // TODO make a right number
float fTargetRatioThreshold = 100.f; // TODO make a right number
float fTargetDistanceThreshold = 0.5f;

float fTargetHeight_a = 0.0084f;
float fTargetHeight_b = -1.4737f;
float fTargetHeight_c = 76.27f;

float fTargetRow_a = -0.0056f;
float fTargetRow_b = 4.4264f;
float fTargetRow_c = -832.33f;

namespace tf
{

	struct Target {
		Target() :
			angle(0.f), distance(0.f), center(), width(0.f), height(0.f) {}

		float angle;
		float distance;
		cv::Point2f center;
		float width;
		float height;

		float area() {
			return height * width;
		}

	};

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

		float getInterpolatedDistanceFromTargetHeight(float f_targetHeight)
		{
			// distance = a(height^2) + b(height) + c
			// a, b, and c are constants derived from images at distances
			return ((fTargetHeight_a * f_targetHeight * f_targetHeight) + (fTargetHeight_b * f_targetHeight) + fTargetHeight_c);
		}

		float getInterpolatedDistanceFromTargetRow(float f_targetRow)
		{
			// distance = a(height^2) + b(height) + c
			// a, b, and c are constants derived from images at distances
			return ((fTargetRow_a * f_targetRow * f_targetRow) + (fTargetRow_b * f_targetRow) + fTargetRow_c);
		}

		float getAngleFromTarget(tf::Target f_target, int cols)
		{
            // Returns a number in degrees
			// Positive angle - right turn
			// Negative angle - left turn
			return (((f_target.center.x - (cols / 2)) / cols) * fFieldOfViewHorizRad) * (180.f / M_PI);
		}

		bool checkTargetProportion(tf::Target f_targetRect)
		{
			float ratio = (float)f_targetRect.height / (float)f_targetRect.center.y;
			return ((std::abs(ratio) - fTargetHeightRatio) < fTargetRatioThreshold);
		}

		bool distancesInBounds(float dist1, float dist2) {
			return true; // Disable to implement for testing
			// return (std::abs(dist1 - dist2) < fTargetDistanceThreshold);
		}
        
        void setConstants(float f_fieldOfViewVert, float f_fieldOfViewHoriz, float f_targetHeightRatio, 
                            float f_targetRatioThreshold, float f_targetDistanceThreshold, float f_targetHeight_a,
                            float f_targetHeight_b, float f_targetHeight_c, float f_targetRow_a, float f_targetRow_b, float f_targetRow_c) {
            fFieldOfViewHorizRad = f_fieldOfViewHoriz * (M_PI / 180.f);
            fFieldOfViewVertRad = f_fieldOfViewVert * (M_PI / 180.f);
            fTargetHeightRatio = f_targetHeightRatio;
            fTargetRatioThreshold = f_targetRatioThreshold;
            fTargetDistanceThreshold = f_targetDistanceThreshold;
            fTargetHeight_a = f_targetHeight_a;
            fTargetHeight_b = f_targetHeight_b;
            fTargetHeight_c = f_targetHeight_c;
            fTargetRow_a = f_targetRow_a;
            fTargetRow_b = f_targetRow_b;
            fTargetRow_c = f_targetRow_c;
        }

		// would rather use std::max_element; however we would recalc
		// boundingRect too often, mapping to boundingRect first would
		// waste memory and use extra ram, so we do it by hand
		//
		// TODO: Use height of bounding box, as a ratio to height of 
		// contour to filter
		bool findBestTarget(tf::Target& target) {
			auto contours = GetContours();
			if (contours->size() == 0) return false;

			// target = cv::Rect(0, 0, 0, 0);
			target = tf::Target();

			for (const auto object : *contours) {
				auto rect = cv::boundingRect(object);
				if ((rect.area() > target.area())) {
					tf::Target temp;
					temp.height = rect.height;
					temp.center.x = (rect.x + (rect.width / 2));
					temp.center.y = (rect.y + (rect.height / 2));
					if (checkTargetProportion(temp)) {
						float tempDist = getInterpolatedDistanceFromTargetHeight(temp.height);
						float tempDistTest = getInterpolatedDistanceFromTargetRow((rect.y + rect.height)); // Bottom Row of Target
						if (distancesInBounds(tempDist, tempDistTest)) {
							target.distance = tempDist;
							target.height = rect.height;
							target.width = rect.width;
							target.center.x = (rect.x + (rect.width / 2));
							target.center.y = (rect.y + (rect.height / 2));
							target.angle = getAngleFromTarget(target, GetSource()->cols);
						}
					}
				}
			}
			return true;
		}

		unsigned long GetDuration() { return elapsed; }
	private:
		cv::Mat source;
		double elapsed;
	};

}