package pipelines.examples;

import org.opencv.core.Mat;

import edu.wpi.first.networktables.NetworkTable;
import grip.examples.YellowBallSeeker;
import util.AbstractVisionPipeline;
import util.annotation.Pipeline;
import util.annotation.Disabled;

/**
 * Example GRIP wrapper pipeline. 
 * Sends relevant data from generated GRIP pipeline to Network Table.
 */
@Pipeline(camera=0)
@Disabled
public class BallPipeline implements AbstractVisionPipeline {

    private YellowBallSeeker inst;
    private NetworkTable ntab;

    public BallPipeline() {
        inst = new YellowBallSeeker();
        ntab = ntinst.getTable("SmartDashboard");
    }

    @Override
    public void process(Mat mat) {
        long enter = System.nanoTime();
        inst.process(mat);
        long elapsed = System.nanoTime() - enter;
        ntab.getEntry("NanoSecsPerProcess_Ball").setNumber(elapsed);
        ntab.getEntry("SecsPerProcess_Ball").setNumber(elapsed*1e-09);
        ntab.getEntry("FramesProcessedPerSec_Ball").setNumber(1/(elapsed*1e-09));
    }

    @Override
    public void log() {
        int numBalls = inst.filterContoursOutput().size();
        ntab.getEntry("Num Balls Seeing").setNumber(numBalls);
    }
    
}
