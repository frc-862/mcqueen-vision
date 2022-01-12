package pipelines.infiniterecharge;

import util.annotation.Disabled;
import util.annotation.Pipeline;
import util.pipeline.LoggingPipeline;

@Pipeline(camera=0)
@Disabled
public class PowerPortLogging extends LoggingPipeline {

    public PowerPortLogging() {
        super("power-port", "raw-image");
    }
    
}
