/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.vector.milvus.collection;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class DocumentDeleteRequest {
    private List<String> ids;
    private Map<String, String> where;
    @JSONField(name = "where_document")
    private Map<String, String> whereDocument;

    public void where(String key, String value) {
        if (where == null) {
            where = new HashMap<>();
        }
        where.put(key, value);
    }

    public void whereDocument(String key, String value) {
        if (whereDocument == null) {
            whereDocument = new HashMap<>();
        }
        whereDocument.put(key, value);
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
}
