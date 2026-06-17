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

import java.util.List;

/**
 * 动态 SQL AST 节点标记接口。
 */
public interface SqlNode {
}

class MixedSqlNode implements SqlNode {
    public final List<SqlNode> children;

    public MixedSqlNode(List<SqlNode> children) {
        this.children = children;
    }
}

class BindSqlNode implements SqlNode {
    public final String name;
    public final String value;

    public BindSqlNode(String name, String value) {
        this.name = name;
        this.value = value;
    }
}

class TextSqlNode implements SqlNode {
    public final String text;

    public TextSqlNode(String text) {
        this.text = text;
    }
}

class IfSqlNode implements SqlNode {
    public final String test;
    public final List<SqlNode> children;

    public IfSqlNode(String test, List<SqlNode> children) {
        this.test = test;
        this.children = children;
    }
}

class WhereSqlNode implements SqlNode {
    public final List<SqlNode> children;

    public WhereSqlNode(List<SqlNode> children) {
        this.children = children;
    }
}

class SetSqlNode implements SqlNode {
    public final List<SqlNode> children;

    public SetSqlNode(List<SqlNode> children) {
        this.children = children;
    }
}

class TrimSqlNode implements SqlNode {
    public final String prefix;
    public final String prefixOverrides;
    public final String suffix;
    public final String suffixOverrides;
    public final List<SqlNode> children;

    public TrimSqlNode(String prefix, String prefixOverrides, String suffix, String suffixOverrides, List<SqlNode> children) {
        this.prefix = prefix;
        this.prefixOverrides = prefixOverrides;
        this.suffix = suffix;
        this.suffixOverrides = suffixOverrides;
        this.children = children;
    }
}

class ForeachSqlNode implements SqlNode {
    public final String collection;
    public final String item;
    public final String index;
    public final String separator;
    public final String open;
    public final String close;
    public final List<SqlNode> children;

    public ForeachSqlNode(String collection, String item, String index, String separator, String open, String close, List<SqlNode> children) {
        this.collection = collection;
        this.item = item;
        this.index = index;
        this.separator = separator;
        this.open = open;
        this.close = close;
        this.children = children;
    }
}

class ChooseSqlNode implements SqlNode {
    public final List<SqlNode> children;

    public ChooseSqlNode(List<SqlNode> children) {
        this.children = children;
    }
}

class WhenSqlNode implements SqlNode {
    public final String test;
    public final List<SqlNode> children;

    public WhenSqlNode(String test, List<SqlNode> children) {
        this.test = test;
        this.children = children;
    }
}

class OtherwiseSqlNode implements SqlNode {
    public final List<SqlNode> children;

    public OtherwiseSqlNode(List<SqlNode> children) {
        this.children = children;
    }
}
class IncludeSqlNode implements SqlNode {
    public final String refid;

    public IncludeSqlNode(String refid) {
        this.refid = refid;
    }
}

class SqlSqlNode implements SqlNode {
    public final String id;
    public final List<SqlNode> children;

    public SqlSqlNode(String id, List<SqlNode> children) {
        this.id = id;
        this.children = children;
    }
}
