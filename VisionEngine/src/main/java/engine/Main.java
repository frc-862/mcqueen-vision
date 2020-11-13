package engine;

import edu.wpi.cscore.VideoSource;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionPipeline;
import edu.wpi.first.vision.VisionRunner;
import edu.wpi.first.vision.VisionThread;

import java.lang.reflect.Constructor;
import java.util.Set;

import org.opencv.core.Mat;

import util.LightningVisionPipeline;
import util.annotation.*;

public final class Main {

	private static NetworkTableInstance ntinst;

	private static int numPipelines = 0;

	public Main() {}

	public static void main(String... args) {

		System.out.println("Application Started");

		if (args.length > 0) CameraServerConfig.configFile = args[0];
		if (!CameraServerConfig.readConfig()) return;
		startNetworkTables();
		startCameras();

		// get all the pipelines from Pipeline module
		PipelineFinder pf = new PipelineFinder("pipelines");
		Set<String> pipeNames = pf.retrieve();
		numPipelines = pipeNames.size();

		// start pipelines
		for(String pipelineName : pipeNames) {
			try {
				Object pipelineInstance = Class.forName(pipelineName).getConstructor().newInstance();
				final LightningVisionPipeline inst = (LightningVisionPipeline) pipelineInstance;
				final VideoSource camera = CameraServerConfig.cameras.get(PipelineFinder.getCamera(inst));
				new Thread(() -> {
					VisionRunner<LightningVisionPipeline> runner = new VisionRunner<LightningVisionPipeline>(camera, inst, pipeline -> pipeline.log());
					runner.runForever();
				}).start();
			} catch(ArrayIndexOutOfBoundsException aioobe) {
				printFailure("The Selected Camera Cannot Be Found", pipelineName);
			} catch (ClassNotFoundException cnfe) {
				printFailure("The Pipeline Was Not Found", pipelineName);
			} catch(Exception e) {
				printFailure("Something Weird Happened", pipelineName);
			}
		}

		// keep running program
		System.out.println("Running " + numPipelines + " Pipelines");
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
			if(numPipelines == 0) break;
		}
	}

	private static void printFailure(String msg, String pipe) {
		System.out.println(msg + " on " + pipe);
		numPipelines--;
	}

	public synchronized static void print(String msg) {
		System.out.println(msg);
	}

}
