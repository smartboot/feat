/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.common.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class ByteTree<T> {
    public final static ByteTree<?> DEFAULT = new ByteTree<>();
    public final static EndMatcher SP_END_MATCHER = endByte -> endByte == Constant.SP;
    public final static EndMatcher COLON_END_MATCHER = endByte -> endByte == ':';
    public final static EndMatcher CR_END_MATCHER = endByte -> endByte == Constant.CR;
    private static final int MAX_DEPTH = 128;
    private static final EndMatcher NULL_END_MATCHER = endByte -> false;
    private final byte value;
    private final int depth;
    private final ByteTree<T> parent;
    protected String stringValue;
    private int shift = -1;
    private ByteTree<T>[] nodes = new ByteTree[1];
    private int nodeCount;
    /**
     * 有效节点总数
     */
    private int totalCount;
    /**
     * 节点数总容量
     */
    private int capacity;
    /**
     * 总容量上限
     */
    private int limit;
    /**
     * 捆绑附件对象
     */
    private T attach;

    /**
     * 创建一个默认的字节树，容量限制为16K
     */
    ByteTree() {
        this(16 * 1024);
    }

    /**
     * 创建一个指定容量限制的字节树
     *
     * @param limit 容量上限
     */
    public ByteTree(int limit) {
        this(null, Byte.MIN_VALUE);
        this.limit = limit;
    }

    /**
     * 创建一个字节树节点
     *
     * @param parent 父节点
     * @param value  节点值
     * @throws IllegalStateException 如果节点深度超过最大深度限制
     */
    ByteTree(ByteTree<T> parent, byte value) {
        this.parent = parent;
        this.value = value;
        this.depth = parent == null ? 0 : parent.depth + 1;
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException("maxDepth is " + MAX_DEPTH + " , current is " + depth);
        }
    }

    public ByteTree<T> search(ByteBuffer buffer, EndMatcher endMatcher, boolean cache) {
        boolean trimState = true;
        int markPosition = buffer.position();
        ByteTree<T> byteTree = this;
        while (buffer.hasRemaining()) {
            byte v = buffer.get();
            if (trimState) {
                if (v == Constant.SP) {
                    continue;
                } else {
                    trimState = false;
                    markPosition = buffer.position() - 1;
                }
            }

            if (endMatcher.match(v)) {
                return byteTree;
            }

            int i = v - byteTree.shift;
            if (i < 0 || i >= byteTree.nodes.length) {
                break;
            }

            ByteTree<T> b = byteTree.nodes[i];
            if (b != null) {
                byteTree = b;
            } else {
                break;
            }
        }
        //buffer已读完，未匹配到，重置position
        if (!buffer.hasRemaining()) {
            buffer.position(markPosition);
            return null;
        }
        //ByteTree已遍历结束
        if (cache && byteTree.depth < MAX_DEPTH) {
            //在当前节点上追加子节点
            buffer.position(markPosition);
            this.addNode(buffer, endMatcher);
            buffer.position(markPosition);
            return search(buffer, endMatcher, cache);
        } else {
            buffer.position(buffer.position() - 1);
            // 构建临时对象，用完由JVM回收
            while (buffer.hasRemaining()) {
                if (endMatcher.match(buffer.get())) {
                    int length = buffer.position() - markPosition;
                    byte[] data = new byte[length];
                    buffer.position(buffer.position() - length);
                    buffer.get(data, 0, length);
                    return new VirtualByteTree(new String(data, 0, length - 1, StandardCharsets.US_ASCII));
                }
            }
            buffer.position(markPosition);
            return null;
        }
    }

    public static <T> void reduceCapacity(ByteTree<T> byteTree, int releaseCount) {
        if (releaseCount <= 1) {
            return;
        }
        int index = -1;
        for (int i = 0; i < byteTree.nodes.length; i++) {
            ByteTree<?> child = byteTree.nodes[i];
            if (child == null) {
                continue;
            }
            if (index == -1 || child.capacity == 0 || byteTree.nodes[index].capacity == 0) {
                index = i;
            } else if ((child.totalCount / child.capacity) < (byteTree.nodes[index].totalCount / byteTree.nodes[index].capacity)) {
                index = i;
            }
        }
        if (index == -1) {
            if (byteTree.parent != null) {
                reduceCapacity(byteTree.parent, releaseCount);
            }
            return;
        }
        ByteTree<?> removed = byteTree.nodes[index];
        if (removed.capacity < releaseCount) {
            releaseCount -= removed.capacity;
            byteTree.nodes[index] = null;
            byteTree.nodeCount--;
            if (index == 0) {
                while (index < byteTree.nodes.length && byteTree.nodes[index] == null) {
                    index++;
                }
                ByteTree[] newNodes = new ByteTree[byteTree.nodes.length - index];
                System.arraycopy(byteTree.nodes, index, newNodes, 0, newNodes.length);
                byteTree.nodes = newNodes;
                byteTree.shift++;
            } else if (index == byteTree.nodes.length - 1) {
                while (index > 0 && byteTree.nodes[byteTree.nodes.length - index] == null) {
                    index--;
                }
                ByteTree[] newNodes = new ByteTree[index];
                System.arraycopy(byteTree.nodes, 0, newNodes, 0, newNodes.length);
                byteTree.nodes = newNodes;
            }
            System.out.println("remove node " + removed);
            updateCounter(byteTree);
            if (releaseCount > 0) {
                reduceCapacity(byteTree, releaseCount);
            }
        } else {
            System.out.println("continue node " + removed.capacity + " " + releaseCount);
            reduceCapacity(removed, releaseCount);
        }

    }

    /**
     * 添加一个带附件的节点
     *
     * @param value  节点值
     * @param attach 附件对象
     */
    public void addNode(String value, T attach) {
        byte[] bytes = value.getBytes();
        // 获取根节点
        ByteTree<T> tree = this;
        while (tree.depth > 0) {
            tree = tree.parent;
        }
        // 添加节点并设置附件
        ByteTree<T> leafNode = tree.addNode(bytes, 0, bytes.length, NULL_END_MATCHER);
        leafNode.stringValue = value;
        leafNode.attach = attach;
    }

    /**
     * 从根节点开始，为入参字符串创建节点
     *
     * @param value 节点值
     */
    public void addNode(String value) {
        addNode(value, null);
    }

    /**
     * 递归添加节点
     *
     * @param value      字节数组
     * @param offset     当前处理的偏移量
     * @param limit      字节数组的长度限制
     * @param endMatcher 结束匹配器
     * @return 添加的叶子节点
     */
    private ByteTree<T> addNode(byte[] value, int offset, int limit, EndMatcher endMatcher) {
        // 已处理完所有字节或达到最大深度，返回当前节点
        if (offset == limit || this.depth >= MAX_DEPTH) {
            return this;
        }

        byte b = value[offset];
        // 如果遇到结束字符，返回当前节点
        if (endMatcher.match(b)) {
            return this;
        }
        
        // 初始化shift值
        if (shift == -1) {
            shift = b;
        }
        
        // 确保数组有足够空间
        if (b - shift < 0) {
            increase(b - shift);
        } else {
            increase(b + 1 - shift);
        }

        // 获取或创建下一级节点
        ByteTree<T> nextTree = nodes[b - shift];
        if (nextTree == null) {
            nextTree = nodes[b - shift] = new ByteTree<T>(this, b);
            nodeCount++;
            updateCounter(this);
        }
        
        // 递归处理下一个字节
        return nextTree.addNode(value, offset + 1, limit, endMatcher);
    }

    /**
     * 从ByteBuffer递归添加节点
     *
     * @param value      字节缓冲区
     * @param endMatcher 结束匹配器
     * @return 添加的叶子节点
     */
    private synchronized ByteTree<T> addNode(ByteBuffer value, EndMatcher endMatcher) {
        // 已处理完所有字节或达到最大深度，返回当前节点
        if (!value.hasRemaining() || this.depth >= MAX_DEPTH) {
            return this;
        }

        byte b = value.get();
        // 如果遇到结束字符，返回当前节点
        if (endMatcher.match(b)) {
            return this;
        }
        
        // 初始化shift值
        if (shift == -1) {
            shift = b;
        }
        
        // 确保数组有足够空间
        if (b - shift < 0) {
            increase(b - shift);
        } else {
            increase(b + 1 - shift);
        }

        // 获取或创建下一级节点
        ByteTree<T> nextTree = nodes[b - shift];
        if (nextTree == null) {
            nextTree = nodes[b - shift] = new ByteTree<T>(this, b);
            nodeCount++;
            updateCounter(this);
        }
        
        // 递归处理下一个字节
        return nextTree.addNode(value, endMatcher);
    }

    /**
     * 更新节点及其所有父节点的计数器
     *
     * @param node 要更新的节点
     */
    private static <T> void updateCounter(ByteTree<T> node) {
        // 重置当前节点的计数
        node.totalCount = node.nodeCount;
        node.capacity = node.nodes.length;
        
        // 累加所有子节点的计数
        for (ByteTree<T> child : node.nodes) {
            if (child != null) {
                node.capacity += child.capacity;
                node.totalCount += child.totalCount;
            }
        }
        
        // 递归更新父节点
        if (node.parent != null) {
            updateCounter(node.parent);
        }
    }

    private void increase(int size) {
        if (size == 0) size = -1;
        if (size < 0) {
            ByteTree<T>[] temp = new ByteTree[nodes.length - size];
            System.arraycopy(nodes, 0, temp, -size, nodes.length);
            nodes = temp;
            shift += size;
        } else if (nodes.length < size) {
            ByteTree<T>[] temp = new ByteTree[size];
            System.arraycopy(nodes, 0, temp, 0, nodes.length);
            nodes = temp;
        }
    }

    /**
     * 获取节点的字符串值
     * 如果字符串值未缓存，则从节点路径构建字符串
     *
     * @return 节点的字符串值
     */
    public String getStringValue() {
        if (stringValue == null) {
            // 从节点路径构建字符串
            byte[] b = new byte[depth];
            ByteTree<T> tree = this;
            while (tree.depth != 0) {
                b[tree.depth - 1] = tree.value;
                tree = tree.parent;
            }
            stringValue = new String(b);
        }
        return stringValue;
    }

    /**
     * 获取节点附加的对象
     *
     * @return 附加对象
     */
    public T getAttach() {
        return attach;
    }

    /**
     * 结束匹配器接口，用于确定何时结束字节序列的处理
     */
    public interface EndMatcher {
        /**
         * 判断当前字节是否为结束字符
         *
         * @param endByte 要检查的字节
         * @return 如果是结束字符则返回true，否则返回false
         */
        boolean match(byte endByte);
    }

    /**
     * 获取字节树的容量上限
     *
     * @return 容量上限
     */
    public int getLimit() {
        return limit;
    }

    /**
     * 获取字节树当前的容量
     *
     * @return 当前容量
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * 虚拟字节树节点，用于临时存储字符串值
     * 不会添加到实际的树结构中，由JVM回收
     */
    private class VirtualByteTree extends ByteTree<T> {

        /**
         * 创建一个虚拟字节树节点
         *
         * @param value 节点的字符串值
         */
        public VirtualByteTree(String value) {
            super();
            this.stringValue = value;
        }
    }
}
