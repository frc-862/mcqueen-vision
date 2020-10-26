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
public class GripPipeline implements AbstractVisionPipeline {

    private grip.GripPipeline inst;

    public GripPipeline() {
        inst = new grip.GripPipeline();
    }

    @Override
    public void process(Mat mat) {
        inst.process(mat);
        System.out.println("Oh My, Grip Pipe Is Mutable! blurRadius: " + grip.GripPipeline.getParam("blurRadius"));
    }

    @Override
    public void log() {
        // System.out.println("Log Call");
        NetworkTable ntab = ntinst.getTable("SmartDashboard");
        ntab.getEntry("blurRadius").setNumber((double) grip.GripPipeline.getParam("blurRadius"));
        grip.GripPipeline.setParam("blurRadius", ntab.getEntry("blurRadius").getValue().getDouble());
    }

}
