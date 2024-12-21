package tech.smartboot.feat.core.common.codec.h2.constants;

/**
 * HTTP/2 Error Codes
 *
 * @author 三刀
 * @version V1.0 , 2020/5/25
 * @see <a href="https://tools.ietf.org/html/rfc7540#section-7">RFC7540</a>
 */
public class ErrorCodes {
    public static final int NO_ERROR = 0x0;
    /**
     * The endpoint detected an unspecific protocol error.
     * This error is for use when a more specific error code is not available.
     */
    public static final int PROTOCOL_ERROR = 0x1;
    public static final int INTERNAL_ERROR = 0x2;
    public static final int FLOW_CONTROL_ERROR = 0x3;
    public static final int SETTINGS_TIMEOUT = 0x4;
    public static final int STREAM_CLOSED = 0x5;
    public static final int FRAME_SIZE_ERROR = 0x6;
    public static final int REFUSED_STREAM = 0x7;
    public static final int CANCEL = 0x8;
    public static final int COMPRESSION_ERROR = 0x9;
    public static final int CONNECT_ERROR = 0xa;
    public static final int ENHANCE_YOUR_CALM = 0xb;
    public static final int INADEQUATE_SECURITY = 0xc;
    public static final int HTTP_1_1_REQUIRED = 0xd;
}
