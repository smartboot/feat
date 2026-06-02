package tech.smartboot.feat.cloud.annotation.orm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Result {
    /**
     * 数据库列名
     */
    String column();
    
    /**
     * Java 对象属性名
     */
    String property();
    
    /**
     * Java 类型（可选）
     * 用于类型转换
     */
    Class<?> javaType() default void.class;
    
    /**
     * JDBC 类型（可选）
     * 用于指定数据库列的 JDBC 类型
     */
    String jdbcType() default "";
    
    /**
     * 是否为主键
     */
    boolean id() default false;
}
