package util.pipeline;

import org.opencv.core.Mat;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

public abstract class TensorFlowPipeline implements LightningVisionPipeline {

    private final String modelFile;

    private final String configFile;

    private Net net;

    public Enum<?> inferenceResult;

    public TensorFlowPipeline(String modelFile) {
        this(modelFile, null);
    }

    public TensorFlowPipeline(String modelFile, String configFile) {

        this.modelFile = modelFile;
        this.configFile = configFile;

        if(configFile == null) {
            net = Dnn.readNetFromTensorflow(modelFile);
        } else {
            net = Dnn.readNetFromTensorflow(modelFile, configFile);
        }

    }

    @Override
    public void process(Mat inputMat) {

        // Get Image Blob
        Mat blob = Dnn.blobFromImage(inputMat);

        // Determine Path by Running Inference
        inferenceResult = inference(blob);
        
    }

    public abstract Enum<?> inference(Mat blob);
    
}
