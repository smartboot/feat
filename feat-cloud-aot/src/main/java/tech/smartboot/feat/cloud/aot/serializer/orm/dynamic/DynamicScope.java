package tech.smartboot.feat.cloud.aot.serializer.orm.dynamic;

import tech.smartboot.feat.cloud.aot.serializer.orm.ParameterMetadata;

import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态 SQL 代码生成时的变量作用域。
 *
 * <p>保存参数、bind 变量、foreach item/index 等到 Java 表达式/类型的映射，
 * 使表达式翻译和占位符解析能直接生成参数引用，无需运行时反射。</p>
 */
public final class DynamicScope {

    public static final class Variable {
        public final String expr;
        public final TypeMirror type;

        public Variable(String expr, TypeMirror type) {
            this.expr = expr;
            this.type = type;
        }
    }

    private final List<ParameterMetadata> paramInfos;
    private final Map<String, Variable> bindings;

    public DynamicScope(List<ParameterMetadata> paramInfos) {
        this.paramInfos = paramInfos;
        this.bindings = new HashMap<>();
        if (paramInfos != null) {
            for (ParameterMetadata info : paramInfos) {
                addParameter(info);
            }
        }
    }

    private DynamicScope(List<ParameterMetadata> paramInfos, Map<String, Variable> bindings) {
        this.paramInfos = paramInfos;
        this.bindings = new HashMap<>(bindings);
    }

    public void addParameter(ParameterMetadata info) {
        Variable v = new Variable(info.getVarName(), info.getType());
        bindings.put(info.getVarName(), v);
        if (info.getAlias() != null && !info.getAlias().isEmpty()) {
            bindings.put(info.getAlias(), v);
        }
        bindings.put("param" + info.getIndex(), v);
    }

    public void addVariable(String name, String expr, TypeMirror type) {
        bindings.put(name, new Variable(expr, type));
    }

    public Variable resolve(String name) {
        return bindings.get(name);
    }

    /**
     * 解析占位符/属性路径（如 user.password）为直接 Java 表达式。
     */
    public String resolvePath(String path) {
        String[] parts = path.split("\\.");
        String rootName = parts[0];
        Variable root = resolve(rootName);
        int start = 1;
        if (root == null) {
            if (paramInfos != null && paramInfos.size() == 1) {
                root = new Variable(paramInfos.get(0).getVarName(), paramInfos.get(0).getType());
                start = 0;
            } else {
                throw new RuntimeException("无法解析动态 SQL 变量: " + path);
            }
        }
        StringBuilder sb = new StringBuilder(root.expr);
        for (int i = start; i < parts.length; i++) {
            sb.append(".get").append(capitalize(parts[i])).append("()");
        }
        return sb.toString();
    }

    /**
     * 解析表达式中的根变量，支持单参数隐式属性访问。
     */
    public String resolveRoot(String name) {
        Variable v = resolve(name);
        if (v != null) {
            return v.expr;
        }
        if (paramInfos != null && paramInfos.size() == 1) {
            return paramInfos.get(0).getVarName();
        }
        throw new RuntimeException("无法解析表达式变量: " + name);
    }

    public DynamicScope copy() {
        return new DynamicScope(paramInfos, bindings);
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
