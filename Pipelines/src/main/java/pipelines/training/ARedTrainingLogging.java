package pipelines.training;

import util.LoggingPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=1)
@Disabled
public class ARedTrainingLogging extends LoggingPipeline {
    public ARedTrainingLogging() {
        super("A-RED", "A-RED");
    }
}
