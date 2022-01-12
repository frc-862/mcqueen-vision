package pipelines.infiniterecharge.training;

import util.annotation.Disabled;
import util.annotation.Pipeline;
import util.pipeline.LoggingPipeline;

@Pipeline(camera=1)
@Disabled
public class ABlueTrainingLogging extends LoggingPipeline {
    public ABlueTrainingLogging() {
        super("A-BLUE", "A-BLUE");
    }
}
