package tech.smartboot.feat.core.common.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ByteCache<T> {
    private Node[] nodes = new Node[256];

    class Node<T> {
        private int hash;
        /**
         * 捆绑附件对象
         */
        private T attach;
        private String value;

        public Node(int hash, T attach, String value) {
            this.hash = hash;
            this.attach = attach;
            this.value = value;
        }
    }

    public Node<T>[] addNode(String value, T attach, Node<T>[] nodes) {
        int hash = value.hashCode();
        Node node = nodes[hash % nodes.length];
        if (node == null) {
            node = new Node(hash, attach, value);
            nodes[hash % nodes.length] = node;
            return nodes;
        } else if (node.value.equals(value)) {
            throw new RuntimeException("value is exist");
        } else {
            Node[] newNodes = new Node[nodes.length * 2];
            for (int i = 0; i < nodes.length; i++) {
                if (nodes[i] != null) {
                    newNodes = addNode(nodes[i].value, nodes[i].attach, newNodes);
                }
            }
            return addNode(value, attach, newNodes);
        }
    }

    public Node<T> search(ByteBuffer bytes, ByteTree.EndMatcher endMatcher, boolean cache) {
        boolean init = true;
        int hash = 0;
        int p = 0;
        while (bytes.hasRemaining()) {
            byte v = bytes.get();
            if (init) {
                if (v == Constant.SP) {
                    continue;
                }
                bytes.mark();
                p = bytes.position();
                init = false;
            }
            if (endMatcher.match(v)) {
                Node<T> node = nodes[hash % nodes.length];
                if (node == null && cache) {
                    byte[] bytes1 = new byte[bytes.position() - p];
                    bytes.position(p);
                    bytes.get(bytes1);
                    String str = new String(bytes1, 0, bytes1.length - 1, StandardCharsets.US_ASCII);
                    nodes = addNode(str, null, nodes);
                    node = new Node<>(hash, null, str);
                }
                if (node == null) {
                    byte[] bytes1 = new byte[bytes.position() - p];
                    bytes.position(p);
                    bytes.get(bytes1);
                    String str = new String(bytes1, 0, bytes1.length - 1, StandardCharsets.US_ASCII);
                    node = new Node<>(hash, null, str);
                }
                return node;
            }
            hash = 31 * hash + v;
        }
        bytes.reset();
        return null;
    }
}