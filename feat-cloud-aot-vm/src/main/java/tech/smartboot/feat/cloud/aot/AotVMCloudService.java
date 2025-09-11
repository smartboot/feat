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

import com.alibaba.fastjson2.JSONObject;
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
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.cloud.annotation.mcp.McpEndpoint;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.Interceptor;
import tech.smartboot.feat.router.Router;
import tech.smartboot.feat.router.RouterHandler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * AOT 虚拟机
 *
 * @author 三刀
 * @version v1.0 9/4/25
 */
public class AotVMCloudService extends AbstractCloudService {
    private static final Logger logger = LoggerFactory.getLogger(AotVMCloudService.class);
    private List<Object> beans = new ArrayList<>();
    private List<Object> controllers = new ArrayList<>();
    private List<Object> mappers = new ArrayList<>();
    private Map<Object, McpServer> mcpServers = new HashMap<>();
    private McpServer rootMcpServer = new McpServer();

    @Override
    public void loadBean(ApplicationContext context) throws Throwable {
        //扫描Bean
        System.err.println("===================================================================");
        System.err.println("=                                                                 =");
        System.err.println("=  WARNING: Current application is running in AOT virtual mode    =");
        System.err.println("=           It is strongly incompatible with production           =");
        System.err.println("=           environments. Do not use in production!               =");
        System.err.println("=                                                                 =");
        System.err.println("===================================================================");
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
            } else if (clazz.isAnnotationPresent(McpEndpoint.class)) {
                throw new RuntimeException("@McpEndpoint can only be used with @Controller!");
            }
        }
        for (Object controller : controllers) {
            initMethodBean(context, controller);
        }
        for (Object o : beans) {
            initMethodBean(context, o);
        }

        classes.stream().filter(clazz -> clazz.isAnnotationPresent(Mapper.class)).forEach(clazz -> {
            SqlSessionFactory sqlSessionFactory = context.getBean("sessionFactory");
            Object bean = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, (proxy, method, args) -> {
                try (SqlSession session = sqlSessionFactory.openSession(true)) {
                    return method.invoke(session.getMapper(clazz), args);
                }
            });
            mappers.add(bean);
            context.addBean(clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1), bean);
        });
    }

    @Override
    public void autowired(ApplicationContext context) throws Throwable {
        List<Object> list = new ArrayList<>();
        list.addAll(controllers);
        list.addAll(beans);
//        for (Object bean : list) {
//            initMethodBean(context, bean);
//        }

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

    private static void initMethodBean(ApplicationContext context, Object bean) throws InvocationTargetException, IllegalAccessException {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            Bean beanAnnotation = method.getAnnotation(Bean.class);
            if (beanAnnotation != null) {
                Object o;
                // 检查方法是否有参数
                if (method.getParameterCount() > 0) {
                    // 创建参数数组，从ApplicationContext中获取相应的bean
                    Object[] args = new Object[method.getParameterCount()];
                    java.lang.reflect.Parameter[] parameters = method.getParameters();
                    for (int i = 0; i < parameters.length; i++) {
                        String paramName = parameters[i].getName();
                        if (FeatUtils.startsWith(paramName, "arg")) {
                            System.err.println("方法参数名称无法解析，请确保已开启maven-compiler-plugin的parameters选项");
                            System.err.println("示例配置:");
                            System.err.println("<plugin>");
                            System.err.println("    <groupId>org.apache.maven.plugins</groupId>");
                            System.err.println("    <artifactId>maven-compiler-plugin</artifactId>");
                            System.err.println("    <configuration>");
                            System.err.println("\033[1;31m        <parameters>true</parameters>\033[0m");
                            System.err.println("    </configuration>");
                            System.err.println("</plugin>");

                            throw new FeatException("方法参数名称无法解析，请确保已开启maven-compiler-plugin的parameters选项");
                        }
                        args[i] = context.getBean(paramName);
                    }
                    o = method.invoke(bean, args);
                } else {
                    // 原有逻辑，无参数方法
                    o = method.invoke(bean);
                }
                if (FeatUtils.isNotBlank(beanAnnotation.value())) {
                    context.addBean(beanAnnotation.value(), o);
                } else {
                    context.addBean(method.getName(), o);
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
                    final Object controllerInstance = controller;
                    final Method methodInstance = method;
                    RouterHandler handler = new RouterHandler() {
                        @Override
                        public void handle(Context ctx) throws Throwable {
                            logger.warn("当前请求正处于AOT虚拟机模式运行，请勿在生产环境中使用！request: {} controller: {} method: {}", ctx.Request.getRequestURI(), controllerInstance.getClass().getSimpleName(), methodInstance.getName());
                            invokeControllerMethod(controllerInstance, methodInstance, ctx);
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
                    // 调用拦截器方法，应该返回一个Interceptor实例
                    method.setAccessible(true);
                    try {
                        Interceptor interceptor = (Interceptor) method.invoke(controller);
                        router.addInterceptors(urls, interceptor);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
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
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            // 处理URLClassLoader的特殊情况
            if (classLoader instanceof URLClassLoader) {
                URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
                // 直接获取URLClassLoader中的所有URL
                URL[] urls = urlClassLoader.getURLs();

                for (URL url : urls) {
                    String protocol = url.getProtocol();

                    if (protocol.equals("file")) {
                        scanClassesInDirectory("", new File(url.getFile()), classes, annotations);
                    } else if (protocol.equals("jar")) {
                        scanClassesInJar(url, "", classes, annotations);
                    }
                }
            } else {
                // 非URLClassLoader，使用常规方法
                Enumeration<URL> rootUrls = classLoader.getResources("");
                while (rootUrls.hasMoreElements()) {
                    URL url = rootUrls.nextElement();
                    // 处理逻辑与之前相同...
                    if (url.getProtocol().equals("file")) {
                        scanClassesInDirectory("", new File(url.getFile()), classes, annotations);
                    } else if (url.getProtocol().equals("jar")) {
                        scanClassesInJar(url, "", classes, annotations);
                    }
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
        if (directory.isFile() && directory.getName().endsWith(".jar")) {
            try (JarFile jarFile = new JarFile(directory)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith(packageName) && entry.getName().endsWith(".class")) {
                        String className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                        checkAndAddClass(className, classes, annotations);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        } catch (ClassNotFoundException | UnsupportedClassVersionError | NoClassDefFoundError e) {
            // 忽略无法加载的类
        }
    }

    /**
     * 调用控制器方法，处理参数解析和返回值
     */
    private void invokeControllerMethod(Object controller, Method method, Context ctx) throws Throwable {
        method.setAccessible(true);

        // 准备方法参数
        Object[] args = prepareMethodArguments(method, ctx);

        // 调用方法
        Object result = method.invoke(controller, args);

        // 处理返回值
        handleMethodResult(result, method.getReturnType(), ctx);
    }

    /**
     * 准备方法参数
     */
    private Object[] prepareMethodArguments(Method method, Context ctx) throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        java.lang.annotation.Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] args = new Object[parameterTypes.length];

        JSONObject jsonObject = null;
        boolean needJsonParams = false;

        // 首先检查是否需要解析JSON参数
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> paramType = parameterTypes[i];
            if (paramType != tech.smartboot.feat.core.server.HttpRequest.class &&
                    paramType != tech.smartboot.feat.core.server.HttpResponse.class &&
                    paramType != tech.smartboot.feat.core.server.Session.class &&
                    !hasPathParamAnnotation(parameterAnnotations[i])) {
                needJsonParams = true;
                break;
            }
        }

        if (needJsonParams) {
            jsonObject = getParams(ctx.Request);
        }

        // 逐个处理参数
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> paramType = parameterTypes[i];
            java.lang.annotation.Annotation[] annotations = parameterAnnotations[i];

            if (paramType == tech.smartboot.feat.core.server.HttpRequest.class) {
                args[i] = ctx.Request;
            } else if (paramType == tech.smartboot.feat.core.server.HttpResponse.class) {
                args[i] = ctx.Response;
            } else if (paramType == tech.smartboot.feat.core.server.Session.class) {
                args[i] = ctx.session();
            } else {
                PathParam pathParam = getPathParamAnnotation(annotations);
                if (pathParam != null) {
                    args[i] = ctx.pathParam(pathParam.value());
                } else {
                    // 处理JSON参数或查询参数
                    Param param = getParamAnnotation(annotations);
                    if (param != null) {
                        args[i] = jsonObject.getObject(param.value(), paramType);
                    } else {
                        // 如果没有@Param注解，尝试将整个JSON转换为该类型
                        if (paramType.getName().startsWith("java")) {
                            throw new RuntimeException("Java内置类型参数必须使用@Param注解");
                        }
                        args[i] = jsonObject.to(paramType);
                    }
                }
            }
        }

        return args;
    }

    /**
     * 处理方法返回值
     */
    private void handleMethodResult(Object result, Class<?> returnType, Context ctx) throws Exception {
        if (result == null || returnType == void.class || returnType == Void.class) {
            return;
        }

        if (returnType == String.class) {
            byte[] bytes = ((String) result).getBytes("UTF-8");
            ctx.Response.setContentLength(bytes.length);
            ctx.Response.write(bytes);
        } else if (returnType == byte[].class) {
            byte[] bytes = (byte[]) result;
            ctx.Response.setContentLength(bytes.length);
            ctx.Response.write(bytes);
        } else if (returnType == int.class || returnType == Integer.class) {
            String str = String.valueOf(result);
            byte[] bytes = str.getBytes("UTF-8");
            ctx.Response.setContentLength(bytes.length);
            ctx.Response.write(bytes);
        } else if (returnType == boolean.class || returnType == Boolean.class) {
            ctx.Response.setContentType(tech.smartboot.feat.core.common.HeaderValue.ContentType.APPLICATION_JSON);
            String json = String.valueOf(result);
            byte[] bytes = json.getBytes("UTF-8");
            ctx.Response.setContentLength(bytes.length);
            ctx.Response.write(bytes);
        } else {
            // 处理对象类型，序列化为JSON
            ctx.Response.setContentType(tech.smartboot.feat.core.common.HeaderValue.ContentType.APPLICATION_JSON);
            String json = com.alibaba.fastjson2.JSON.toJSONString(result);
            byte[] bytes = json.getBytes("UTF-8");
            ctx.Response.setContentLength(bytes.length);
            ctx.Response.write(bytes);
        }
    }

    /**
     * 检查参数注解中是否有PathParam
     */
    private boolean hasPathParamAnnotation(java.lang.annotation.Annotation[] annotations) {
        for (java.lang.annotation.Annotation annotation : annotations) {
            if (annotation instanceof PathParam) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取PathParam注解
     */
    private PathParam getPathParamAnnotation(java.lang.annotation.Annotation[] annotations) {
        for (java.lang.annotation.Annotation annotation : annotations) {
            if (annotation instanceof PathParam) {
                return (PathParam) annotation;
            }
        }
        return null;
    }

    /**
     * 获取Param注解
     */
    private Param getParamAnnotation(java.lang.annotation.Annotation[] annotations) {
        for (java.lang.annotation.Annotation annotation : annotations) {
            if (annotation instanceof Param) {
                return (Param) annotation;
            }
        }
        return null;
    }
}