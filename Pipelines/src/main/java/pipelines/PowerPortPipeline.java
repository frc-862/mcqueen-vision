package pipelines;

import org.opencv.core.Mat;
import edu.wpi.first.networktables.NetworkTable;
import grip.InfiniteRecharge;
import util.LightningVisionPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=0)
public class PowerPortPipeline implements LightningVisionPipeline {
    private InfiniteRecharge inst;
    private NetworkTable ntab;

    public PowerPortPipeline() {
        inst = new InfiniteRecharge();
        ntab = ntinst.getTable("Vision");
    }

    @Override
    public void process(Mat mat) {
        inst.process(mat);
    }

    @Override
    public void log() {
        // Log to Network Table `ntab` here.

        ntab.getEntry("FieldOfViewVert").setDouble(43.30);
        ntab.getEntry("FieldOfViewHoriz").setDouble(70.42);
        ntab.getEntry("TargetHeightRatio").setDouble(1.0);
        ntab.getEntry("TargetRatioThreshold").setDouble(100.0);
        ntab.getEntry("TargetDistanceThreshold").setDouble(0.5);
        ntab.getEntry("TargetHeightAConstant").setDouble(0.0084);
        ntab.getEntry("TargetHeightBConstant").setDouble(-1.4737);
        ntab.getEntry("TargetHeightCConstant").setDouble(76.27);
        ntab.getEntry("TargetRowAConstant").setDouble(-0.0056);
        ntab.getEntry("TargetRowBConstant").setDouble(4.4264);
        ntab.getEntry("TargetRowCConstant").setDouble(-832.33);

        ntab.getEntry("VisionFound").setDouble(0);
        
        ntab.getEntry("VisionX").setDouble(0);
        ntab.getEntry("VisionY").setDouble(0);
        ntab.getEntry("VisionHeight").setDouble(0);
        ntab.getEntry("VisionDistance").setDouble(0);
        ntab.getEntry("VisionAngle").setDouble(0);
        ntab.getEntry("VisionDelay").setDouble(0.1);
    }  
}