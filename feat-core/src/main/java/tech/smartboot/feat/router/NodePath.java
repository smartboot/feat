package tech.smartboot.feat.router;

import tech.smartboot.feat.core.server.handler.BaseHttpHandler;

import java.util.HashMap;
import java.util.Map;

final class NodePath {
    private final static String PATTERN_KEY = "$E";
    private final static int TYPE_EXACT_PATH_NODE = 1;
    private final static int TYPE_PATH_PARAM_NODE = 2;
    //精准匹配
    private final static int TYPE_EXACT_LEAF = 2;
    //统配
    private final static int TYPE_PATTERN_LEAF = 3;
    //后缀匹配
    private final static int TYPE_ENDING_PATTERN_LEAF = 5;
    private final static int TYPE_PATH_PARAM_LEAF = 6;
    private final String path;
    private final int type;
    private final int depth;
    //精准匹配
    private final Map<String, NodePath> exactPaths;

    //后缀匹配
    private final Map<String, NodePath> patternPaths;
    private final BaseHttpHandler handler;

    public NodePath(String path) {
        this(path, TYPE_EXACT_PATH_NODE, 0);
    }


    public NodePath(String path, int type, int depth) {
        this.path = path;
        this.type = type;
        this.handler = null;
        this.depth = depth;
        exactPaths = new HashMap<>();
        patternPaths = new HashMap<>();
    }

    public NodePath(String path, int type, BaseHttpHandler handler, int depth) {
        this.handler = handler;
        this.path = path;
        this.type = type;
        this.depth = depth;
        exactPaths = new HashMap<>(1);
        patternPaths = new HashMap<>(1);
    }

    public String getPath() {
        return path;
    }

    public int getType() {
        return type;
    }

    public void add(String subPath, BaseHttpHandler handler) {
        add(subPath, 0, handler);
    }

    public BaseHttpHandler match(String uri) {
        NodePath nodePath = match(uri, 0);
        return nodePath == null ? null : nodePath.handler;
    }

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
        NodePath path = exactPaths.get(subPath.substring(offset + 1, nextIndex));
        if (path != null) {
            path = path.match(subPath, nextIndex);
            if (path != null) {
                return path;
            }
        }

        for (NodePath nodePath : patternPaths.values()) {
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

    public void add(final String subPath, int offset, BaseHttpHandler handler) {
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
            if (subPath.charAt(0) == ':') {
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
            throw new IllegalArgumentException("subPath: " + nodePath + " is illegal");
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