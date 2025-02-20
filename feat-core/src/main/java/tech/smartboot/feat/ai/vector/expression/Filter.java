package tech.smartboot.feat.ai.vector.expression;

import com.alibaba.fastjson2.JSONObject;

public abstract class Filter {
    public abstract void build(JSONObject object, Convert convert);
}
