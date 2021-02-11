package pipelines;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import edu.wpi.first.networktables.NetworkTable;
import grip.InfiniteRecharge;
import util.LightningVisionPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;
import java.lang.Math;

@Pipeline(camera=0)
public class PowerPortPipeline implements LightningVisionPipeline {
    private InfiniteRecharge inst;
    private NetworkTable ntab;

    private boolean TargetValid;
    private int TargetCenterX;
    private int TargetCenterY; 
    private double TargetHeight;
    private double TargetWidth;
    private double TargetDistance;
    private double TargetAngle;

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

    public PowerPortPipeline() {
        inst = new InfiniteRecharge();
        ntab = ntinst.getTable("Vision");
    }

    @Override
    public void process(Mat mat) {
        // Pulling out image size to use later
        InputCameraImageRows = mat.rows();
        InputCameraImageCols = mat.cols();
        inst.process(mat);
    }

    @Override
    public void log() {
        // Log to Network Table `ntab` here.

        if ((InputCameraImageRows == 0)||(InputCameraImageCols == 0)) {
            return; // If process hasn't defined image size there is nothing to do
        }

        int count = 0;

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
        ArrayList<MatOfPoint> contours = inst.findContoursOutput();
        count = contours.size();

        ntab.getEntry("VisionFound").setNumber(count);
        
        
        findBestTarget(contours);

        if (!TargetValid) {
            
            ntab.getEntry("VisionX").setNumber(0);
            ntab.getEntry("VisionY").setNumber(0);
            ntab.getEntry("VisionHeight").setDouble(0);
            ntab.getEntry("VisionDistance").setDouble(0);
            ntab.getEntry("VisionAngle").setDouble(0);
            ntab.getEntry("VisionDelay").setDouble(0.1); // FIXTHIS - MAKE CORRECT PROCESSING DURATION

        } else {
            // const auto center = target.center;
            // x = center.x - halfWidth;
            // y = halfHeight - center.y;
            // height = target.height;
           

            ntab.getEntry("VisionX").setNumber(TargetCenterX);
            ntab.getEntry("VisionY").setNumber(TargetCenterY);
            ntab.getEntry("VisionHeight").setNumber(TargetHeight);
            ntab.getEntry("VisionDistance").setDouble(TargetDistance);
            ntab.getEntry("VisionAngle").setDouble(TargetAngle);
            ntab.getEntry("VisionDelay").setDouble(0.1); // FIXTHIS DELAY
        }
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
                //contours.get(i)
                
                // Puts boundaries around current contour
                Rect rect = Imgproc.boundingRect(contours.get(i));

                // If new target area is bigger than current values then do next checks
				if ((rect.area() > TargetWidth * TargetHeight)) {
					double tempHeight = rect.height;
                    int tempCenterX = (rect.x + (rect.width / 2));
                    int tempCenterY = (rect.y + (rect.height / 2));
					if (checkTargetProportion(rect.height, tempCenterY)) {
						double tempDist = getInterpolatedDistanceFromTargetHeight(rect.height);
						double tempDistTest = getInterpolatedDistanceFromTargetRow((rect.y + rect.height)); // Bottom Row of Target
						if (distancesInBounds(tempDist, tempDistTest)) {
							TargetDistance = tempDist;
							TargetHeight = rect.height;
							TargetWidth = rect.width;
							TargetCenterX = (rect.x + (rect.width / 2));
							TargetCenterY = (rect.y + (rect.height / 2));
                            TargetAngle = getAngleFromTarget(tempCenterX, InputCameraImageCols);
                            TargetValid = true;
						}
					}
				}
			}

        }
        return;

    }

    private double getInterpolatedDistanceFromTargetHeight(double f_targetHeight)
    {
        // distance = a(height^2) + b(height) + c
        // a, b, and c are constants derived from images at distances
        return ((TargetHeightAConstant * f_targetHeight * f_targetHeight) + (TargetHeightBConstant * f_targetHeight) + TargetHeightCConstant);
    }

    private double getInterpolatedDistanceFromTargetRow(double f_targetRow)
    {
        // distance = a(height^2) + b(height) + c
        // a, b, and c are constants derived from images at distances
        return ((TargetRowAConstant * f_targetRow * f_targetRow) + (TargetRowBConstant * f_targetRow) + TargetRowCConstant);
    }

    private double getAngleFromTarget(double targetCenterCol, double imageWidthCols)
    {
        // Returns a number in degrees
        // Positive angle - right turn
        // Negative angle - left turn
        return (targetCenterCol - (imageWidthCols / 2)) * (FieldOfViewHoriz / imageWidthCols);
    }

    private boolean checkTargetProportion(int targetBoxHeight, int targetCenterRow)
    {
        float ratio = (float)targetBoxHeight / (float)targetCenterRow;
        return ((Math.abs(ratio) - TargetHeightRatio) < TargetRatioThreshold);
    }

    private boolean distancesInBounds(double dist1, double dist2) {
        return true; // Disable to implement for testing
        // return (Math.abs(dist1 - dist2) < TargetDistanceThreshold);
    }
}

