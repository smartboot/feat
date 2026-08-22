package tech.smartboot.feat.benchmark;

public class Response {
    private String message;
    private String value;

    public Response(String message, String value) {
        this.message = message;
        this.value = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
