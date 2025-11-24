/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tool.standard;

import tech.smartboot.feat.ai.agent.tool.ToolExecutor;
import tech.smartboot.feat.ai.agent.tool.ToolExecutionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 标准工具注册表，提供一组类似DeepAgents的标准工具
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class StandardToolsRegistry {
    
    /**
     * 获取所有标准工具
     *
     * @return 标准工具列表
     */
    public static List<ToolExecutor> getStandardTools() {
        List<ToolExecutor> tools = new ArrayList<>();
        tools.add(new TodoListTool());
        tools.add(new FileOperationTool());
        tools.add(new SubAgentTool());
        tools.add(new SearchTool());
        return tools;
    }
    
    /**
     * 将标准工具注册到工具执行管理器
     *
     * @param toolExecutionManager 工具执行管理器
     */
    public static void registerStandardTools(ToolExecutionManager toolExecutionManager) {
        for (ToolExecutor tool : getStandardTools()) {
            toolExecutionManager.addToolExecutor(tool.getName(), tool);
        }
    }
}