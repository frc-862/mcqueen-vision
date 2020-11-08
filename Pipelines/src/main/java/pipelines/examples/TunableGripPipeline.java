package pipelines.examples;

import java.io.File;
import java.io.IOException;

import org.opencv.core.Mat;

import edu.wpi.first.networktables.NetworkTable;
import grip.examples.GripPipeline;
import util.AbstractVisionPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

/**
 * Example GRIP wrapper pipeline that updates GRIP values at runtime
 */
@Pipeline(camera=0)
// @Disabled
public class TunableGripPipeline implements AbstractVisionPipeline {

    private GripPipeline inst;
    private NetworkTable ntab;

    public TunableGripPipeline() {
        inst = new GripPipeline();
        ntab = ntinst.getTable("SmartDashboard");
        for(String name : inst.getParamNames()) {
            ntab.getEntry(name).setNumber((double) inst.getParam(name));
        }
    }

    @Override
    public void process(Mat mat) {
        long enter = System.nanoTime();
        inst.process(mat);
        long elapsed = System.nanoTime() - enter;
        ntab.getEntry("NanoSecsPerProcess").setNumber(elapsed);
        ntab.getEntry("SecsPerProcess").setNumber(elapsed*1e-09);
        ntab.getEntry("ProcessPerSec").setNumber(1/(elapsed*1e-09));
    }

    @Override
    public void log() {
        for(String name : inst.getParamNames()) {
            inst.initParam(name, ntab.getEntry(name).getValue().getDouble());
        }
    }

}
