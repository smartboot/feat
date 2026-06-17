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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 MyBatis {@code <script>} 注解字符串解析为 {@link SqlNode} AST。
 */
public final class DynamicSqlParser {

    private DynamicSqlParser() {
    }

    public static SqlNode parse(String xml) {
        if (xml == null) {
            return new TextSqlNode("");
        }
        String trimmed = xml.trim();
        if (trimmed.isEmpty()) {
            return new TextSqlNode("");
        }
        String wrapped = trimmed.startsWith("<?xml") ? trimmed : "<root>" + trimmed + "</root>";
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            org.w3c.dom.Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(wrapped)));
            Element root = doc.getDocumentElement();
            if ("script".equalsIgnoreCase(root.getTagName())) {
                return new MixedSqlNode(buildChildren(root));
            }
            return new MixedSqlNode(buildChildren(root));
        } catch (Exception e) {
            throw new RuntimeException("parse dynamic sql error: " + e.getMessage(), e);
        }
    }

    private static List<SqlNode> buildChildren(Node parent) {
        List<SqlNode> children = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            SqlNode child = build(nodes.item(i));
            if (child != null) {
                children.add(child);
            }
        }
        return children;
    }

    private static SqlNode build(Node node) {
        short type = node.getNodeType();
        if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
            String text = node.getTextContent();
            if (text == null) {
                return null;
            }
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return new TextSqlNode(text);
        }
        if (type != Node.ELEMENT_NODE) {
            return null;
        }
        Element element = (Element) node;
        String tag = element.getTagName();
        switch (tag.toLowerCase()) {
            case "script":
                return new MixedSqlNode(buildChildren(element));
            case "bind":
                return new BindSqlNode(attr(element, "name"), attr(element, "value"));
            case "if":
                return new IfSqlNode(attr(element, "test"), buildChildren(element));
            case "where":
                return new WhereSqlNode(buildChildren(element));
            case "set":
                return new SetSqlNode(buildChildren(element));
            case "trim":
                return new TrimSqlNode(
                        attr(element, "prefix"),
                        attr(element, "prefixOverrides"),
                        attr(element, "suffix"),
                        attr(element, "suffixOverrides"),
                        buildChildren(element));
            case "foreach":
                return new ForeachSqlNode(
                        attr(element, "collection"),
                        attr(element, "item"),
                        attr(element, "index"),
                        attr(element, "separator"),
                        attr(element, "open"),
                        attr(element, "close"),
                        buildChildren(element));
            case "choose":
                return new ChooseSqlNode(buildChildren(element));
            case "when":
                return new WhenSqlNode(attr(element, "test"), buildChildren(element));
            case "otherwise":
                return new OtherwiseSqlNode(buildChildren(element));
            default:
                throw new RuntimeException("unsupported dynamic sql tag: <" + tag + ">");
            case "include":
                return new IncludeSqlNode(attr(element, "refid"));
            case "sql":
                return new SqlSqlNode(attr(element, "id"), buildChildren(element));
        }
    }

    private static String attr(Element element, String name) {
        if (element.hasAttribute(name)) {
            String value = element.getAttribute(name);
            return value == null ? "" : value;
        }
        return "";
    }
}
