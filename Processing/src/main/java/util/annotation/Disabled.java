package util.annotation;

import java.lang.annotation.*;

/**
 * Anotation used to prevent pipelines also annotated with 
 * {@link util.annotation.Pipeline Pipeline} from being run.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Disabled {}
