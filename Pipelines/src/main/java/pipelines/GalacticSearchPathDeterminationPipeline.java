package pipelines;

import org.opencv.core.Mat;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import util.LightningVisionPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=1)
@Disabled
public class GalacticSearchPathDeterminationPipeline implements LightningVisionPipeline {

    public enum RobotPaths {

        A_BLUE("A-BLUE", 0), 
        A_RED("A-RED", 1), 
        B_BLUE("B-BLUE", 2), 
        B_RED("B-RED", 3), 
        NONE("NONE", -1);

        private final String str;

        private final int index;

        private RobotPaths(String str, int index) {
            this.str = str;
            this.index = index;
        }

        public String get() {
            return str;
        }

        public int index() {
            return index;
        }

    }

    public static final String POLL_REQUEST_ENTRY_NAME = "DeterminePath";

    public static final String POLL_RESULTS_ENTRY_NAME = "DeterminedPath";

    public static final String MODEL_FILEPATH = "/home/pi/path-classifier.pb";

    private boolean shouldProcess = false;

    NetworkTable ntab;

    NetworkTableEntry pollReq;

    NetworkTableEntry pollRes;

    RobotPaths path = RobotPaths.NONE;

    Net network;

    public GalacticSearchPathDeterminationPipeline() {

        // Vision Network Table
        ntab = ntinst.getTable("Vision");

        // Poll Request Entry
        pollReq = ntab.getEntry(POLL_REQUEST_ENTRY_NAME);
        pollReq.setBoolean(shouldProcess);

        // Process Results Entry
        pollRes = ntab.getEntry(POLL_RESULTS_ENTRY_NAME);
        pollRes.setString(path.get());

        // Get Model from File
        network = Dnn.readNetFromTensorflow(MODEL_FILEPATH);
        if(network.empty()) System.out.print("EMPTY ");
        System.out.print("Model Loaded\n");

    }

    @Override
    public void process(Mat inputMat) {

        // Only Run if Requested
        if (shouldProcess) {
            try {

                // Get Image Blob
                Mat blob = Dnn.blobFromImage(inputMat);

                // Print Matrix Dims, Usually NCHW
                System.out.println(blob.size(0) + " | " + blob.size(1) + " | " + blob.size(2) + " | " + blob.size(3));

                // Determine Path by Running Inference
                path = inference(blob);

            } catch (Exception e) {
                System.out.println("DNN ERROR RUNNING INFERENCE:");
                e.printStackTrace();
            }

            // Only Run Once
            shouldProcess = false;
            pollReq.setBoolean(false);

        }
    }

    @Override
    public void log() {

        // Get Process Request From Dash
        shouldProcess = pollReq.getBoolean(shouldProcess);

        // Update Process Results
        pollRes.setString(path.get());

    }

    public RobotPaths inference(Mat blob) {

        // Placeholder for Visible Path
        RobotPaths res = RobotPaths.NONE;

        // Set Network Input
        network.setInput(blob);

        // Forward Propagation
        Mat out = network.forward();

        // Get One-Hot Encoded Results
        long[] arr = new long[4];
        String outStr = "[ ";
        for(int i = 0 ; i < 4 ; ++i) {
            double[] pix = out.get(0, i);
            double val = pix[0];
            arr[i] = Math.round(val);
        }
        for(double p : arr) outStr += Math.round(p) + " ";
        outStr += "]";

        // Display Encoded Inference Results
        System.out.println("Values: " + outStr);

        // Decode Resulting Values For Selected Path
        for(int i = 0 ; i < arr.length ; ++i) 
            if(arr[i] == 1) 
                for(RobotPaths p : RobotPaths.values()) 
                    if(p.index() == i) 
                        res = p;

        // Display Selected Path
        System.out.println("Path: " + res.get());

        // Return Selected Path
        return res;

    }

}
