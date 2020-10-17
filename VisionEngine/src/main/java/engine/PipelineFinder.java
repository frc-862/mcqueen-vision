package engine;

import java.util.HashSet;
import java.util.Set;

import org.reflections.Reflections;

import edu.wpi.first.networktables.NetworkTable;
import util.AbstractVisionPipeline;
import util.annotation.*;

public class PipelineFinder {

    private static Reflections reflections = new Reflections("pipeline");

    public static Set<String> retrieve() {

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

    public static int getCamera(AbstractVisionPipeline pipe) {
        Pipeline p = pipe.getClass().getAnnotation(Pipeline.class);
        int camera = p.camera();
        return camera;
    }

    public static String getNetworkTable(AbstractVisionPipeline pipe) {
        Pipeline p = pipe.getClass().getAnnotation(Pipeline.class);
        String ntab = p.ntab();
        return ntab;
    }

}