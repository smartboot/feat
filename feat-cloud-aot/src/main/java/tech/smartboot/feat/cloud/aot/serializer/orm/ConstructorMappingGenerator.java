package tech.smartboot.feat.cloud.aot.serializer.orm;

import tech.smartboot.feat.cloud.annotation.orm.Arg;
import tech.smartboot.feat.cloud.annotation.orm.ConstructorArgs;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 生成基于 @ConstructorArgs/@Arg 的构造器结果映射。
 */
public final class ConstructorMappingGenerator {

    private ConstructorMappingGenerator() {
    }

    public static void emitConstructorMapping(PrintWriter printWriter, TypeMirror componentType,
                                               ConstructorArgs constructorArgs, int indent,
                                               TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        Arg[] args = constructorArgs.value();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].select().isEmpty()) {
                continue;
            }
            printWriter.println(tabs(indent) + "int _arg" + i + "Index = -1;");
        }
        printWriter.println(tabs(indent) + "java.sql.ResultSetMetaData _metaData = _rs.getMetaData();");
        printWriter.println(tabs(indent) + "for (int _i = 1; _i <= _metaData.getColumnCount(); _i++) {");
        printWriter.println(tabs(indent + 1) + "String _columnLabel = _metaData.getColumnLabel(_i);");
        for (int i = 0; i < args.length; i++) {
            if (!args[i].select().isEmpty()) {
                continue;
            }
            String column = args[i].column();
            printWriter.println(tabs(indent + 1) + "if (_arg" + i + "Index == -1 && \"" + column + "\".equalsIgnoreCase(_columnLabel)) {");
            printWriter.println(tabs(indent + 2) + "_arg" + i + "Index = _i;");
            printWriter.println(tabs(indent + 2) + "continue;");
            printWriter.println(tabs(indent + 1) + "}");
        }
        printWriter.println(tabs(indent) + "}");
    }

    public static void emitConstructorPopulation(PrintWriter printWriter, TypeMirror componentType,
                                                  ConstructorArgs constructorArgs, int indent,
                                                  TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        Arg[] args = constructorArgs.value();
        List<TypeMirror> argTypes = resolveArgTypes(componentType, args);
        StringBuilder sb = new StringBuilder();
        sb.append(tabs(indent)).append(componentType.toString()).append(" _result = new ").append(componentType.toString()).append("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            if (args[i].select().isEmpty()) {
                sb.append(buildArgExpr(i, argTypes.get(i), args[i]));
            } else {
                sb.append(buildNestedArgExpr(args[i], mapperElement, processingEnv));
            }
        }
        sb.append(");");
        printWriter.println(sb);
    }

    private static String buildArgExpr(int index, TypeMirror type, Arg arg) {
        String getter = JdbcTypeMapping.resultSetGetter(type);
        TypeMirror typeHandler = resolveTypeHandler(arg);
        if (typeHandler != null) {
            return buildTypeHandlerArgExpr(index, type, typeHandler.toString());
        }
        if (type.getKind().isPrimitive()) {
            String def = primitiveDefault(type);
            if (getter == null || "getObject".equals(getter)) {
                return "(_arg" + index + "Index != -1 ? _rs.getObject(_arg" + index + "Index, " + type.toString() + ".class) : " + def + ")";
            }
            return "(_arg" + index + "Index != -1 ? _rs." + getter + "(_arg" + index + "Index) : " + def + ")";
        }
        if (getter == null || "getObject".equals(getter)) {
            return "(_arg" + index + "Index != -1 ? _rs.getObject(_arg" + index + "Index, " + type.toString() + ".class) : null)";
        }
        return "(_arg" + index + "Index != -1 ? _rs." + getter + "(_arg" + index + "Index) : null)";
    }

    private static String buildTypeHandlerArgExpr(int index, TypeMirror type, String handlerType) {
        String box = JdbcTypeMapping.boxType(type);
        String expr = "new " + handlerType + "().getResult(_rs, _arg" + index + "Index)";
        if (type.getKind().isPrimitive()) {
            return "(_arg" + index + "Index != -1 ? ((" + box + ") " + expr + ")." + unboxMethod(type) + "() : " + primitiveDefault(type) + ")";
        }
        return "(_arg" + index + "Index != -1 ? ((" + box + ") " + expr + ") : null)";
    }

    private static String unboxMethod(TypeMirror type) {
        switch (type.toString()) {
            case "boolean":
                return "booleanValue";
            case "byte":
                return "byteValue";
            case "short":
                return "shortValue";
            case "int":
                return "intValue";
            case "long":
                return "longValue";
            case "float":
                return "floatValue";
            case "double":
                return "doubleValue";
            case "char":
                return "charValue";
            default:
                return "intValue";
        }
    }

    private static String buildNestedArgExpr(Arg arg, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        String select = arg.select();
        String column = arg.column();
        if (select.isEmpty()) {
            throw new RuntimeException("@Arg select 不能为空");
        }
        if (select.contains(".")) {
            int dot = select.lastIndexOf('.');
            String namespace = select.substring(0, dot);
            String methodName = select.substring(dot + 1);
            TypeElement targetMapper = processingEnv.getElementUtils().getTypeElement(namespace);
            if (targetMapper == null) {
                throw new RuntimeException("@Arg 嵌套查询找不到目标 Mapper: " + namespace);
            }
            ExecutableElement targetMethod = findMethod(targetMapper, methodName);
            if (targetMethod == null) {
                throw new RuntimeException("@Arg 嵌套查询方法不存在: " + select);
            }
            if (targetMethod.getParameters().size() != 1) {
                throw new RuntimeException("@Arg 嵌套查询方法必须只有一个参数: " + select);
            }
            TypeMirror paramType = targetMethod.getParameters().get(0).asType();
            String rawParamType = processingEnv.getTypeUtils().erasure(paramType).toString();
            String beanName = lowerFirst(targetMapper.getSimpleName().toString());
            String mapperType = targetMapper.getQualifiedName().toString();
            return "((" + mapperType + ") applicationContext.getBean(\"" + beanName + "\"))." + methodName + "(_rs.getObject(\"" + column + "\", " + rawParamType + ".class))";
        }
        ExecutableElement targetMethod = findMethod(mapperElement, select);
        if (targetMethod == null) {
            throw new RuntimeException("@Arg 嵌套查询方法不存在: " + select);
        }
        if (targetMethod.getParameters().size() != 1) {
            throw new RuntimeException("@Arg 嵌套查询方法必须只有一个参数: " + select);
        }
        TypeMirror paramType = targetMethod.getParameters().get(0).asType();
        String rawParamType = processingEnv.getTypeUtils().erasure(paramType).toString();
        return "this." + select + "(_rs.getObject(\"" + column + "\", " + rawParamType + ".class))";
    }

    private static String primitiveDefault(TypeMirror type) {
        switch (type.toString()) {
            case "boolean":
                return "false";
            case "long":
                return "0L";
            case "float":
                return "0F";
            case "double":
                return "0D";
            default:
                return "0";
        }
    }

    private static List<TypeMirror> resolveArgTypes(TypeMirror componentType, Arg[] args) {
        if (!(componentType instanceof DeclaredType)) {
            throw new RuntimeException("构造器映射仅支持声明类型: " + componentType);
        }
        TypeElement element = (TypeElement) ((DeclaredType) componentType).asElement();
        ExecutableElement matched = null;
        for (Element e : element.getEnclosedElements()) {
            if (e.getKind() != ElementKind.CONSTRUCTOR) {
                continue;
            }
            ExecutableElement ctor = (ExecutableElement) e;
            if (ctor.getParameters().size() == args.length) {
                if (matched != null) {
                    throw new RuntimeException(componentType + " 存在多个参数数量为 " + args.length + " 的构造器，请在 @Arg 中指定 javaType");
                }
                matched = ctor;
            }
        }
        if (matched == null) {
            throw new RuntimeException(componentType + " 找不到参数数量为 " + args.length + " 的构造器");
        }
        List<? extends VariableElement> params = matched.getParameters();
        List<TypeMirror> types = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++) {
            TypeMirror override = resolveJavaType(args[i]);
            types.add(override != null ? override : params.get(i).asType());
        }
        return types;
    }

    private static TypeMirror resolveJavaType(Arg arg) {
        try {
            arg.javaType();
        } catch (MirroredTypeException e) {
            TypeMirror type = e.getTypeMirror();
            if (!"void".equals(type.toString())) {
                return type;
            }
        }
        return null;
    }

    private static TypeMirror resolveTypeHandler(Arg arg) {
        try {
            arg.typeHandler();
        } catch (MirroredTypeException e) {
            TypeMirror type = e.getTypeMirror();
            if (!"void".equals(type.toString())) {
                return type;
            }
        }
        return null;
    }

    private static ExecutableElement findMethod(TypeElement mapperElement, String methodName) {
        for (Element e : mapperElement.getEnclosedElements()) {
            if (e.getKind() != ElementKind.METHOD) {
                continue;
            }
            ExecutableElement method = (ExecutableElement) e;
            if (method.getSimpleName().toString().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    private static String lowerFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static String tabs(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append('\t');
        }
        return sb.toString();
    }
}
