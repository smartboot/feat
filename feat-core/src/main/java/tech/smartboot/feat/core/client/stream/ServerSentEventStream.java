package tech.smartboot.feat.core.client.stream;

import tech.smartboot.feat.core.client.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class ServerSentEventStream implements Stream {
    private static final int STATE_COLON = 1;
    private static final int STATE_COLON_RIGHT_TRIM = 2;
    private static final int STATE_LF = 3;
    private static final int STATE_END_CHECK = 4;
    public static final String DATA = "data";
    public static final String TYPE = "type";
    public static final String ID = "id";
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final Map<String, String> event = new HashMap<>();

    @Override
    public void stream(HttpResponse response, byte[] bytes, boolean end) throws IOException {
        if (baos.size() > 0) {
            baos.write(bytes);
            bytes = baos.toByteArray();
            baos.reset();
        }
        int keyPos = 0;
        int colonPos = -1;
        int valuePos = -1;

        int state = STATE_COLON;

        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            switch (state) {
                case STATE_COLON:
                    if (b == ':') {
                        colonPos = i;
                        state = STATE_COLON_RIGHT_TRIM;
                    }
                    break;
                case STATE_COLON_RIGHT_TRIM:
                    if (b != ' ') {
                        valuePos = i;
                        state = STATE_LF;
                    }
                    break;
                case STATE_LF:
                    if (b == '\n') {
                        event.put(new String(bytes, keyPos, colonPos - keyPos), new String(bytes, valuePos, i - valuePos));
                        state = STATE_END_CHECK;
                    }
                    break;
                case STATE_END_CHECK:
                    if (b == '\n') {
                        keyPos = i + 1;
                        //结束
//                        System.out.println(event);
                        onEvent(response, event);
                        event.clear();
                    } else {
                        state = STATE_COLON;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + state);
            }
        }
        if (keyPos + 1 < bytes.length) {
            baos.write(bytes, keyPos, bytes.length - keyPos);
        }
    }

    public abstract void onEvent(HttpResponse httpResponse, Map<String, String> event);
}
