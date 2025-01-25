package tech.smartboot.feat.restful.sse;

class SseEventBuilderImpl implements SseEventBuilder {

    private final StringBuilder sb = new StringBuilder();

    @Override
    public SseEventBuilder id(String id) {
        sb.append("id:").append(id).append('\n');
        return this;
    }

    @Override
    public SseEventBuilder name(String name) {
        sb.append("event:").append(name).append('\n');
        return this;
    }

    @Override
    public SseEventBuilder reconnectTime(long reconnectTimeMillis) {
        sb.append("retry:").append(String.valueOf(reconnectTimeMillis)).append('\n');
        return this;
    }

    @Override
    public SseEventBuilder comment(String comment) {
        sb.append(':').append(comment).append('\n');
        return this;
    }

    @Override
    public SseEventBuilder data(String data) {
        sb.append("data:").append(data).append('\n');
        return this;
    }

    @Override
    public String build() {
        return sb.append('\n').toString();
    }

}