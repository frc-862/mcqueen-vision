package pipelines;

import org.opencv.core.Mat;

import edu.wpi.first.networktables.NetworkTable;
import grip.YellowBallSeeker;
import util.AbstractVisionPipeline;
import util.annotation.Pipeline;

@Pipeline(camera=0)
public class BallPipeline implements AbstractVisionPipeline {

    private YellowBallSeeker inst;
    private NetworkTable ntab;

    public BallPipeline() {
        inst = new YellowBallSeeker();
        ntab = ntinst.getTable("SmartDashboard");
    }

    @Override
    public void process(Mat arg0) {
        inst.process(arg0);
    }

    @Override
    public void log() {
        int numBalls = inst.filterContoursOutput().size();
        ntab.getEntry("Num Balls Seeing").setNumber(numBalls);
    }
    
}
