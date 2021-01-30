package pipelines.examples;

import util.LoggingPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=0)
@Disabled
public class SampleLoggingPipeline extends LoggingPipeline {

    public SampleLoggingPipeline() {
        super("sample", "raw-image");
    }
    
}
