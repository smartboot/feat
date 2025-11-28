/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent;

/**
 * ReActAgent测试类
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ReActAgentTest {

    public static void main(String[] args) {
        // 创建ReActAgent实例
        FeatAgent agent = new ReActAgent();

        // 执行测试
        String result = agent.execute("阅读spring官方文档，对比下spring4和3的差别，生成一份详细的分析报告");
        System.out.println("\n\n最终结果:\n" + result);
        System.out.println("\nAgent最终状态: " + agent.getState());
    }
}