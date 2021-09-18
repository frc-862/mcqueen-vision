package util.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

/**
 * Logging pipeline writes images to /mnt/log on a USB device. 
 * By default, the raw image from the camera wil be used but this can be changed
 * by overriding {@link #process(Mat)}
 */
public class LoggingPipeline implements LightningVisionPipeline {
    
    private static boolean enabled = true;

    private int counter = 0;
    private long unixTime;
    private String time;
    private String pathName;
    private Path logFileDir;
    private String imgPrefix; 
    private String dirName; 

    /**
     * Creates a new logging pipeline that logs to `img/log-dirName-unixTimeStamp/imagePrefix-imageNumber.jpg
     * @param dirName The directory name to write the files to
     * @param imgPrefix The prefix for the image '.jpg' file
     */
    public LoggingPipeline(String dirName, String imgPrefix) {

        this.imgPrefix = imgPrefix;
        this.dirName = dirName;

        unixTime = System.currentTimeMillis() / 1000L;
        time = "" + unixTime;
        pathName = "/mnt/log/img/log-" + this.dirName + "-" + time + "/";

        logFileDir = Paths.get(pathName);
        System.out.println("LOGGING Path Name: " + pathName);

        try {
            if (!Files.exists(logFileDir)) {
                Files.createDirectories(logFileDir);
            }
        } catch (IOException ioe) {
            enabled = false;
            System.out.println("LOGGING: USB Device not mounted correctly\nLOGGING DISABLED");
        }

    }

    @Override
    public void process(Mat mat) {
        if(enabled) {
            String fileName = pathName + imgPrefix + "-" + counter + ".jpg";
            Imgcodecs.imwrite(fileName, mat);
            counter++;
        }
    }

    @Override
    public void log() {}

}
