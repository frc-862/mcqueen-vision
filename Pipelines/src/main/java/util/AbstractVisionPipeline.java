package util;

import edu.wpi.first.networktables.NetworkTable;

import edu.wpi.first.vision.VisionPipeline;

public interface AbstractVisionPipeline extends VisionPipeline {

    public abstract void log(NetworkTable ntab);

}
