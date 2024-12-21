package tech.smartboot.feat.client;

public class FormItemMultipart extends Multipart {
    private final String name;
    private final String value;

    FormItemMultipart(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    void write(PostBody post) {
        post.write("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n");
    }
}
