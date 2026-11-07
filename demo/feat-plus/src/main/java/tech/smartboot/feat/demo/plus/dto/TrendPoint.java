package tech.smartboot.feat.demo.plus.dto;

/**
 * 趋势图上的一个点。
 */
public class TrendPoint {
    private final String label;
    private final int value;

    public TrendPoint(String label, int value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public int getValue() {
        return value;
    }
}
