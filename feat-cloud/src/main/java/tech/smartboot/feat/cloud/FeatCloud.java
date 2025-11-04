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

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.HttpServer;

import java.util.function.Consumer;

/**
 * Feat云平台入口类，对标Spring Boot的使用方式
 * <p>
 * 该类提供了简化云应用启动的静态方法，隐藏了应用上下文和服务器配置的复杂性，
 * 让开发者可以像使用Spring Boot一样快速启动Feat云应用。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class FeatCloud {

    /**
     * 创建默认配置的云服务器实例
     * <p>
     * 使用默认配置创建云服务器实例，不进行任何额外配置。
     * </p>
     *
     * @return 配置完成的HTTP服务器实例
     */
    public static HttpServer cloudServer() {
        return cloudServer(opts -> {
        });
    }

    /**
     * 创建自定义配置的云服务器实例
     * <p>
     * 通过传入配置消费者函数来自定义云服务器配置，包括端口、包扫描范围等。
     * </p>
     *
     * @param options 云服务器配置消费者函数
     * @return 配置完成的HTTP服务器实例
     * @throws FeatException 当应用启动过程中出现异常时抛出
     */
    public static HttpServer cloudServer(Consumer<CloudOptions> options) {
        // 创建云应用配置选项实例
        CloudOptions opt = new CloudOptions();
        // 应用自定义配置
        options.accept(opt);
        // 创建应用上下文实例
        ApplicationContext application = new ApplicationContext(opt);
        // 注册外部Bean
        opt.getExternalBeans().forEach(application::addBean);
        try {
            // 启动应用上下文
            application.start();
        } catch (Throwable e) {
            // 如果启动过程中出现异常，则封装为FeatException抛出
            throw new FeatException("application start exception", e);
        }

        // 创建HTTP服务器实例
        HttpServer server = Feat.httpServer(opt);
        // 获取原有的关闭钩子
        Runnable shutdownHook = server.options().shutdownHook();
        if (shutdownHook != null) {
            // 如果存在原有关闭钩子，则组合执行应用销毁和原有关闭钩子
            server.options().shutdownHook(() -> {
                application.destroy();
                shutdownHook.run();
            });
        } else {
            // 如果不存在原有关闭钩子，则只执行应用销毁
            server.options().shutdownHook(application::destroy);
        }
        // 设置HTTP请求处理器为应用路由器
        server.httpHandler(application.getRouter());
        // 添加JVM关闭钩子，确保服务器能够优雅关闭
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        return server;
    }
}