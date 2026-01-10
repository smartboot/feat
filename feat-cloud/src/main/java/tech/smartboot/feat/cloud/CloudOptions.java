/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud;

import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.ServerOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * 云应用配置选项类
 * <p>
 * 该类继承自ServerOptions，扩展了云应用特有的配置选项，
 * 包括包扫描范围和外部Bean注册等功能。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class CloudOptions extends ServerOptions {
    /**
     * 包扫描范围数组，用于指定需要扫描的包路径
     */
    private String[] packages;

    /**
     * 外部Bean映射表，用于注册应用启动前需要注入的Bean实例
     */
    private final Map<String, Object> externalBeans = new HashMap<>();

    /**
     * 静态资源路径，用于指定静态资源文件的存放位置。默认为"classpath:static"。
     *
     */
    private String staticLocations = "classpath:static";

    /**
     * 获取包扫描范围数组
     *
     * @return 包扫描范围数组
     */
    String[] getPackages() {
        return packages;
    }

    /**
     * 设置包扫描范围
     * <p>
     * 用于指定需要扫描的包路径，只有在指定包范围内的类才会被处理。
     * </p>
     *
     * @param packages 包扫描范围数组
     * @return 当前CloudOptions实例，支持链式调用
     */
    public CloudOptions setPackages(String... packages) {
        this.packages = packages;
        return this;
    }

    /**
     * 获取外部Bean映射表
     *
     * @return 外部Bean映射表
     */
    Map<String, Object> getExternalBeans() {
        return externalBeans;
    }

    /**
     * 注册外部Bean实例
     * <p>
     * 用于在应用启动前注册需要注入的Bean实例，确保这些Bean在应用启动时可用。
     * </p>
     *
     * @param key   Bean名称
     * @param value Bean实例
     * @return 当前CloudOptions实例，支持链式调用
     * @throws FeatException 当Bean名称已存在时抛出异常
     */
    public CloudOptions registerBean(String key, Object value) {
        // 检查是否已存在同名Bean
        if (externalBeans.containsKey(key)) {
            throw new FeatException("bean " + key + " already exists");
        }
        // 注册Bean实例
        externalBeans.put(key, value);
        return this;
    }

    /**
     * 获取静态资源路径
     *
     * @return 静态资源路径
     */
    public String getStaticLocations() {
        return staticLocations;
    }

    /**
     * 设置静态资源路径
     * <p>
     * 用于指定静态资源文件的存放位置。默认为"classpath:static"。
     * </p>
     *
     * @param staticLocations 静态资源路径
     * @return 当前CloudOptions实例，支持链式调用
     */
    public CloudOptions setStaticLocations(String staticLocations) {
        this.staticLocations = staticLocations;
        return this;
    }
}