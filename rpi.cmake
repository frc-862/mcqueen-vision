# Define our host system
SET(CMAKE_SYSTEM_NAME Linux)
SET(CMAKE_SYSTEM_VERSION 1)

# Define the cross compiler locations
if(DEFINED ENV{ARM_HOME})
  SET(CMAKE_C_COMPILER $ENV{ARM_HOME}/bin/arm-raspbian10-linux-gnueabihf-gcc)
  SET(CMAKE_CXX_COMPILER $ENV{ARM_HOME}/bin/arm-raspbian10-linux-gnueabihf-g++)
  SET(CMAKE_FIND_ROOT_PATH $ENV{ARM_HOME}/sys-root)
else()
  # I wrote it, I get to pick the defaults
  MESSAGE(FATAL_ERROR "Please set an environement variable named ARM_HOME\nLinux:\nexport ARM_HOME=/Users/phurley/wpilib/raspbian10\nWindows:\nSET ARM_HOME=c:\\wpilib\\raspbian10\n before generating the project")
endif()

# Use our definitions for compiler tools
SET(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)

# Search for libraries and headers in the target directories only
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)

add_link_options(-Wl,--unresolved-symbols=ignore-in-shared-libs)
add_compile_options(-Wall -Wno-psabi -std=c++2a)

set(CMAKE_BUILD_TYPE Release)

#set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${PROJECT_BINARY_DIR}/lib)
#set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${PROJECT_BINARY_DIR}/bin)

include_directories(BEFORE SYSTEM include include/opencv)
link_directories(lib)
# --unresolved-symbols=ignore-in-shared-libs


