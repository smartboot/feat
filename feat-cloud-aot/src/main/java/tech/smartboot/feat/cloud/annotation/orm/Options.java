/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *  and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.annotation.orm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MyBatis 风格的额外选项配置。
 *
 * <p>与 {@link Insert}、{@link Update}、{@link Delete} 配合使用时，
 * 该注解优先生效；未配置时回退到 SQL 注解自身的属性。</p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Options {
    /**
     * 是否使用数据库生成的主键。
     */
    boolean useGeneratedKeys() default false;

    /**
     * 主键对应的 Java 属性名。
     */
    String keyProperty() default "";

    /**
     * 主键对应的数据库列名。
     */
    String keyColumn() default "";

    /**
     * 执行超时时间（秒），0 表示不限制。
     */
    int timeout() default 0;

    /**
     * JDBC 每次从数据库获取的行数，0 表示使用驱动默认值。
     */
    int fetchSize() default 0;

    /**
     * 结果集滚动类型。
     */
    ResultSetType resultSetType() default ResultSetType.FORWARD_ONLY;

    /**
     * JDBC 语句类型。
     */
    StatementType statementType() default StatementType.PREPARED;
}
