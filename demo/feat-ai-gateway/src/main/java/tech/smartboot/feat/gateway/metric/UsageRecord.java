package tech.smartboot.feat.gateway.metric;

/**
 * 一条请求用量记录。字段与设计文档 §6 对齐，方便以后直接喂给计费/报表。
 */
public final class UsageRecord {
    public long tsMs;
    public String traceId;
    public String keyId;
    public String model;
    public String provider;
    public int status;
    public boolean stream;
    public long ttftMs = -1;
    public long totalMs;
    public long promptTokens;
    public long completionTokens;
    public boolean estimated;
    public long bytesOut;
    public String error;
}
