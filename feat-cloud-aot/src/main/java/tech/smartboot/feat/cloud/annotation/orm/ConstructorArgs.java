package tech.smartboot.feat.cloud.annotation.orm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 构造器参数映射，与 MyBatis {@code @ConstructorArgs} 行为一致。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ConstructorArgs {
    /**
     * 构造器参数映射数组。
     */
    Arg[] value() default {};
}
