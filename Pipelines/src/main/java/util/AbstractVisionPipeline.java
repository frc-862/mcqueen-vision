package util;

import edu.wpi.first.vision.VisionPipeline;

public abstract class AbstractVisionPipeline implements VisionPipeline {

    public int getDesiredCamera() {
        return 0;
    }

    public abstract void log(); // TODO: will need to take a network tables instance in order to send logs

}
