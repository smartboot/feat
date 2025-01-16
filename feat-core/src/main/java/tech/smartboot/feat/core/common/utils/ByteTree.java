/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: BitTree.java
 * Date: 2022-01-02
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.common.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/2
 */
public class ByteTree<T> {
    public final static ByteTree<?> DEFAULT = new ByteTree<>();
    public final static ByteTree.EndMatcher SP_END_MATCHER = endByte -> endByte == Constant.SP;
    public final static ByteTree.EndMatcher COLON_END_MATCHER = endByte -> endByte == ':';
    public final static ByteTree.EndMatcher CR_END_MATCHER = endByte -> endByte == Constant.CR;
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
    private int limit = 1000;
    /**
     * 捆绑附件对象
     */
    private T attach;

    public ByteTree() {
        this(1024);
    }

    public ByteTree(int limit) {
        this(null, Byte.MIN_VALUE);
        this.limit = limit;
    }

    public ByteTree(ByteTree<T> parent, byte value) {
        this.parent = parent;
        this.value = value;
        this.depth = parent == null ? 0 : parent.depth + 1;
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException("maxDepth is " + MAX_DEPTH + " , current is " + depth);
        }
    }

    public ByteTree<T> search(ByteBuffer bytes, EndMatcher endMatcher, boolean cache) {
        int p = bytes.position();
        ByteTree<T> byteTree = this;
        bytes.mark();
        while (bytes.hasRemaining()) {
            byte v = bytes.get();
            if (endMatcher.match(v)) {
                bytes.mark();
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
        if (!bytes.hasRemaining()) {
            bytes.reset();
            return null;
        }
        if (cache && byteTree.depth < MAX_DEPTH) {
            //在当前节点上追加子节点
            bytes.reset();
            this.addNode(bytes, endMatcher);
            bytes.reset();
            return search(bytes, endMatcher, cache);
        } else {
            bytes.position(bytes.position() - 1);
            // 构建临时对象，用完由JVM回收
            while (bytes.hasRemaining()) {
                if (endMatcher.match(bytes.get())) {
                    int length = bytes.position() - p;
                    byte[] data = new byte[length];
                    bytes.position(bytes.position() - length);
                    bytes.get(data, 0, length);
                    return new VirtualByteTree(new String(data, 0, length - 1, StandardCharsets.US_ASCII));
                }
            }
            bytes.reset();
            return null;
        }
    }

    public static <T> void reduceCapacity(ByteTree<T> byteTree, int releaseCount) {
        if (releaseCount <= 0) {
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
            System.out.println("continue node " + removed);
            reduceCapacity(removed, releaseCount);
        }

    }

    public void addNode(String value, T attach) {
        byte[] bytes = value.getBytes();
        ByteTree<T> tree = this;
        while (tree.depth > 0) {
            tree = tree.parent;
        }
        ByteTree<T> leafNode = tree.addNode(bytes, 0, bytes.length, NULL_END_MATCHER);
        leafNode.stringValue = value;
        leafNode.attach = attach;
    }

    /**
     * 从根节点开始，为入参字符串创建节点
     */
    public void addNode(String value) {
        addNode(value, null);
    }

    private ByteTree<T> addNode(byte[] value, int offset, int limit, EndMatcher endMatcher) {
        if (offset == limit) {
            return this;
        }
        if (this.depth >= MAX_DEPTH) {
            return this;
        }

        byte b = value[offset];
        if (endMatcher.match(b)) {
            return this;
        }
//        System.out.println("add Node");
        if (shift == -1) {
            shift = b;
        }
        if (b - shift < 0) {
            increase(b - shift);
        } else {
            increase(b + 1 - shift);
        }

        ByteTree<T> nextTree = nodes[b - shift];
        if (nextTree == null) {
            nextTree = nodes[b - shift] = new ByteTree<T>(this, b);
            nodeCount++;
            updateCounter(this);
        }
        return nextTree.addNode(value, offset + 1, limit, endMatcher);
    }

    private ByteTree<T> addNode(ByteBuffer value, EndMatcher endMatcher) {
        if (!value.hasRemaining()) {
            return this;
        }
        if (this.depth >= MAX_DEPTH) {
            return this;
        }

        byte b = value.get();
        if (endMatcher.match(b)) {
            return this;
        }
//        System.out.println("add Node");
        if (shift == -1) {
            shift = b;
        }
        if (b - shift < 0) {
            increase(b - shift);
        } else {
            increase(b + 1 - shift);
        }

        ByteTree<T> nextTree = nodes[b - shift];
        if (nextTree == null) {
            nextTree = nodes[b - shift] = new ByteTree<T>(this, b);
            nodeCount++;
            updateCounter(this);
        }
        return nextTree.addNode(value, endMatcher);
    }

    private static <T> void updateCounter(ByteTree<T> node) {
        node.totalCount = node.nodeCount;
        node.capacity = node.nodes.length;
        for (ByteTree<T> child : node.nodes) {
            if (child != null) {
                node.capacity += child.capacity;
                node.totalCount += child.totalCount;
            }
        }
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

    public String getStringValue() {
        if (stringValue == null) {
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

    public T getAttach() {
        return attach;
    }

    public interface EndMatcher {
        boolean match(byte endByte);
    }

    public int getLimit() {
        return limit;
    }

    public int getCapacity() {
        return capacity;
    }

    private class VirtualByteTree extends ByteTree<T> {

        public VirtualByteTree(String value) {
            super();
            this.stringValue = value;
        }
    }
}
