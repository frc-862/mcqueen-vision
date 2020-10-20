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

	private static int numPipelines = 0;

	public Main() {
	}

	public static void main(String... args) {

		System.out.println("Application Started");

		if (args.length > 0)
			CameraServerConfig.configFile = args[0];

		if (!CameraServerConfig.readConfig())
			return;
		
		System.out.println("Config Read Successfully");

		startNetworkTables();
		System.out.println("Network Tables Started Successfully");

		startCameras();
		System.out.println("Cameras Started Successfully");

		// get all the pipelines from Pipeline module
		PipelineFinder pf = new PipelineFinder("pipelines");
		Set<String> pipeNames = pf.retrieve();
		numPipelines = pipeNames.size();
		System.out.println(numPipelines + " Pipelines Retrieved");

		// start all pipelines
		for (String pipeName : pipeNames) {
			System.out.println("Starting : " + pipeName);

			Object pipelineInstance;
			String className = pipeName;

			// create instance of pipeline
			try {
				Class<?> classObj = Class.forName(className);
				Constructor<?> constObj = classObj.getConstructor();
				pipelineInstance = constObj.newInstance();
			} catch (ClassNotFoundException cnfe) {
				printFailure("The Pipeline Was Not Found");
				continue; // TODO: log when pipeline is skipped b/c it doesn't exist . . .
			} catch (Exception e) {
				printFailure("The Pipeline Could Not Be Instantiated");
				continue; // TODO: log when pipeline is skipped b/c unexpected error
			}

			// cast pipeline instance
			AbstractVisionPipeline inst = null;
			try {
				inst = (AbstractVisionPipeline) pipelineInstance;
			} catch (Exception e) {
				printFailure("The Pipeline is Not a Pipeline");
				continue; // TODO: log when pipeline is skipped b/c it is not a pipeline . . . 
			}

			// get camera stream the pipeline wants to use
			VideoSource camera = null;
			try {
				CameraServerConfig.cameras.get(PipelineFinder.getCamera(inst));
			} catch(ArrayIndexOutOfBoundsException aioobe) {
				printFailure("There is No Camera");
				continue; // TODO: log when pipeline is skipped b/c there is no camera
			}

			final NetworkTable ntab = ntinst.getTable(PipelineFinder.getNetworkTable(inst));

			// start thread for pipeline
			if (CameraServerConfig.cameras.size() >= 1 && pipelineInstance instanceof AbstractVisionPipeline) {
				VisionThread visionThread = new VisionThread(camera, inst, pipeline -> {
					pipeline.log(ntab); // to ntab does not exist?
				});
				visionThread.start();
				System.out.println("Pipeline Started");
			}

		}

		System.out.println("Running " + numPipelines + " Pipelines");
		runThreads();
		System.out.println("Application Successfully Terminated");
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
			if(numPipelines == 0) break;
		}
	}

	private static void printFailure(String msg) {
		System.out.println(msg);
		numPipelines--;
	}

}
