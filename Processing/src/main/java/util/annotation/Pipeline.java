package util.annotation;

import java.lang.annotation.*;

/**
 * Annotation used to register pipelines. To declare a pipeline, 
 * but not have it be run see {@link util.annotation.Disabled Disabled}.
 */
@Retention(RetentionPolicy.RUNTIME)  
@Target(ElementType.TYPE)
public @interface Pipeline {
    /**
     * Camera the pipeline wants to run on.
     * @return The camera port as an {@code int}
     */
    int camera() default 0;
}
