package tech.smartboot.feat.ai.vector.expression;

import com.alibaba.fastjson2.JSONObject;

import java.util.List;

public interface Convert {
    public void build(JSONObject object, SimpleExpression expression);

    default void and(JSONObject object, List<Expression> filters) {
    }

    default void or(JSONObject object, List<Expression> filters) {
    }
}
