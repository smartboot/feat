/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 注解用于标识一个类作为控制器，处理HTTP请求。
 * 该注解应用在类级别上，表明此类中包含的请求处理方法。
 *
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 * @see tech.smartboot.feat.cloud.annotation.RequestMapping
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Controller {
    /**
     * 定义控制器的请求路径前缀。
     *
     * @return 控制器级别的请求路径前缀，默认为空字符串。
     */
    String value() default "";
}
