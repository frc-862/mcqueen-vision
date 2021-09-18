package pipelines.training;

import util.annotation.Disabled;
import util.annotation.Pipeline;
import util.pipeline.LoggingPipeline;

@Pipeline(camera=1)
@Disabled
public class BBlueTrainingLogging extends LoggingPipeline {
    public BBlueTrainingLogging() {
        super("B-BLUE", "B-BLUE");
    }
}
