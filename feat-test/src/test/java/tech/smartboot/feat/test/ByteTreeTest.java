/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: ByteTreeTest.java
 * Date: 2022-01-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.test;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.feat.core.common.utils.ByteTree;

import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/4
 */
public class ByteTreeTest {

    @Test
    public void test1() {
        ByteTree byteTree = new ByteTree( Byte.MAX_VALUE);
        byteTree.addNode("Hello");
        byteTree.addNode("Hello1");
        byteTree.addNode("Hello2");
        byte[] bytes = {'H', 'e', 'l', 'l', 'o', '1'};
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        ByteTree searchTree = byteTree.search(byteBuffer, endByte -> endByte == '1', false);
        Assert.assertEquals(searchTree.getStringValue(), "Hello");
        System.out.println(searchTree.getStringValue());

        byteBuffer.position(0);
        searchTree = byteTree.search(byteBuffer, endByte -> endByte == 'l', false);
        Assert.assertEquals(searchTree.getStringValue(), "He");
        System.out.println(searchTree.getStringValue());

        byteBuffer.position(0);
        searchTree = byteTree.search(byteBuffer, endByte -> endByte == 'o', false);
        System.out.println(searchTree.getStringValue());
        Assert.assertEquals(searchTree.getStringValue(), "Hell");
    }

    @Test
    public void test2() {
        ByteTree byteTree = new ByteTree( Byte.MAX_VALUE);
        byte[] bytes = "Hello Worldaa".getBytes();
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        ByteTree searchTree = byteTree.search(byteBuffer, endByte -> endByte == 'a', true);
        Assert.assertEquals(searchTree.getStringValue(), "Hello World");
        System.out.println(searchTree.getStringValue());

        byteBuffer.position(0);
        searchTree = byteTree.search(byteBuffer, endByte -> endByte == ' ', true);
        System.out.println(searchTree.getStringValue());
        Assert.assertEquals(searchTree.getStringValue(), "Hello");

    }
}
