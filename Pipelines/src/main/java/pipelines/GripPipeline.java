package pipelines;

import java.io.File;
import java.io.IOException;

import org.opencv.core.Mat;

import edu.wpi.first.networktables.NetworkTable;

import util.AbstractVisionPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=0) // configures pipeline to read from camera 0 and to write to SmartDashboard
@Disabled
public class GripPipeline implements AbstractVisionPipeline {

    private grip.GripPipeline inst;
    private NetworkTable ntab;

    public GripPipeline() {
        inst = new grip.GripPipeline();
        ntab = ntinst.getTable("SmartDashboard");
        for(String name : inst.getParamNames()) {
            ntab.getEntry(name).setNumber((double) inst.getParam(name));
        }
    }

    @Override
    public void process(Mat mat) {
        inst.process(mat);
    }

    @Override
    public void log() {
        for(String name : inst.getParamNames()) {
            inst.setParam(name, ntab.getEntry(name).getValue().getDouble());
        }
    }

}
