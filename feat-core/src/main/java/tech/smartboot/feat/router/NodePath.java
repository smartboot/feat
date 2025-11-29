/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.router;

import tech.smartboot.feat.core.server.HttpHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 路由节点路径类，用于构建路由匹配的前缀树结构
 * <p>
 * NodePath实现了基于前缀树（Trie）的路由匹配算法，支持精确匹配、路径参数匹配、
 * 通配符匹配和后缀匹配等多种路由匹配模式。通过树形结构组织路由规则，
 * 提高路由匹配的效率和准确性。
 * </p>
 * <p>
 * 支持的路由匹配模式：
 * <ul>
 *   <li>精确匹配：如 /user/profile</li>
 *   <li>路径参数匹配：如 /user/:id</li>
 *   <li>通配符匹配：如 /static/*</li>
 *   <li>后缀匹配：如 *.html</li>
 * </ul>
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
final class NodePath {
    /**
     * 通配符键名，用于标识通配符节点
     */
    private final static String PATTERN_KEY = "$E";
    
    /**
     * 精确路径节点类型
     */
    private final static int TYPE_EXACT_PATH_NODE = 1;
    
    /**
     * 路径参数节点类型
     */
    private final static int TYPE_PATH_PARAM_NODE = 2;
    
    /**
     * 精确叶子节点类型
     */
    private final static int TYPE_EXACT_LEAF = 2;
    
    /**
     * 通配符叶子节点类型
     */
    private final static int TYPE_PATTERN_LEAF = 3;
    
    /**
     * 后缀匹配叶子节点类型
     */
    private final static int TYPE_ENDING_PATTERN_LEAF = 5;
    
    /**
     * 路径参数叶子节点类型
     */
    private final static int TYPE_PATH_PARAM_LEAF = 6;
    
    /**
     * 节点路径字符串
     * <p>
     * 表示当前节点对应的路径片段，如"user"、":id"等
     * </p>
     */
    private final String path;
    
    /**
     * 节点类型
     * <p>
     * 用于标识当前节点的类型，如精确匹配节点、路径参数节点等
     * </p>
     */
    private final int type;
    
    /**
     * 节点深度
     * <p>
     * 表示当前节点在树形结构中的深度，根节点深度为0
     * </p>
     */
    private final int depth;
    
    /**
     * 精确匹配子路径映射表
     * <p>
     * 存储所有精确匹配的子路径节点，键为路径片段，值为对应的节点对象
     * </p>
     */
    private final Map<String, NodePath> exactPaths;

    /**
     * 模式匹配子路径映射表
     * <p>
     * 存储所有模式匹配的子路径节点，包括通配符、路径参数和后缀匹配等类型
     * </p>
     */
    private final Map<String, NodePath> patternPaths;
    
    /**
     * 路由处理器
     * <p>
     * 当前节点对应的路由处理器，只有叶子节点才会有非空的处理器
     * </p>
     */
    private RouterHandlerImpl handler;

    /**
     * 构造一个根路径节点
     *
     * @param path 节点路径
     */
    public NodePath(String path) {
        this(path, TYPE_EXACT_PATH_NODE, 0);
    }

    /**
     * 构造一个指定类型和深度的路径节点
     *
     * @param path  节点路径
     * @param type  节点类型
     * @param depth 节点深度
     */
    public NodePath(String path, int type, int depth) {
        this.path = path;
        this.type = type;
        this.handler = null;
        this.depth = depth;
        exactPaths = new HashMap<>();
        patternPaths = new HashMap<>();
    }

    /**
     * 构造一个带有处理器的叶子节点
     *
     * @param path    节点路径
     * @param type    节点类型
     * @param handler 路由处理器
     * @param depth   节点深度
     */
    public NodePath(String path, int type, RouterHandlerImpl handler, int depth) {
        this.handler = handler;
        this.path = path;
        this.type = type;
        this.depth = depth;
        exactPaths = new HashMap<>(1);
        patternPaths = new HashMap<>(1);
    }

    /**
     * 获取节点路径
     *
     * @return 节点路径字符串
     */
    public String getPath() {
        return path;
    }

    /**
     * 获取节点类型
     *
     * @return 节点类型标识
     */
    public int getType() {
        return type;
    }

    /**
     * 添加子路径和对应的处理器
     *
     * @param subPath 子路径
     * @param handler 路由处理器
     */
    public void add(String subPath, RouterHandlerImpl handler) {
        add(subPath, 0, handler);
    }

    /**
     * 匹配指定URI并返回对应的处理器
     *
     * @param uri 待匹配的URI
     * @return 匹配到的处理器，如果没有匹配到则返回null
     */
    public HttpHandler match(String uri) {
        NodePath nodePath = match(uri, 0);
        return nodePath == null ? null : nodePath.handler;
    }

    /**
     * 递归匹配指定URI的子路径
     *
     * @param subPath 被匹配的子路径
     * @param offset  当前匹配的偏移量
     * @return 匹配到的节点，如果没有匹配到则返回null
     */
    private NodePath match(final String subPath, int offset) {
        int nextIndex;
        //尾部匹配
        if (subPath.charAt(offset) != '/') {
            nextIndex = -1;
        } else {
            nextIndex = subPath.indexOf("/", offset + 1);
        }
        if (nextIndex == -1) {
            String nodePath = subPath.substring(offset + 1);
            if (exactPaths.containsKey(nodePath)) {
                return exactPaths.get(nodePath);
            }
            for (Map.Entry<String, NodePath> entry : patternPaths.entrySet()) {
                if (entry.getValue().type == TYPE_PATH_PARAM_NODE) {
                    continue;
                }
                if (nodePath.endsWith(entry.getValue().path)) {
                    return entry.getValue();
                }
            }
            return patternPaths.get(PATTERN_KEY);
        }
        String node = subPath.substring(offset + 1, nextIndex);
        NodePath path = exactPaths.get(node);
        if (path != null) {
            path = path.match(subPath, nextIndex);
            if (path != null) {
                return path;
            }
        }

        for (NodePath nodePath : patternPaths.values()) {
            //通配匹配 /*
            if (nodePath.type == TYPE_PATTERN_LEAF) {
                path = nodePath;
                break;
            }
            nodePath = nodePath.match(subPath, nextIndex);
            if (nodePath == null) {
                continue;
            }
            if (path == null || path.depth < nodePath.depth) {
                path = nodePath;
            }
        }
        return path;
    }

    /**
     * 递归添加子路径和对应的处理器
     *
     * @param subPath  子路径
     * @param offset   当前处理的偏移量
     * @param handler  路由处理器
     */
    private void add(final String subPath, int offset, RouterHandlerImpl handler) {
        int nextIndex;
        //尾部匹配
        if (subPath.charAt(offset) != '/') {
            nextIndex = -1;
        } else {
            nextIndex = subPath.indexOf("/", offset + 1);
        }

        if (nextIndex != -1) {
            String nodePath = subPath.substring(offset + 1, nextIndex);
            NodePath curNode = null;
            if (subPath.charAt(offset + 1) == ':') {
                curNode = patternPaths.computeIfAbsent(nodePath, ptah -> new NodePath(ptah, TYPE_PATH_PARAM_NODE, depth + 1));
            } else {
                curNode = exactPaths.computeIfAbsent(nodePath, ptah -> new NodePath(ptah, TYPE_EXACT_PATH_NODE, depth + 1));
            }
            curNode.add(subPath, nextIndex, handler);
            return;
        }

        //处理叶子节点
        String nodePath = subPath.substring(offset + 1);
        int type = 0;
        NodePath curNode;
        if (!nodePath.isEmpty() && nodePath.charAt(0) == ':') {
            type = TYPE_PATH_PARAM_LEAF;
            curNode = patternPaths.get(PATTERN_KEY);
            nodePath = PATTERN_KEY;
        } else if (nodePath.equals("*")) {
            type = TYPE_PATTERN_LEAF;
            nodePath = PATTERN_KEY;
            curNode = patternPaths.get(PATTERN_KEY);
        } else if (nodePath.startsWith("*.")) {
            type = TYPE_ENDING_PATTERN_LEAF;
            nodePath = nodePath.substring(1);
            curNode = patternPaths.get(nodePath);
        } else {
            type = TYPE_EXACT_LEAF;
            curNode = exactPaths.get(nodePath);
        }
        if (curNode != null) {
            if (curNode.handler == null) {
                curNode.handler = handler;
            } else {
                curNode.handler.addMethodHandler(handler);
            }
            return;
        }

        switch (type) {
            case TYPE_EXACT_LEAF:
                curNode = new NodePath(nodePath, TYPE_EXACT_LEAF, handler, depth + 1);
                exactPaths.put(nodePath, curNode);
                break;
            case TYPE_ENDING_PATTERN_LEAF:
                curNode = new NodePath(nodePath, TYPE_ENDING_PATTERN_LEAF, handler, depth + 1);
                patternPaths.put(nodePath, curNode);
                break;
            case TYPE_PATH_PARAM_LEAF:
            case TYPE_PATTERN_LEAF:
                curNode = new NodePath(nodePath, type, handler, depth + 1);
                patternPaths.put(nodePath, curNode);
                break;
        }
    }
}