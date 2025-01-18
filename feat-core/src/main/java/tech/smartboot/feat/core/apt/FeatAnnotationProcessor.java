package tech.smartboot.feat.core.apt;

import tech.smartboot.feat.core.apt.annotation.Autowired;
import tech.smartboot.feat.core.apt.annotation.Bean;
import tech.smartboot.feat.core.apt.annotation.Controller;
import tech.smartboot.feat.core.apt.annotation.RequestMapping;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.handler.Router;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 该注解表示该处理器支持的 Java 源代码版本
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class FeatAnnotationProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(Bean.class.getCanonicalName());
        types.add(Autowired.class.getCanonicalName());
        return types;
    }

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
            createAptLoader(element, bean, services);
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(Controller.class)) {
            Controller controller = element.getAnnotation(Controller.class);
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

        return true;
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


//                Writer origwriter = origFileObject.openWriter();
            char[] bytes = new char[1024];
            int length;
            StringBuilder sb = new StringBuilder();
//                while ((length = reader.read(bytes)) != -1) {
//                    sb.append(new String(bytes, 0, length));
//                }
            System.out.println(sb.toString());

            String loaderName = element.getSimpleName() + "BeanAptLoader";
            JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(loaderName);
            Writer writer = javaFileObject.openWriter();
            writer.write("package " + element.getEnclosingElement().toString() + ";\n");
            writer.write("import " + AptLoader.class.getName() + ";\n");
            writer.write("import " + Router.class.getName() + ";\n");
            writer.write("import " + ApplicationContext.class.getName() + ";\n");
            writer.write("public class " + loaderName + "  implements  " + AptLoader.class.getSimpleName() + "{\n");
            writer.write("    private " + element.getSimpleName() + " bean;\n");
            writer.write("    public void loadBean(ApplicationContext applicationContext) {\n");
            writer.write("         bean=new " + element.getSimpleName() + "(); \n");
            writer.write("    applicationContext.addBean(\"" + element.getSimpleName() + "\", bean);\n");
            writer.write("          \n");
            writer.write("    }\n");

            writer.write("    public void autowired(ApplicationContext applicationContext) {\n");
            for (Element field : autowiredFields) {
                String name = field.getSimpleName().toString();
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                writer.write("    bean.set" + name + "(applicationContext.getBean(\"" + field.getSimpleName() + "\"))" +
                        ";\n");
            }
            writer.write("          \n");
            writer.write("    }\n");

            writer.write("public void router(" + Router.class.getSimpleName() + " router){\n");

            if (annotation instanceof Controller) {
                Controller controller = (Controller) annotation;
                //遍历所有方法,获得RequestMapping注解

                for (Element se : element.getEnclosedElements()) {
//                    processingEnv.getElementUtils().getAllAnnotationMirrors(se).stream()
                    for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                        if (RequestMapping.class.getName().equals(mirror.getAnnotationType().toString())) {
                            String requestURL = "";

                            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                                ExecutableElement k = entry.getKey();
                                AnnotationValue v = entry.getValue();
                                if ("value".equals(k.getSimpleName().toString())) {
                                    requestURL = v.toString();

                                } else if ("method".equals(k.getSimpleName().toString())) {
                                    System.out.println(v.getValue());
//                                    writer.write(v.getValue().toString());
                                }
                            }
                            writer.write("    router.route(" + requestURL + ", req->{\n");
                            writer.write("        bean." + se.getSimpleName() + "(");
                            boolean first = true;
                            for (VariableElement param : ((ExecutableElement) se).getParameters()) {
                                if (first) {
                                    first = false;
                                } else {
                                    writer.write(",");
                                }
                                if (param.asType().toString().equals(HttpRequest.class.getName())) {
                                    writer.write("req");
                                } else if (param.asType().toString().equals(HttpResponse.class.getName())) {
                                    writer.write("req.getResponse()");
                                }
//                                writer.write("req.getParam(\"" + param.getSimpleName() + "\")");
                            }
                            writer.write("   );\n });\n");
                        }
                    }
//                    se.getAnnotationsByType(RequestMapping.class);
//                    RequestMapping requestMapping = se.getAnnotation(RequestMapping.class);
//                    if (requestMapping == null) {
//                        continue;
//                    }
//                    writer.write("    router.addRouter(\"" + controller.value() + "\", bean);\n");
                }
            }
            writer.write("}\n");
            writer.write("    public void destroy() {\n");
            writer.write("          \n");
            writer.write("    }\n");
            writer.write("}");
            writer.close();

            //生成service配置
            services.add(element.getEnclosingElement().toString() + "." + loaderName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
