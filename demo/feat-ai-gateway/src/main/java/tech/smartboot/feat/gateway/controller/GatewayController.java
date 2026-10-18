package tech.smartboot.feat.gateway.controller;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.gateway.config.ConfigStore;
import tech.smartboot.feat.gateway.config.KeyPolicy;
import tech.smartboot.feat.gateway.policy.AuthPolicy;
import tech.smartboot.feat.gateway.proxy.ChatProxy;
import tech.smartboot.feat.gateway.proxy.Responder;
import tech.smartboot.feat.gateway.surface.ChatRoute;
import tech.smartboot.feat.gateway.surface.NotFoundRoute;
import tech.smartboot.feat.router.Router;

/**
 * 用于处理 AI 网关的自定义路由。
 * <p>
 * 两类路由都在这个类里，区别只在于"需不需要知道连接什么时候断"：
 * <ul>
 *   <li>{@link #models} —— 普通请求-响应，{@code @RequestMapping} 声明即可，路由由注解处理器编译期生成；</li>
 *   <li>{@link #dataPlaneRoutes} —— 转发与流式端点，必须覆写 {@code onClose} 才能在调用方断开时取消上游，
 *       而 {@code @Controller} 生成的处理器只有 {@code handle()}，所以改用 {@code @Bean} 在启动阶段
 *       拿 {@link Router} 手工注册（见 DESIGN.md ADR-8）。</li>
 * </ul>
 * 两条路径进的是同一棵路由树：精确节点优先于 {@code /v1/*} 通配，互不遮蔽。
 */
@Controller
public class GatewayController {

    @Autowired
    private ConfigStore configStore;

    @Autowired
    private ChatProxy chatProxy;

    public GatewayController() {
    }

    public void setConfigStore(ConfigStore configStore) {
        this.configStore = configStore;
    }

    public void setChatProxy(ChatProxy chatProxy) {
        this.chatProxy = chatProxy;
    }

    // ------------------------------------------------------------------ 声明式路由

    /**
     * {@code GET /v1/models}：只暴露配置里声明过的模型名，不透出上游清单。
     * <p>
     * 返回 {@code void}：生成的处理器在方法返回后完成 future，此时响应已写完，
     * 语义正好等于"同步处理完毕"。
     */
    @RequestMapping(value = "/v1/models", method = RequestMethod.GET)
    public void models(HttpRequest request) {
        KeyPolicy key = AuthPolicy.authenticate(configStore.current(), request.getHeader("Authorization"));
        if (key == null) {
            request.getResponse().setHeader("WWW-Authenticate", "Bearer");
            Responder.error(request, 401, "invalid or missing api key", "authentication_error", "invalid_api_key");
            return;
        }
        chatProxy.listModels(request);
    }

    // ------------------------------------------------------------------ 数据面路由

    /**
     * 注册转发端点。方法名即 Bean 名，参数按名字从容器取（容器里有外部注册的 {@code router}）。
     * <p>
     * 这个 {@code @Bean} 方法在 {@code loadMethodBean} 阶段执行，早于控制器路由的生成，
     * 但两者写入的是同一个 {@link Router}，注册顺序不影响匹配结果。
     */
    @Bean
    public ChatRoute dataPlaneRoutes(Router router, ConfigStore configStore, ChatProxy chatProxy) {
        ChatRoute chatRoute = new ChatRoute(configStore, chatProxy);
        router.route(ChatProxy.PATH_CHAT, "POST", chatRoute);
        router.route(ChatProxy.PATH_COMPLETIONS, "POST", chatRoute);
        router.route(ChatProxy.PATH_EMBEDDINGS, "POST", chatRoute);
        // 未知端点也要吐 OpenAI 的错误结构，不能返回静态资源的 HTML 页
        router.route("/v1/*", new NotFoundRoute());
        return chatRoute;
    }
}
