package pipelines;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import util.LightningVisionPipeline;
import util.annotation.Pipeline;

@Pipeline(camera=0) // TODO this should be camera 1 on comp bot
public class GalacticSearchPathDeterminationPipeline implements LightningVisionPipeline {

    public enum Paths {

        A_BLUE("A-BLUE", 0), 
        A_RED("A-RED", 1), 
        B_BLUE("B-BLUE", 2), 
        B_RED("B-RED", 3), 
        NONE("NONE", -1);

        private final String str;

        private final int index;

        private Paths(String str, int index) {
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

    public static final String MODEL_FILEPATH = "/home/pi/final-classifier.pb";

    private boolean shouldProcess = true; // false;

    NetworkTable ntab;

    NetworkTableEntry pollReq;

    NetworkTableEntry pollRes;

    Paths path = Paths.NONE;

    Net network;

    public GalacticSearchPathDeterminationPipeline() {
        ntab = ntinst.getTable("Vision");

        pollReq = ntab.getEntry(POLL_REQUEST_ENTRY_NAME);
        pollReq.setBoolean(shouldProcess);

        pollRes = ntab.getEntry(POLL_RESULTS_ENTRY_NAME);
        pollRes.setString(path.get());

        network = Dnn.readNetFromTensorflow(MODEL_FILEPATH);
        if(network.empty()) System.out.print("EMPTY ");
        System.out.print("Model Loaded\n");

    }

    @Override
    public void process(Mat img) {
        if (shouldProcess) {
            try {

                // get and scale image
                // Mat blob = Dnn.blobFromImage(img, 1.0/255.0, new Size(224, 224)); //, new Scalar(0), true, false, CV_32F);
                // TODO see about converting to RGB
                Mat blob = Dnn.blobFromImage(img, 1.0/255.0, new Size(224, 224), new Scalar(0), true);

                // set nnet input
                network.setInput(blob);

                // run inference
                Mat output = network.forward();

                // print output object (should be 1x4)
                // System.out.println("Output is: \n\n" + output + "\n\n");

                // go over all 'pixels'
                for (int row = 0 ; row < output.rows() ; ++row) {
                    for (int col = 0 ; col < output.cols() ; ++col) {

                        // get the image output
                        double[] pix = output.get(row, col);
                        // go over the one-hot encoded outputs
                        for(int i = 0 ; i < pix.length ; ++i) {
                            // they are doubles so make them longs 
                            long curr = Math.round(pix[i]);
                            // if the result is 1, then select the path at that index
                            if(curr == 1) {
                                for(Paths p : Paths.values()) {
                                    if(p.index() == i) {
                                        path = p;
                                        break;
                                    }
                                }
                            }
                        }

                    }
                }

                // System.out.println("Determined Path: " + path.get());

            } catch (Exception e) {
                e.printStackTrace();
            }
            shouldProcess = false;
            //pollReq.setBoolean(false);
        } else {
            path = Paths.NONE;
        }
    }

    @Override
    public void log() {
        // Determine if robot has requested that a path be determined
        shouldProcess = pollReq.getBoolean(shouldProcess);
        pollRes.setString(path.get());
    }

}
