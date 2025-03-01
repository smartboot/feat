package tech.smartboot.feat.router;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Interceptor {
    /**
     * 拦截路径
     * @return 路径
     */
    List<String> pathPatterns();

    /**
     * 拦截请求
     * @param context 上下文
     * @param completableFuture 完成回调
     * @param chain 链
     */
    void intercept(Context context, CompletableFuture<Object> completableFuture, Chain chain) throws Throwable;


}