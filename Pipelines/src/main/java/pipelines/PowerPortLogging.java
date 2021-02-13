package pipelines;

import util.LoggingPipeline;
import util.annotation.Pipeline;

@Pipeline(camera=0)
public class PowerPortLogging extends LoggingPipeline {

    public PowerPortLogging() {
        super("power-port", "raw-image");
    }
    
}
