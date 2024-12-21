package tech.smartboot.feat.client;

public abstract class Multipart {
    public static FormItemMultipart newFormMultipart(String name, String value) {
        return new FormItemMultipart(name, value);
    }

    abstract void write(PostBody post);
}
