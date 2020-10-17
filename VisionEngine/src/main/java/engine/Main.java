package engine;

import edu.wpi.cscore.VideoSource;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionPipeline;
import edu.wpi.first.vision.VisionThread;

import java.lang.reflect.Constructor;
import java.util.Set;

import org.opencv.core.Mat;

import util.AbstractVisionPipeline;
import pipelines.MyPipeline;
import util.annotation.*;

public final class Main {

	private static NetworkTableInstance ntinst;

	public Main() {
	}

	public static void main(String... args) {

		if (args.length > 0)
			CameraServerConfig.configFile = args[0];

		if (!CameraServerConfig.readConfig())
			return;

		startNetworkTables();

		startCameras();

		// get all the pipelines from Pipeline module
		Set<String> pipeNames = PipelineFinder.retrieve();

		// start all pipelines
		for (String pipeName : pipeNames) {
			Object pipelineInstance;
			String className = "pipeline." + pipeName;

			// create instance of pipeline
			try {
				Class<?> classObj = Class.forName(className);
				Constructor<?> constObj = classObj.getConstructor();
				pipelineInstance = constObj.newInstance();
			} catch (ClassNotFoundException cnfe) {
				continue; // TODO: log when pipeline is skipped b/c it doesn't exist . . .
			} catch (Exception e) {
				continue; // TODO: log when pipeline is skipped b/c unexpected error
			}

			// cast pipeline instance
			AbstractVisionPipeline inst = null;
			try {
				inst = (AbstractVisionPipeline) pipelineInstance;
			} catch (Exception e) {
				continue; // TODO: log when pipeline is skipped b/c it is not a pipeline . . . 
			}

			// get camera stream the pipeline wants to use
			VideoSource camera = null;
			try {
				CameraServerConfig.cameras.get(PipelineFinder.getCamera(inst));
			} catch(ArrayIndexOutOfBoundsException aioobe) {
				continue; // TODO: log when pipeline is skipped b/c there is no camera
			}

			NetworkTable ntab = ntinst.getTable(PipelineFinder.getNetworkTable(inst));

			// start thread for pipeline
			if (CameraServerConfig.cameras.size() >= 1 && pipelineInstance instanceof AbstractVisionPipeline) {
				VisionThread visionThread = new VisionThread(camera, inst, pipeline -> {
					pipeline.log(ntab);
				});
				visionThread.start();
			}

		}

		runThreads();
	}

	private static void startNetworkTables() {
		ntinst = NetworkTableInstance.getDefault();
		if (CameraServerConfig.server) {
			System.out.println("Setting up NetworkTables server");
			ntinst.startServer();
		} else {
			System.out.println("Setting up NetworkTables client for team " + CameraServerConfig.team);
			ntinst.startClientTeam(CameraServerConfig.team);
		}
	}

	private static void startCameras() {
		// start cameras & switch cameras
		for (CameraServerConfig.CameraConfig config : CameraServerConfig.cameraConfigs) {
			CameraServerConfig.cameras.add(CameraServerConfig.startCamera(config));
		}
		// start switched cameras
		for (CameraServerConfig.SwitchedCameraConfig config : CameraServerConfig.switchedCameraConfigs) {
			CameraServerConfig.startSwitchedCamera(config);
		}
	}

	private static void runThreads() {
		for (;;) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException ex) {
				return;
			}
		}
	}

}
