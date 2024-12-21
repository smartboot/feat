package tech.smartboot.feat.restful.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.smartboot.feat.core.common.utils.CollectionUtils;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.restful.annotation.Autowired;
import tech.smartboot.feat.restful.annotation.Bean;
import tech.smartboot.feat.restful.annotation.Controller;
import tech.smartboot.feat.restful.annotation.PostConstruct;
import tech.smartboot.feat.restful.annotation.PreDestroy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final List<Class<? extends Annotation>> annotations = new ArrayList<>(Arrays.asList(Controller.class, Bean.class));
    private final ServiceLoader<Expand> serviceLoader = ServiceLoader.load(Expand.class);
    private final Map<Expand, List<Class<?>>> expandListMap = new HashMap<>();

    {
        serviceLoader.forEach(expand -> annotations.add(expand.expandAnnotation()));
    }

    public void start() throws Exception {
        for (Map.Entry<Expand, List<Class<?>>> entry : expandListMap.entrySet()) {
            Expand key = entry.getKey();
            List<Class<?>> value = entry.getValue();
            for (Class clazz : value) {
                if (clazz.getAnnotation(key.expandAnnotation()) == null) {
                    throw new IllegalAccessException("@" + key.expandAnnotation().getName() + " annotation must be added to the class: " + clazz.getName());
                }
            }
            key.initializeBean(this, value);
        }
        //依赖注入
        for (Map.Entry<String, Object> entry : namedBeans.entrySet()) {
            initialBean(entry.getValue());
        }
    }

    public void scan(List<String> packages) throws Exception {
        for (String p : packages) {
            Map<Class<? extends Annotation>, List<Class<?>>> map = ClassScanner.findClassesWithAnnotation(annotations, p);
            //注册Bean
            if (map.containsKey(Bean.class)) {
                for (Class<?> clazz : map.get(Bean.class)) {
                    addBean(clazz);
                }
            }

            //注册Controller
            if (map.containsKey(Controller.class)) {
                for (Class<?> clazz : map.get(Controller.class)) {
                    addController(clazz);
                }
            }

            for (Expand expand : serviceLoader) {
                List<Class<?>> classes = map.get(expand.expandAnnotation());
                if (CollectionUtils.isNotEmpty(classes)) {
                    expandListMap.put(expand, classes);
                }
            }
        }
    }

    public void addBean(String name, Object object) throws Exception {
        if (namedBeans.containsKey(name)) {
            throw new IllegalStateException("duplicated name[" + name + "] for " + object.getClass().getName());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("add bean:{} for class:{}", name, object);
        }
        namedBeans.put(name, object);
        for (Method method : object.getClass().getDeclaredMethods()) {
            Bean bean = method.getAnnotation(Bean.class);
            if (bean == null) {
                continue;
            }
            Object o = method.invoke(object);
            if (StringUtils.isNotBlank(bean.value())) {
                addBean(bean.value(), o);
            } else {
                addBean(method.getReturnType().getSimpleName().substring(0, 1).toLowerCase() + method.getReturnType().getSimpleName().substring(1), o);
            }
        }
    }

    public void addBean(Class<?> clazz) throws Exception {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Bean bean = clazz.getAnnotation(Bean.class);
        boolean suc = false;
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameters().length != 0) {
                continue;
            }
            constructor.setAccessible(true);
            Object object = constructor.newInstance();
            if (StringUtils.isNotBlank(bean.value())) {
                addBean(bean.value(), object);
            } else {
                addBean(clazz.getName(), object);
            }
            suc = true;
        }
        if (!suc) {
            LOGGER.warn("no public no-args constructor found for beanClass:{}", clazz.getName());
        }
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

    public List<Object> getControllers() {
        return controllers;
    }

    private void initialBean(Object object) throws IllegalAccessException, InvocationTargetException {
        for (Field field : object.getClass().getDeclaredFields()) {
            Autowired autowired = field.getAnnotation(Autowired.class);
            if (autowired != null) {
                field.setAccessible(true);
                //优先bean name匹配
                Object value = getBean(field.getName());
                // 尝试类名匹配
                if (value == null) {
                    value = getBean(field.getType().getSimpleName().substring(0, 1).toLowerCase() + field.getType().getSimpleName().substring(1));
                }
                // 完整包路径匹配
                if (value == null) {
                    value = getBean(field.getType().getName());
                }
                // 类型推导
                if (value == null) {
                    for (Object o : namedBeans.values()) {
                        if (field.getType().isAssignableFrom(o.getClass())) {
                            if (value != null) {
                                throw new IllegalStateException("match mutil bean for filed:" + field.getName() + " of" + object.getClass());
                            }
                            value = o;
                        }
                    }
                }
                if (value == null) {
                    throw new IllegalStateException("this field [" + field.getName() + "] bean of " + object.getClass() + " is null");
                }
                if (field.getType().isAssignableFrom(value.getClass())) {
                    field.set(object, value);
                } else {
                    throw new IllegalStateException();
                }
            }
        }
        for (Method method : object.getClass().getDeclaredMethods()) {
            PostConstruct postConstruct = method.getAnnotation(PostConstruct.class);
            if (postConstruct != null) {
                method.setAccessible(true);
                method.invoke(object);
            }
        }
    }


    public void destroy() throws InvocationTargetException, IllegalAccessException {
        //依赖注入
        for (Map.Entry<String, Object> entry : namedBeans.entrySet()) {
            Object bean = entry.getValue();
            for (Method method : bean.getClass().getDeclaredMethods()) {
                PreDestroy preDestroy = method.getAnnotation(PreDestroy.class);
                if (preDestroy != null) {
                    method.setAccessible(true);
                    method.invoke(bean);
                }
            }
        }
    }

    public <T> T getBean(String name) {
        return (T) namedBeans.get(name);
    }
}
