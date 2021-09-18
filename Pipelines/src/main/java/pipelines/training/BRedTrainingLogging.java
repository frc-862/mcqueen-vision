package pipelines.training;

import util.annotation.Disabled;
import util.annotation.Pipeline;
import util.pipeline.LoggingPipeline;

@Pipeline(camera=1)
@Disabled
public class BRedTrainingLogging extends LoggingPipeline {
    public BRedTrainingLogging() {
        super("B-RED", "B-RED");
    }
}
