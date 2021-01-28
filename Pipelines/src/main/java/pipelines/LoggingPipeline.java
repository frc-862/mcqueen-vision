package pipelines;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import util.LightningVisionPipeline;
import util.annotation.Pipeline;

@Pipeline (camera=0)
public class LoggingPipeline implements LightningVisionPipeline {

    // Potentially log multiple cameras in same run (i.e. front facing camera, back facing camera) 
    // Make constructor to create file path and mount USB drive
    // Counter in process to update what image string you're processing
    // Use opencv imwrite to write image with process name to a file on usb drive

    private int counter = 0;
    private long unixTime;
    String pathName;
    Path logFileDir;


    public LoggingPipeline() {
        unixTime = System.currentTimeMillis() / 1000L;
        pathName = "/mnt/log/img/log-" + unixTime + "/";
        logFileDir = Paths.get(pathName);
        System.out.println("Path Name: " + pathName);
    }

    @Override
    public void process(Mat arg0) {
        // Create if statement to check if path for image logging already exists. If not then create the proper path.

        try {
            if (!Files.exists(logFileDir)) {
                Files.createDirectories(logFileDir);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // Log image files in the newly created directory

        String fileName = pathName + "raw-frame-" + counter + ".jpg";

        Imgcodecs.imwrite(fileName, arg0);

        counter++;

    }

    @Override
    public void log() {}


    
}
