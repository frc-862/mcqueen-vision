/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

#include <cstdio>
#include <string>
#include <thread>
#include <vector>
#include <filesystem>

#include <networktables/NetworkTableInstance.h>
#include <vision/VisionPipeline.h>
#include <vision/VisionRunner.h>
#include <wpi/StringRef.h>
#include <wpi/json.h>
#include <wpi/raw_istream.h>
#include <wpi/raw_ostream.h>
#include <opencv2/opencv.hpp>

#include "cameraserver/CameraServer.h"
#include "InfiniteRecharge.h"
#include "ballfinder.h"
#include "safe_queue.h"
#include "pipeline.h"

namespace fs = std::filesystem;

const int width = 640;
const int height = 480;
const int halfWidth = width / 2;
const int halfHeight = height / 2;


/*
   JSON format:
   {
       "team": <team number>,
       "ntmode": <"client" or "server", "client" if unspecified>
       "cameras": [
           {
               "name": <camera name>
               "path": <path, e.g. "/dev/video0">
               "pixel format": <"MJPEG", "YUYV", etc>   // optional
               "width": <video mode width>              // optional
               "height": <video mode height>            // optional
               "fps": <video mode fps>                  // optional
               "brightness": <percentage brightness>    // optional
               "white balance": <"auto", "hold", value> // optional
               "exposure": <"auto", "hold", value>      // optional
               "properties": [                          // optional
                   {
                       "name": <property name>
                       "value": <property value>
                   }
               ],
               "stream": {                              // optional
                   "properties": [
                       {
                           "name": <stream property name>
                           "value": <stream property value>
                       }
                   ]
               }
           }
       ]
       "switched cameras": [
           {
               "name": <virtual camera name>
               "key": <network table key used for selection>
               // if NT value is a string, it's treated as a name
               // if NT value is a double, it's treated as an integer index
           }
       ]
   }
 */

static const char* configFile = "/boot/frc.json";

struct ImageInfo {
  const char* tag;
  int x;
  int y;
  int height;
  size_t count;
};

static SafeQueue<std::pair<cv::Mat,ImageInfo>> loggingQueue;

namespace {

bool debug = false;
unsigned int team;
bool server = false;

struct CameraConfig {
    std::string name;
    std::string path;
    wpi::json config;
    wpi::json streamConfig;
};

struct SwitchedCameraConfig {
    std::string name;
    std::string key;
};

std::vector<CameraConfig> cameraConfigs;
std::vector<SwitchedCameraConfig> switchedCameraConfigs;
std::vector<cs::VideoSource> cameras;

wpi::raw_ostream& ParseError() {
    return wpi::errs() << "config error in '" << configFile << "': ";
}

bool ReadCameraConfig(const wpi::json& config) {
    CameraConfig c;

    // name
    try {
        c.name = config.at("name").get<std::string>();
    } catch (const wpi::json::exception& e) {
        ParseError() << "could not read camera name: " << e.what() << '\n';
        return false;
    }

    // path
    try {
        c.path = config.at("path").get<std::string>();
    } catch (const wpi::json::exception& e) {
        ParseError() << "camera '" << c.name
                     << "': could not read path: " << e.what() << '\n';
        return false;
    }

    // stream properties
    if (config.count("stream") != 0) c.streamConfig = config.at("stream");

    c.config = config;

    cameraConfigs.emplace_back(std::move(c));
    return true;
}

bool ReadSwitchedCameraConfig(const wpi::json& config) {
    SwitchedCameraConfig c;

    // name
    try {
        c.name = config.at("name").get<std::string>();
    } catch (const wpi::json::exception& e) {
        ParseError() << "could not read switched camera name: " << e.what() << '\n';
        return false;
    }

    // key
    try {
        c.key = config.at("key").get<std::string>();
    } catch (const wpi::json::exception& e) {
        ParseError() << "switched camera '" << c.name
                     << "': could not read key: " << e.what() << '\n';
        return false;
    }

    switchedCameraConfigs.emplace_back(std::move(c));
    return true;
}

bool ReadConfig() {
    // open config file
    std::error_code ec;
    wpi::raw_fd_istream is(configFile, ec);
    if (ec) {
        wpi::errs() << "could not open '" << configFile << "': " << ec.message()
                    << '\n';
        return false;
    }

    // parse file
    wpi::json j;
    try {
        j = wpi::json::parse(is);
    } catch (const wpi::json::parse_error& e) {
        ParseError() << "byte " << e.byte << ": " << e.what() << '\n';
        return false;
    }

    // top level must be an object
    if (!j.is_object()) {
        ParseError() << "must be JSON object\n";
        return false;
    }

    // team number
    try {
        team = j.at("team").get<unsigned int>();
    } catch (const wpi::json::exception& e) {
        ParseError() << "could not read team number: " << e.what() << '\n';
        return false;
    }

    // debug mode
    try {
        debug = j.at("debug").get<bool>();
    } catch (const wpi::json::exception& e) {
        debug = false;
    }

    // ntmode (optional)
    if (j.count("ntmode") != 0) {
        try {
            auto str = j.at("ntmode").get<std::string>();
            wpi::StringRef s(str);
            if (s.equals_lower("client")) {
                server = false;
            } else if (s.equals_lower("server")) {
                server = true;
            } else {
                ParseError() << "could not understand ntmode value '" << str << "'\n";
            }
        } catch (const wpi::json::exception& e) {
            ParseError() << "could not read ntmode: " << e.what() << '\n';
        }
    }

    // cameras
    try {
        for (auto&& camera : j.at("cameras")) {
            if (!ReadCameraConfig(camera)) return false;
        }
    } catch (const wpi::json::exception& e) {
        ParseError() << "could not read cameras: " << e.what() << '\n';
        return false;
    }

    // switched cameras (optional)
    if (j.count("switched cameras") != 0) {
        try {
            for (auto&& camera : j.at("switched cameras")) {
                if (!ReadSwitchedCameraConfig(camera)) return false;
            }
        } catch (const wpi::json::exception& e) {
            ParseError() << "could not read switched cameras: " << e.what() << '\n';
            return false;
        }
    }

    return true;
}

cs::UsbCamera StartCamera(const CameraConfig& config) {
    wpi::outs() << "Starting camera '" << config.name << "' on " << config.path
                << '\n';
    auto inst = frc::CameraServer::GetInstance();
    cs::UsbCamera camera{config.name, config.path};
    auto server = inst->StartAutomaticCapture(camera);

    camera.SetConfigJson(config.config);
    camera.SetConnectionStrategy(cs::VideoSource::kConnectionKeepOpen);

    if (config.streamConfig.is_object())
        server.SetConfigJson(config.streamConfig);

    return camera;
}

cs::MjpegServer StartSwitchedCamera(const SwitchedCameraConfig& config) {
    wpi::outs() << "Starting switched camera '" << config.name << "' on "
                << config.key << '\n';
    auto server =
        frc::CameraServer::GetInstance()->AddSwitchedCamera(config.name);

    nt::NetworkTableInstance::GetDefault()
    .GetEntry(config.key)
    .AddListener(
    [server](const auto& event) mutable {
        if (event.value->IsDouble()) {
            size_t i = event.value->GetDouble();
            if (i >= 0 && i < cameras.size()) server.SetSource(cameras[i]);
        } else if (event.value->IsString()) {
            auto str = event.value->GetString();
            for (size_t i = 0; i < cameraConfigs.size(); ++i) {
                if (str == cameraConfigs[i].name) {
                    server.SetSource(cameras[i]);
                    break;
                }
            }
        }
    },
    NT_NOTIFY_IMMEDIATE | NT_NOTIFY_NEW | NT_NOTIFY_UPDATE);

    return server;
}

// wrapper pipeline
class BallPipeline {
public:
    bf::ballList_t result;
    void Process(cv::Mat& mat) {
        finder.work(mat, result);
    }
    void setConstants(int yellowLowerH, int yellowLowerS, int yellowLowerV,
                        int yellowUpperH, int yellowUpperS, int yellowUpperV,
                        float minDetectedRadius, float maxDetectedRadius, float fieldOfViewVert,
                        float fieldOfViewHoriz, float focalLength, float pixelSize)
    {
        finder.setConstants(cv::Scalar(yellowLowerH, yellowLowerS, yellowLowerV), cv::Scalar(yellowUpperH, yellowUpperS, yellowUpperV), 
                            minDetectedRadius, maxDetectedRadius, fieldOfViewVert, fieldOfViewHoriz, focalLength, pixelSize);
    }

private: 
    bf::CBallFinder finder;
    int i = 0;
};


}  // namespace

std::string meta_data_name(int index, std::string path, const ImageInfo& info, std::string suffix = ".json") {
    int leading = 5; //6 at max
    return path + "/" + std::string(info.tag) + "-" + 
           std::to_string(index * 0.000001).substr(8-leading) + suffix;
}

std::string image_name(int index, std::string path, const ImageInfo& info, std::string suffix = ".jpg") {
    int leading = 5; //6 at max
    return path + "/" + std::string(info.tag) + "-" + 
           std::to_string(index * 0.000001).substr(8-leading) + suffix;
}

std::string image_name(int index, std::string prefix = "image", std::string suffix = ".jpg") {
    int leading = 5; //6 at max
    return prefix + std::to_string(index * 0.000001).substr(8-leading) + suffix;
}

int main(int argc, char* argv[]) {

    std::cout << "\n################# APPLICATION RUNNING #################\n" << std::endl;

    if (argc >= 2) configFile = argv[1];

    // read configuration
    std::cout << "Reading Config File" << std::endl;
    if (!ReadConfig()) return EXIT_FAILURE;
    std::cout << "Config File Read Successfully" << std::endl;

    // start NetworkTables
    auto ntinst = nt::NetworkTableInstance::GetDefault();
    if (server) {
        wpi::outs() << "Setting up NetworkTables server\n";
        ntinst.StartServer();
        std::cout << "Server Set Up Successfully" << std::endl;
    } else {
        wpi::outs() << "Setting up NetworkTables client for team " << team << '\n';
        ntinst.StartClientTeam(team);
        std::cout << "Client Set Up Successfully" << std::endl;
    }

    // start cameras
    for (const auto& config : cameraConfigs)
        cameras.emplace_back(StartCamera(config));

    // start switched cameras
    for (const auto& config : switchedCameraConfigs) StartSwitchedCamera(config);

    std::cout << "Cameras Started Successfully" << std::endl;

    // start image processing on camera 0 if present
    int x, y, height, count;
    double elapsed;
    cs::CvSource source;
    cs::CvSource fout;
    cv::Scalar blue(255, 0, 0);

    if (debug) {
      source = frc::CameraServer::GetInstance()->PutVideo("debug", 640, 480);
      fout = frc::CameraServer::GetInstance()->PutVideo("fout", 640, 480);
      std::cout << "Debug Output Set" << std::endl;
    }
 
    if (cameras.size() >= 1) {
        std::cout << "Creating Target Pipeline and Logging" << std::endl;
        std::thread([&] {
            bool log_images = fs::exists("/mnt/log/img");
            int counter = 0;
            auto ntab = ntinst.GetTable("SmartDashboard");

            frc::VisionRunner<tf::Pipeline> runner(cameras[0], new tf::Pipeline(),
            [&](tf::Pipeline &pipeline) {

                if(counter == 0) {
                    ntab->PutNumber("FieldOfViewVert", 43.30f);
                    ntab->PutNumber("FieldOfViewHoriz", 70.42f);
                    ntab->PutNumber("TargetHeightRatio", 1.f);
                    ntab->PutNumber("TargetRatioThreshold", 100.f);
                    ntab->PutNumber("TargetDistanceThreshold", 0.5f);
                    ntab->PutNumber("TargetHeightAConstant", 0.0084f);
                    ntab->PutNumber("TargetHeightBConstant", -1.4737f);
                    ntab->PutNumber("TargetHeightCConstant", 76.27f);
                    ntab->PutNumber("TargetRowAConstant", -0.0056f);
                    ntab->PutNumber("TargetRowBConstant", 4.4264f);
                    ntab->PutNumber("TargetRowCConstant", -832.33f);
                }

                auto start = std::chrono::steady_clock::now();

                // do something with pipeline results
                const auto& contours = *pipeline.GetContours();
                if (debug) source.PutFrame(*pipeline.GetMasked());

                auto& smooth = contours;
                count = smooth.size();

                if (debug) {
                  auto fun = pipeline.GetSource()->clone();
                  drawContours(fun, smooth, -1, blue, 2);
                  std::string msg{"Count: "};
                  msg += std::to_string(count);
                  putText(fun, msg, cvPoint(30,30), cv::FONT_HERSHEY_COMPLEX_SMALL, 0.8, 
                      cvScalar(200,200,250), 1, CV_AA);
                  fout.PutFrame(fun);
                }

                ntab->PutNumber("VisionFound", count);

                tf::Target target;

                //if (pipeline.findBestTarget(target)) {
                if (!pipeline.findBestTarget(target)) {
                    ntab->PutNumber("VisionX", x = 0);
                    ntab->PutNumber("VisionY", y = 0);
                    ntab->PutNumber("VisionHeight", 0);
                    ntab->PutNumber("VisionDistance", 0);
                    ntab->PutNumber("VisionAngle", 0);
                    ntab->PutNumber("VisionDelay", pipeline.GetDuration());

                } else {
                    // const auto center = target.center;
                    // x = center.x - halfWidth;
                    // y = halfHeight - center.y;
                    // height = target.height;

                    ntab->PutNumber("VisionX", target.center.x);
                    ntab->PutNumber("VisionY", target.center.y);
                    ntab->PutNumber("VisionHeight", target.height);
                    ntab->PutNumber("VisionDistance", target.distance);
                    ntab->PutNumber("VisionAngle", target.angle);
                    ntab->PutNumber("VisionDelay", pipeline.GetDuration() + elapsed);
                }

                counter++;
                if ((counter % 90) && log_images == true) {
                    ImageInfo info = { "src", x, y, height, smooth.size() };
                    loggingQueue.push(std::make_pair(*pipeline.GetSource(), info));
                    ImageInfo info2 = { "mask", x, y, height, smooth.size() };
                    loggingQueue.push(std::make_pair(*pipeline.GetMasked(), info2));
                }

                if(counter % 90 == 0){ // Update about once a second
                    pipeline.setConstants(
                        ntab->GetNumber("FieldOfViewVert", 43.30f),
                        ntab->GetNumber("FieldOfViewHoriz", 70.42f),
                        ntab->GetNumber("TargetHeightRatio", 1.f),
                        ntab->GetNumber("TargetRatioThreshold", 100.f),
                        ntab->GetNumber("TargetDistanceThreshold", 0.5f),
                        ntab->GetNumber("TargetHeightAConstant", 0.0084f),
                        ntab->GetNumber("TargetHeightBConstant", -1.4737f),
                        ntab->GetNumber("TargetHeightCConstant", 76.27f),
                        ntab->GetNumber("TargetRowAConstant", -0.0056f),
                        ntab->GetNumber("TargetRowBConstant", 4.4264f),
                        ntab->GetNumber("TargetRowCConstant", -832.33f)
                    );
                    std::cout << counter << "\n##### Target Acquired #####\n"
				        << "(X,Y)-> (" << target.center.x << " , " << target.center.y << ")\n"
				        << "Width-> " << target.width << "\n"
				        << "Height-> " << target.height << "\n"
				        << "Angle-> " << target.angle << "\n"
				        << "Distance-> " << target.distance
				        << std::endl;
                }
                

                auto end = std::chrono::steady_clock::now();
                elapsed = std::chrono::duration_cast<std::chrono::microseconds>(end - start).count();
            });

            runner.RunForever();
        }).detach();        
        std::cout << "Target Pipeline Detached" << std::endl;

        std::thread([] {
            const fs::path log_path("/mnt/log/img");
            bool log_images = fs::exists(log_path);
            int index = 1;
            if (log_images) { 
                std::cerr << "Logging Images\n";

                for (auto& p : fs::directory_iterator(log_path)) {
                    if (p.is_block_file()) ++index;
                }

                while (fs::exists(image_name(index, log_path))) {
                    ++index;
                }

                std::cerr << "Index at " << index << "\n";
                for(;;) {
                    auto info_pair = loggingQueue.shift();
                    const auto& img = info_pair.first;
                    const auto& info = info_pair.second;

                    std::ofstream meta(meta_data_name(index, log_path, info));
                    meta << "{\"tag\":\"" << info.tag << "\",\"x\":" << info.x <<
                            ",\"y\":" << info.y << ",\"height\":" << info.height <<
                            ",\"count\":" << info.count << "}";
                    meta.close();

                    auto fname { image_name(index++, log_path, info) };
                    cv::imwrite(fname, img);
                }
            }

        }).detach();
        std::cout << "Logging Detached" << std::endl;
    }

    if (cameras.size() >= 2) {
        std::cout << "Creating Ball Pipeline" << std::endl;
        std::thread([&] {
            auto ntab = ntinst.GetTable("SmartDashboard");
            int pipelineCount = 0;

            frc::VisionRunner<BallPipeline> runner(cameras[1], new BallPipeline(),
            [&](BallPipeline &pipeline) {
                if(pipelineCount == 0) {
                    (int) ntab->PutNumber("YellowLowerH", 20);//lower: cv::Scalar(20, 170, 100), upper: cv::Scalar(30, 255, 255)
                    (int) ntab->PutNumber("YellowLowerS", 170);
                    (int) ntab->PutNumber("YellowLowerV", 100);
                    (int) ntab->PutNumber("YellowUpperH", 30);
                    (int) ntab->PutNumber("YellowUpperS", 255);
                    (int) ntab->PutNumber("YellowUpperV", 255);
                    ntab->PutNumber("MinBallRadius", 20.f);
                    ntab->PutNumber("MaxBallRadius", 200.f);
                    ntab->PutNumber("FieldOfViewVert", 43.30f);
                    ntab->PutNumber("FieldOfViewHoriz", 70.42f);
                    ntab->PutNumber("FocalLength", 3.67f);
                    ntab->PutNumber("PixelSize",  0.00398f);
                }
                pipelineCount++;
                if(pipelineCount % 90 == 0){ // Update about once a second
                    pipeline.setConstants(
                        (int) ntab->GetNumber("YellowLowerH", 20),//lower: cv::Scalar(20, 170, 100), upper: cv::Scalar(30, 255, 255)
                        (int) ntab->GetNumber("YellowLowerS", 170),
                        (int) ntab->GetNumber("YellowLowerV", 100),
                        (int) ntab->GetNumber("YellowUpperH", 30),
                        (int) ntab->GetNumber("YellowUpperS", 255),
                        (int) ntab->GetNumber("YellowUpperV", 255),
                        ntab->GetNumber("MinBallRadius", 20.f),
                        ntab->GetNumber("MaxBallRadius", 200.f),
                        ntab->GetNumber("FieldOfViewVert", 43.30f),
                        ntab->GetNumber("FieldOfViewHoriz", 70.42f),
                        ntab->GetNumber("FocalLength", 3.67f),
                        ntab->GetNumber("PixelSize",  0.00398f)
                    );
                }
                if(pipeline.result.empty()){
                    ntab->PutNumber("NumBalls", 0);
                    ntab->PutNumber("ClosestBallCenterX", 0);
                    ntab->PutNumber("ClosestBallCenterY", 0);
                    ntab->PutNumber("ClosestBallAngle", 0);
                    ntab->PutNumber("ClosestBallDistance", 0);
                } else {
                    ntab->PutNumber("NumBalls", pipeline.result.size());
                    ntab->PutNumber("ClosestBallCenterX", pipeline.result.at(0).center.x);
                    ntab->PutNumber("ClosestBallCenterY", pipeline.result.at(0).center.x);
                    ntab->PutNumber("ClosestBallAngle", pipeline.result.at(0).angle);
                    ntab->PutNumber("ClosestBallDistance", pipeline.result.at(0).distance);
                }  
            });

            runner.RunForever();
        }).detach();
        std::cout << "Ball Pipeline Detached" << std::endl;
    }
    for(;;); // TODO there is probably a better way to do this ... oh well
    std::cout << "\n################# MAIN THREAD TERMINATING #################\n" << std::endl;
}

