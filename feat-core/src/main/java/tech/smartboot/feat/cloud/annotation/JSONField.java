package tech.smartboot.feat.cloud.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD})
public @interface JSONField {
    /**
     * 定义json字段名
     */
    String name() default "";

    /**
     * 是否序列化字段
     */
    boolean serialize() default true;

    /**
     * 时间格式化
     */
    String format() default "";
}
