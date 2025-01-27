package tech.smartboot.feat.demo.controller;


import tech.smartboot.feat.cloud.annotation.JSONField;

import java.util.Date;

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