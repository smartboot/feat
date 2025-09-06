/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import tech.smartboot.feat.ai.mcp.server.McpServer;
import tech.smartboot.feat.cloud.AbstractCloudService;
import tech.smartboot.feat.cloud.ApplicationContext;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.InterceptorMapping;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.cloud.annotation.mcp.McpEndpoint;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.router.Chain;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.Interceptor;
import tech.smartboot.feat.router.Router;
import tech.smartboot.feat.router.RouterHandler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author 三刀
 * @version v1.0 9/4/25
 */
public class NoneAotCloudService extends AbstractCloudService {
    private List<Object> beans = new ArrayList<>();
    private List<Object> controllers = new ArrayList<>();
    private List<Object> mappers = new ArrayList<>();
    private Map<Object, McpServer> mcpServers = new HashMap<>();
    private McpServer rootMcpServer = new McpServer();

    @Override
    public void loadBean(ApplicationContext context) throws Throwable {
        //扫描Bean
        System.out.println("开始加载Bean");
        List<Class> annotations = new ArrayList<>();
        annotations.add(Bean.class);
        annotations.add(Controller.class);
        annotations.add(Mapper.class);
        annotations.add(McpEndpoint.class);
        List<Class> classes = scanBean(annotations);

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Bean.class)) {
                Bean beanAnnotation = clazz.getDeclaredAnnotation(Bean.class);
                Object bean = clazz.newInstance();
                beans.add(bean);
                String beanName = FeatUtils.isBlank(beanAnnotation.value()) ? clazz.getSimpleName().toString().substring(0, 1).toLowerCase() + clazz.getSimpleName().toString().substring(1) : beanAnnotation.value();
                context.addBean(beanName, bean);
            } else if (clazz.isAnnotationPresent(Controller.class)) {
                Constructor constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object constructorBean = constructor.newInstance();
                controllers.add(constructorBean);
                McpEndpoint mcpEndpoint = clazz.getDeclaredAnnotation(McpEndpoint.class);
                if (mcpEndpoint != null) {
                    McpServer mcpServer = new McpServer(opt -> {
                        opt.getImplementation().setName(mcpEndpoint.name()).setTitle(mcpEndpoint.title()).setVersion(mcpEndpoint.version());
                        opt.setMcpEndpoint(mcpEndpoint.streamableEndpoint()).setSseEndpoint(mcpEndpoint.sseEndpoint()).setSseMessageEndpoint(mcpEndpoint.sseMessageEndpoint());
                        if (mcpEndpoint.resourceEnable()) {
                            opt.resourceEnable();
                        }
                        if (mcpEndpoint.toolEnable()) {
                            opt.toolEnable();
                        }
                        if (mcpEndpoint.promptsEnable()) {
                            opt.promptsEnable();
                        }
                        if (mcpEndpoint.loggingEnable()) {
                            opt.loggingEnable();
                        }
                    });
                    mcpServers.put(constructorBean, mcpServer);
                }
            } else if (clazz.isAnnotationPresent(Mapper.class)) {
                Object bean = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, (proxy, method, args) -> {
                    SqlSessionFactory sqlSessionFactory = context.getBean("sqlSessionFactory");
                    try (SqlSession session = sqlSessionFactory.openSession(true)) {
                        return method.invoke(session.getMapper(clazz), args);
                    }
                });
                mappers.add(bean);
                context.addBean(clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1), bean);
            } else if (clazz.isAnnotationPresent(McpEndpoint.class)) {
                throw new RuntimeException("@McpEndpoint can only be used with @Controller!");
            }
        }
    }

    @Override
    public void autowired(ApplicationContext context) throws Throwable {
        List<Object> list = new ArrayList<>();
        list.addAll(controllers);
        list.addAll(beans);
        for (Object bean : list) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                Bean beanAnnotation = method.getAnnotation(Bean.class);
                if (beanAnnotation != null) {
                    Object o = method.invoke(bean);
                    if (FeatUtils.isNotBlank(beanAnnotation.value())) {
                        context.addBean(beanAnnotation.value(), o);
                    } else {
                        context.addBean(method.getReturnType().getSimpleName().substring(0, 1).toLowerCase() + method.getReturnType().getSimpleName().substring(1), o);
                    }
                }
            }
        }

        for (Object bean : list) {
            for (Field field : bean.getClass().getDeclaredFields()) {
                Autowired autowired = field.getAnnotation(Autowired.class);
                if (autowired == null) {
                    continue;
                }
                if (field.getType() == McpServer.class) {
                    McpServer mcpServer = mcpServers.containsKey(bean) ? mcpServers.get(bean) : rootMcpServer;
                    reflectAutowired(bean, field.getName(), mcpServer);
                } else {
                    reflectAutowired(bean, field.getName(), loadBean(field.getName(), context));
                }

            }
        }
    }

    @Override
    public void postConstruct(ApplicationContext context) throws Throwable {
        List<Object> list = new ArrayList<>();
        list.addAll(controllers);
        list.addAll(beans);
        for (Object bean : list) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                PostConstruct annotation = method.getAnnotation(PostConstruct.class);
                if (annotation != null) {
                    method.setAccessible(true);
                    method.invoke(bean);
                }
            }
        }
    }

    @Override
    public void destroy() throws Throwable {
        List<Object> list = new ArrayList<>();
        list.addAll(controllers);
        list.addAll(beans);
        for (Object bean : list) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                PreDestroy annotation = method.getAnnotation(PreDestroy.class);
                if (annotation != null) {
                    method.invoke(bean);
                }
            }
        }
    }

    @Override
    public void router(ApplicationContext context, Router router) {
        for (Object controller : controllers) {
            Controller controllerAnnotation = controller.getClass().getAnnotation(Controller.class);
            for (Method method : controller.getClass().getDeclaredMethods()) {
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                if (requestMapping != null) {
                    String url = getFullUrl(controllerAnnotation.value(), requestMapping.value());
                    printRouter(url, controller.getClass().getName(), method.getName());
                    RouterHandler handler = new RouterHandler() {
                        @Override
                        public void handle(Context ctx) throws Throwable {

                        }
                    };
                    if (requestMapping.method().length == 0) {
                        router.route(url, handler);
                    } else {
                        for (RequestMethod requestMethod : requestMapping.method()) {
                            router.route(url, requestMethod.name(), handler);
                        }
                    }
                }
                InterceptorMapping interceptorMapping = method.getAnnotation(InterceptorMapping.class);
                if (interceptorMapping != null) {
                    List<String> urls = new ArrayList<>();
                    for (String url : interceptorMapping.value()) {
                        urls.add(getFullUrl(controllerAnnotation.value(), url));
                    }
                    router.addInterceptors(urls, new Interceptor() {

                        @Override
                        public void intercept(Context context, CompletableFuture<Void> completableFuture, Chain chain) throws Throwable {

                        }
                    });
                }

            }
        }
    }

    private static String getFullUrl(String baseURL, String tailUrl) {
        String url = baseURL.startsWith("/") ? baseURL : "/" + baseURL;
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        if (tailUrl.startsWith("/")) {
            url = url + tailUrl.substring(1);
        } else {
            url = url + tailUrl;
        }
        return url;
    }

    private List<Class> scanBean(List<Class> annotations) {
        List<Class> classes = new ArrayList<>();
        String packagePath = "./";
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    scanClassesInDirectory("", new File(resource.getFile()), classes, annotations);
                } else if (resource.getProtocol().equals("jar")) {
                    scanClassesInJar(resource, "", classes, annotations);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return classes;
    }

    private void scanClassesInDirectory(String packageName, File directory, List<Class> classes, List<Class> annotations) {
        if (!directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                if (FeatUtils.isBlank(packageName)) {
                    scanClassesInDirectory(file.getName(), file, classes, annotations);
                } else {
                    scanClassesInDirectory(packageName + "." + file.getName(), file, classes, annotations);
                }
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                checkAndAddClass(className, classes, annotations);
            }
        }
    }

    private void scanClassesInJar(URL jarUrl, String packagePath, List<Class> classes, List<Class> annotations) {
        String jarPath = jarUrl.getPath().substring(5, jarUrl.getPath().indexOf("!"));
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith(packagePath) && entry.getName().endsWith(".class")) {
                    String className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                    checkAndAddClass(className, classes, annotations);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkAndAddClass(String className, List<Class> classes, List<Class> annotations) {
        try {
            Class<?> clazz = Class.forName(className);
            for (Class annotation : annotations) {
                if (clazz.isAnnotationPresent(annotation)) {
                    classes.add(clazz);
                    break;
                }
            }
        } catch (ClassNotFoundException e) {
            // 忽略无法加载的类
        }
    }
}