package tech.smartboot.feat.cloud;

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.router.Router;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 6/23/23
 */
public class ApplicationContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContext.class);
    private final Map<String, Object> namedBeans = new HashMap<>();

    private final java.util.ServiceLoader<ServiceLoader> serviceLoader = java.util.ServiceLoader.load(ServiceLoader.class);
    private final Router router = new Router(new StaticResourceHandler());
    private final CloudOptions options;


    public ApplicationContext(CloudOptions options) {
        this.options = options;
    }

    public void start() {
        for (ServiceLoader aptLoader : serviceLoader) {
            if (skip(aptLoader)) {
                continue;
            }
            try {
                aptLoader.loadBean(this);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        for (ServiceLoader aptLoader : serviceLoader) {
            if (skip(aptLoader)) {
                continue;
            }
            aptLoader.autowired(this);
        }
        for (ServiceLoader aptLoader : serviceLoader) {
            if (skip(aptLoader)) {
                continue;
            }
            try {
                aptLoader.postConstruct(this);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("\u001B[32mFeat Router:\u001B[0m");
        for (ServiceLoader aptLoader : serviceLoader) {
            if (skip(aptLoader)) {
                continue;
            }
            aptLoader.router(router);
        }
    }

    private boolean skip(ServiceLoader aptLoader) {
        if (options.getPackages() != null && options.getPackages().length > 0) {
            for (String pkg : options.getPackages()) {
                if (aptLoader.getClass().getName().startsWith(pkg)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void addBean(String name, Object object) {
        if (namedBeans.containsKey(name)) {
            throw new IllegalStateException("duplicated name[" + name + "] for " + object.getClass().getName());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("add bean:{} for class:{}", name, object);
        }
        namedBeans.put(name, object);
    }

    public void destroy() {
        for (ServiceLoader aptLoader : serviceLoader) {
            if (skip(aptLoader)) {
                continue;
            }
            try {
                aptLoader.destroy();
            } catch (Throwable e) {
                LOGGER.error("error destroying apt loader", e);
            }
        }
    }

    public Router getRouter() {
        return router;
    }

    public <T> T getBean(String name) {
        return (T) namedBeans.get(name);
    }
}
