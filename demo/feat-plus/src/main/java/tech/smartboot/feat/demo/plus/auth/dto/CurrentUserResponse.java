package tech.smartboot.feat.demo.plus.auth.dto;

public class CurrentUserResponse {
    private long id;
    private String username;
    private String displayName;
    private String roleCode;

    public CurrentUserResponse() {
    }

    public CurrentUserResponse(long id, String username, String displayName, String roleCode) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.roleCode = roleCode;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
}
