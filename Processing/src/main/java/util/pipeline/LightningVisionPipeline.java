package util.pipeline;

import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionPipeline;

/**
 * Lightning vision pipeline interface. All types annotated 
 * {@link util.annotation.Pipeline Pipeline} must implement this type.
 */
public interface LightningVisionPipeline extends VisionPipeline {

    /**
     * Network Table Instance
     */
    public static NetworkTableInstance ntinst = NetworkTableInstance.getDefault();

    /**
     * Log pipeline information after 
     * {@link edu.wpi.first.vision.VisionPipeline#process(org.opencv.core.Mat) process} 
     * by implementing this method.
     */
    public abstract void log();

}
