package pipelines.training;

import util.LoggingPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=1)
@Disabled
public class BBlueTrainingLogging extends LoggingPipeline {
    public BBlueTrainingLogging() {
        super("B-BLUE", "B-BLUE");
    }
}
