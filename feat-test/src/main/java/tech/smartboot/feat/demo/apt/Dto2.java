package tech.smartboot.feat.demo.apt;

import java.util.List;

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
