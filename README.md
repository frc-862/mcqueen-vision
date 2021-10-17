# McQueen Vision

![gradle build](https://github.com/frc-862/mcqueen-vision/workflows/Build/badge.svg)
[![Docs](https://readthedocs.org/projects/pip/badge/)](https://frc-862.github.io/mcqueen-vision/)

<img src="https://repository-images.githubusercontent.com/223694691/2a138780-0ed4-11ea-979c-27afe32caebe" height="300">

Program to be uploaded to rPi vision module with [this disk image](https://github.com/wpilibsuite/WPILibPi/releases/latest).

## Program Set Up

After setting up the pi with [the WPILibPi image](https://github.com/wpilibsuite/WPILibPi/releases/latest), the configuration file at `/boot/frc.json` must be setup. It shoold look something like as follows. See [here](https://github.com/frc-862/mcqueen-vision/blob/master/VisionEngine/src/main/resources/json-format.txt) for more information.

If not specified, `ntmode` defaults to `client` but in order to use the pi independently of the robot, `ntmode` must be set to `server`.

```json
{
    "team": 862,
    "ntmode": "client",
    "cameras": [
        {
            "name": "rPi Camera 0",
            "path": "/dev/video0",
            "pixel format": "mjpeg",
            "width": 160,
            "height": 120,
            "fps": 30
        }
    ]
}
```

After the pi is set up, this application can be added as a jar file. See the [deployment](#building) section below. If logging to a USB stick is needed, further setup is required. See [here](#logging-pipelines) for more details.

## Program Overview

For ease-of-use, the program is broken up into three individual components: `VisionEngine` which is the main backend for the program, `Processing` which contains the base functionality needed to process OpenCv pipelines, and `Pipelines` which contains OpenCv pipelines to be used on the robot. Most of the work needed will be in this directory.

### `VisionEngine`

This is the main application that will automatically configure and run pipelines configured in Pipelines when the application is run on the rPi.

### `Processing`

This contains classes that are used to create and process pipelines defined in Pipelines.

### `Pipelines`

OpenCv vision pipelines are written here. These pipelines will typically implement the [`LightningVisionPipeline`](https://github.com/frc-862/mcqueen-vision/blob/master/Processing/src/main/java/util/LightningVisionPipeline.java) type which includes an interface to WPILib network tables.

#### GRIP Generated Pipelines

It is recommended that pipelines generated from [GRIP](https://github.com/WPIRoboticsProjects/GRIP/releases/latest) are left unmodified and a wrapper class is written such as below.

```java
@Pipeline(camera=0)
public class GRIPPipelineWrapper implements LightningVisionPipeline {
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

#### Logging Pipelines

Logging pipelines write the camera's frames to a `.jpg` file on a USB key. Logging pipelines are implemented with the [`LoggingPipeline`](https://github.com/frc-862/mcqueen-vision/blob/master/Processing/src/main/java/util/LoggingPipeline.java) type as can be seen below.

```java
@Pipeline(camera=0)
public class SampleLoggingPipeline extends LoggingPipeline {
    public SampleLoggingPipeline() {
        super("sample", "raw-image");
    }
}
```

The above pipeline will log images to the USB drive in the format `img/log-sample-<unixTimeStamp>/raw-image-<imageNumber>.jpg`.

In order to log to a USB key, the following line must be added to the end of `/etc/fstab` on the pi.

```bash
/dev/sda1 /mnt/log vfat auto,nofail,noatime,users,rw,uid=pi,gid=pi 0 0
```

- `/dev/sda1` is the default mounting point for the first USB inserted (sda is device name, 1 is the partition number on the USB key).
  - If a second UBS key is plugged in (or the same USB key is removed without being unmounted) the mounting point may change to `/dev/sdb1`.
- `/mnt/log` is the location where the logging pipelines will write logged images. This may need to be created manually if it does not already exist. After shelling into the pi, `sudo mkdir /mnt/log` will do this from the working directory.
- `vfat` is the file system driver for the USB device.
- The rest of the parameters (generally speaking) allow the USB key to be written to by the pi.

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

## Configuring for Competition

Some network issues have occured at competitions.
To prevent this from happening, add the following to the end of `/etc/hosts`:

```bash
10.8.62.10      roboRIO-862-FRC.lan     # name of roborio
10.8.62.10      roboRIO-862-FRC.frc-field.lan   # name of roborio when connected to FMS
```

Where `10.8.62.10` is the IP address of the raspberry pi on the local network.
