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
 * <p>
 * RequestMapping 注解用于将 HTTP 请求映射到特定的处理方法上。
 * 它可以定义请求路径以及允许的请求方法类型。
 * <p>
 * 示例:
 * <pre>
 * {@code
 * @Controller("/example")
 * public class ExampleController {
 *     @RequestMapping(value = "/get", method = RequestMethod.GET)
 *     public String getExample() {
 *         return "GET request received";
 *     }
 * }
 * }
 * </pre>
 * <p>
 * 在上面的例子中，`@RequestMapping("/example")` 设置了控制器的基础路径，
 * `@RequestMapping(value = "/get", method = RequestMethod.GET)` 则指定了具体的 GET 请求路径。
 *
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface RequestMapping {
    /**
     * 定义与该方法关联的 URL 路径。
     * 默认为空字符串，意味着该方法可以通过基础路径访问。
     *
     * @return 请求路径，默认为空
     */
    String value() default "";

    /**
     * 指定该方法支持的 HTTP 请求类型（如 GET, POST 等）。
     * 默认为空数组，意味着不限制请求方法。
     *
     * @return 支持的请求方法数组，默认为空
     */
    RequestMethod[] method() default {};
}