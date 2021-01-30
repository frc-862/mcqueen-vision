package pipelines.training;

import util.LoggingPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=0)
// @Disabled
public class BBlueTrainingLogging extends LoggingPipeline {
    public BBlueTrainingLogging() {
        super("B-BLUE", "B-BLUE");
    }
}
