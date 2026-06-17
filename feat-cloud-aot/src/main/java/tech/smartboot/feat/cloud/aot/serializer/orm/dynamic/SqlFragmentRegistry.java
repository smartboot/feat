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

import tech.smartboot.feat.cloud.annotation.orm.Delete;
import tech.smartboot.feat.cloud.annotation.orm.Insert;
import tech.smartboot.feat.cloud.annotation.orm.Select;
import tech.smartboot.feat.cloud.annotation.orm.Update;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收集 Mapper 接口中的 <sql> 片段，并在动态 SQL 生成前展开 <include> 引用。
 */
public final class SqlFragmentRegistry {

    private SqlFragmentRegistry() {
    }

    /**
     * 扫描整个 Mapper 接口，收集所有 <sql id="..."> 片段。
     */
    public static Map<String, List<SqlNode>> build(TypeElement mapper) {
        Map<String, List<SqlNode>> fragments = new HashMap<>();
        for (Element e : mapper.getEnclosedElements()) {
            if (e.getKind() != ElementKind.METHOD) {
                continue;
            }
            ExecutableElement method = (ExecutableElement) e;
            String sql = extractSql(method);
            if (sql == null || !DynamicSqlCodeGenerator.isDynamic(sql)) {
                continue;
            }
            SqlNode root = DynamicSqlParser.parse(sql);
            collectFragments(root, fragments);
        }
        return fragments;
    }

    /**
     * 将当前 AST 中的 <include refid="..."/> 替换为对应片段内容。
     */
    public static SqlNode expandIncludes(SqlNode node, Map<String, List<SqlNode>> fragments) {
        if (node instanceof IncludeSqlNode) {
            IncludeSqlNode include = (IncludeSqlNode) node;
            List<SqlNode> replacement = fragments.get(include.refid);
            if (replacement == null) {
                throw new RuntimeException("未定义的 <sql> 片段: " + include.refid);
            }
            return new MixedSqlNode(new ArrayList<>(replacement));
        }
        if (node instanceof MixedSqlNode) {
            List<SqlNode> expanded = new ArrayList<>();
            for (SqlNode child : ((MixedSqlNode) node).children) {
                SqlNode e = expandIncludes(child, fragments);
                if (e instanceof MixedSqlNode) {
                    expanded.addAll(((MixedSqlNode) e).children);
                } else {
                    expanded.add(e);
                }
            }
            return new MixedSqlNode(expanded);
        }
        List<SqlNode> children = childrenOf(node);
        if (children == null) {
            return node;
        }
        return copyWithChildren(node, expandChildren(children, fragments));
    }

    private static List<SqlNode> expandChildren(List<SqlNode> children, Map<String, List<SqlNode>> fragments) {
        List<SqlNode> expanded = new ArrayList<>();
        for (SqlNode child : children) {
            SqlNode e = expandIncludes(child, fragments);
            if (e instanceof MixedSqlNode) {
                expanded.addAll(((MixedSqlNode) e).children);
            } else {
                expanded.add(e);
            }
        }
        return expanded;
    }

    private static void collectFragments(SqlNode node, Map<String, List<SqlNode>> fragments) {
        if (node instanceof SqlSqlNode) {
            SqlSqlNode sqlNode = (SqlSqlNode) node;
            if (sqlNode.id != null && !sqlNode.id.isEmpty()) {
                fragments.put(sqlNode.id, new ArrayList<>(sqlNode.children));
            }
            return;
        }
        List<SqlNode> children = childrenOf(node);
        if (children != null) {
            for (SqlNode child : children) {
                collectFragments(child, fragments);
            }
        }
    }

    private static List<SqlNode> childrenOf(SqlNode node) {
        if (node instanceof MixedSqlNode) {
            return ((MixedSqlNode) node).children;
        }
        if (node instanceof IfSqlNode) {
            return ((IfSqlNode) node).children;
        }
        if (node instanceof WhereSqlNode) {
            return ((WhereSqlNode) node).children;
        }
        if (node instanceof SetSqlNode) {
            return ((SetSqlNode) node).children;
        }
        if (node instanceof TrimSqlNode) {
            return ((TrimSqlNode) node).children;
        }
        if (node instanceof ForeachSqlNode) {
            return ((ForeachSqlNode) node).children;
        }
        if (node instanceof ChooseSqlNode) {
            return ((ChooseSqlNode) node).children;
        }
        if (node instanceof WhenSqlNode) {
            return ((WhenSqlNode) node).children;
        }
        if (node instanceof OtherwiseSqlNode) {
            return ((OtherwiseSqlNode) node).children;
        }
        if (node instanceof SqlSqlNode) {
            return ((SqlSqlNode) node).children;
        }
        return null;
    }

    private static SqlNode copyWithChildren(SqlNode node, List<SqlNode> children) {
        if (node instanceof IfSqlNode) {
            return new IfSqlNode(((IfSqlNode) node).test, children);
        }
        if (node instanceof WhereSqlNode) {
            return new WhereSqlNode(children);
        }
        if (node instanceof SetSqlNode) {
            return new SetSqlNode(children);
        }
        if (node instanceof TrimSqlNode) {
            TrimSqlNode t = (TrimSqlNode) node;
            return new TrimSqlNode(t.prefix, t.prefixOverrides, t.suffix, t.suffixOverrides, children);
        }
        if (node instanceof ForeachSqlNode) {
            ForeachSqlNode f = (ForeachSqlNode) node;
            return new ForeachSqlNode(f.collection, f.item, f.index, f.separator, f.open, f.close, children);
        }
        if (node instanceof ChooseSqlNode) {
            return new ChooseSqlNode(children);
        }
        if (node instanceof WhenSqlNode) {
            return new WhenSqlNode(((WhenSqlNode) node).test, children);
        }
        if (node instanceof OtherwiseSqlNode) {
            return new OtherwiseSqlNode(children);
        }
        if (node instanceof SqlSqlNode) {
            return new SqlSqlNode(((SqlSqlNode) node).id, children);
        }
        return node;
    }

    private static String extractSql(ExecutableElement method) {
        Select select = method.getAnnotation(Select.class);
        if (select != null) {
            return select.value();
        }
        Insert insert = method.getAnnotation(Insert.class);
        if (insert != null) {
            return insert.value();
        }
        Update update = method.getAnnotation(Update.class);
        if (update != null) {
            return update.value();
        }
        Delete delete = method.getAnnotation(Delete.class);
        if (delete != null) {
            return delete.value();
        }
        return null;
    }
}
