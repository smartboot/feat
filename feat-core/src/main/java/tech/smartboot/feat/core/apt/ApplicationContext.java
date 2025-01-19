package tech.smartboot.feat.core.apt;

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.handler.Router;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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


    public void start(Router router) throws Exception {
        for (AptLoader aptLoader : serviceLoader) {
            aptLoader.loadBean(this);
        }
        for (AptLoader aptLoader : serviceLoader) {
            aptLoader.autowired(this);
        }
        for (AptLoader aptLoader : serviceLoader) {
            aptLoader.router(router);
        }
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


    public void addController(Class<?> clazz) throws Exception {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        boolean suc = false;
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameters().length != 0) {
                continue;
            }
            constructor.setAccessible(true);
            Object object = constructor.newInstance();
            addBean(clazz.getName(), object);
            controllers.add(object);
            suc = true;
        }
        if (!suc) {
            LOGGER.warn("no public no-args constructor found for controllerClass: {}", clazz.getName());
        }
    }

    public void destroy() throws InvocationTargetException, IllegalAccessException {
        for (AptLoader aptLoader : serviceLoader) {
            aptLoader.destroy();
        }
    }

    public <T> T getBean(String name) {
        return (T) namedBeans.get(name);
    }
}
