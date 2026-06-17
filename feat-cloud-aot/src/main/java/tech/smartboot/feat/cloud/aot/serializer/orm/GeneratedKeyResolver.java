package tech.smartboot.feat.cloud.aot.serializer.orm;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
 
 /**
  * 生成键设置表达式解析。
  */
 public final class GeneratedKeyResolver {
 
     private GeneratedKeyResolver() {
     }
 
    public static String resolve(ExecutableElement method, List<ParameterMetadata> paramInfos, String keyProperty) {
        KeyContext ctx = resolveContext(paramInfos, keyProperty);
        return ctx == null ? null : ctx.setter;
    }

    /**
     * 解析生成键的 setter 表达式及其参数类型。
     */
    public static GeneratedKey resolveKey(ExecutableElement method, List<ParameterMetadata> paramInfos, String keyProperty) {
        KeyContext ctx = resolveContext(paramInfos, keyProperty);
        if (ctx == null) {
            return null;
        }
        TypeMirror keyType = resolvePropertyType(ctx.param.getType(), ctx.path, ctx.start, ctx.path.length - 1);
        return new GeneratedKey(ctx.setter, keyType);
    }
 
    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static KeyContext resolveContext(List<ParameterMetadata> paramInfos, String keyProperty) {
        String[] parts = keyProperty.split("\\.");
        ParameterMetadata param = null;
        int start = 0;
        if (parts.length > 1) {
            param = ParameterResolver.findParameter(parts[0], paramInfos);
            start = 1;
        }
        if (param == null && paramInfos.size() == 1) {
            param = paramInfos.get(0);
        }
        if (param == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(param.getVarName());
        for (int i = start; i < parts.length; i++) {
            sb.append(".set").append(capitalize(parts[i]));
        }
        return new KeyContext(param, parts, start, sb.toString());
    }

    private static TypeMirror resolvePropertyType(TypeMirror beanType, String[] path, int start, int end) {
        if (start > end) {
            return beanType;
        }
        if (!(beanType instanceof DeclaredType)) {
            return null;
        }
        TypeElement element = (TypeElement) ((DeclaredType) beanType).asElement();
        for (int i = start; i <= end; i++) {
            String prop = path[i];
            String setterName = "set" + capitalize(prop);
            TypeMirror nextType = null;
            for (Element e : element.getEnclosedElements()) {
                if (e.getKind() != ElementKind.METHOD) {
                    continue;
                }
                ExecutableElement setter = (ExecutableElement) e;
                if (!setter.getSimpleName().toString().equals(setterName)) {
                    continue;
                }
                if (setter.getParameters().size() != 1) {
                    continue;
                }
                nextType = setter.getParameters().get(0).asType();
                break;
            }
            if (nextType == null) {
                return null;
            }
            if (i == end) {
                return nextType;
            }
            if (nextType instanceof DeclaredType) {
                element = (TypeElement) ((DeclaredType) nextType).asElement();
            } else {
                return null;
            }
        }
        return null;
    }

    private static class KeyContext {
        final ParameterMetadata param;
        final String[] path;
        final int start;
        final String setter;

        KeyContext(ParameterMetadata param, String[] path, int start, String setter) {
            this.param = param;
            this.path = path;
            this.start = start;
            this.setter = setter;
        }
    }

    public static class GeneratedKey {
        private final String setter;
        private final TypeMirror type;

        public GeneratedKey(String setter, TypeMirror type) {
            this.setter = setter;
            this.type = type;
        }

        public String getSetter() {
            return setter;
        }

        public TypeMirror getType() {
            return type;
        }
    }
}
