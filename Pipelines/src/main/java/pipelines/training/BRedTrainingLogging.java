package pipelines.training;

import util.LoggingPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=0)
@Disabled
public class BRedTrainingLogging extends LoggingPipeline {
    public BRedTrainingLogging() {
        super("B-RED", "B-RED");
    }
}
