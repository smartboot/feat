package tech.smartboot.feat.cloud.annotation.orm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Insert {
    /**
     * SQL 插入语句
     * 支持占位符参数，如: #{paramName}
     */
    String value();
    
    /**
     * 是否使用生成的主键（可选）
     * 如果设置为 true，将尝试获取数据库生成的主键值
     */
    boolean useGeneratedKeys() default false;
    
    /**
     * 主键列名（可选）
     * 当 useGeneratedKeys 为 true 时，指定主键列名
     */
    String keyProperty() default "";
    
    /**
     * 主键列的 Java 属性名（可选）
     * 当 useGeneratedKeys 为 true 时，指定将生成的主键值设置到哪个属性
     */
    String keyColumn() default "";
}
