package pipelines;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import edu.wpi.first.networktables.NetworkTable;
import grip.examples.YellowBallSeeker;
import util.LightningVisionPipeline;
import util.annotation.Pipeline;
import util.annotation.Disabled;

/**
 * Sends relevant data from generated GRIP pipeline to Network Table.
 */



@Pipeline(camera=0)
@Disabled
public class FindBallPipeline implements LightningVisionPipeline {

    private boolean TargetValid;
    private int TargetCenterX;
    private int TargetCenterY;
    private double TargetHeight;
    private double TargetWidth;
    private double TargetDistance;
    private double TargetAngle;
    private double TargetDelay;

    private int InputCameraImageRows = 0;
    private int InputCameraImageCols = 0;

    private YellowBallSeeker inst;
    private NetworkTable ntab;

    public FindBallPipeline() {
        inst = new YellowBallSeeker();
        ntab = ntinst.getTable("SmartDashboard");
    }

    @Override
    public void process(Mat mat) {
        long enter = System.nanoTime();
        InputCameraImageRows = mat.rows();
        InputCameraImageCols = mat.cols();
        inst.process(mat);

        long elapsed = System.nanoTime() - enter;
        ntab.getEntry("NanoSecsPerProcess_Ball").setNumber(elapsed);
        ntab.getEntry("SecsPerProcess_Ball").setNumber(elapsed*1e-09);
        ntab.getEntry("FramesProcessedPerSec_Ball").setNumber(1/(elapsed*1e-09));


        // Find closest ball and then post angle & distance to smart dashboard

        ArrayList<MatOfPoint> contours = inst.findContoursOutput();
        int count = contours.size();

        ntab.getEntry("BallVisionFound").setNumber(count);
        
        
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
            
        ntab.getEntry("BallVisionX").setNumber(TargetCenterX);
        ntab.getEntry("BallVisionY").setNumber(TargetCenterY);
        ntab.getEntry("BallVisionHeight").setNumber(TargetHeight);
        ntab.getEntry("BallVisionDistance").setDouble(TargetDistance);
        ntab.getEntry("BallVisionAngle").setDouble(TargetAngle);
        ntab.getEntry("BallVisionDelay").setDouble(TargetDelay);
    }

    // Pipeline parameters - initial values to be moved to constants table

    // TODO - Rename for network tables

    private double FieldOfViewVert = 43.30;
    private double FieldOfViewHoriz = 70.42;

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
            for(int i=0; i < contours.size();i++) {
                
                // Puts boundaries around current contour
                Rect rect = Imgproc.boundingRect(contours.get(i));

                // If new target area is bigger than current values then do next checks
				if ((rect.area() > TargetWidth * TargetHeight)) {

                    // Finds the center of the current contour being processed on the x and y axes
                    int tempCenterX = (rect.x + (rect.width / 2));
                    int tempCenterY = (rect.y + (rect.height / 2));

                    // Set the current target variables equal to variables of the contour that has been computed and checked
                    TargetDistance = 0; // TODO - comes from some math that reflects a fit for size of ball at certain distances
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
        return;

    }

    private double getAngleFromTarget(double targetCenterCol, double imageWidthCols) {
        // Returns a number in degrees
        // Positive angle - right turn
        // Negative angle - left turn
        // Use estimated center of target to estimate in degrees how much the robot needs to turn to be in line with the target
        return (targetCenterCol - (imageWidthCols / 2)) * (FieldOfViewHoriz / imageWidthCols);
    }

    @Override
    public void log() {
        int numBalls = inst.filterContoursOutput().size();
        ntab.getEntry("Num Balls Seeing").setNumber(numBalls);
    }
    
}
