package tech.smartboot.feat.cloud.annotation.orm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 单个构造器参数映射，与 MyBatis {@code @Arg} 行为一致。
 */
@Target({})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Arg {
    /**
     * 数据库列名。
     */
    String column();

    /**
     * Java 类型。
     */
    Class<?> javaType() default void.class;

    /**
     * JDBC 类型。
     */
    String jdbcType() default "";

    /**
     * 是否为主键。
     */
    boolean id() default false;

    /**
     * 嵌套查询语句 ID（暂预留）。
     */
    String select() default "";

    /**
     * 类型处理器（暂预留）。
     */
    Class<?> typeHandler() default void.class;
}
