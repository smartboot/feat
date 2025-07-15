/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.apt;

import java.util.List;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Dto2 {
    private String name;
    private int age;
    private String address;
    private List<Dto3> list;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Dto3> getList() {
        return list;
    }

    public void setList(List<Dto3> list) {
        this.list = list;
    }
}
