package tech.smartboot.feat.cloud.annotation.orm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Results {
    /**
     * 结果映射数组
     */
    Result[] value() default {};
    
    /**
     * 结果映射的 ID（可选）
     * 可用于其他地方引用此结果映射
     */
    String id() default "";
}
