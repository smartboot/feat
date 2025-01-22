/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: RouteHandle.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.handler;

import tech.smartboot.feat.core.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀
 * @version V1.0 , 2018/3/24
 */
public final class Router extends BaseHttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Router.class);
    /**
     * 默认404
     */
    private final BaseHttpHandler defaultHandler;
    private final NodePath rootPath = new NodePath("/");

    public Router() {
        this(new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws IOException {
                request.getResponse().setHttpStatus(HttpStatus.NOT_FOUND);
            }
        });
    }

    public Router(BaseHttpHandler defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    @Override
    public void onHeaderComplete(HttpEndpoint request) throws IOException {
        BaseHttpHandler httpServerHandler = matchHandler(request.getRequestURI());
//        System.out.println("match: " + request.getRequestURI() + " : " + httpServerHandler);
        //注册 URI 与 Handler 的映射关系
        request.getConfiguration().getUriByteTree().addNode(request.getUri(), httpServerHandler);
        //更新本次请求的实际 Handler
        request.setServerHandler(httpServerHandler);
        httpServerHandler.onHeaderComplete(request);
    }

    @Override
    public void onClose(HttpEndpoint request) {
        LOGGER.warn("connection is closed before route match.");
        defaultHandler.onClose(request);
    }

    @Override
    public void handle(HttpRequest request, CompletableFuture<Object> completableFuture) throws Throwable {
        if (request.getProtocol() == HttpProtocolEnum.HTTP_2) {
            BaseHttpHandler httpServerHandler = matchHandler(request.getRequestURI());
            httpServerHandler.handle(request, completableFuture);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * 配置URL路由
     *
     * @param urlPattern  url匹配
     * @param httpHandler 处理handler
     * @return
     */
    public Router route(String urlPattern, BaseHttpHandler httpHandler) {
        LOGGER.info("route: " + urlPattern);
        rootPath.add(urlPattern, httpHandler);
        return this;
    }

    public Router route(String urlPattern, HttpHandler httpHandler) {
        return route(urlPattern, new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws Throwable {
                httpHandler.handle(request);
            }
        });
    }

    private BaseHttpHandler matchHandler(String uri) {
        BaseHttpHandler httpHandler = rootPath.match(uri);
        if (httpHandler == null) {
            System.out.println("request: " + uri + " ,match: default    ");
            httpHandler = defaultHandler;
        }
        return httpHandler;
    }

    public static class NodePath {
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

}
