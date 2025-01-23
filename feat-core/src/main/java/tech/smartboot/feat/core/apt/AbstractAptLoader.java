package tech.smartboot.feat.core.apt;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.HttpRequest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public abstract class AbstractAptLoader implements AptLoader {
    protected JSONObject getParams(HttpRequest request) {
        try {
            if (request.getContentType() != null && request.getContentType().startsWith("application/json")) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] bytes = new byte[1024];
                int len = 0;
                InputStream inputStream = request.getInputStream();
                while ((len = inputStream.read(bytes)) != -1) {
                    out.write(bytes, 0, len);
                }
                return JSON.parseObject(out.toByteArray());
            } else {
                JSONObject jsonObject = new JSONObject();
                request.getParameters().keySet().forEach(param -> {
                    jsonObject.put(param, request.getParameter(param));
                });
                return jsonObject;
            }
        } catch (Exception e) {
            throw new FeatException(e);
        }
    }
}
