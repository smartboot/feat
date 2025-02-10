package tech.smartboot.feat.ai.chat.entity;

public class Message {
    /**
     * 用户角色，表示用户的问题或指令。
     */
    public static final String ROLE_USER = "user";
    /**
     * 系统角色，通常用于设定 AI 的行为和性格，比如 "你是一个专家"。
     */
    public static final String ROLE_SYSTEM = "system";
    /**
     * 助手角色，表示 AI 的回答，可以用来模拟多轮对话。
     */
    public static final String ROLE_ASSISTANT = "assistant";

    private String role;
    private String content;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
