package tech.smartboot.feat.core.apt.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author qinluo
 * @date 2023-07-07 14:49:11
 * @since 1.3.0
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    /**
     * 参数名称
     *
     * @return 参数名称
     */
    String value();

    /**
     * 参数提取范围
     *
     * @return 参数提取范围
     */
    Scope scope() default Scope.URL;
}
