package pipelines;

import java.io.File;
import java.io.IOException;

import org.opencv.core.Mat;

import edu.wpi.first.networktables.NetworkTable;

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
        
    }  
}