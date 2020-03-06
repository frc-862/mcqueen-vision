#include <iostream>
#include <filesystem>
#include <regex>

#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>

#include "pipeline.h"

namespace fs = std::filesystem;

cv::Scalar blue(255, 0, 0);

template<typename T>
void withEachFile(const std::string path, T lambda) {
  for (const auto & entry : fs::directory_iterator(path)) {
    auto path = entry.path();
    auto fname = path.filename().string();
    if (fname.starts_with("src") && fname.ends_with(".jpg")) {
      lambda(path);
      //std::cout << entry.path() << std::endl;
      //auto img = cv::imread(path);
      //if (img.empty()) {
        //std::cout << "Unable to read " << fname << "\n";
      //}
    }
  }
}

template<typename T>
void withEachImage(const std::string path, T lambda) {
  withEachFile(path, [lambda](auto fn) {
      std::string number = std::regex_replace(
          std::string(fn),
          std::regex(".*src-([0-9]+).*"),
          std::string("$1")
          );
      std::cout << fn << " -- " << number << "\n";
      auto img = cv::imread(fn);
      if (img.data) lambda(number, img);
  });
}

int main(int argc, const char* argv[]) {
  std::cout << "Hello" << std::endl;

  tf::Pipeline p;

  std::string path = ".";
  if (argc > 1) path = argv[1];

  int index = 1;
  tf::Target target;
  withEachImage(path, [&p, &target, &index](auto number, auto img) {
      p.Process(img);

      std::string fname = "thresh-";
      fname += number;
      fname += ".jpg";

      cv::imwrite(fname, *p.GetMasked());
      if (p.findBestTarget(target)) {
        //bool found = p.checkTargetProportion(target);
        p.checkTargetProportion(target);
        //std::cout << "Found: " << found << "\n";
        //std::cout << target << "\n";
      }

      p.smartContourFilter();
      auto fun = img;
      drawContours(fun, *p.GetContours(), -1, blue, 2);
      std::string msg{"Count: "};
      msg += std::to_string(target.found);
      putText(fun, msg, cv::Point(30,30), cv::FONT_HERSHEY_COMPLEX_SMALL, 0.8, 
          cv::Scalar(200,200,250), 1, cv::LINE_AA);

      fname = "mask-";
      fname += number;
      fname += ".jpg";
      cv::imwrite(fname, fun);

      index += 1;
  }); 

  return 0;
}

