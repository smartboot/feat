package tech.smartboot.feat.cloud.annotation.orm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Mapper {
    /**
     * 可选的命名空间，用于区分不同的 Mapper
     * 默认为空，使用类的全限定名作为命名空间
     */
    String namespace() default "";
}
