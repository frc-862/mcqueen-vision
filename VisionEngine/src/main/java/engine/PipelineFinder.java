package engine;

import java.util.HashSet;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.*;

import edu.wpi.first.networktables.NetworkTable;
import util.LightningVisionPipeline;
import util.annotation.*;

public class PipelineFinder {

    private Reflections reflections;

    public PipelineFinder(String pkg) {
        reflections = new Reflections(pkg, new TypeAnnotationsScanner(), new SubTypesScanner());
    }

    public Set<String> retrieve() {

        Set<Class<?>> types = reflections.getTypesAnnotatedWith(Pipeline.class);

        Set<Class<?>> disabledTypes = reflections.getTypesAnnotatedWith(Disabled.class);

        types.removeAll(disabledTypes);

        Set<String> typeNames = new HashSet<>();

        for (Class<?> type : types) {
            String name = type.getName();
            typeNames.add(name);
        }

        return typeNames;
    }

    public static int getCamera(LightningVisionPipeline pipe) {
        Pipeline p = pipe.getClass().getAnnotation(Pipeline.class);
        int camera = p.camera();
        return camera;
    }

}