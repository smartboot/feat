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

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.Map;

public class Request {
    private String[] ids;
    private Map<String, String> where;
    @JSONField(name = "where_document")
    private Map<String, String> whereDocument;
    private String sort;
    private int offset;
    private int limit;
    private String[] include;

    public static Request of() {
        return new Request();
    }

    // 修改setter方法返回值为自身
    public Request setIds(String[] ids) {
        this.ids = ids;
        return this;
    }

    public Request setWhere(Map<String, String> where) {
        this.where = where;
        return this;
    }

    public Request setWhereDocument(Map<String, String> whereDocument) {
        this.whereDocument = whereDocument;
        return this;
    }

    public Request setSort(String sort) {
        this.sort = sort;
        return this;
    }

    public Request setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public Request setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public Request setInclude(String[] include) {
        this.include = include;
        return this;
    }

    // getter方法保持不变
    public String[] getIds() {
        return ids;
    }

    public Map<String, String> getWhere() {
        return where;
    }

    public Map<String, String> getWhereDocument() {
        return whereDocument;
    }

    public String getSort() {
        return sort;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public String[] getInclude() {
        return include;
    }
}
