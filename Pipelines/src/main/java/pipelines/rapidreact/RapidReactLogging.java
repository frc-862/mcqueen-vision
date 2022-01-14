package pipelines.rapidreact;

import util.annotation.Disabled;
import util.annotation.Pipeline;
import util.pipeline.LoggingPipeline;

@Pipeline(camera=0)
@Disabled
public class RapidReactLogging extends LoggingPipeline {

    public RapidReactLogging() {
        super("power-port", "raw-image");
    }
    
}
