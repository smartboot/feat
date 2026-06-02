package tech.smartboot.feat.cloud.annotation.orm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Select {
    /**
     * SQL 查询语句
     * 支持占位符参数，如: #{paramName}
     */
    String value();
    
    /**
     * 结果集映射配置（可选）
     * 用于指定如何将查询结果映射到 Java 对象
     */
    String resultMap() default "";
    
    /**
     * 返回类型（可选）
     * 当不使用 resultMap 时，指定返回的 Java 类型
     */
    Class<?> resultType() default void.class;
}
