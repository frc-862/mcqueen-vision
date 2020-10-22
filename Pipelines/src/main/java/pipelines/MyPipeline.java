package pipelines;

import java.io.File;
import java.io.IOException;

import org.opencv.core.Mat;

import edu.wpi.first.networktables.NetworkTable;

import util.AbstractVisionPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=0) // configures pipeline to read from camera 0 and to write to SmartDashboard
// @Disabled
public class MyPipeline implements AbstractVisionPipeline {

    public int val = 0;

    @Override
    public void process(Mat mat) {
        val += 1;
    }

    @Override
    public void log() {
        System.out.println("Call");
        NetworkTable ntab = ntinst.getTable("SmartDashboard");
        ntab.getEntry("Val").setNumber(val);
    }

}
