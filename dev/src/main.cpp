#include "opencv2/imgproc.hpp"
#include "opencv2/videoio.hpp"
#include "opencv2/highgui.hpp"
#include "opencv2/video/background_segm.hpp"
#include <stdio.h>
#include <string>
#include "ballfinder.h"
#include "InfiniteRecharge.h"
#include "../../mcqueen-vision/pipeline.h"

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
	bool video = true;
    VideoCapture cap;
    bool update_bg_model = true;
    bf::CBallFinder finder;
	grip::InfiniteRecharge gripPipeline;
	tf::Pipeline pipeline;

    CommandLineParser parser(argc, argv, "{help h||}{@input||}");
    if (parser.has("help"))
    {
        help();
        return 0;
    }
    string input = parser.get<std::string>("@input");
	if (input.empty()) {
		cap.open(0);
		// cap.open(1);
	}
	else
	{
		cap.open(samples::findFileOrKeep(input));
		video = false; // C:\Users\edurso\frc\_2020_robotcode\imageTarget25ft.jpg
	}

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

    namedWindow("stream", 1);
    
    for(;;)
    {
		if (video) {
			cap >> tmp_frame;
		}
        if( tmp_frame.empty() )
            break;

        /////////////////////////////////////////////////////////////////////////////

        bf::ballList_t balls;
        finder.work(tmp_frame, balls);

		// cv::Rect target;
		tf::Target target;
		pipeline.Process(tmp_frame);
		if (pipeline.findBestTarget(target)) {
			std::cout << "##### Target Acquired #####\n"
				<< "(X,Y)-> (" << target.center.x << " , " << target.center.y << ")\n"
				<< "Width-> " << target.width << "\n"
				<< "Height-> " << target.height << "\n"
				<< "Angle-> " << target.angle << "\n"
				<< "Distance-> " << target.distance
				<< std::endl;
		}

        /////////////////////////////////////////////////////////////////////////////

        imshow("stream", tmp_frame);

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
