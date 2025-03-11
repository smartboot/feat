/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.controller;



import com.alibaba.fastjson2.annotation.JSONField;

import java.util.Date;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class TestParam {
    @JSONField(serialize = false)
    private String param1;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date date;
    private String param2;

    public String getParam1() {
        return param1;
    }

    public void setParam1(String param1) {
        this.param1 = param1;
    }

    public String getParam2() {
        return param2;
    }

    public void setParam2(String param2) {
        this.param2 = param2;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}