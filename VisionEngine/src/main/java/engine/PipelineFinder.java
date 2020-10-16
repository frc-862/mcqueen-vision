package engine;

import java.util.HashSet;
import java.util.Set;

import org.reflections.Reflections;

import util.AbstractVisionPipeline;

public class PipelineFinder {

    private static Reflections reflections = new Reflections("pipeline");

    public static Set<String> retrieve() {
        Set<Class<? extends AbstractVisionPipeline>> subTypes = reflections.getSubTypesOf(AbstractVisionPipeline.class);
        Set<String> typeNames = new HashSet<>();
        for(Class<? extends AbstractVisionPipeline> type : subTypes) {
            String name = type.getName();
            typeNames.add(name);
        }
        return typeNames;
    }

}