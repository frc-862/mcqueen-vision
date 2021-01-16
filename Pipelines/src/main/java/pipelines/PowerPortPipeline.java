package pipelines;

import java.util.ArrayList;

import org.opencv.core.Mat;
import edu.wpi.first.networktables.NetworkTable;
import grip.InfiniteRecharge;
import util.LightningVisionPipeline;
import util.annotation.Disabled;
import util.annotation.Pipeline;

@Pipeline(camera=0)
public class PowerPortPipeline implements LightningVisionPipeline {
    private InfiniteRecharge inst;
    private NetworkTable ntab;

    private bool TargetValid;
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

    public PowerPortPipeline() {
        inst = new InfiniteRecharge();
        ntab = ntinst.getTable("Vision");
    }

    @Override
    public void process(Mat mat) {
        inst.process(mat);
    }

    @Override
    public void log() {
        // Log to Network Table `ntab` here.

        int x, y, height, count;
        double elapsed;

        int counter = 0;

        ntab.getEntry("FieldOfViewVert").setDouble(43.30);
        ntab.getEntry("FieldOfViewHoriz").setDouble(70.42);
        ntab.getEntry("TargetHeightRatio").setDouble(1.0);
        ntab.getEntry("TargetRatioThreshold").setDouble(100.0);
        ntab.getEntry("TargetDistanceThreshold").setDouble(0.5);
        ntab.getEntry("TargetHeightAConstant").setDouble(0.0084);
        ntab.getEntry("TargetHeightBConstant").setDouble(-1.4737);
        ntab.getEntry("TargetHeightCConstant").setDouble(76.27);
        ntab.getEntry("TargetRowAConstant").setDouble(-0.0056);
        ntab.getEntry("TargetRowBConstant").setDouble(4.4264);
        ntab.getEntry("TargetRowCConstant").setDouble(-832.33);

        // do something with pipeline results
        ArrayList<MatOfPoint> contours = findContoursOutput();
        count = contours.size();

        ntab.getEntry("VisionFound").setInt(count);
        
        
        findBestTarget(contours);

        if (!TargetValid) {
            
            ntab.getEntry("VisionX").setInt(0)
            ntab.getEntry("VisionY").setInt(0);
            ntab.getEntry("VisionHeight").setDouble(0);
            ntab.getEntry("VisionDistance").setDouble(0);
            ntab.getEntry("VisionAngle").setDouble(0);
            ntab.getEntry("VisionDelay").setDouble(0.1); // FIXTHIS - MAKE CORRECT PROCESSING DURATION

        } else {
            // const auto center = target.center;
            // x = center.x - halfWidth;
            // y = halfHeight - center.y;
            // height = target.height;
           

            ntab.getEntry("VisionX").setInt(TargetCenterX);
            ntab.getEntry("VisionY").setInt(TargetCenterY);
            ntab.getEntry("VisionHeight").setInt(TargetHeight);
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
				if ((rect.area() > TargetWidth * TargetHeight) {
					double tempHeight = rect.height;
                    int tempCenterX = (rect.x + (rect.width / 2));
                    int tempCenterY = (rect.y + (rect.height / 2));
					if (checkTargetProportion(temp)) {
						float tempDist = getInterpolatedDistanceFromTargetHeight(temp.height);
						float tempDistTest = getInterpolatedDistanceFromTargetRow((rect.y + rect.height)); // Bottom Row of Target
						if (distancesInBounds(tempDist, tempDistTest)) {
							TargetDistance = tempDist;
							TargetHeight = rect.height;
							TargetWidth = rect.width;
							TargetCenterX = (rect.x + (rect.width / 2));
							TargetCenterY = (rect.y + (rect.height / 2));
                            TargetAngle = getAngleFromTarget(target, GetSource()->cols);
                            TargetValid = true;
						}
					}
				}
			}

        }
        return;

    }

    float getInterpolatedDistanceFromTargetHeight(float f_targetHeight)
    {
        // distance = a(height^2) + b(height) + c
        // a, b, and c are constants derived from images at distances
        return ((fTargetHeight_a * f_targetHeight * f_targetHeight) + (fTargetHeight_b * f_targetHeight) + fTargetHeight_c);
    }

    float getInterpolatedDistanceFromTargetRow(float f_targetRow)
    {
        // distance = a(height^2) + b(height) + c
        // a, b, and c are constants derived from images at distances
        return ((fTargetRow_a * f_targetRow * f_targetRow) + (fTargetRow_b * f_targetRow) + fTargetRow_c);
    }

    float getAngleFromTarget(tf::Target f_target, int cols)
    {
        // Returns a number in degrees
        // Positive angle - right turn
        // Negative angle - left turn
        return (((f_target.center.x - (cols / 2)) / cols) * fFieldOfViewHorizRad) * (180.f / M_PI);
    }

    bool checkTargetProportion(tf::Target f_targetRect)
    {
        float ratio = (float)f_targetRect.height / (float)f_targetRect.center.y;
        return ((std::abs(ratio) - fTargetHeightRatio) < fTargetRatioThreshold);
    }

    bool distancesInBounds(float dist1, float dist2) {
        return true; // Disable to implement for testing
        // return (std::abs(dist1 - dist2) < fTargetDistanceThreshold);
    }
}

