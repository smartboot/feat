/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import org.apache.ibatis.annotations.Mapper;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.InterceptorMapping;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.Session;
import tech.smartboot.feat.router.Router;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 该注解表示该处理器支持的 Java 源代码版本

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
//@SupportedAnnotationTypes({"tech.smartboot.feat.core.apt.annotation.Bean", "tech.smartboot.feat.core.apt.annotation.Controller", "org.apache.ibatis.annotations.Mapper"})
public class FeatAnnotationProcessor extends AbstractProcessor {
    private static final int RETURN_TYPE_VOID = 0;
    private static final int RETURN_TYPE_STRING = 1;
    private static final int RETURN_TYPE_OBJECT = 2;
    private static final int RETURN_TYPE_BYTE_ARRAY = 3;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(Bean.class.getCanonicalName());
        types.add(Autowired.class.getCanonicalName());
        types.add(Controller.class.getCanonicalName());
        types.add(Mapper.class.getCanonicalName());
        return types;
    }

    FileObject serviceFile;
    Writer serviceWrite;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            serviceFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + CloudService.class.getName());
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
//            //element增加setter方法
            if (!autowiredFields.isEmpty()) {
                FileObject origFileObject = processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH, element.getEnclosingElement().toString(), element.getSimpleName() + ".java");
                String origContent = origFileObject.getCharContent(false).toString();
                int lastIndex = origContent.lastIndexOf("}");
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
            writer.write("import " + CloudService.class.getName() + ";\n");
            writer.write("import " + Router.class.getName() + ";\n");
            writer.write("import " + ApplicationContext.class.getName() + ";\n");
            writer.write("import " + JSONObject.class.getName() + ";\n");
            writer.write("import " + AbstractServiceLoader.class.getName() + ";\n");
            writer.write("import com.alibaba.fastjson2.JSON;\n");
            writer.write("public class " + loaderName + "  extends  " + AbstractServiceLoader.class.getSimpleName() + "{\n");
            writer.write("    private " + element.getSimpleName() + " bean;\n");
            if (annotation instanceof Mapper) {
                writer.write("    private org.apache.ibatis.session.SqlSessionFactory factory;\n");
            }
            writer.write("    public void loadBean(ApplicationContext applicationContext) throws Throwable{\n");
            if (annotation instanceof Mapper) {
                createMapperBean(element, (Mapper) annotation, writer);
            } else {
                writer.write("         bean=new " + element.getSimpleName() + "(); \n");
            }

            String beanName = element.getSimpleName().toString().substring(0, 1).toLowerCase() + element.getSimpleName().toString().substring(1);
            if (annotation instanceof Bean && !((Bean) annotation).value().isEmpty()) {
                beanName = ((Bean) annotation).value();
            }
            writer.write("    applicationContext.addBean(\"" + beanName + "\", bean);\n");
            for (Element se : element.getEnclosedElements()) {
                for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                    if (Bean.class.getName().equals(mirror.getAnnotationType().toString())) {
                        writer.write("    applicationContext.addBean(\"" + se.getSimpleName() + "\", bean." + se.getSimpleName() + "());\n");
                    }
                }
            }
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
            //扫描拦截器
            addInterceptor(element, writer);

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

        for (Element se : element.getEnclosedElements()) {
            for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                if (RequestMapping.class.getName().equals(mirror.getAnnotationType().toString())) {
                    String requestURL = basePath;

                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                        ExecutableElement k = entry.getKey();
                        AnnotationValue v = entry.getValue();
                        if ("value".equals(k.getSimpleName().toString())) {
                            requestURL = v.getValue().toString();
                            if (basePath.endsWith("/") && requestURL.startsWith("/")) {
                                requestURL = basePath + requestURL.substring(1);
                            } else if (basePath.endsWith("/") && !requestURL.startsWith("/") || !basePath.endsWith("/") && requestURL.startsWith("/") || requestURL.isEmpty()) {
                                requestURL = basePath + requestURL;
                            } else {
                                requestURL = basePath + "/" + requestURL;
                            }
                        } else if ("method".equals(k.getSimpleName().toString())) {
                            System.out.println(v.getValue());
//                                    writer.write(v.getValue().toString());
                        }
                    }
                    if (StringUtils.isBlank(requestURL)) {
                        throw new FeatException("the value of RequestMapping on " + element.getSimpleName() + "@" + se.getSimpleName() + " is not allowed to be empty.");
                    }
                    writer.write("    System.out.println(\" \\u001B[32m|->\\u001B[0m " + requestURL + " ==> " + element.getSimpleName() + "@" + se.getSimpleName() + "\");\n");
                    writer.write("    router.route(\"" + requestURL + "\", ctx->{\n");

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
                            params.append("ctx.Request");
                        } else if (param.asType().toString().equals(HttpResponse.class.getName())) {
                            params.append("ctx.Response");
                        } else if (param.asType().toString().equals(Session.class.getName())) {
                            params.append("ctx.session()");
                        } else if (param.getAnnotation(PathParam.class) != null) {
                            PathParam pathParam = param.getAnnotation(PathParam.class);
                            params.append("ctx.pathParam(\"" + pathParam.value() + "\")");
                        } else {
                            if (i == 0) {
                                newParams.append("JSONObject jsonObject=getParams(ctx.Request);\n");
                            }
                            Param paramAnnotation = param.getAnnotation(Param.class);
                            if (paramAnnotation == null && param.asType().toString().startsWith("java")) {
                                throw new FeatException("the param of " + element.getSimpleName() + "@" + se.getSimpleName() + " is not allowed to be empty.");
                            }
                            if (paramAnnotation != null) {
                                if (param.asType().toString().startsWith(List.class.getName())) {
                                    newParams.append(param.asType().toString()).append(" param").append(i).append("=jsonObject.getObject(\"").append(paramAnnotation.value()).append("\",java.util" + ".List.class);\n");
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
                    } else if ("byte[]".equals(returnType.toString())) {
                        returnTypeInt = RETURN_TYPE_BYTE_ARRAY;
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
                        case RETURN_TYPE_BYTE_ARRAY:
                            writer.write("      byte[] bytes = bean." + se.getSimpleName() + "(");
                            break;
                        case RETURN_TYPE_OBJECT:
                            writer.write("ctx.Response.setContentType(\"application/json\");");
                            writer.write("      " + returnType + " rst = bean." + se.getSimpleName() + "(");
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
                            writer.write("        ctx.Response.setContentLength(bytes.length);\n");
                            writer.write("        ctx.Response.write(bytes);\n");
                            break;
                        case RETURN_TYPE_BYTE_ARRAY:
                            writer.write("        ctx.Response.setContentLength(bytes.length);\n");
                            writer.write("        ctx.Response.write(bytes);\n");
                            break;
                        case RETURN_TYPE_OBJECT:
                            writer.write("java.io.ByteArrayOutputStream os=new java.io.ByteArrayOutputStream();");
                            writeJsonObject(writer, returnType, "rst", 0, new HashMap<>());
                            writer.write("        byte[] bytes=os.toByteArray();\n ");
                            writer.write("        ctx.Response.setContentLength(bytes.length);\n");
                            writer.write("        ctx.Response.write(bytes);\n");
//                            System.out.println("typeMirror:" + stringBuilder);
//                            writer.write("        byte[] bytes=JSON.toJSONBytes(rst);\n ");
//                            writer.write("        ctx.Response.setContentLength(bytes.length);\n");
//                            writer.write("        ctx.Response.write(bytes);\n");
                            break;
//                        case RETURN_TYPE_OBJECT:
//                            writeJsonObject(writer,returnType);
//                            writer.write("        byte[] bytes=JSON.toJSONBytes(rst);\n ");
//                            writer.write("        ctx.Response.setContentLength(bytes.length);\n");
//                            writer.write("        ctx.Response.write(bytes);\n");
//                            break;
                        default:
                            throw new RuntimeException("不支持的返回类型");
                    }
                    writer.write("    });\n");
                }
            }
        }
    }

    int paramIndex = 0;

    public static void writeJsonObject(Writer writer, TypeMirror typeMirror, String obj, int i, Map<TypeMirror, TypeMirror> typeMap0) throws IOException {
        //深层级采用JSON框架序列化，防止循环引用
        if (i > 4) {
            writer.write("if(" + obj + "!=null){\n");
            writer.write("os.write(JSON.toJSONBytes(" + obj + "));\n");
            writer.write("}else{\n");
            writer.write("byte[] bnull={'n','u','l','l'};\n");
            writer.append("os.write(bnull);");
            writer.write("}\n");
            return;
        }
        if (typeMirror instanceof ArrayType) {
            writer.append("os.write('[');\n");
            writer.append("for(" + typeMirror + ")");
            writer.append("os.write(']');\n");
            return;
        } else if (typeMirror.toString().startsWith("java.util.List") || typeMirror.toString().startsWith("java.util.Collection")) {
            writer.append(" if(" + obj + "!=null){\n");
            writer.append("os.write('[');\n");
            TypeMirror type = ((DeclaredType) typeMirror).getTypeArguments().get(0);
            if (typeMap0.containsKey(type)) {
                type = typeMap0.get(type);
            }
            writer.append("boolean first" + i + "=true;\n");
            writer.append("for(" + type + " p" + i + " : " + obj + " ){\n");
            writer.append("if(first" + i + "){\n");
            writer.append("first" + i + "=false;\n");
            writer.append("}\n");
            writer.append("else{\n");
            writer.append("os.write(',');\n");
            writer.append("}\n");
            if (String.class.getName().equals(type.toString())) {
                writer.append("os.write('\"');");
                writer.append("os.write(p" + i + ".getBytes());");
                writer.append("os.write('\"');\n");
            } else {
                writeJsonObject(writer, type, "p" + i, i + 1, typeMap0);
            }

            writer.append("}\n");
            writer.append("os.write(']');\n");
            writer.write("}else{\n");
            writer.write("byte[] bnull={'n','u','l','l'};\n");
            writer.append("os.write(bnull);");
            writer.write("}\n");
            return;
        } else if (typeMirror.toString().startsWith("java.util.Map")) {
            writer.append("os.write(new JSONObject(" + obj + ").toString().getBytes());\n");
            return;
        } else if (typeMirror.toString().endsWith(".JSONObject")) {
            writer.write("if(" + obj + "!=null){\n");
            writer.write("os.write(" + obj + ".toString().getBytes());\n");
            writer.write("}else{\n");
            writer.write("byte[] bnull={'n','u','l','l'};\n");
            writer.append("os.write(bnull);");
            writer.write("}\n");
            return;
        }
        writer.append("os.write('{');\n");

        //获取泛型参数
        List<? extends TypeMirror> typeKey = ((DeclaredType) (((DeclaredType) typeMirror).asElement().asType())).getTypeArguments();
        List<? extends TypeMirror> typeArgs = ((DeclaredType) typeMirror).getTypeArguments();
        Map<TypeMirror, TypeMirror> typeMap = new HashMap<>();
        for (int z = 0; z < typeArgs.size(); z++) {
            typeMap.put(typeKey.get(z), typeArgs.get(z));
        }
        int j = i * 10;
        for (Element se : ((DeclaredType) typeMirror).asElement().getEnclosedElements()) {
            if (se.getKind() != ElementKind.FIELD) {
                continue;
            }
            if (se.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            JSONField jsonField = se.getAnnotation(JSONField.class);
            if (jsonField != null && !jsonField.serialize()) {
                continue;
            }
            if (j++ > i * 10) {
                writer.append("os.write(',');\n");
            }

            TypeMirror type = se.asType();
            if (se.asType().getKind() == TypeKind.TYPEVAR) {
                type = ((DeclaredType) typeMirror).getTypeArguments().get(0);
            }
            String fieldName = se.getSimpleName().toString();
            if (jsonField != null && StringUtils.isNotBlank(jsonField.name())) {
                fieldName = jsonField.name();
            }
            if (type.toString().equals(boolean.class.getName())) {
                writer.append("if(" + obj + ".is").append(se.getSimpleName().toString().substring(0, 1).toUpperCase()).append(se.getSimpleName().toString().substring(1)).append("()){\n");
                String s = toBytesStr("\"" + fieldName + "\":true");
                writer.write("byte[] b" + j + "=" + s + ";\n");
                writer.write("os.write(b" + j + ");\n");
                writer.write("}else{\n");
                String s1 = toBytesStr("\"" + fieldName + "\":false");
                writer.write("byte[] b" + j + "=" + s1 + ";\n");
                writer.write("os.write(b" + j + ");\n");
                writer.write("}\n");
            } else if (Arrays.asList("int", "short", "byte", "long", "float", "double").contains(type.toString())) {
                String s = toBytesStr("\"" + fieldName + "\":");
                writer.write("byte[] b" + j + "=" + s + ";\n");
                writer.write("os.write(b" + j + ");\n");
                writer.append("os.write(String.valueOf(").append(obj).append(".get").append(se.getSimpleName().toString().substring(0, 1).toUpperCase()).append(se.getSimpleName().toString().substring(1)).append("()).getBytes());");
            } else if (String.class.getName().equals(type.toString())) {
                String s = toBytesStr("\"" + fieldName + "\":");
                writer.write("byte[] b" + j + "=" + s + ";\n");
                writer.write("os.write(b" + j + ");\n");
                writer.append("if(").append(obj).append(".get").append(se.getSimpleName().toString().substring(0, 1).toUpperCase()).append(se.getSimpleName().toString().substring(1)).append("()" + "!=null){\n");
                writer.append("os.write('\"');\n");
                writer.write("String s=" + obj + ".get" + se.getSimpleName().toString().substring(0, 1).toUpperCase() + se.getSimpleName().toString().substring(1) + "();\n");
                writer.write("int i=0;\n");
                writer.write("while(true){\n");
                writer.write("int j=s.indexOf(\"\\\"\",i);\n");
                writer.write("if (j == -1) {\n");
                writer.write("os.write(s.substring(i).getBytes());\n");
                writer.write("break;\n");
                writer.write("}else{\n");
                writer.write("os.write(s.substring(i,j).getBytes());\n");
                writer.write("os.write('\\\\');\n");
                writer.write("os.write('\\\"');\n");
                writer.write("i=j+1;\n");
                writer.write("}\n");
                writer.write("}\n");
                writer.append("os.write('\"');\n");
                writer.append("}else{\n");
                writer.write("byte[] bnull={'n','u','l','l'};\n");
                writer.append("os.write(bnull);");
                writer.append("}\n");
            } else if (Date.class.getName().equals(type.toString()) || Timestamp.class.getName().equals(type.toString())) {
                String s = toBytesStr("\"" + fieldName + "\":");
                writer.write("byte[] b" + j + "=" + s + ";\n");
                writer.write("os.write(b" + j + ");\n");
                writer.append("java.util.Date " + fieldName + "=").append(obj).append(".get").append(se.getSimpleName().toString().substring(0, 1).toUpperCase()).append(se.getSimpleName().toString().substring(1)).append("();");
                writer.append("if(" + fieldName + "!=null){\n");
//                writer.append("os.write('\"');\n");
                if (jsonField != null && StringUtils.isNotBlank(jsonField.format())) {
                    writer.append("java.text.SimpleDateFormat sdf=new java.text.SimpleDateFormat(\"" + jsonField.format() + "\");\n");
                    writer.append("os.write('\"');\n");
                    writer.append("os.write(sdf.format(" + fieldName + ").getBytes());\n");
                    writer.append("os.write('\"');\n");
                } else {
                    writer.append("os.write(String.valueOf(" + fieldName + ".getTime()).getBytes());\n");
                }

//                writer.append("os.write('\"');\n");
                writer.append("}else{\n");
                writer.write("byte[] bnull={'n','u','l','l'};\n");
                writer.append("os.write(bnull);");
                writer.append("}\n");
            } else {
                String s = toBytesStr("\"" + fieldName + "\":");
                writer.write("byte[] b" + j + "=" + s + ";\n");
                writer.write("os.write(b" + j + ");\n");
                writeJsonObject(writer, type, obj + ".get" + se.getSimpleName().toString().substring(0, 1).toUpperCase() + se.getSimpleName().toString().substring(1) + "()", i + 1, typeMap);
            }
        }
        writer.append(";\n");
        writer.append("os.write('}');\n");
    }

    private static String toBytesStr(String str) {
        StringBuilder s = new StringBuilder("{");

        for (int i = 0; i < str.length(); i++) {
            if (i > 0) {
                s.append(",");
            }
            s.append('\'').append(str.charAt(i)).append('\'');
        }
        return s + "}";
    }

    private static <T extends Annotation> void addInterceptor(Element element, Writer writer) throws IOException {
        for (Element se : element.getEnclosedElements()) {
            for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                if (!InterceptorMapping.class.getName().equals(mirror.getAnnotationType().toString())) {
                    continue;
                }
                String patterns = "";

                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                    ExecutableElement k = entry.getKey();
                    AnnotationValue v = entry.getValue();
                    if ("value".equals(k.getSimpleName().toString())) {
                        patterns = v.getValue().toString();
                    }
                }
                writer.write("    router.addInterceptors(java.util.Arrays.asList(" + patterns + ")");
                writer.write(",bean." + se.getSimpleName() + "()");
                writer.write(");\n");
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
