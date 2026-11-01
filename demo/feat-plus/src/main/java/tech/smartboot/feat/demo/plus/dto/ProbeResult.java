package tech.smartboot.feat.demo.plus.dto;

/**
 * 一条地基自检结果。
 * <p>
 * 字段全部是字符串 / 布尔，不返回异常对象：自检的用途是「接线是否正确」，
 * 出错时的栈和 SQL 属于日志，塞进 HTTP 响应既没人看也会漏出内部信息。
 */
public class ProbeResult {

    private final String name;

    private final boolean ok;

    private final String detail;

    public ProbeResult(String name, boolean ok, String detail) {
        this.name = name;
        this.ok = ok;
        this.detail = detail;
    }

    public String getName() {
        return name;
    }

    public boolean isOk() {
        return ok;
    }

    public String getDetail() {
        return detail;
    }
}
