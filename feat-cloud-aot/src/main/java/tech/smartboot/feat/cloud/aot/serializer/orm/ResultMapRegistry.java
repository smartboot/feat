/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *  and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer.orm;

import tech.smartboot.feat.cloud.annotation.orm.Result;
import tech.smartboot.feat.cloud.annotation.orm.Results;
import tech.smartboot.feat.cloud.annotation.orm.Select;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.Map;

/**
 * 收集并解析 Mapper 接口中的 {@link Results} 结果映射。
 *
 * <p>支持 {@link Select#resultMap()} 引用同一接口中其他方法声明的命名结果映射，
 * 保持与 MyBatis 注解一致的体验。</p>
 */
public final class ResultMapRegistry {

    private ResultMapRegistry() {
    }

    /**
     * 扫描 Mapper 接口，建立 resultMap id -> {@link Results} 的索引。
     */
    public static Map<String, Results> build(TypeElement mapper) {
        Map<String, Results> registry = new HashMap<>();
        for (Element e : mapper.getEnclosedElements()) {
            if (!(e instanceof ExecutableElement)) {
                continue;
            }
            Results results = e.getAnnotation(Results.class);
            if (results == null || results.id().isEmpty()) {
                continue;
            }
            registry.put(results.id(), results);
        }
        return registry;
    }

    /**
     * 解析当前 {@link Select} 方法实际使用的 {@link Results}。
     *
     * <p>优先级：{@code resultMap} 引用 > 方法自身 {@link Results}。</p>
     */
    public static Results resolve(ExecutableElement method, Select select, Map<String, Results> registry) {
        String ref = select.resultMap();
        if (!ref.isEmpty() && registry.containsKey(ref)) {
            return registry.get(ref);
        }
        return method.getAnnotation(Results.class);
    }

    /**
     * 将 {@link Results} 转换为 property -> {@link Result} 的索引。
     */
    public static Map<String, Result> toResultMap(Results results) {
        Map<String, Result> map = new HashMap<>();
        if (results != null) {
            for (Result result : results.value()) {
                map.put(result.property(), result);
            }
        }
        return map;
    }
}
