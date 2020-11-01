package pipelines.examples;

import java.io.File;
import java.io.IOException;

import org.opencv.core.Mat;

import edu.wpi.first.networktables.NetworkTable;

import util.AbstractVisionPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

/**
 * Example vision pipeline. 
 * Sends relevant data from generated GRIP pipeline to Network Table.
 */
@Pipeline(camera=0)
@Disabled
public class MyPipeline implements AbstractVisionPipeline {

    public int val = 0;

    @Override
    public void process(Mat mat) {
        System.out.println("Process Call");
        val += 1;
    }

    @Override
    public void log() {
        System.out.println("Log Call");
        NetworkTable ntab = ntinst.getTable("SmartDashboard");
        ntab.getEntry("Val").setNumber(val);
    }

}
