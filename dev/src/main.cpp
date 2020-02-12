#include "opencv2/imgproc.hpp"
#include "opencv2/videoio.hpp"
#include "opencv2/highgui.hpp"
#include "opencv2/video/background_segm.hpp"
#include <stdio.h>
#include <string>
#include "ballfinder.h"
#include "InfiniteRecharge.h"
#include "targetfinder.h"

using namespace std;
using namespace cv;

static void help()
{
    printf("\n"
            "This program demonstrated a simple method of connected components clean up of background subtraction\n"
            "When the program starts, it begins learning the background.\n"
            "You can toggle background learning on and off by hitting the space bar.\n"
            "Call\n"
            "./segment_objects [video file, else it reads camera 0]\n\n");
}

int main(int argc, char** argv)
{
    VideoCapture cap;
    bool update_bg_model = true;
    bf::CBallFinder finder;
	tf::CTargetFinder tFinder;
	grip::InfiniteRecharge gripPipeline;

    CommandLineParser parser(argc, argv, "{help h||}{@input||}");
    if (parser.has("help"))
    {
        help();
        return 0;
    }
    string input = parser.get<std::string>("@input");
    if (input.empty())
        cap.open(0);
    else
        cap.open(samples::findFileOrKeep(input));

    if( !cap.isOpened() )
    {
        printf("\nCan not open camera or video file\n");
        return -1;
    }

    Mat tmp_frame, bgmask, out_frame;

    cap >> tmp_frame;
    if(tmp_frame.empty())
    {
        printf("can not read data from the video source\n");
        return -1;
    }

    namedWindow("video", 1);
    namedWindow("targetPipeline", 1);
    
    for(;;)
    {
        cap >> tmp_frame;
        if( tmp_frame.empty() )
            break;

        /////////////////////////////////////////////////////////////////////////////


        bf::ballList_t balls;
		tf::targetList_t targets;


        finder.work(tmp_frame, balls);

		gripPipeline.Process(tmp_frame);
		tFinder.work(tmp_frame, * gripPipeline.GetFindContoursOutput(), targets);
		
		// imshow("targetPipeline", * gripPipeline.GetHsvThresholdOutput());


        /////////////////////////////////////////////////////////////////////////////

        imshow("video", tmp_frame);

        char keycode = (char)waitKey(30);
        if( keycode == 27 )
            break;
        if( keycode == ' ' )
        {
            update_bg_model = !update_bg_model;
            printf("Learn background is in state = %d\n",update_bg_model);
        }
    }

    return 0;
}
