package tech.smartboot.feat.ai.chat.entity;

class TextMessage extends Message {
    /**
     * 消息内容
     */
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
