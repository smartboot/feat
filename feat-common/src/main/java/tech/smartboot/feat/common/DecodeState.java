package tech.smartboot.feat.common;

public class DecodeState {
    public static final int STATE_PROTOCOL_DECODE = 0;
    public static final int STATE_STATUS_CODE = 1;
    public static final int STATE_STATUS_DESC = 2;
    public static final int STATE_START_LINE_END = 3;
    public static final int STATE_HEADER_END_CHECK = 4;
    public static final int STATE_HEADER_NAME = 5;
    public static final int STATE_HEADER_VALUE = 6;
    public static final int STATE_HEADER_LINE_END = 7;
    public static final int STATE_HEADER_IGNORE = 8;

    public static final int STATE_HEADER_CALLBACK = 9;
    public static final int STATE_BODY = 10;
    public static final int STATE_BODY_READING_MONITOR = 11;
    public static final int STATE_BODY_READING_CALLBACK = 12;
    public static final int STATE_FINISH = 13;

    public static final int STATE_METHOD = 14;
    public static final int STATE_URI = 15;
    public static final int STATE_URI_QUERY = 16;

    /**
     * HTTP响应报文解析状态
     */
    private int state;


    public DecodeState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

}
