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


import tech.smartboot.feat.router.Router;

/**
 * 云服务接口，定义了云应用的生命周期管理方法
 * <p>
 * 该接口是Feat云平台的核心组件之一，用于管理云服务的整个生命周期，
 * 包括Bean加载、依赖注入、初始化、销毁和路由配置等阶段。
 * </p>
 * <p>
 * 实现类需要通过Java的ServiceLoader机制进行加载，
 * 系统启动时会自动发现并注册所有实现类。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 * @see ApplicationContext 服务上下文环境
 * @see Router 路由配置器
 */
public interface CloudService {
    /**
     * 加载Bean实例到应用上下文中
     * <p>
     * 该方法在应用启动的早期阶段被调用，用于创建和注册Bean实例。
     * 实现类应该在此阶段创建所需的对象实例，并通过context.addBean()方法注册到上下文中。
     * </p>
     *
     * @param context 应用上下文环境
     * @throws Throwable 加载过程中可能抛出的异常
     * @see ApplicationContext#addBean(String, Object)
     */
    void loadBean(ApplicationContext context) throws Throwable;

    void loadMethodBean(ApplicationContext context) throws Throwable;

    /**
     * 执行依赖注入
     * <p>
     * 在所有Bean实例加载完成后调用，用于处理Bean之间的依赖关系。
     * 实现类应该在此阶段为已创建的Bean实例注入其依赖的其他Bean。
     * </p>
     *
     * @param context 应用上下文环境，可通过getContext.getBean()获取依赖的Bean
     * @throws Throwable 依赖注入过程中可能抛出的异常
     * @see ApplicationContext#getBean(String)
     */
    void autowired(ApplicationContext context) throws Throwable;

    /**
     * 执行初始化后处理
     * <p>
     * 在依赖注入完成后调用，用于执行Bean的初始化逻辑。
     * 实现类可以在此阶段执行一些需要在所有依赖注入完成后的初始化操作，
     * 如启动定时任务、初始化缓存等。
     * </p>
     *
     * @param context 应用上下文环境
     * @throws Throwable 初始化过程中可能抛出的异常
     */
    void postConstruct(ApplicationContext context) throws Throwable;

    /**
     * 执行销毁前处理
     * <p>
     * 在应用关闭时调用，用于执行资源清理工作。
     * 实现类应该在此阶段释放占用的资源，如关闭数据库连接、
     * 停止定时任务、清理缓存等。
     * </p>
     *
     * @throws Throwable 销毁过程中可能抛出的异常
     */
    void destroy() throws Throwable;

    /**
     * 配置HTTP路由
     * <p>
     * 在所有初始化完成后调用，用于配置HTTP请求的路由映射。
     * 实现类应该在此阶段将URL路径与处理逻辑进行绑定。
     * </p>
     *
     * @param context 应用上下文环境
     * @param router  路由配置器，用于注册URL与处理逻辑的映射关系
     * @see Router#route(String, tech.smartboot.feat.router.RouterHandler)
     */
    void router(ApplicationContext context, Router router);

}