# Lightning Vision

![gradle build](https://github.com/edurso/lightning-vision/workflows/gradle%20build/badge.svg)

Program to be uploaded to rPi vision module with [this disk image](https://github.com/wpilibsuite/FRCVision-pi-gen/releases/latest).

## Program Set Up

After setting up the pi with [the WPILibPi image](https://github.com/wpilibsuite/FRCVision-pi-gen/releases/latest), the configuration file at `/boot/frc.json` must be setup. It shoold look something like as follows. See [this](https://github.com/frc-862/mcqueen-vision/blob/master/VisionEngine/src/main/resources/json-format.txt) for more information.

```json
{
    "team": 862,
    "ntmode": <"client" or "server", "client" if unspecified>
    "cameras": [
        {
            "name": "rPi Camera 0",
            "path": "/dev/video0",
            "pixel format": "mjpg",
            "width": 160,
            "height": 120,
            "fps": 30
        }
    ]
}
```

## Program Modules

### `VisionEngine`

This is the main application that will automatically configure and run pipelines configured in Pipelines when the application is run on the rPi.

### `Processing`

This contains classes that are used to create and process pipelines defined in Pipelines.

### `Pipelines`

OpenCV vision pipelines are written here. These pipelines will typically implement the `LightningVisionPipeline` type which includes an interface to WPILib network tables.

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

Logging pipelines write the camera's frames to a `.jpg` file on a USB key. Logging pipelines are implemented with the `LoggingPipeline` type as can be seen below.

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
- `/mnt/log` is the location where the logging pipelines will write logged images.
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
