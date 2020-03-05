cmake_minimum_required(VERSION 3.16)
project( utils )

FIND_PACKAGE( OpenCV REQUIRED )

include_directories(AFTER ${PROJECT_SOURCE_DIR}/include ..)

add_executable( analyze analyze.cpp ../InfiniteRecharge.cpp )
set_target_properties( analyze PROPERTIES CXX_STANDARD 20 )
target_compile_options( analyze PRIVATE -Werror -Wall )

target_link_libraries( analyze ${OpenCV_LIBS} )

