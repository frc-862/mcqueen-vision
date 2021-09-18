package pipelines;

import util.annotation.Pipeline;
import util.pipeline.LoggingPipeline;

@Pipeline(camera=0)
public class PowerPortLogging extends LoggingPipeline {

    public PowerPortLogging() {
        super("power-port", "raw-image");
    }
    
}
