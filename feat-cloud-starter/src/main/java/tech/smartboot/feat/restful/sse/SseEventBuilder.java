package tech.smartboot.feat.restful.sse;

public interface SseEventBuilder {

    /**
     * Add an SSE "id" line.
     */
    SseEventBuilder id(String id);

    /**
     * Add an SSE "event" line.
     */
    SseEventBuilder name(String eventName);

    /**
     * Add an SSE "retry" line.
     */
    SseEventBuilder reconnectTime(long reconnectTimeMillis);

    /**
     * Add an SSE "comment" line.
     */
    SseEventBuilder comment(String comment);

    /**
     * Add an SSE "data" line.
     */
    SseEventBuilder data(String object);

    String build();

}