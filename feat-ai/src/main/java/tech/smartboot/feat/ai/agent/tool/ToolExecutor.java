/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tool;

import com.alibaba.fastjson2.JSONObject;

/**
 * 工具执行器接口
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public interface ToolExecutor {
    
    /**
     * 执行工具
     *
     * @param parameters 工具参数
     * @return 执行结果
     */
    String execute(JSONObject parameters);
    
    /**
     * 获取工具名称
     *
     * @return 工具名称
     */
    String getName();
    
    /**
     * 获取工具描述
     *
     * @return 工具描述
     */
    String getDescription();
    
    /**
     * 获取工具参数定义
     *
     * @return 参数定义JSON字符串
     */
    String getParametersSchema();
}