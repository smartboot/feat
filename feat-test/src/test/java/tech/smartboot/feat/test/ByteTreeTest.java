/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.feat.core.common.ByteTree;

import java.nio.ByteBuffer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ByteTreeTest {

    @Test
    public void test1() {
        ByteTree byteTree = new ByteTree();
        byteTree.addNode("Hello");
        byteTree.addNode("Hello1");
        byteTree.addNode("Hello2");
        byte[] bytes = {'H', 'e', 'l', 'l', 'o', '1'};
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        ByteTree searchTree = byteTree.search(byteBuffer, endByte -> endByte == '1');
        Assert.assertEquals(searchTree.getStringValue(), "Hello");
        System.out.println(searchTree.getStringValue());

        byteBuffer.position(0);
        searchTree = byteTree.search(byteBuffer, endByte -> endByte == 'l');
        Assert.assertEquals(searchTree.getStringValue(), "He");
        System.out.println(searchTree.getStringValue());

        byteBuffer.position(0);
        searchTree = byteTree.search(byteBuffer, endByte -> endByte == 'o');
        System.out.println(searchTree.getStringValue());
        Assert.assertEquals(searchTree.getStringValue(), "Hell");
    }

    @Test
    public void test2() {
        ByteTree byteTree = new ByteTree();
        byte[] bytes = "Hello Worldaa".getBytes();
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        ByteTree searchTree = byteTree.search(byteBuffer, endByte -> endByte == 'a');
        Assert.assertEquals(searchTree.getStringValue(), "Hello World");
        System.out.println(searchTree.getStringValue());

        byteBuffer.position(0);
        searchTree = byteTree.search(byteBuffer, endByte -> endByte == ' ');
        System.out.println(searchTree.getStringValue());
        Assert.assertEquals(searchTree.getStringValue(), "Hello");

    }

    @Test
    public void testMaxDepthGuard() {
        ByteTree<String> byteTree = new ByteTree<>();
        //超过MAX_DEPTH(32)的序列不入树，避免截断后与32字节前缀路径互相覆盖
        String longUri = "/01234567890123456789012345678901";
        Assert.assertEquals(33, longUri.getBytes().length);
        byteTree.addNode(longUri, "longHandler");
        ByteBuffer buffer = ByteBuffer.wrap((longUri + " ").getBytes());
        ByteTree<String> node = byteTree.search(buffer, ByteTree.SP_END_MATCHER);
        Assert.assertNull(node.getAttach());

        //32字节以内的序列正常缓存命中
        String shortUri = longUri.substring(0, 32);
        Assert.assertEquals(32, shortUri.getBytes().length);
        byteTree.addNode(shortUri, "shortHandler");
        buffer = ByteBuffer.wrap((shortUri + " ").getBytes());
        node = byteTree.search(buffer, ByteTree.SP_END_MATCHER);
        Assert.assertEquals("shortHandler", node.getAttach());
        Assert.assertEquals(shortUri, node.getStringValue());
    }
}
