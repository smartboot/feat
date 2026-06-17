/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *  and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer.orm.dynamic;

import tech.smartboot.feat.cloud.aot.serializer.orm.JdbcCodeEmitter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 {@link SqlNode} AST 节点翻译为纯 JDBC 代码。
 *
 * <p>直接操作 {@link StringBuilder} 与参数列表，不再依赖运行时的 {@code FeatDynamicSql}。
 */
public final class DynamicSqlNodeGenerator {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(#\\{|\\$\\{)\\s*([a-zA-Z0-9_.]+)\\b[^}]*}");

    private DynamicSqlNodeGenerator() {
    }

    public static void generateNode(PrintWriter printWriter, SqlNode node, int indent, DynamicScope scope,
                                    String sqlVar, String paramsVar, NameGenerator names,
                                    ProcessingEnvironment processingEnv) {
        if (node instanceof MixedSqlNode) {
            for (SqlNode child : ((MixedSqlNode) node).children) {
                generateNode(printWriter, child, indent, scope, sqlVar, paramsVar, names, processingEnv);
            }
        } else if (node instanceof BindSqlNode) {
            BindSqlNode bind = (BindSqlNode) node;
            String bindVar = names.next("bind");
            printWriter.println(tabs(indent) + "Object " + bindVar + " = " + ExpressionTranslator.toObject(bind.value, scope) + ";");
            scope.addVariable(bind.name, bindVar, null);
        } else if (node instanceof TextSqlNode) {
            generateText(printWriter, ((TextSqlNode) node).text, indent, scope, sqlVar, paramsVar);
        } else if (node instanceof IfSqlNode) {
            IfSqlNode ifNode = (IfSqlNode) node;
            printWriter.println(tabs(indent) + "if (" + ExpressionTranslator.toBoolean(ifNode.test, scope) + ") {");
            for (SqlNode child : ifNode.children) {
                generateNode(printWriter, child, indent + 1, scope, sqlVar, paramsVar, names, processingEnv);
            }
            printWriter.println(tabs(indent) + "}");
        } else if (node instanceof WhereSqlNode) {
            generateWhere(printWriter, (WhereSqlNode) node, indent, scope, sqlVar, paramsVar, names, processingEnv);
        } else if (node instanceof SetSqlNode) {
            generateSet(printWriter, (SetSqlNode) node, indent, scope, sqlVar, paramsVar, names, processingEnv);
        } else if (node instanceof TrimSqlNode) {
            generateTrim(printWriter, (TrimSqlNode) node, indent, scope, sqlVar, paramsVar, names, processingEnv);
        } else if (node instanceof ForeachSqlNode) {
            generateForeach(printWriter, (ForeachSqlNode) node, indent, scope, sqlVar, paramsVar, names, processingEnv);
        } else if (node instanceof ChooseSqlNode) {
            generateChoose(printWriter, (ChooseSqlNode) node, indent, scope, sqlVar, paramsVar, names, processingEnv);
        } else if (node instanceof SqlSqlNode) {
            // <sql> 片段仅作为定义，不直接输出。
        }
    }

    private static void generateWhere(PrintWriter printWriter, WhereSqlNode node, int indent, DynamicScope scope,
                                      String sqlVar, String paramsVar, NameGenerator names,
                                      ProcessingEnvironment processingEnv) {
        String whereVar = names.next("where");
        String trimmedVar = names.next("whereTrimmed");
        String lenVar = names.next("whereLen");
        printWriter.println(tabs(indent) + "{");
        printWriter.println(tabs(indent + 1) + "StringBuilder " + whereVar + " = new StringBuilder();");
        for (SqlNode child : node.children) {
            generateNode(printWriter, child, indent + 1, scope, whereVar, paramsVar, names, processingEnv);
        }
        printWriter.println(tabs(indent + 1) + "String " + trimmedVar + " = " + whereVar + ".toString().trim();");
        printWriter.println(tabs(indent + 1) + "int " + lenVar + " = " + trimmedVar + ".length();");
        printWriter.println(tabs(indent + 1) + "if (" + lenVar + " > 0) {");
        printWriter.println(tabs(indent + 2) + "if (" + trimmedVar + ".regionMatches(true, 0, \"and \", 0, Math.min(4, " + lenVar + "))) {");
        printWriter.println(tabs(indent + 3) + trimmedVar + " = " + trimmedVar + ".substring(4).trim();");
        printWriter.println(tabs(indent + 2) + "} else if (" + trimmedVar + ".regionMatches(true, 0, \"or \", 0, Math.min(3, " + lenVar + "))) {");
        printWriter.println(tabs(indent + 3) + trimmedVar + " = " + trimmedVar + ".substring(3).trim();");
        printWriter.println(tabs(indent + 2) + "}");
        printWriter.println(tabs(indent + 2) + "if (!" + trimmedVar + ".isEmpty()) {");
        printWriter.println(tabs(indent + 3) + sqlVar + ".append(\" WHERE \").append(" + trimmedVar + ");");
        printWriter.println(tabs(indent + 2) + "}");
        printWriter.println(tabs(indent + 1) + "}");
        printWriter.println(tabs(indent) + "}");
    }

    private static void generateSet(PrintWriter printWriter, SetSqlNode node, int indent, DynamicScope scope,
                                    String sqlVar, String paramsVar, NameGenerator names,
                                    ProcessingEnvironment processingEnv) {
        String setVar = names.next("set");
        String trimmedVar = names.next("setTrimmed");
        printWriter.println(tabs(indent) + "{");
        printWriter.println(tabs(indent + 1) + "StringBuilder " + setVar + " = new StringBuilder();");
        for (SqlNode child : node.children) {
            generateNode(printWriter, child, indent + 1, scope, setVar, paramsVar, names, processingEnv);
        }
        printWriter.println(tabs(indent + 1) + "String " + trimmedVar + " = " + setVar + ".toString().trim();");
        printWriter.println(tabs(indent + 1) + "if (" + trimmedVar + ".startsWith(\",\")) {");
        printWriter.println(tabs(indent + 2) + trimmedVar + " = " + trimmedVar + ".substring(1).trim();");
        printWriter.println(tabs(indent + 1) + "}");
        printWriter.println(tabs(indent + 1) + "if (" + trimmedVar + ".endsWith(\",\")) {");
        printWriter.println(tabs(indent + 2) + trimmedVar + " = " + trimmedVar + ".substring(0, " + trimmedVar + ".length() - 1).trim();");
        printWriter.println(tabs(indent + 1) + "}");
        printWriter.println(tabs(indent + 1) + "if (!" + trimmedVar + ".isEmpty()) {");
        printWriter.println(tabs(indent + 2) + sqlVar + ".append(\" SET \").append(" + trimmedVar + ");");
        printWriter.println(tabs(indent + 1) + "}");
        printWriter.println(tabs(indent) + "}");
    }

    private static void generateTrim(PrintWriter printWriter, TrimSqlNode node, int indent, DynamicScope scope,
                                     String sqlVar, String paramsVar, NameGenerator names,
                                     ProcessingEnvironment processingEnv) {
        String trimVar = names.next("trim");
        String trimmedVar = names.next("trimmed");
        printWriter.println(tabs(indent) + "{");
        printWriter.println(tabs(indent + 1) + "StringBuilder " + trimVar + " = new StringBuilder();");
        for (SqlNode child : node.children) {
            generateNode(printWriter, child, indent + 1, scope, trimVar, paramsVar, names, processingEnv);
        }
        printWriter.println(tabs(indent + 1) + "String " + trimmedVar + " = " + trimVar + ".toString().trim();");
        emitOverrides(printWriter, node.prefixOverrides, trimmedVar, true, indent + 1);
        emitOverrides(printWriter, node.suffixOverrides, trimmedVar, false, indent + 1);
        printWriter.println(tabs(indent + 1) + "if (!" + trimmedVar + ".isEmpty()) {");
        if (node.prefix != null && !node.prefix.isEmpty()) {
            printWriter.println(tabs(indent + 2) + sqlVar + ".append(\" \").append(\"" + escape(node.prefix) + "\");");
        }
        printWriter.println(tabs(indent + 2) + sqlVar + ".append(\" \").append(" + trimmedVar + ");");
        if (node.suffix != null && !node.suffix.isEmpty()) {
            printWriter.println(tabs(indent + 2) + sqlVar + ".append(\" \").append(\"" + escape(node.suffix) + "\");");
        }
        printWriter.println(tabs(indent + 1) + "}");
        printWriter.println(tabs(indent) + "}");
    }

    private static void emitOverrides(PrintWriter printWriter, String overrides, String trimmedVar,
                                      boolean prefix, int indent) {
        if (overrides == null || overrides.trim().isEmpty()) {
            return;
        }
        String[] parts = overrides.split("\\|");
        boolean first = true;
        for (String raw : parts) {
            String override = raw.trim();
            if (override.isEmpty()) {
                continue;
            }
            String escaped = escape(override);
            int len = override.length();
            String keyword = first ? "if" : "else if";
            first = false;
            if (prefix) {
                printWriter.println(tabs(indent) + keyword + " (" + trimmedVar + ".length() >= " + len
                        + " && " + trimmedVar + ".regionMatches(true, 0, \"" + escaped + "\", 0, " + len + ")) {");
                printWriter.println(tabs(indent + 1) + trimmedVar + " = " + trimmedVar + ".substring(" + len + ").trim();");
            } else {
                printWriter.println(tabs(indent) + keyword + " (" + trimmedVar + ".length() >= " + len
                        + " && " + trimmedVar + ".regionMatches(true, " + trimmedVar + ".length() - " + len
                        + ", \"" + escaped + "\", 0, " + len + ")) {");
                printWriter.println(tabs(indent + 1) + trimmedVar + " = " + trimmedVar + ".substring(0, "
                        + trimmedVar + ".length() - " + len + ").trim();");
            }
            printWriter.println(tabs(indent) + "}");
        }
    }

    private static void generateText(PrintWriter printWriter, String text, int indent, DynamicScope scope,
                                     String sqlVar, String paramsVar) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder sqlExpr = new StringBuilder();
        List<String> params = new ArrayList<>();
        int last = 0;
        boolean first = true;
        while (matcher.find()) {
            String before = text.substring(last, matcher.start());
            String type = matcher.group(1);
            String name = matcher.group(2);
            if (first) {
                sqlExpr.append("\"").append(JdbcCodeEmitter.escapeSql(before)).append("\"");
                first = false;
            } else {
                sqlExpr.append(" + \"").append(JdbcCodeEmitter.escapeSql(before)).append("\"");
            }
            if (type.startsWith("#")) {
                sqlExpr.append(" + \"?\"");
                params.add(scope.resolvePath(name));
            } else {
                sqlExpr.append(" + String.valueOf(").append(scope.resolvePath(name)).append(")");
            }
            last = matcher.end();
        }
        String after = text.substring(last);
        if (first) {
            sqlExpr.append("\"").append(JdbcCodeEmitter.escapeSql(after)).append("\"");
        } else if (!after.isEmpty()) {
            sqlExpr.append(" + \"").append(JdbcCodeEmitter.escapeSql(after)).append("\"");
        }

        printWriter.println(tabs(indent) + sqlVar + ".append(" + sqlExpr + ");");
        for (String param : params) {
            printWriter.println(tabs(indent) + paramsVar + ".add(" + param + ");");
        }
    }

    private static void generateForeach(PrintWriter printWriter, ForeachSqlNode node, int indent, DynamicScope scope,
                                        String sqlVar, String paramsVar, NameGenerator names,
                                        ProcessingEnvironment processingEnv) {
        if (node.collection == null || node.collection.isEmpty()) {
            throw new RuntimeException("<foreach> 缺少 collection 属性");
        }
        String collExpr = scope.resolvePath(node.collection);
        DynamicScope.Variable collVar = scope.resolve(node.collection);
        TypeMirror collType = collVar != null ? collVar.type : null;

        String itemTypeExpr = "Object";
        String loopExpr = collExpr;
        boolean enhancedFor = false;
        TypeMirror itemType = null;
        if (collType != null) {
            if (collType.getKind() == TypeKind.ARRAY) {
                itemType = ((ArrayType) collType).getComponentType();
                itemTypeExpr = itemType.toString();
                enhancedFor = true;
            } else if (collType.getKind() == TypeKind.DECLARED) {
                DeclaredType dt = (DeclaredType) collType;
                javax.lang.model.element.TypeElement elem = (javax.lang.model.element.TypeElement) dt.asElement();
                String qName = elem.getQualifiedName().toString();
                if ("java.util.Collection".equals(qName) || "java.util.List".equals(qName)
                        || "java.util.Set".equals(qName) || "java.lang.Iterable".equals(qName)) {
                    if (!dt.getTypeArguments().isEmpty()) {
                        itemType = dt.getTypeArguments().get(0);
                        itemTypeExpr = itemType.toString();
                    }
                    enhancedFor = true;
                } else if ("java.util.Map".equals(qName)) {
                    itemTypeExpr = "java.util.Map.Entry<?,?>";
                    loopExpr = collExpr + ".entrySet()";
                    enhancedFor = true;
                }
            }
        }

        if (enhancedFor) {
            emitForeachLoop(printWriter, node, indent, scope, sqlVar, paramsVar, names, processingEnv,
                    itemTypeExpr, loopExpr, itemType, 0);
            return;
        }

        // 退化方案：运行时无法确定具体集合类型，仍使用 FeatSqlContext.asCollection
        String collHolder = names.next("collection");
        printWriter.println(tabs(indent) + "java.util.Collection<?> " + collHolder
                + " = tech.smartboot.feat.cloud.jdbc.FeatSqlContext.asCollection(" + collExpr + ");");
        printWriter.println(tabs(indent) + "if (" + collHolder + " != null) {");
        emitForeachLoop(printWriter, node, indent + 1, scope, sqlVar, paramsVar, names, processingEnv,
                "Object", collHolder, null, 1);
        printWriter.println(tabs(indent) + "}");
    }

    private static void emitForeachLoop(PrintWriter printWriter, ForeachSqlNode node, int indent, DynamicScope scope,
                                        String sqlVar, String paramsVar, NameGenerator names,
                                        ProcessingEnvironment processingEnv, String itemTypeExpr,
                                        String loopExpr, TypeMirror itemType, int indentDelta) {
        String itemVar = names.next("item");
        String idxVar = names.next("idx");
        if (node.open != null && !node.open.isEmpty()) {
            printWriter.println(tabs(indent) + sqlVar + ".append(\"" + escape(node.open) + "\");");
        }
        printWriter.println(tabs(indent) + "int " + idxVar + " = 0;");
        printWriter.println(tabs(indent) + "for (" + itemTypeExpr + " " + itemVar + " : " + loopExpr + ") {");
        if (node.separator != null && !node.separator.isEmpty()) {
            printWriter.println(tabs(indent + 1) + "if (" + idxVar + " > 0) "
                    + sqlVar + ".append(\"" + escape(node.separator) + "\");");
        }
        DynamicScope childScope = scope.copy();
        childScope.addVariable(node.item, itemVar, itemType);
        if (node.index != null && !node.index.isEmpty()) {
            childScope.addVariable(node.index, idxVar, processingEnv.getTypeUtils().getPrimitiveType(TypeKind.INT));
        }
        for (SqlNode child : node.children) {
            generateNode(printWriter, child, indent + 1, childScope, sqlVar, paramsVar, names, processingEnv);
        }
        printWriter.println(tabs(indent + 1) + idxVar + "++;");
        printWriter.println(tabs(indent) + "}");
        if (node.close != null && !node.close.isEmpty()) {
            printWriter.println(tabs(indent) + sqlVar + ".append(\"" + escape(node.close) + "\");");
        }
    }

    private static void generateChoose(PrintWriter printWriter, ChooseSqlNode node, int indent, DynamicScope scope,
                                       String sqlVar, String paramsVar, NameGenerator names,
                                       ProcessingEnvironment processingEnv) {
        boolean first = true;
        OtherwiseSqlNode otherwise = null;
        for (SqlNode child : node.children) {
            if (child instanceof WhenSqlNode) {
                WhenSqlNode when = (WhenSqlNode) child;
                String keyword = first ? "if" : "else if";
                first = false;
                printWriter.println(tabs(indent) + keyword + " (" + ExpressionTranslator.toBoolean(when.test, scope) + ") {");
                for (SqlNode c : when.children) {
                    generateNode(printWriter, c, indent + 1, scope, sqlVar, paramsVar, names, processingEnv);
                }
                printWriter.println(tabs(indent) + "}");
            } else if (child instanceof OtherwiseSqlNode) {
                otherwise = (OtherwiseSqlNode) child;
            }
        }
        if (otherwise != null) {
            printWriter.println(tabs(indent) + "else {");
            for (SqlNode c : otherwise.children) {
                generateNode(printWriter, c, indent + 1, scope, sqlVar, paramsVar, names, processingEnv);
            }
            printWriter.println(tabs(indent) + "}");
        }
    }

    private static String tabs(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("\t");
        }
        return sb.toString();
    }

    private static String escape(String value) {
        return JdbcCodeEmitter.escapeSql(value);
    }
}
