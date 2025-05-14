/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.serializer;

import com.alibaba.fastjson2.annotation.JSONField;
import tech.smartboot.feat.core.common.utils.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author 三刀
 * @version v1.0 5/13/25
 */
public class JsonSerializer {
    private final Map<String, AbstractSerializer> jsonFieldSerializerMap = new HashMap<>();
    private final Map<String, String> byteCache = new HashMap<>();
    private final PrintWriter printWriter;

    public JsonSerializer(PrintWriter printWriter) {
        jsonFieldSerializerMap.put(boolean.class.getName(), new BooleanSerializer(this));
        jsonFieldSerializerMap.put(String.class.getName(), new StringSerializer(this));
        jsonFieldSerializerMap.put(Date.class.getName(), new DateSerializer(this));
        jsonFieldSerializerMap.put(Timestamp.class.getName(), new DateSerializer(this));
        this.printWriter = printWriter;
    }

    public void serialize(final TypeMirror typeMirror, String obj, int i, DeclaredType parent) throws IOException {
        //深层级采用JSON框架序列化，防止循环引用
        if (i > 4) {
            printWriter.println(headBlank(i) + "if (" + obj + " != null) {");
            printWriter.println(headBlank(i + 1) + "os.write(JSON.toJSONBytes(" + obj + "));");
            printWriter.println(headBlank(i) + "} else {");
            toBytesPool("null");
            printWriter.println(headBlank(i) + "}");
            return;
        }
        if (typeMirror instanceof ArrayType) {
//            printWriter.append("os.write('[');");
//            printWriter.append("for (" + typeMirror + ")");
//            printWriter.append("os.write(']');");
//            return;
            throw new UnsupportedEncodingException();
        } else if (typeMirror.toString().startsWith("java.util.List") || typeMirror.toString().startsWith("java.util.Collection")) {
            new ListSerializer(this).serialize(typeMirror, obj, i, parent);
            return;
        } else if (typeMirror.toString().startsWith("java.util.Map")) {
            printWriter.println("os.write(new JSONObject(" + obj + ").toString().getBytes());");
            return;
        } else if (typeMirror.toString().endsWith(".JSONObject")) {
            printWriter.println("if (" + obj + " != null) {");
            printWriter.println("os.write(" + obj + ".toString().getBytes());");
            printWriter.println("} else {");
            toBytesPool("null");
            printWriter.println("}");
            return;
        }
        printWriter.println(headBlank(i) + "os.write('{');");


        int j = i * 10;

        List<Element> elements = new ArrayList<>();
        //当子类存在相同字段时，子类的字段会覆盖父类的字段
        Set<String> fields = new HashSet<>();

        //提取父类的字段
        TypeMirror temp = typeMirror;
        while (!temp.toString().startsWith("java.")) {
            for (Element element : ((DeclaredType) temp).asElement().getEnclosedElements()) {
                if (element.getKind() != ElementKind.FIELD) {
                    continue;
                }
                if (element.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }
                if (fields.contains(element.getSimpleName().toString())) {
                    continue;
                }
                fields.add(element.getSimpleName().toString());

                JSONField jsonField = element.getAnnotation(JSONField.class);
                if (jsonField != null && !jsonField.serialize()) {
                    continue;
                }

                elements.add(element);
            }
            temp = ((TypeElement) ((DeclaredType) temp).asElement()).getSuperclass();
        }

        for (Element se : elements) {
            if (j++ > i * 10) {
                printWriter.append(headBlank(i));
                printWriter.println("os.write(',');");
            }

            TypeMirror type = se.asType();
            if (se.asType().getKind() == TypeKind.TYPEVAR) {
                type = ((DeclaredType) typeMirror).getTypeArguments().get(0);
            }
            String fieldName = se.getSimpleName().toString();
            JSONField jsonField = se.getAnnotation(JSONField.class);
            if (jsonField != null && StringUtils.isNotBlank(jsonField.name())) {
                fieldName = jsonField.name();
            }
            AbstractSerializer serializer = jsonFieldSerializerMap.get(type.toString());
            if (serializer != null) {
                serializer.serialize(se, obj, i);
            } else if (Arrays.asList("int", "short", "byte", "long", "float", "double", "java.lang.Integer", "java.lang.Short", "java.lang.Byte", "java.lang.Long", "java.lang.Float", "java.lang.Double").contains(type.toString())) {
                printWriter.append(headBlank(i));
                toBytesPool("\"" + fieldName + "\":");
                printWriter.append(headBlank(i));
                printWriter.append("os.write(String.valueOf(").append(obj).append(".get").append(se.getSimpleName().toString().substring(0, 1).toUpperCase()).append(se.getSimpleName().toString().substring(1)).println("()).getBytes());");
            } else {
                printWriter.append(headBlank(i));
                toBytesPool("\"" + fieldName + "\":");
                String filedName = obj + ".get" + se.getSimpleName().toString().substring(0, 1).toUpperCase() + se.getSimpleName().toString().substring(1) + "()";
                printWriter.append(headBlank(i)).println("if (" + filedName + " == null) {");
                printWriter.append(headBlank(i + 1));
                toBytesPool("null");
                printWriter.append(headBlank(i)).println("} else {");
                serialize(type, filedName, i + 1, (DeclaredType) typeMirror);
                printWriter.append(headBlank(i)).println("}");
            }
        }
        printWriter.append(headBlank(i)).println("os.write('}');");
    }

    public static String headBlank(int i) {
        StringBuilder sb = new StringBuilder("\t\t");
        do {
            sb.append("\t");
        } while (i-- > 0);
        return sb.toString();
    }

    public void toBytesPool(String value) {
        String key = ("b_" + value.hashCode()).replace("-", "$");
        byteCache.put(key, "private static final byte[] " + key + " = " + toBytesStr(value) + ";");
        printWriter.append("os.write(").append(key).println(");");
    }

    private String toBytesStr(String str) {
        StringBuilder s = new StringBuilder("{");

        for (int i = 0; i < str.length(); i++) {
            if (i > 0) {
                s.append(", ");
            }
            s.append('\'').append(str.charAt(i)).append('\'');
        }
        return s + "}";
    }

    public PrintWriter getPrintWriter() {
        return printWriter;
    }

    public Map<String, String> getByteCache() {
        return byteCache;
    }
}
