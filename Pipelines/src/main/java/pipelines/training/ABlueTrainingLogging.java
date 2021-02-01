package pipelines.training;

import util.LoggingPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=0)
@Disabled
public class ABlueTrainingLogging extends LoggingPipeline {
    public ABlueTrainingLogging() {
        super("A-BLUE", "A-BLUE");
    }
}
