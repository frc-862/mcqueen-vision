package pipelines.training;

import util.annotation.Disabled;
import util.annotation.Pipeline;
import util.pipeline.LoggingPipeline;

@Pipeline(camera=1)
@Disabled
public class ARedTrainingLogging extends LoggingPipeline {
    public ARedTrainingLogging() {
        super("A-RED", "A-RED");
    }
}
