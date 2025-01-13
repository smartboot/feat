package tech.smartboot.feat.restful;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;
import tech.smartboot.feat.restful.context.ApplicationContext;
import tech.smartboot.feat.restful.handler.RestfulHandler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/7/2
 */
public class RestfulBootstrap {
    private final ApplicationContext applicationContext = new ApplicationContext();
    private final HttpServer httpServer = new HttpServer() {
        @Override
        public void listen(String host, int port) {
            try {
                applicationContext.start();
                applicationContext.getControllers().forEach(restfulHandler::addInterceptor);
                applicationContext.getControllers().forEach(restfulHandler::addController);
            } catch (Exception e) {
                throw new IllegalStateException("start application exception", e);
            }

            super.listen(host, port);
        }

        @Override
        public void shutdown() {
            try {
                applicationContext.destroy();
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                super.shutdown();
            }
        }
    };
    private final RestfulHandler restfulHandler;

    private static final BaseHttpHandler DEFAULT_HANDLER = new BaseHttpHandler() {
        private final byte[] BYTES = "hello feat-rest".getBytes();

        @Override
        public void handle(HttpRequest request) throws IOException {
            request.getResponse().getOutputStream().write(BYTES);
        }
    };

    private RestfulBootstrap(BaseHttpHandler defaultHandler) {
        if (defaultHandler == null) {
            throw new NullPointerException();
        }
        this.restfulHandler = new RestfulHandler(defaultHandler);
        httpServer.httpHandler(restfulHandler);
    }

    public RestfulBootstrap addBean(String name, Object object) throws Exception {
        applicationContext.addBean(name, object);
        return this;
    }

    public RestfulBootstrap addBean(Object object) throws Exception {
        applicationContext.addBean(object.getClass().getSimpleName().substring(0, 1).toLowerCase() + object.getClass().getSimpleName().substring(1), object);
        return this;
    }

    public RestfulBootstrap scan(String... packageName) throws Exception {
        getApplicationContext().scan(Arrays.asList(packageName));
        return this;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public RestfulBootstrap controller(Class<?>... classes) throws Exception {
        for (Class<?> clazz : classes) {
            applicationContext.addController(clazz);
        }
        return this;
    }

    public static RestfulBootstrap getInstance() throws Exception {
        return getInstance(DEFAULT_HANDLER);
    }

    public static RestfulBootstrap getInstance(BaseHttpHandler defaultHandler) throws Exception {
        return new RestfulBootstrap(defaultHandler);
    }

    public void setAsyncExecutor(ExecutorService asyncExecutor) {
        restfulHandler.setAsyncExecutor(asyncExecutor);
    }

    public HttpServer bootstrap() {
        return httpServer;
    }
}
