package pipelines.training;

import util.LoggingPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=0)
@Disabled
public class ARedTrainingLogging extends LoggingPipeline {
    public ARedTrainingLogging() {
        super("A-RED", "A-RED");
    }
}
