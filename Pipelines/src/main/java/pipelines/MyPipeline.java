package pipelines;

import org.opencv.core.Mat;

import edu.wpi.first.networktables.NetworkTable;

import util.AbstractVisionPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=0,ntab="SmartDashboard") // configures pipeline to read from camera 0 and to write to SmartDashboard
@Disabled
public class MyPipeline implements AbstractVisionPipeline {

    public int val;

    @Override
    public void process(Mat mat) {
        val += 1;
    }

    @Override
    public void log(NetworkTable ntab) {
        //ntab.putData("Value", val);
    }

}
