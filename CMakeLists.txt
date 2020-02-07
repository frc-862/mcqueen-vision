cmake_minimum_required(VERSION 3.15.0)
project(lightningVision)

#--------------------------------------------------------------------------

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

#--------------------------------------------------------------------------

include_directories(AFTER ${PROJECT_SOURCE_DIR}/libSSD1306)
add_executable(lightningVision main.cpp InfiniteRecharge.cpp)
set_target_properties(lightningVision PROPERTIES CXX_STANDARD 17)
target_compile_options(lightningVision PRIVATE -Werror -Wall)

target_link_libraries(lightningVision wpilibc wpiHal cameraserver ntcore cscore opencv_dnn opencv_highgui opencv_ml opencv_objdetect opencv_shape opencv_stitching opencv_superres opencv_videostab opencv_calib3d opencv_videoio opencv_imgcodecs opencv_features2d opencv_video opencv_photo opencv_imgproc opencv_flann opencv_core wpiutil atomic stdc++fs Threads::Threads opencv_dnn)

