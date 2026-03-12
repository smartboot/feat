/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.model;

import com.alibaba.fastjson2.JSONObject;

import java.util.List;

/**
 * A2A 技能描述类
 *
 * <p>描述智能体具备的具体技能。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class Skill {
    /**
     * 技能ID
     */
    private String id;

    /**
     * 技能名称
     */
    private String name;

    /**
     * 技能描述
     */
    private String description;

    /**
     * 支持的输入类型列表
     */
    private List<String> inputModes;

    /**
     * 支持的输出类型列表
     */
    private List<String> outputModes;

    /**
     * 技能配置参数
     */
    private JSONObject parameters;

    /**
     * 技能标签列表
     */
    private List<String> tags;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getInputModes() {
        return inputModes;
    }

    public void setInputModes(List<String> inputModes) {
        this.inputModes = inputModes;
    }

    public List<String> getOutputModes() {
        return outputModes;
    }

    public void setOutputModes(List<String> outputModes) {
        this.outputModes = outputModes;
    }

    public JSONObject getParameters() {
        return parameters;
    }

    public void setParameters(JSONObject parameters) {
        this.parameters = parameters;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
