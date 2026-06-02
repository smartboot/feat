package tech.smartboot.feat.cloud.annotation.orm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Param {
    /**
     * 参数名称
     * 用于在 SQL 语句中引用该参数，如: #{paramName}
     */
    String value();
}
