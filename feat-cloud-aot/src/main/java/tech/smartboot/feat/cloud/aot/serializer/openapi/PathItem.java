package tech.smartboot.feat.cloud.aot.serializer.openapi;

public class PathItem {
    private Operation get;
    private Operation post;
    private Operation put;
    private Operation delete;
    private Operation patch;
    private Operation options;
    private Operation head;
    private Operation trace;

    public Operation getGet() {
        return get;
    }

    public void setGet(Operation get) {
        this.get = get;
    }

    public Operation getPost() {
        return post;
    }

    public void setPost(Operation post) {
        this.post = post;
    }

    public Operation getPut() {
        return put;
    }

    public void setPut(Operation put) {
        this.put = put;
    }

    public Operation getDelete() {
        return delete;
    }

    public void setDelete(Operation delete) {
        this.delete = delete;
    }

    public Operation getPatch() {
        return patch;
    }

    public void setPatch(Operation patch) {
        this.patch = patch;
    }

    public Operation getOptions() {
        return options;
    }

    public void setOptions(Operation options) {
        this.options = options;
    }

    public Operation getHead() {
        return head;
    }

    public void setHead(Operation head) {
        this.head = head;
    }

    public Operation getTrace() {
        return trace;
    }

    public void setTrace(Operation trace) {
        this.trace = trace;
    }
}