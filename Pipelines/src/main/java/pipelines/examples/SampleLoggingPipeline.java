package pipelines.examples;

import util.annotation.Disabled;
import util.annotation.Pipeline;
import util.pipeline.LoggingPipeline;

@Pipeline(camera=0)
@Disabled
public class SampleLoggingPipeline extends LoggingPipeline {

    public SampleLoggingPipeline() {
        super("sample", "raw-image");
    }
    
}
