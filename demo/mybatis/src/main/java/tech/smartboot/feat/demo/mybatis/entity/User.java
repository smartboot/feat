package tech.smartboot.feat.demo.mybatis.entity;

import java.util.Date;
import java.util.List;

public class User {
    private String username;
    private String password;
    private String desc;
    private String role;
    private Date createTime;
    private Date editTime;

    private List<Post> posts;

    private Address address;

    // Constructors
    public User() {}

   public User(String username, String password, String desc, String role) {
       this.username = username;
       this.password = password;
       this.desc = desc;
       this.role = role;
   }

    public User(String username, List<Post> posts) {
        this.username = username;
        this.posts = posts;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getEditTime() {
        return editTime;
    }

    public void setEditTime(Date editTime) {
        this.editTime = editTime;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", desc='" + desc + '\'' +
                ", role='" + role + '\'' +
                ", createTime=" + createTime +
                ", editTime=" + editTime +
                '}';
    }
}
