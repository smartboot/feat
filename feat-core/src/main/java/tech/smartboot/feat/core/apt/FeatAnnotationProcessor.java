package tech.smartboot.feat.core.apt;

import com.alibaba.fastjson2.JSONObject;
import org.apache.ibatis.annotations.Mapper;
import tech.smartboot.feat.core.apt.annotation.Autowired;
import tech.smartboot.feat.core.apt.annotation.Bean;
import tech.smartboot.feat.core.apt.annotation.Controller;
import tech.smartboot.feat.core.apt.annotation.Param;
import tech.smartboot.feat.core.apt.annotation.PostConstruct;
import tech.smartboot.feat.core.apt.annotation.PreDestroy;
import tech.smartboot.feat.core.apt.annotation.RequestMapping;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.handler.Router;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 该注解表示该处理器支持的 Java 源代码版本
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"tech.smartboot.feat.core.apt.annotation.Bean", "tech.smartboot.feat.core.apt.annotation.Controller"})
public class FeatAnnotationProcessor extends AbstractProcessor {
    private static final int RETURN_TYPE_VOID = 0;
    private static final int RETURN_TYPE_STRING = 1;
    private static final int RETURN_TYPE_OBJECT = 2;
//    @Override
//    public Set<String> getSupportedAnnotationTypes() {
//        Set<String> types = new HashSet<>();
//        types.add(Bean.class.getCanonicalName());
//        types.add(Autowired.class.getCanonicalName());
//        return types;
//    }

    FileObject serviceFile;
    Writer serviceWrite;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            serviceFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + AptLoader.class.getName());
            serviceWrite = serviceFile.openWriter();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<String> services = new ArrayList<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(Bean.class)) {
            Bean bean = element.getAnnotation(Bean.class);
            if (element.getKind() == ElementKind.CLASS) {
                createAptLoader(element, bean, services);
            }
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(Controller.class)) {
            Controller controller = element.getAnnotation(Controller.class);
            createAptLoader(element, controller, services);
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(Mapper.class)) {
            Mapper controller = element.getAnnotation(Mapper.class);
            createAptLoader(element, controller, services);
        }
        // 如果不希望后续的处理器继续处理这些注解，返回 true，否则返回 false
        try {
            for (String service : services) {
                serviceWrite.append(service).append("\n");
            }
            serviceWrite.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    private <T extends Annotation> void createAptLoader(Element element, T annotation, List<String> services) {
        try {

            //获取所有包含Autowired注解的字段
            List<Element> autowiredFields = new ArrayList<>();
            for (Element field : element.getEnclosedElements()) {
                if (field.getAnnotation(Autowired.class) != null) {
                    autowiredFields.add(field);
                }
            }
            //element增加setter方法
            if (!autowiredFields.isEmpty()) {
                FileObject origFileObject = processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH, element.getEnclosingElement().toString(), element.getSimpleName() + ".java");
                String origContent = origFileObject.getCharContent(false).toString();
                int lastIndex = origContent.lastIndexOf("}");
//                    JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(element.getSimpleName(), element);
                StringBuilder writer = new StringBuilder();
                writer.append(origContent.substring(0, lastIndex));
                for (Element field : autowiredFields) {
                    writer.append("    public void set" + field.getSimpleName() + "(" + field.asType() + " " + field.getSimpleName() + ") {\n");
                    writer.append("        this." + field.getSimpleName() + " = " + field.getSimpleName() + ";\n");
                    writer.append("    }\n");
                }
                writer.append("}");
                writer.append("}");

            }


            String loaderName = element.getSimpleName() + "BeanAptLoader";
            JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(loaderName);
            Writer writer = javaFileObject.openWriter();
            writer.write("package " + element.getEnclosingElement().toString() + ";\n");
            writer.write("import " + AptLoader.class.getName() + ";\n");
            writer.write("import " + Router.class.getName() + ";\n");
            writer.write("import " + ApplicationContext.class.getName() + ";\n");
            writer.write("import " + JSONObject.class.getName() + ";\n");
            writer.write("import " + AbstractAptLoader.class.getName() + ";\n");
            writer.write("import com.alibaba.fastjson2.JSON;\n");
            writer.write("public class " + loaderName + "  extends  " + AbstractAptLoader.class.getSimpleName() + "{\n");
            writer.write("    private " + element.getSimpleName() + " bean;\n");
            if (annotation instanceof Mapper) {
                writer.write("    private org.apache.ibatis.session.SqlSessionFactory factory;\n");
            }
            writer.write("    public void loadBean(ApplicationContext applicationContext) {\n");
            if (annotation instanceof Mapper) {
                createMapperBean(element, (Mapper) annotation, writer);
            } else {
                writer.write("         bean=new " + element.getSimpleName() + "(); \n");
            }

            writer.write("    applicationContext.addBean(\"" + element.getSimpleName() + "\", bean);\n");
            writer.write("          \n");
            writer.write("    }\n");

            writer.write("    public void autowired(ApplicationContext applicationContext) {\n");
            for (Element field : autowiredFields) {
                String name = field.getSimpleName().toString();
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                writer.write("    bean.set" + name + "(applicationContext.getBean(\"" + field.getSimpleName() + "\"))" + ";\n");
            }
            if (annotation instanceof Mapper) {
                writer.write("    factory=applicationContext.getBean(\"sessionFactory\");\n");
            }
            writer.write("          \n");
            writer.write("    }\n");

            writer.write("public void router(" + Router.class.getSimpleName() + " router){\n");

            if (annotation instanceof Controller) {
                createController(element, (Controller) annotation, writer);
            }
            writer.write("}\n");
            writer.write("    public void destroy() throws Throwable{\n");
            for (Element se : element.getEnclosedElements()) {
                for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                    if (PreDestroy.class.getName().equals(mirror.getAnnotationType().toString())) {
                        writer.write("    bean." + se.getSimpleName() + "();\n");
                    }
                }
            }
            writer.write("    }\n");
            writer.write("    public void postConstruct(ApplicationContext applicationContext) throws Throwable{\n");
            for (Element se : element.getEnclosedElements()) {
                for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                    if (PostConstruct.class.getName().equals(mirror.getAnnotationType().toString())) {
                        writer.write("    bean." + se.getSimpleName() + "();\n");
                    }
                }
            }
            writer.write("    }\n");
            writer.write("}");
            writer.close();

            //生成service配置
            services.add(element.getEnclosingElement().toString() + "." + loaderName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T extends Annotation> void createController(Element element, Controller annotation, Writer writer) throws IOException {
        Controller controller = annotation;
        //遍历所有方法,获得RequestMapping注解
        String basePath = controller.value();
        if (!StringUtils.startsWith(basePath, "/")) {
            basePath = "/" + basePath;
        }
        if (!StringUtils.endsWith(basePath, "/")) {
            basePath = basePath + "/";
        }

        for (Element se : element.getEnclosedElements()) {
            for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                if (RequestMapping.class.getName().equals(mirror.getAnnotationType().toString())) {
                    String requestURL = "";

                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                        ExecutableElement k = entry.getKey();
                        AnnotationValue v = entry.getValue();
                        if ("value".equals(k.getSimpleName().toString())) {
                            requestURL = v.getValue().toString();
                            if (StringUtils.startsWith(requestURL, "/")) {
                                requestURL = basePath + requestURL.substring(1);
                            } else {
                                requestURL = basePath + requestURL;
                            }
                        } else if ("method".equals(k.getSimpleName().toString())) {
                            System.out.println(v.getValue());
//                                    writer.write(v.getValue().toString());
                        }
                    }
                    if (StringUtils.isBlank(requestURL)) {
                        throw new FeatException("the value of RequestMapping on " + element.getSimpleName() + "@" + se.getSimpleName() + " is not allowed to be empty.");
                    }
                    writer.write("    router.route(\"" + requestURL + "\", req->{\n");

                    boolean first = true;
                    StringBuilder newParams = new StringBuilder();
                    StringBuilder params = new StringBuilder();
                    int i = 0;
                    for (VariableElement param : ((ExecutableElement) se).getParameters()) {
                        if (first) {
                            first = false;
                        } else {
                            params.append(",");
                        }
                        if (param.asType().toString().equals(HttpRequest.class.getName())) {
                            params.append("req");
                        } else if (param.asType().toString().equals(HttpResponse.class.getName())) {
                            params.append("req.getResponse()");
                        } else {
                            if (i == 0) {
                                newParams.append("JSONObject jsonObject=getParams(req);\n");
                            }
                            Param paramAnnotation = param.getAnnotation(Param.class);
                            if (paramAnnotation == null && param.asType().toString().startsWith("java")) {
                                throw new FeatException("the param of " + element.getSimpleName() + "@" + se.getSimpleName() + " is not allowed to be empty.");
                            }
                            if (paramAnnotation != null) {
                                if (param.asType().toString().startsWith(List.class.getName())) {
                                    newParams.append(param.asType().toString()).append(" param").append(i).append("=jsonObject.getObject(\"")
                                            .append(paramAnnotation.value()).append("\",java.util.List.class);\n");
                                } else {
                                    newParams.append(param.asType().toString()).append(" param").append(i).append("=jsonObject.getObject(\"").append(paramAnnotation.value()).append("\",").append(param.asType().toString()).append(".class);\n");
                                }
                            } else {
                                newParams.append(param.asType().toString()).append(" param").append(i).append("=jsonObject.to(").append(param.asType().toString()).append(".class);\n");
                            }
//                                    newParams.append(param.asType().toString()).append(" param").append(i).append("=jsonObject.getObject(").append(param.asType().toString()).append(".class);\n");
                            params.append("param").append(i);
                            i++;
                        }
//                                writer.write("req.getParam(\"" + param.getSimpleName() + "\")");
                    }
                    if (newParams.length() > 0) {
                        writer.write(newParams.toString());
                    }

                    TypeMirror returnType = ((ExecutableElement) se).getReturnType();
                    int returnTypeInt = -1;
                    if (returnType.toString().equals("void")) {
                        returnTypeInt = RETURN_TYPE_VOID;
                    } else if (String.class.getName().equals(returnType.toString())) {
                        returnTypeInt = RETURN_TYPE_STRING;
                    } else {
                        returnTypeInt = RETURN_TYPE_OBJECT;
                    }
                    switch (returnTypeInt) {
                        case RETURN_TYPE_VOID:
                            writer.write("        bean." + se.getSimpleName() + "(");
                            break;
                        case RETURN_TYPE_STRING:
                            writer.write("      String rst = bean." + se.getSimpleName() + "(");
                            break;
                        case RETURN_TYPE_OBJECT:
                            writer.write("      Object rst = bean." + se.getSimpleName() + "(");
                            break;
                        default:
                            throw new RuntimeException("不支持的返回类型");
                    }
                    writer.write(params.toString());

                    writer.write(");\n");

                    switch (returnTypeInt) {
                        case RETURN_TYPE_VOID:
                            break;
                        case RETURN_TYPE_STRING:
                            writer.write("        byte[] bytes=rst.getBytes(\"UTF-8\");\n ");
                            writer.write("        req.getResponse().setContentLength(bytes.length);\n");
                            writer.write("        req.getResponse().write(bytes);\n");
                            break;
                        case RETURN_TYPE_OBJECT:
                            writer.write("        byte[] bytes=JSON.toJSONBytes(rst);\n ");
                            writer.write("        req.getResponse().setContentLength(bytes.length);\n");
                            writer.write("        req.getResponse().write(bytes);\n");
                            break;
                        default:
                            throw new RuntimeException("不支持的返回类型");
                    }
                    writer.write("    });\n");
                }
            }
        }
    }

    private static <T extends Annotation> void createMapperBean(Element element, Mapper annotation, Writer writer) throws IOException {
        writer.write("         bean=new " + element.getSimpleName() + "(){ \n");
        for (Element se : element.getEnclosedElements()) {
            String returnType = ((ExecutableElement) se).getReturnType().toString();
            writer.write("         public " + returnType + " " + se.getSimpleName() + "(");
            boolean first = true;
            for (VariableElement param : ((ExecutableElement) se).getParameters()) {
                if (first) {
                    first = false;
                } else {
                    writer.write(",");
                }
                writer.write(param.asType().toString() + " " + param.getSimpleName());
            }
            writer.write("){\n");
            writer.write("             try (org.apache.ibatis.session.SqlSession session = factory.openSession(true)) {\n");
            writer.write("                 ");
            if (!"void".equals(returnType)) {
                writer.write("return ");
            }
            writer.write("session.getMapper(" + element.getSimpleName() + ".class)." + se.getSimpleName() + "(");
            first = true;
            for (VariableElement param : ((ExecutableElement) se).getParameters()) {
                if (first) {
                    first = false;
                } else {
                    writer.write(",");
                }
                writer.write(param.getSimpleName().toString());
            }
            writer.write(");\n");
            writer.write("             }\n");
            writer.write("         }\n");
        }
        writer.write("};\n");
    }

    public static String getElementPath(Element element) {
        List<String> pathElements = new ArrayList<>();
        while (element != null) {
            pathElements.add(0, element.getSimpleName().toString());
            element = element.getEnclosingElement();
        }
        return String.join(".", pathElements);
    }
}
