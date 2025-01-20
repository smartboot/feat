package tech.smartboot.feat.core.apt;

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.handler.Router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 6/23/23
 */
public class ApplicationContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContext.class);
    private final Map<String, Object> namedBeans = new HashMap<>();

    private final List<Object> controllers = new ArrayList<>();

    private final ServiceLoader<AptLoader> serviceLoader = ServiceLoader.load(AptLoader.class);

    private String[] packages;

    public ApplicationContext() {
        this(new String[0]);
    }

    public ApplicationContext(String[] packages) {
        this.packages = packages;
    }

    public void start(Router router) {
        for (AptLoader aptLoader : serviceLoader) {
            if (skip(aptLoader)) {
                continue;
            }
            aptLoader.loadBean(this);
        }
        for (AptLoader aptLoader : serviceLoader) {
            if (skip(aptLoader)) {
                continue;
            }
            aptLoader.autowired(this);
        }
        for (AptLoader aptLoader : serviceLoader) {
            if (skip(aptLoader)) {
                continue;
            }
            aptLoader.postConstruct(this);
        }
        for (AptLoader aptLoader : serviceLoader) {
            if (skip(aptLoader)) {
                continue;
            }
            aptLoader.router(router);
        }
    }

    private boolean skip(AptLoader aptLoader) {
        if (packages != null && packages.length > 0) {
            for (String pkg : packages) {
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
        for (AptLoader aptLoader : serviceLoader) {
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

    public <T> T getBean(String name) {
        return (T) namedBeans.get(name);
    }
}
