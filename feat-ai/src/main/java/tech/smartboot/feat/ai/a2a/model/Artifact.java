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
 * A2A 产出物类
 *
 * <p>表示任务执行过程中或完成后产生的产出物，可以是文件、数据等。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class Artifact {
    /**
     * 产出物名称
     */
    private String name;

    /**
     * 产出物描述
     */
    private String description;

    /**
     * 产出物部分列表
     */
    private List<Part> parts;

    /**
     * 产出物索引（用于排序）
     */
    private int index;

    /**
     * 产出物是否已完成
     */
    private boolean complete;

    /**
     * 产出物元数据
     */
    private JSONObject metadata;

    public Artifact() {
        this.parts = new ArrayList<>();
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

    public List<Part> getParts() {
        return parts;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public JSONObject getMetadata() {
        return metadata;
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    /**
     * 添加产出物部分
     *
     * @param part 内容部分
     * @return 当前Artifact实例（链式调用）
     */
    public Artifact addPart(Part part) {
        if (this.parts == null) {
            this.parts = new ArrayList<>();
        }
        this.parts.add(part);
        return this;
    }
}
