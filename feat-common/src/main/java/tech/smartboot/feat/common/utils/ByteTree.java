/*******************************************************************************
 * Copyright (c) 2017-2022, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: BitTree.java
 * Date: 2022-01-02
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.common.utils;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/2
 */
public class ByteTree<T> {
    public final static ByteTree<?> DEFAULT = new ByteTree<>();
    public final static ByteTree.EndMatcher SP_END_MATCHER = endByte -> endByte == Constant.SP;
    public final static ByteTree.EndMatcher COLON_END_MATCHER = endByte -> endByte == Constant.COLON;
    public final static ByteTree.EndMatcher CR_END_MATCHER = endByte -> endByte == Constant.CR;
    private static final int MAX_DEPTH = 128;
    private static final EndMatcher NULL_END_MATCHER = endByte -> false;
    private final byte value;
    private final int depth;
    private final ByteTree<T> parent;
    protected String stringValue;
    private int shift = -1;
    private ByteTree<T>[] nodes = new ByteTree[1];
    /**
     * 捆绑附件对象
     */
    private T attach;

    public ByteTree() {
        this(null, Byte.MIN_VALUE);
    }

    public ByteTree(ByteTree<T> parent, byte value) {
        this.parent = parent;
        this.value = value;
        this.depth = parent == null ? 0 : parent.depth + 1;
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException("maxDepth is " + MAX_DEPTH + " , current is " + depth);
        }
    }

    public int getDepth() {
        return depth;
    }

    public ByteTree<T> search(byte[] bytes, int offset, int len, EndMatcher endMatcher) {
        return search(bytes, offset, len, endMatcher, true);
    }

    /**
     * 从给定的字节数组总匹配出特定结尾的区块
     *
     * @param bytes      待匹配的字节数组
     * @param offset     起始位置
     * @param limit      截止位置
     * @param endMatcher 匹配接口
     * @param cache      是否缓存新节点
     * @return
     */
    public ByteTree<T> search(byte[] bytes, int offset, int limit, EndMatcher endMatcher, boolean cache) {
        ByteTree<T> byteTree = this;
        while (true) {
            if (offset >= limit) {
                return null;
            }
            if (endMatcher.match(bytes[offset])) {
                return byteTree;
            }

            int i = bytes[offset] - byteTree.shift;
            if (i >= byteTree.nodes.length || i < 0) {
                break;
            }
            ByteTree<T> b = byteTree.nodes[i];
            if (b != null) {
                byteTree = b;
                offset++;
            } else {
                break;
            }
        }
        if (cache && byteTree.depth < MAX_DEPTH) {
            //在当前节点上追加子节点
            byteTree.addNode(bytes, offset, limit, endMatcher);
            return byteTree.search(bytes, offset, limit, endMatcher, cache);
        } else {
            // 构建临时对象，用完由JVM回收
            for (int i = offset; i < limit; i++) {
                if (endMatcher.match(bytes[i])) {
                    int length = i - offset + byteTree.depth;
                    return new VirtualByteTree(new String(bytes, offset - byteTree.depth, length), length);
                }
            }
            return null;
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
        }
        return nextTree.addNode(value, offset + 1, limit, endMatcher);
    }

    private void increase(int size) {
        if (size == 0)
            size = -1;
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


    private class VirtualByteTree extends ByteTree<T> {
        private final int virtualDepth;

        public VirtualByteTree(String value, int depth) {
            super();
            this.stringValue = value;
            this.virtualDepth = depth;
        }

        @Override
        public int getDepth() {
            return virtualDepth;
        }
    }
}
