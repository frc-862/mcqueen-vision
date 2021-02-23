package pipelines;

import java.util.ArrayList;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoMode;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import grip.InfiniteRecharge;
import util.CameraServerConfig;
import util.LightningVisionPipeline;
import util.annotation.Pipeline;
import java.lang.Math;

@Pipeline(camera=0)
public class PowerPortPipeline implements LightningVisionPipeline {

    private InfiniteRecharge inst;
    private NetworkTable ntab;
    private NetworkTable griptab;
    private CvSource output;
    private CvSource output2;
    private MjpegServer processedVideoServer;
    private MjpegServer processedVideoServer2;

    // Following variables will be used later to store target values

    private boolean TargetValid;
    private int TargetCenterX;
    private int TargetCenterY; 
    private double TargetHeight;
    private double TargetWidth;
    private double TargetDistance;
    private double TargetAngle;
    private double TargetDelay;

    // Pipeline parameters - initial values to be moved to constants table

    private double FieldOfViewVert = 43.30;
    private double FieldOfViewHoriz = 70.42;
    private double TargetHeightRatio = 1.0;
    private double TargetRatioThreshold = 100.0;
    private double TargetDistanceThreshold = 0.5;
    private double TargetHeightAConstant = 0.0084;
    private double TargetHeightBConstant = -1.4737;
    private double TargetHeightCConstant = 76.27;
    private double TargetRowAConstant = -0.0056;
    private double TargetRowBConstant = 4.4264;
    private double TargetRowCConstant = -832.33;

    private int InputCameraImageRows = 0;
    private int InputCameraImageCols = 0;

    // Initializes network table
    public PowerPortPipeline() {
        inst = new InfiniteRecharge();
        ntab = ntinst.getTable("Vision");
        // griptab = ntinst.getTable("PowerPortParams");
        // for(String name : inst.getParamNames()) {
        //     griptab.getEntry(name).setNumber((double) inst.getParam(name));
        // }            
        VideoSource camera = CameraServerConfig.cameras.get(0);
        int framesPerSec = 90;
		// camera.setVideoMode(VideoMode.PixelFormat.kMJPEG, InputCameraImageCols, InputCameraImageRows, framesPerSec);

		// Processed video server for HSV image
        output = CameraServer.getInstance().putVideo("Threshold", InputCameraImageCols, InputCameraImageRows);
		processedVideoServer = new MjpegServer("processed_video_server", 8083);
		processedVideoServer.setSource(output);

        // Processed video server for displaying contours
        output2 = CameraServer.getInstance().putVideo("Threshold2", InputCameraImageCols, InputCameraImageRows);
		processedVideoServer2 = new MjpegServer("processed_video_server2", 8084);
		processedVideoServer2.setSource(output2);
    }

    @Override
    public void process(Mat mat) {
        // Pulling out image size to use later
        long enter = System.nanoTime();
        InputCameraImageRows = mat.rows();
        InputCameraImageCols = mat.cols();
        inst.process(mat);
        long elapsed = System.nanoTime() - enter;
        ntab.getEntry("NanoSecsPerProcess_Tune").setNumber(elapsed);
        ntab.getEntry("SecsPerProcess_Tune").setNumber(elapsed*1e-09);
        ntab.getEntry("FramesProcessedPerSec_Tune").setNumber(1/(elapsed*1e-09));
        output.putFrame(inst.hsvThresholdOutput());

        // Draw all the contours such that they are filled in.
        Mat contourImg = new Mat(mat.size(), CvType.CV_8UC3, new Scalar(0,0,0));
        var contours = inst.filterContoursOutput();
        Imgproc.drawContours(contourImg, contours, -1, new Scalar(255, 255, 255), 3);


        output2.putFrame(contourImg);

        // works -> output2.putFrame(inst.cvDilateOutput());
        
    }

    @Override
    public void log() {
       
        // TODO add process output as a video stream to dashboard
        //gripstab.add("Contour Output", () -> new Mat(inst.filterContoursOutput()));
        
        // Log to Network Table `ntab` here.

        if ((InputCameraImageRows == 0) || (InputCameraImageCols == 0)) {
            return; // If process hasn't defined image size there is nothing to do
        }

        // Posting current values to SmartDashboard
        ntab.getEntry("FieldOfViewVert").setDouble(FieldOfViewVert);
        ntab.getEntry("FieldOfViewHoriz").setDouble(FieldOfViewHoriz);
        ntab.getEntry("TargetHeightRatio").setDouble(TargetHeightRatio);
        ntab.getEntry("TargetRatioThreshold").setDouble(TargetRatioThreshold);
        ntab.getEntry("TargetDistanceThreshold").setDouble(TargetDistanceThreshold);
        ntab.getEntry("TargetHeightAConstant").setDouble(TargetHeightAConstant);
        ntab.getEntry("TargetHeightBConstant").setDouble(TargetHeightBConstant);
        ntab.getEntry("TargetHeightCConstant").setDouble(TargetHeightCConstant);
        ntab.getEntry("TargetRowAConstant").setDouble(TargetRowAConstant);
        ntab.getEntry("TargetRowBConstant").setDouble(TargetRowBConstant);
        ntab.getEntry("TargetRowCConstant").setDouble(TargetRowCConstant);

        // do something with pipeline results
        ArrayList<MatOfPoint> contours = inst.filterContoursOutput(); // inst.findContoursOutput();
        int count = contours.size();

        ntab.getEntry("VisionFound").setNumber(count);
        
        
        findBestTarget(contours); // Function will set number of classwide variables

        // If no valid target is found, set default values. 
        if (!TargetValid) {
            
            TargetCenterX = 0;
            TargetCenterY = 0;
            TargetHeight = 0;
            TargetDistance = 0;
            TargetAngle = 0;
            TargetDelay = 0.1; // FIXTHIS at a later point for proper processing time

        }
            
        ntab.getEntry("VisionX").setNumber(TargetCenterX);
        ntab.getEntry("VisionY").setNumber(TargetCenterY);
        ntab.getEntry("VisionHeight").setNumber(TargetHeight);
        ntab.getEntry("VisionDistance").setDouble(TargetDistance);
        ntab.getEntry("VisionAngle").setDouble(TargetAngle);
        ntab.getEntry("VisionDelay").setDouble(TargetDelay);
        
    }
    

    public void findBestTarget (ArrayList<MatOfPoint> contours) {
		// Look at target list and return most likely power port target entry
        TargetValid = false;

        // Reset active target info
        TargetDistance = 0;
        TargetHeight = 0;
        TargetWidth = 0;
        TargetCenterX = 0;
        TargetCenterY = 0;
        TargetAngle = 0;
        int count = contours.size();

		if (count != 0) {

            // Iterates throught contour list
            for(int i=0; i< contours.size();i++) {
                
                // Puts boundaries around current contour
                Rect rect = Imgproc.boundingRect(contours.get(i));

                // If new target area is bigger than current values then do next checks
				if ((rect.area() > TargetWidth * TargetHeight)) {

                    // Finds the center of the current contour being processed on the x and y axes
                    int tempCenterX = (rect.x + (rect.width / 2));
                    int tempCenterY = (rect.y + (rect.height / 2));
                    
                    // If it passes checkTargetProportion <- FIXTHIS, explain in greater detail
                    if (checkTargetProportion(rect.height, tempCenterY)) {
                        // Checks distance of target based on both height and row to ensure proper distance
						double tempDist = getInterpolatedDistanceFromTargetHeight(rect.height);
						double tempDistTest = getInterpolatedDistanceFromTargetRow((rect.y + rect.height)); // Bottom Row of Target
                        
                        // Ensures the difference in distances is within threshold
                        if (distancesInBounds(tempDist, tempDistTest)) {
                            // Set the current target variables equal to variables of the contour that has been computed and checked
							TargetDistance = tempDist;
							TargetHeight = rect.height;
							TargetWidth = rect.width;
							TargetCenterX = tempCenterX;
                            TargetCenterY = tempCenterY;
                            // Calculating how much our robot should turn in degrees to center the shooter on the target
                            TargetAngle = getAngleFromTarget(tempCenterX, InputCameraImageCols);
                            TargetValid = true;
						}
					}
				}
			}

        }
        return;

    }

    /* Given a collected image that provides target height in pixels, and a set of prior collected data (target height in pixels vs robot distance from target) 
    This function estimates distance of target based on a second order polynomial fit to collected data
    Requires that a, b, and c are computed offline ahead of time */
    private double getInterpolatedDistanceFromTargetHeight(double f_targetHeight) {
        // distance = a(height^2) + b(height) + c
        // a, b, and c are constants derived from images at distances
        return ((TargetHeightAConstant * f_targetHeight * f_targetHeight) + (TargetHeightBConstant * f_targetHeight) + TargetHeightCConstant);
    }

    /* Given a collected image that provides target rows in pixels, and a set of prior collected data (target rows in pixels vs robot distance from target) 
    This function estimates distance of target based on a second order polynomial fit to collected data
    Requires that a, b, and c are computed offline ahead of time */
    private double getInterpolatedDistanceFromTargetRow(double f_targetRow) {
        // distance = a(height^2) + b(height) + c
        // a, b, and c are constants derived from images at distances
        return ((TargetRowAConstant * f_targetRow * f_targetRow) + (TargetRowBConstant * f_targetRow) + TargetRowCConstant);
    }

    private double getAngleFromTarget(double targetCenterCol, double imageWidthCols) {
        // Returns a number in degrees
        // Positive angle - right turn
        // Negative angle - left turn
        // Use estimated center of target to estimate in degrees how much the robot needs to turn to be in line with the target
        return (targetCenterCol - (imageWidthCols / 2)) * (FieldOfViewHoriz / imageWidthCols);
    }

    // Investigate efficacy of this further
    // Checks to ensure ratio of the current found target is similar to that of a model target <- FIXTHIS, don't really understand
    private boolean checkTargetProportion(int targetBoxHeight, int targetCenterRow) {
        float ratio = (float)targetBoxHeight / (float)targetCenterRow;
        return ((Math.abs(ratio) - TargetHeightRatio) < TargetRatioThreshold);
    }

    private boolean distancesInBounds(double dist1, double dist2) {
        return true; // Disable to implement for testing <- FIXTHIS
        // return (Math.abs(dist1 - dist2) < TargetDistanceThreshold);
    }
}

