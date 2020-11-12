package util;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionPipeline;

public interface LightningVisionPipeline extends VisionPipeline {

    public static NetworkTableInstance ntinst = NetworkTableInstance.getDefault();

    public abstract void log();

}
