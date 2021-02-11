package pipelines.training;

import util.LoggingPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=1)
@Disabled
public class ABlueTrainingLogging extends LoggingPipeline {
    public ABlueTrainingLogging() {
        super("A-BLUE", "A-BLUE");
    }
}
