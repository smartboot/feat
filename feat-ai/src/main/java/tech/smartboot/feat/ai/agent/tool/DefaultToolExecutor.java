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

import java.util.function.Function;

/**
 * 默认工具执行器实现
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class DefaultToolExecutor implements ToolExecutor {
    
    private final String name;
    private final String description;
    private final String parametersSchema;
    private final Function<JSONObject, String> executor;
    
    public DefaultToolExecutor(String name, String description, String parametersSchema, Function<JSONObject, String> executor) {
        this.name = name;
        this.description = description;
        this.parametersSchema = parametersSchema;
        this.executor = executor;
    }
    
    @Override
    public String execute(JSONObject parameters) {
        return executor.apply(parameters);
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getParametersSchema() {
        return parametersSchema;
    }
}