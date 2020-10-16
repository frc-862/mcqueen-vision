package pipelines;

import org.opencv.core.Mat;

import util.AbstractVisionPipeline;

public class MyPipeline extends AbstractVisionPipeline {

    public int val;

    @Override
    public void process(Mat mat) {
        val += 1;
    }

    @Override
    public void log() {
        return;
    }

    @Override
    public int getDesiredCamera() {
        return 0;
    }
    
}
