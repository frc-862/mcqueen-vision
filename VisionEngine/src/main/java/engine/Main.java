package engine;

import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionPipeline;
import edu.wpi.first.vision.VisionThread;

import java.lang.reflect.Constructor;
import java.util.Set;

import org.opencv.core.Mat;

import util.AbstractVisionPipeline;
import pipelines.MyPipeline;

public final class Main {

	public Main() {
	}

	/**
	 * Main.
	 */
	public static void main(String... args) {

		if (args.length > 0) {
			CameraServerConfig.configFile = args[0];
		}

		// read configuration
		if (!CameraServerConfig.readConfig()) {
			return;
		}

		// start NetworkTables
		startNetworkTables();

		// start cameras
		for (CameraServerConfig.CameraConfig config : CameraServerConfig.cameraConfigs) {
			CameraServerConfig.cameras.add(CameraServerConfig.startCamera(config));
		}

		// start switched cameras
		for (CameraServerConfig.SwitchedCameraConfig config : CameraServerConfig.switchedCameraConfigs) {
			CameraServerConfig.startSwitchedCamera(config);
		}

		// TODO: go through all pipelines, log pipeline results

		// Set<Class<? extends AbstractVisionPipeline>> pipeClasses = PipelineFinder.retrieve();

		Set<String> pipeNames = PipelineFinder.retrieve();

		for (String pipeName : pipeNames) {

			Object pipelineInstance;

			String className = "pipeline." + pipeName;

			try {
				Class<?> classObj = Class.forName(className);
				Constructor<?> constObj = classObj.getConstructor();
				pipelineInstance = constObj.newInstance();
			} catch(ClassNotFoundException cnfe) {
				continue;
			} catch(Exception e) {
				continue;
			}

			AbstractVisionPipeline inst = (AbstractVisionPipeline) pipelineInstance;

			if (CameraServerConfig.cameras.size() >= 1 && pipelineInstance instanceof AbstractVisionPipeline) {
				VisionThread visionThread = new VisionThread(CameraServerConfig.cameras.get(inst.getDesiredCamera()), inst,
						pipeline -> {
							pipeline.log();
						});
				visionThread.start();
			}

		}

		// loop forever
		for (;;) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException ex) {
				return;
			}
		}
	}

	private static void startNetworkTables() {
		NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
		if (CameraServerConfig.server) {
			System.out.println("Setting up NetworkTables server");
			ntinst.startServer();
		} else {
			System.out.println("Setting up NetworkTables client for team " + CameraServerConfig.team);
			ntinst.startClientTeam(CameraServerConfig.team);
		}
	}

}
