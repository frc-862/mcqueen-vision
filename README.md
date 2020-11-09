# Lightning Vision

![gradle build](https://github.com/edurso/lightning-vision/workflows/gradle%20build/badge.svg)

Program to be uploaded to rPi vision module with [this disk image](https://github.com/wpilibsuite/FRCVision-pi-gen/releases/latest).

## Program Components

### `VisionEngine`

This is the main application that will automatically configure and run pipelines configured in Pipelines when the application is run on the rPi.

### `Processing`

This contains classes that are used to create and process pipelines defined in Pipelines.

### `Pipelines`

Pipelines are written here. It is recommended that pipelines generated from [GRIP](https://github.com/WPIRoboticsProjects/GRIP/releases/latest) are left unmodified and a wrapper class is written such as below.

```java
@Pipeline(camera=0)
@Disabled
public class GRIPPipelineWrapper implements AbstractVisionPipeline {
    private GRIPPipeline inst;
    private NetworkTable ntab;

    public GRIPPipelineWrapper() {
        inst = new GRIPPipeline();
        ntab = ntinst.getTable("SmartDashboard");
    }

    @Override
    public void process(Mat mat) {
        inst.process(mat);
    }

    @Override
    public void log() {
        // Log to Network Table `ntab` here.
    }  
}
```

## Building on Desktop

### Building

Java 11 is required to build.  Set your path and/or JAVA_HOME environment variable appropriately.

1) Run `./gradlew build`

### Deploying

On the rPi web dashboard:

1) Make the rPi writable by selecting the `Writable` tab
2) In the rPi web dashboard Application tab, select the `Uploaded Java jar` option for Application
3) Click `Browse...` and select the `VisionEngine.jar` file in your desktop project directory in the `install` subdirectory
4) Click Save

The application will be automatically started. Console output can be seen by enabling console output in the Vision Status tab.

## Building Locally on rPi

1) Run `./gradlew build`
2) Run `./install.sh` (replaces scripts and executables) 
3) Run `./runInteractive` in /home/pi or `sudo svc -t /service/camera` to restart service.
