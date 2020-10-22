package util.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)  
@Target(ElementType.TYPE)
public @interface Pipeline {
    int camera() default 0;
}
