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
import java.util.List;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class TestParam0<T, K> {
    private T param1;
    private List<K> param2;
    private int param3;
    private byte param4;
    private short param5;
    private long param6;
    private float param7;
    private double param8;
    private boolean param9;
    private char param10='"';
    private Integer param11;
    private Byte param12;
    private Short param13;
    private Long param14;
    private Float param15;
    private Double param16;
    private Boolean param17;
    private Character param18;

    private List param19;
    private Map param20;
    private Map<String, String> param21;

    private Date param22;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date param23 = new Date();

    public T getParam1() {
        return param1;
    }

    public void setParam1(T param1) {
        this.param1 = param1;
    }

    public List<K> getParam2() {
        return param2;
    }

    public void setParam2(List<K> param2) {
        this.param2 = param2;
    }

    public int getParam3() {
        return param3;
    }

    public void setParam3(int param3) {
        this.param3 = param3;
    }

    public byte getParam4() {
        return param4;
    }

    public void setParam4(byte param4) {
        this.param4 = param4;
    }

    public short getParam5() {
        return param5;
    }

    public void setParam5(short param5) {
        this.param5 = param5;
    }

    public long getParam6() {
        return param6;
    }

    public void setParam6(long param6) {
        this.param6 = param6;
    }

    public float getParam7() {
        return param7;
    }

    public void setParam7(float param7) {
        this.param7 = param7;
    }

    public double getParam8() {
        return param8;
    }

    public void setParam8(double param8) {
        this.param8 = param8;
    }

    public boolean isParam9() {
        return param9;
    }

    public void setParam9(boolean param9) {
        this.param9 = param9;
    }

    public char getParam10() {
        return param10;
    }

    public void setParam10(char param10) {
        this.param10 = param10;
    }

    public Integer getParam11() {
        return param11;
    }

    public void setParam11(Integer param11) {
        this.param11 = param11;
    }

    public Byte getParam12() {
        return param12;
    }

    public void setParam12(Byte param12) {
        this.param12 = param12;
    }

    public Short getParam13() {
        return param13;
    }

    public void setParam13(Short param13) {
        this.param13 = param13;
    }

    public Long getParam14() {
        return param14;
    }

    public void setParam14(Long param14) {
        this.param14 = param14;
    }

    public Float getParam15() {
        return param15;
    }

    public void setParam15(Float param15) {
        this.param15 = param15;
    }

    public Double getParam16() {
        return param16;
    }

    public void setParam16(Double param16) {
        this.param16 = param16;
    }

    public Boolean getParam17() {
        return param17;
    }

    public void setParam17(Boolean param17) {
        this.param17 = param17;
    }

    public Character getParam18() {
        return param18;
    }

    public void setParam18(Character param18) {
        this.param18 = param18;
    }

    public List getParam19() {
        return param19;
    }

    public void setParam19(List param19) {
        this.param19 = param19;
    }

    public Map getParam20() {
        return param20;
    }

    public void setParam20(Map param20) {
        this.param20 = param20;
    }

    public Map<String, String> getParam21() {
        return param21;
    }

    public void setParam21(Map<String, String> param21) {
        this.param21 = param21;
    }

    public Date getParam22() {
        return param22;
    }

    public void setParam22(Date param22) {
        this.param22 = param22;
    }

    public Date getParam23() {
        return param23;
    }

    public void setParam23(Date param23) {
        this.param23 = param23;
    }
}