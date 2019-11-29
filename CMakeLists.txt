cmake_minimum_required(VERSION 3.15.0)
project(lightningVision)

#--------------------------------------------------------------------------

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

#--------------------------------------------------------------------------

add_library(SSD1306 STATIC libSSD1306/FileDescriptor.cxx
						   libSSD1306/OledHardware.cxx
						   libSSD1306/OledPixel.cxx
						   libSSD1306/OledFont8x8.cxx
						   libSSD1306/OledFont8x12.cxx
						   libSSD1306/OledFont8x16.cxx
						   libSSD1306/OledGraphics.cxx
						   libSSD1306/OledI2C.cxx)

include_directories(${PROJECT_SOURCE_DIR}/libSSD1306lib)

#--------------------------------------------------------------------------

add_executable(lightningVision main.cpp FilterOne.cpp)
set_target_properties(lightningVision PROPERTIES CXX_STANDARD 17)

target_link_libraries(lightningVision SSD1306 wpilibc wpiHal cameraserver ntcore cscore opencv_dnn opencv_highgui opencv_ml opencv_objdetect opencv_shape opencv_stitching opencv_superres opencv_videostab opencv_calib3d opencv_videoio opencv_imgcodecs opencv_features2d opencv_video opencv_photo opencv_imgproc opencv_flann opencv_core wpiutil atomic stdc++fs Threads::Threads opencv_dnn)

