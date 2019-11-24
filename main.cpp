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
#include "FilterOne.h"
#include "safe_queue.h"

namespace fs = std::filesystem;

const int width = 640;
const int height = 480; 

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
static SafeQueue<std::pair<cv::Mat,int>> loggingQueue;

namespace {

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
            int i = event.value->GetDouble();
            if (i >= 0 && i < cameras.size()) server.SetSource(cameras[i]);
        } else if (event.value->IsString()) {
            auto str = event.value->GetString();
            for (int i = 0; i < cameraConfigs.size(); ++i) {
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

// example pipeline
class MyPipeline : public frc::VisionPipeline {
public:
    int val = 0;
    nt::NetworkTableInstance ntinst;
    std::shared_ptr< NetworkTable > ntab;

    MyPipeline() : ntinst(nt::NetworkTableInstance::GetDefault()) {
        ntab = ntinst.GetTable("Flash");
    }

    void Process(cv::Mat& mat) override {
        ++val;
        cv::imwrite("/mnt/log/img/img" + std::to_string(val) + ".jpg", mat);
        ntab->PutNumber("Frame", val);
    }
};
}  // namespace

std::string image_name(int index, std::string prefix = "image", std::string suffix = ".jpg") {
    int leading = 5; //6 at max
    return prefix + std::to_string(index * 0.000001).substr(8-leading) + suffix;
}

int main(int argc, char* argv[]) {
    if (argc >= 2) configFile = argv[1];

    // read configuration
    if (!ReadConfig()) return EXIT_FAILURE;

    // start NetworkTables
    auto ntinst = nt::NetworkTableInstance::GetDefault();
    if (server) {
        wpi::outs() << "Setting up NetworkTables server\n";
        ntinst.StartServer();
    } else {
        wpi::outs() << "Setting up NetworkTables client for team " << team << '\n';
        ntinst.StartClientTeam(team);
    }

    // start cameras
    for (const auto& config : cameraConfigs)
        cameras.emplace_back(StartCamera(config));

    // start switched cameras
    for (const auto& config : switchedCameraConfigs) StartSwitchedCamera(config);

    // start image processing on camera 0 if present
    if (cameras.size() >= 1) {
        std::thread([&] {
            bool log_images = fs::exists("/mnt/log/img");
            int counter = 0;
            auto ntab = ntinst.GetTable("Vision");

            frc::VisionRunner<grip::FilterOne> runner(cameras[0], new grip::FilterOne(),
            [&](grip::FilterOne &pipeline) {

                // do something with pipeline results
                //const auto& contours = *pipeline.GetFilterContoursOutput();
                const auto& contours = *pipeline.GetFindContoursOutput();
                ntab->PutNumber("Found", contours.size());

                if (contours.size() < 1) {
                    ntab->PutNumber("X", 0);
                    ntab->PutNumber("Y", 0);
                } else {

                    // would rather use std::max_element; however we would recalc
                    // boundingRect too often, mapping to boundingRect first would
                    // waste memory and use extra ram, so we do it by hand
                    cv::Rect largest(0,0,0,0);
                    for (const auto object : contours) {
                        auto rect = cv::boundingRect(object);
                        if (rect.area() > largest.area()) {
                            largest = rect;
                        }
                    }

                    auto center = (largest.br() + largest.tl()) / 2;
                    ntab->PutNumber("X", center.x - width / 2);
                    ntab->PutNumber("Y", height / 2 - center.y);
                }

                if (log_images && (counter++ % 90) == 0) {
                    std::cout << "Queue image to log\n";
                    loggingQueue.push(std::make_pair(*pipeline.GetBlurOutput(), contours.size()));
                    loggingQueue.push(std::make_pair(*pipeline.GetHslThresholdOutput(), contours.size()));
                    std::cout << "Milliseconds to process: " << pipeline.GetDuration() << "\n";
                }

            });

            runner.RunForever();
        }).detach();

        std::thread([] {
            const fs::path log_path("/mnt/log/img");
            bool log_images = fs::exists(log_path);
            int index = 1;
            std::cout << "Image logger started\n";
            if (log_images) {
                std::cout << "Logging Images\n";
                while (fs::exists(image_name(index, log_path))) {
                    ++index;
                }

                const fs::path base_name = log_path / "image";
                for(;;) {
                    auto info = loggingQueue.shift();
                    std::cout << "We have an image\n";
                    cv::imwrite(image_name(index++, base_name.c_str() + std::string("-") + std::to_string(info.second) + "-"), info.first);
                }
            }

        }).detach();

    }

    // loop forever
    for (;;) std::this_thread::sleep_for(std::chrono::seconds(10));
}
