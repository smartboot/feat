package tech.smartboot.feat.core.client;

import java.util.List;
import java.util.Map;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/13
 */
public abstract class PostBody extends CommonBody {
    PostBody(Body body) {
        super(body);
    }

    public abstract HttpPost formUrlencoded(Map<String, String> params);

    public abstract HttpPost multipart(List<Multipart> multiparts);
}
