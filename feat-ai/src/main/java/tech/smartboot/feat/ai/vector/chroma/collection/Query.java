/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.vector.chroma.collection;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class Query {
    private JSONObject where;
    @JSONField(name = "where_document")
    private Map<String, String> whereDocument;
    @JSONField(name = "query_embeddings")
    private List<float[]> queryEmbeddings;

    @JSONField(serialize = false)
    private List<String> queryTexts;

    @JSONField(name = "n_results")
    private int results = 10;
    private List<String> include;

    public static Query of() {
        return new Query();
    }

    public void where(String key, String value) {
        if (where == null) {
            where = new JSONObject();
        }
        where.put(key, value);
    }

    public void where(JSONObject where) {
        this.where = where;
    }

    public void whereDocument(String key, String value) {
        if (whereDocument == null) {
            whereDocument = new HashMap<>();
        }
        whereDocument.put(key, value);
    }


    public void setQueryEmbeddings(List<float[]> queryEmbeddings) {
        this.queryEmbeddings = queryEmbeddings;
    }

    public List<float[]> getQueryEmbeddings() {
        return queryEmbeddings;
    }

    public int getResults() {
        return results;
    }

    public void setResults(int results) {
        this.results = results;
    }

    public List<String> getInclude() {
        return include;
    }

    public void setInclude(List<String> include) {
        this.include = include;
    }

    public JSONObject getWhere() {
        return where;
    }


    public Map<String, String> getWhereDocument() {
        return whereDocument;
    }

    public List<String> getQueryTexts() {
        return queryTexts;
    }

    public void setQueryTexts(List<String> queryTexts) {
        this.queryTexts = queryTexts;
    }

    public void setQueryText(String queryText) {
        this.queryTexts = Collections.singletonList(queryText);
    }
}
