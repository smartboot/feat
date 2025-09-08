/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.controller;

import com.alibaba.fastjson2.annotation.JSONField;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;

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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 5/13/25
 */
public final class JsonSerializer {
    private final Map<String, AbstractSerializer> jsonFieldSerializerMap = new HashMap<>();

    private final Map<String, String> byteCache = new HashMap<>();
    private final ListSerializer listSerializer;
    private final MapSerializer mapSerializer;
    private final PrintWriter printWriter;

    public JsonSerializer(PrintWriter printWriter) {
        jsonFieldSerializerMap.put(boolean.class.getName(), new BoolSerializer(this));
        jsonFieldSerializerMap.put(Boolean.class.getName(), new BooleanSerializer(this));
        jsonFieldSerializerMap.put(String.class.getName(), new StringSerializer(this));
        jsonFieldSerializerMap.put(Date.class.getName(), new DateSerializer(this));
        jsonFieldSerializerMap.put(Timestamp.class.getName(), new DateSerializer(this));
        jsonFieldSerializerMap.put(int.class.getName(), new PrimitiveSerializer(this, PrimitiveSerializer.TYPE_INT));
        jsonFieldSerializerMap.put(long.class.getName(), new PrimitiveSerializer(this, PrimitiveSerializer.TYPE_LONG));
        jsonFieldSerializerMap.put(short.class.getName(), new PrimitiveSerializer(this, PrimitiveSerializer.TYPE_SHORT));
        jsonFieldSerializerMap.put(byte.class.getName(), new PrimitiveSerializer(this, PrimitiveSerializer.TYPE_BYTE));
        jsonFieldSerializerMap.put(float.class.getName(), new PrimitiveSerializer(this, PrimitiveSerializer.TYPE_FLOAT));
        jsonFieldSerializerMap.put(double.class.getName(), new PrimitiveSerializer(this, PrimitiveSerializer.TYPE_DOUBLE));
        jsonFieldSerializerMap.put(char.class.getName(), new PrimitiveSerializer(this, PrimitiveSerializer.TYPE_CHAR));

        jsonFieldSerializerMap.put(Integer.class.getName(), new WrapsPrimitiveSerializer(this, WrapsPrimitiveSerializer.TYPE_INT));
        jsonFieldSerializerMap.put(Long.class.getName(), new WrapsPrimitiveSerializer(this, WrapsPrimitiveSerializer.TYPE_LONG));
        jsonFieldSerializerMap.put(Short.class.getName(), new WrapsPrimitiveSerializer(this, WrapsPrimitiveSerializer.TYPE_SHORT));
        jsonFieldSerializerMap.put(Byte.class.getName(), new WrapsPrimitiveSerializer(this, WrapsPrimitiveSerializer.TYPE_BYTE));
        jsonFieldSerializerMap.put(Float.class.getName(), new WrapsPrimitiveSerializer(this, WrapsPrimitiveSerializer.TYPE_FLOAT));
        jsonFieldSerializerMap.put(Double.class.getName(), new WrapsPrimitiveSerializer(this, WrapsPrimitiveSerializer.TYPE_DOUBLE));
        jsonFieldSerializerMap.put(Character.class.getName(), new WrapsPrimitiveSerializer(this, WrapsPrimitiveSerializer.TYPE_CHAR));

        listSerializer = new ListSerializer(this);
        mapSerializer = new MapSerializer(this);
        this.printWriter = printWriter;
    }

    public void serialize(final TypeMirror typeMirror, String obj, int i, DeclaredType parent) throws IOException {
        //深层级采用JSON框架序列化，防止循环引用
        if (i > 4 || typeMirror.toString().equals(Object.class.getName())) {
            printWriter.println(headBlank(i) + "if (" + obj + " != null) {");
            printWriter.println(headBlank(i + 1) + "os.write(JSON.toJSONBytes(" + obj + "));");
            printWriter.println(headBlank(i) + "} else {");
            printWriter.append(headBlank(i + 1));
            toBytesPool("null");
            printWriter.println(headBlank(i) + "}");
            return;
        }
        if (typeMirror.toString().equals(Void.class.getName())) {
            printWriter.println(headBlank(i) + "//void类型按null处理");
            printWriter.println(headBlank(i) + "os.write(b_null);");
            return;
        }
        if (typeMirror instanceof ArrayType) {
            printWriter.println(headBlank(i) + "if (" + obj + " == null) {");
            printWriter.append(headBlank(i + 1));
            toBytesPool("null");

            printWriter.println(headBlank(i) + "} else if (" + obj + ".length == 0) {");
            printWriter.append(headBlank(i + 1));
            toBytesPool("[]");
            printWriter.println(headBlank(i) + "} else {");
            printWriter.println(headBlank(i + 1) + "os.write(JSON.toJSONBytes(" + obj + "));");
            printWriter.println(headBlank(i) + "}");
            return;
        } else if (typeMirror.toString().startsWith("java.util.List") || typeMirror.toString().startsWith("java.util.Collection")) {
            listSerializer.serialize(typeMirror, obj, i, parent);
            return;
        } else if (typeMirror.toString().startsWith("java.util.Map")) {
            mapSerializer.serialize(typeMirror, obj, i, parent);
            return;
        } else if (typeMirror.toString().endsWith(".JSONObject")) {
            printWriter.println(headBlank(i) + "if (" + obj + " != null) {");
            printWriter.println(headBlank(i + 1) + "os.write(" + obj + ".toString().getBytes());");
            printWriter.println(headBlank(i) + "} else {");
            printWriter.append(headBlank(i + 1));
            toBytesPool("null");
            printWriter.println(headBlank(i) + "}");
            return;
        }


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

        if (elements.isEmpty()) {
            throw new FeatException("");
        }

        printWriter.println(headBlank(i) + "os.write('{');");
        boolean withComma = false;
        for (Element se : elements) {
            TypeMirror type = se.asType();
            if (se.asType().getKind() == TypeKind.TYPEVAR) {
                type = ((DeclaredType) typeMirror).getTypeArguments().get(0);
            }
            String fieldName = se.getSimpleName().toString();
            JSONField jsonField = se.getAnnotation(JSONField.class);
            if (jsonField != null && FeatUtils.isNotBlank(jsonField.name())) {
                fieldName = jsonField.name();
            }
            AbstractSerializer serializer = jsonFieldSerializerMap.get(type.toString());
            if (serializer != null) {
                serializer.serialize(se, obj, i, withComma);
            } else {
                printWriter.append(headBlank(i));
                toBytesPool("\"" + fieldName + "\":", withComma);
                String filedName = obj + ".get" + se.getSimpleName().toString().substring(0, 1).toUpperCase() + se.getSimpleName().toString().substring(1) + "()";
                printWriter.append(headBlank(i)).println("if (" + filedName + " == null) {");
                printWriter.append(headBlank(i + 1));
                toBytesPool("null");
                printWriter.append(headBlank(i)).println("} else {");
                serialize(type, filedName, i + 1, (DeclaredType) typeMirror);
                printWriter.append(headBlank(i)).println("}");
            }
            withComma = true;
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

    public void toBytesPool(String value, boolean withComma) {
        if ("\"success\":false".equals(value)) {
            if (withComma) {
                printWriter.println("os.write(b_success_false);");
            } else {
                printWriter.println("os.write(b_success_false, 1, b_success_false.length - 1);");
            }
        } else if ("\"success\":true".equals(value)) {
            if (withComma) {
                printWriter.println("os.write(b_success_true);");
            } else {
                printWriter.println("os.write(b_success_true, 1, b_success_true.length - 1);");
            }
        } else if ("null".equals(value)) {
            printWriter.println("os.write(b_null);");
        } else if ("\"message\":".equals(value)) {
            if (withComma) {
                printWriter.println("os.write(b_message);");
            } else {
                printWriter.println("os.write(b_message, 1, b_message.length - 1);");
            }
        } else if ("\"data\":".equals(value)) {
            if (withComma) {
                printWriter.println("os.write(b_data);");
            } else {
                printWriter.println("os.write(b_data, 1, b_data.length - 1);");
            }
        } else if ("\"code\":".equals(value)) {
            if (withComma) {
                printWriter.println("os.write(b_code);");
            } else {
                printWriter.println("os.write(b_code, 1, b_code.length - 1);");
            }
        } else if ("[]".equals(value)) {
            printWriter.println("os.write(b_empty_list);");
        } else if ("{}".equals(value)) {
            printWriter.println("os.write(b_empty_map);");
        } else {
            String key = ("b_" + value.hashCode()).replace("-", "$");
            byteCache.put(key, "private static final byte[] " + key + " = " + toBytesStr("," + value) + ";");
            if (withComma) {
                printWriter.append("os.write(").append(key).println(");");
            } else {
                printWriter.append("os.write(").append(key).println(", 1, " + key + ".length - 1);");
            }
        }
    }

    public void toBytesPool(String value) {
        toBytesPool(value, false);
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
