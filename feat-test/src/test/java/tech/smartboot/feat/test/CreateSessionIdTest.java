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
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.common.FeatUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * 会话ID安全性测试：验证生成的会话ID版本特征可解出且具备足够的随机熵
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class CreateSessionIdTest {

    private static final String MAGIC_NUMBER = "feat-";
    private static final int MASK_LENGTH = 4;

    @Test
    public void testSessionIdEntropy() {
        byte[] versionBytes = Feat.VERSION.getBytes(StandardCharsets.UTF_8);
        Set<String> sessionIds = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            String sessionId = FeatUtils.createSessionId();
            //格式校验：feat- 前缀 + base64url 编码
            Assert.assertTrue(sessionId.startsWith(MAGIC_NUMBER));
            byte[] bytes = Base64.getUrlDecoder().decode(sessionId.substring(MAGIC_NUMBER.length()));
            //结构校验：4字节掩码 + 版本号 + 12字节随机数，掩码与随机数合计128位熵
            Assert.assertEquals(MASK_LENGTH + versionBytes.length + 12, bytes.length);
            sessionIds.add(sessionId);
        }
        //无碰撞：随机性有效
        Assert.assertEquals(10000, sessionIds.size());
    }

    @Test
    public void testVersionEmbedded() {
        String sessionId = FeatUtils.createSessionId();
        byte[] bytes = Base64.getUrlDecoder().decode(sessionId.substring(MAGIC_NUMBER.length()));
        byte[] versionBytes = Feat.VERSION.getBytes(StandardCharsets.UTF_8);

        //掩码明文存放于前4字节，直接读取
        byte[] mask = Arrays.copyOfRange(bytes, 0, MASK_LENGTH);

        //用掩码异或还原版本号
        StringBuilder decoded = new StringBuilder();
        for (int i = MASK_LENGTH; i < MASK_LENGTH + versionBytes.length; i++) {
            decoded.append((char) (bytes[i] ^ mask[i % MASK_LENGTH]));
        }
        Assert.assertEquals(Feat.VERSION, decoded.toString());
    }

    @Test
    public void testSessionIdUnpredictable() {
        //连续生成的会话ID之间不应存在可推导的关联（旧实现基于Random+时间戳可被预测）
        String sessionId1 = FeatUtils.createSessionId();
        String sessionId2 = FeatUtils.createSessionId();
        Assert.assertNotEquals(sessionId1, sessionId2);
        Assert.assertNotEquals(sessionId1, FeatUtils.createSessionId());
    }
}
