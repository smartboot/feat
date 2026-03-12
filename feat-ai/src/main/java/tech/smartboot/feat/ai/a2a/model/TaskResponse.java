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

import java.util.ArrayList;
import java.util.List;

/**
 * A2A 任务响应类
 *
 * <p>表示任务执行后的响应，包含最终任务状态、产出物等信息。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class TaskResponse {
    /**
     * 任务ID
     */
    private String id;

    /**
     * 任务状态
     */
    private TaskStatus status;

    /**
     * 产出物列表
     */
    private List<Artifact> artifacts;

    /**
     * 响应元数据
     */
    private JSONObject metadata;

    public TaskResponse() {
        this.artifacts = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    public JSONObject getMetadata() {
        return metadata;
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    /**
     * 添加产出物
     *
     * @param artifact 产出物
     * @return 当前TaskResponse实例（链式调用）
     */
    public TaskResponse addArtifact(Artifact artifact) {
        if (this.artifacts == null) {
            this.artifacts = new ArrayList<>();
        }
        this.artifacts.add(artifact);
        return this;
    }
}
