package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.client.impl.HttpRequestImpl;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;

import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/13
 */
public final class PostBody extends CommonBody {
    private final HttpPost httpPost;

    PostBody(HttpPost httpPost) {
        super(httpPost.rest.body());
        this.httpPost = httpPost;
    }

    public HttpPost formUrlencoded(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            httpPost.done();
            return httpPost;
        }
        try {
            httpPost.rest.willSendRequest();
            //编码Post表单
            Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
            Map.Entry<String, String> entry = iterator.next();
            StringBuilder sb = new StringBuilder();
            sb.append(URLEncoder.encode(entry.getKey(), "utf8")).append("=").append(URLEncoder.encode(entry.getValue(), "utf8"));
            while (iterator.hasNext()) {
                entry = iterator.next();
                sb.append("&").append(URLEncoder.encode(entry.getKey(), "utf8")).append("=").append(URLEncoder.encode(entry.getValue(), "utf8"));
            }
            byte[] bytes = sb.toString().getBytes();
            // 设置 Header
            HttpRequestImpl request = httpPost.rest.getRequest();
            request.setContentLength(bytes.length);
            request.addHeader(HeaderNameEnum.CONTENT_TYPE.getName(), HeaderValue.ContentType.X_WWW_FORM_URLENCODED);
            //输出数据
            request.write(bytes);
            request.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
            httpPost.rest.getCompletableFuture().completeExceptionally(e);
        }
        return httpPost;
    }

    public HttpPost multipart(List<Multipart> multiparts) {
//                if (params == null || params.isEmpty()) {
//                    HttpPost.this.done();
//                    return HttpPost.this;
//                }
        try {
            httpPost.rest.willSendRequest();

            String boundary = "---" + System.currentTimeMillis();

            httpPost.rest.getRequest().addHeader(HeaderNameEnum.CONTENT_TYPE.getName(), HeaderValue.ContentType.MULTIPART_FORM_DATA + "; boundary=" + boundary);
            for (Multipart multipart : multiparts) {
                write("--" + boundary + "\r\n");
                multipart.write(this);
            }
            write("--" + boundary + "--\r\n");
        } catch (Exception e) {
            e.printStackTrace();
            httpPost.rest.getCompletableFuture().completeExceptionally(e);
        }
        return httpPost;
    }
}
