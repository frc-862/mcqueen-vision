package util;

import java.util.HashSet;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.*;

import util.annotation.*;

/**
 * Class to scan for pipelines to be implemented.
 */
public class PipelineProcesser {

    /**
     * Reflections object to reflect pipelines
     * @see <a href="https://github.com/ronmamo/reflections">Reflections on GitHub</a>
     */
    private Reflections reflections;

    /**
     * Constructor initilizes reflections object
     */
    public PipelineProcesser(String pkg) {
        reflections = new Reflections(pkg, new TypeAnnotationsScanner(), new SubTypesScanner());
    }

    /**
     * Gets all declared pipelines as a {@code Set<String>} object
     * @return the names of the declared pipelines in a set
     */
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

    /**
     * Gets the configured camera for a given pipeline
     * @param pipe Pipeline name
     * @return the camera port (an {@code int}) of the camera the pipeline wants to run on
     */
    public static int getCamera(LightningVisionPipeline pipe) {
        Pipeline p = pipe.getClass().getAnnotation(Pipeline.class);
        int camera = p.camera();
        return camera;
    }

}