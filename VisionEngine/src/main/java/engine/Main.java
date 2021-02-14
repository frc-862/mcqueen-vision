package engine;

import edu.wpi.cscore.VideoSource;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionThread;

import java.util.Set;

import util.LightningVisionPipeline;
import util.PipelineProcesser;

public final class Main {

	/**
	 * Network Tables Instance
	 */
	private static NetworkTableInstance ntinst;

	/**
	 * Number of pipelines configured
	 */
	private static int numPipelines = 0;

	public Main() {}

	public static void main(String... args) {

		System.out.println("Application Started");

		if (args.length > 0) CameraServerConfig.configFile = args[0];
		if (!CameraServerConfig.readConfig()) return;
		startNetworkTables();
		startCameras();

		// get all the pipelines from Pipeline module
		PipelineProcesser pf = new PipelineProcesser("pipelines");
		Set<String> pipeNames = pf.retrieve();
		numPipelines = pipeNames.size();

		// start pipelines
		for(String pipelineName : pipeNames) {
			try {
				System.out.println("Starting " + pipelineName);
				Object pipelineInstance = Class.forName(pipelineName).getConstructor().newInstance();
				final LightningVisionPipeline inst = (LightningVisionPipeline) pipelineInstance;
                final VideoSource camera = CameraServerConfig.cameras.get(PipelineProcesser.getCamera(inst));
                VisionThread pipelineThread = new VisionThread(camera, inst, pipeline -> pipeline.log());
                pipelineThread.start();
			} catch(ArrayIndexOutOfBoundsException aioobe) {
				printFailure("The Selected Camera Cannot Be Found", pipelineName);
			} catch (ClassNotFoundException cnfe) {
				printFailure("The Pipeline Was Not Found", pipelineName);
			} catch(Exception e) {
				printFailure("Something Weird Happened", pipelineName);
				e.printStackTrace();
			}
		}

		// keep running program
		System.out.println("Running " + numPipelines + " Pipelines");
		runThreads();
		
	}

	/**
	 * Starts and configures network tables. Starts a server, or connects to robot 
	 * as a client depending on configuration file in {@link engine.CameraServerConfig}
	 */
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

	/**
	 * Starts all cameras, normal and switched
	 */
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

	/**
	 * Runs main thread until all threads are terminated
	 */
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

	/**
	 * Print error to console in the event a pipeline cannot be run
	 * @param msg Error message
	 * @param pipe Pipeline on which the error occurred
	 */
	private static void printFailure(String msg, String pipe) {
		System.out.println(msg + " on " + pipe);
		numPipelines--;
	}

}
