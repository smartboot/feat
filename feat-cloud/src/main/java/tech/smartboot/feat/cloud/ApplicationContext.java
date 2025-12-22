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

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.handler.HttpStaticResourceHandler;
import tech.smartboot.feat.router.Router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * 应用上下文类，负责管理云应用的生命周期
 * <p>
 * 该类是Feat云平台的核心组件之一，负责：
 * 1. 管理应用中的Bean实例
 * 2. 协调各个云服务的生命周期
 * 3. 初始化路由配置
 * 4. 提供Bean的获取和注册功能
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public final class ApplicationContext {
    /**
     * 日志记录器
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContext.class);

    /**
     * 命名Bean映射表，用于存储和管理应用中的Bean实例
     */
    private Map<String, Object> namedBeans = new HashMap<>();

    /**
     * 路由器实例，用于处理HTTP请求路由
     */
    private final Router router = new Router(new HttpStaticResourceHandler(opt -> opt.baseDir("classpath:static")));

    /**
     * 云应用配置选项
     */
    private final CloudOptions options;

    /**
     * 云服务列表，存储所有通过ServiceLoader加载的云服务实例
     */
    private final List<CloudService> services = new ArrayList<>();

    /**
     * 构造函数，创建应用上下文实例
     *
     * @param options 云应用配置选项
     */
    ApplicationContext(CloudOptions options) {
        this.options = options;
    }

    /**
     * 获取云应用配置选项
     *
     * @return 云应用配置选项
     */
    public CloudOptions getOptions() {
        return options;
    }

    /**
     * 启动应用上下文，执行云服务的完整生命周期
     * <p>
     * 启动过程包括以下步骤：
     * 1. 通过ServiceLoader加载所有CloudService实现
     * 2. 依次调用各服务的loadBean方法加载Bean
     * 3. 依次调用各服务的autowired方法执行依赖注入
     * 4. 依次调用各服务的postConstruct方法执行初始化
     * 5. 依次调用各服务的router方法配置路由
     * </p>
     *
     * @throws Throwable 启动过程中可能出现的异常
     */
    public void start() throws Throwable {
        // 通过ServiceLoader加载所有CloudService实现类
        for (CloudService service : ServiceLoader.load(CloudService.class)) {
            services.add(service);
        }

        // 依次调用各服务的loadBean方法加载Bean
        for (CloudService service : services) {
            service.loadBean(this);
        }

        // 依次调用各服务的autowired方法执行依赖注入
        for (CloudService service : services) {
            service.autowired(this);
        }

        // 依次调用各服务的postConstruct方法执行初始化
        for (CloudService service : services) {
            service.postConstruct(this);
        }

        // 打印路由配置信息标题
        System.out.println("\u001B[32mFeat Router:\u001B[0m");

        // 依次调用各服务的router方法配置路由
        for (CloudService service : services) {
            service.router(this, router);
        }

        // 释放namedBeans内存（启动完成后不再需要）
        namedBeans = null;
    }

    /**
     * 向应用上下文中添加Bean实例
     * <p>
     * 该方法用于注册Bean实例，确保每个Bean名称唯一
     * </p>
     *
     * @param name   Bean名称
     * @param object Bean实例
     * @throws IllegalStateException 当Bean名称重复时抛出异常
     */
    public void addBean(String name, Object object) {
        // 检查是否已存在同名Bean
        if (namedBeans.containsKey(name)) {
            throw new IllegalStateException("duplicated name[" + name + "] for " + object.getClass().getName());
        }
        // 如果启用了调试日志，则记录Bean注册信息
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("add bean:{} for class:{}", name, object);
        }
        // 将Bean添加到映射表中
        namedBeans.put(name, object);
    }

    /**
     * 销毁应用上下文，执行清理工作
     * <p>
     * 依次调用所有云服务的destroy方法，执行资源清理工作
     * </p>
     */
    public void destroy() {
        // 依次调用各服务的destroy方法执行清理工作
        for (CloudService service : services) {
            try {
                service.destroy();
            } catch (Throwable e) {
                // 如果某个服务清理过程中出现异常，记录日志但继续处理其他服务
                LOGGER.error("error destroying apt loader", e);
            }
        }
    }

    /**
     * 获取路由器实例
     *
     * @return 路由器实例
     */
    Router getRouter() {
        return router;
    }

    /**
     * 根据名称获取Bean实例
     * <p>
     * 从namedBeans映射表中查找并返回指定名称的Bean实例
     * </p>
     *
     * @param name Bean名称
     * @param <T>  Bean类型
     * @return Bean实例，如果不存在则返回null
     */
    public <T> T getBean(String name) {
        return (T) namedBeans.get(name);
    }
}