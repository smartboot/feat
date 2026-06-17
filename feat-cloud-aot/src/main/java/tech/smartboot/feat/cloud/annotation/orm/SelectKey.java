package tech.smartboot.feat.cloud.annotation.orm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 与 MyBatis {@code @SelectKey} 行为一致：在执行插入前后通过查询获取主键值。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface SelectKey {
    /**
     * 获取主键的 SQL 语句。
     */
    String statement();

    /**
     * 主键值回写到哪个参数属性。
     */
    String keyProperty();

    /**
     * 主键对应的数据库列名。
     */
    String keyColumn() default "";

    /**
     * 是否在插入之前执行。
     */
    boolean before() default false;

    /**
     * 主键的 Java 类型。
     */
    Class<?> resultType() default void.class;

    /**
     * 语句类型，默认使用 PreparedStatement。
     */
    String statementType() default "PREPARED";
}
