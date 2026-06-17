package tech.smartboot.feat.cloud.annotation.orm;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 一对多嵌套查询，与 MyBatis {@code @Many} 行为一致。
 */
@Target({})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Many {
    /**
     * 嵌套查询语句 ID。
     */
    String select() default "";

    /**
     * 加载策略。
     */
    FetchType fetchType() default FetchType.DEFAULT;
}
