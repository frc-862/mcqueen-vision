export PATH := /Users/phurley/Downloads/raspbian10/bin:$(PATH)

CXX=/Users/phurley/Downloads/raspbian10/bin/arm-raspbian10-linux-gnueabihf-g++
DEPS_CFLAGS=-Iinclude -Iinclude/opencv -Iinclude -std=c++17 -Wno-psabi
DEPS_LIBS=-Llib -lwpilibc -lwpiHal -lcameraserver -lntcore -lcscore -lopencv_dnn -lopencv_highgui -lopencv_ml -lopencv_objdetect -lopencv_shape -lopencv_stitching -lopencv_superres -lopencv_videostab -lopencv_calib3d -lopencv_videoio -lopencv_imgcodecs -lopencv_features2d -lopencv_video -lopencv_photo -lopencv_imgproc -lopencv_flann -lopencv_core -lwpiutil -latomic -lstdc++fs

EXE=lightningVision
DESTDIR?=/home/pi/

.PHONY: clean build install

build: ${EXE}

install: build
	cp ${EXE} runCamera ${DESTDIR}

deploy: build
	scp ${EXE} pi@frcvision.local:

clean:
	rm ${EXE} *.o

main.o: main.cpp safe_queue.h FilterOne.h 

OBJS=main.o FilterOne.o

${EXE}: ${OBJS}
	${CXX} -pthread -g -o $@ $^ ${DEPS_LIBS} -Wl,--unresolved-symbols=ignore-in-shared-libs

.cpp.o:
	${CXX} -pthread -g -Og -c -o $@ ${CXXFLAGS} ${DEPS_CFLAGS} $<
