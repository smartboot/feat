package tech.smartboot.feat.gateway.config;

/**
 * 一条模型路由：客户端请求的 model 名 -> 某个 provider 上的真实模型名。
 * <p>
 * 同一个 model 可以有多条路由，priority 小的先试，失败后按顺序降级。
 */
public final class ModelRoute {
    private final String name;
    private final String provider;
    private final String upstream;
    private final int priority;

    public ModelRoute(String name, String provider, String upstream, int priority) {
        this.name = name;
        this.provider = provider;
        this.upstream = upstream;
        this.priority = priority;
    }

    public String name() {
        return name;
    }

    public String provider() {
        return provider;
    }

    public String upstream() {
        return upstream;
    }

    public int priority() {
        return priority;
    }

    @Override
    public String toString() {
        return name + "->" + provider + '/' + upstream;
    }
}
