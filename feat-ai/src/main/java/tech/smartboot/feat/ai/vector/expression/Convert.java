package tech.smartboot.feat.ai.vector.expression;

import com.alibaba.fastjson2.JSONObject;

import java.util.List;

public interface Convert<T> {
    public void build(T object, SimpleExpression expression);

    default void and(T object, List<Expression> filters) {
    }

    default void or(T object, List<Expression> filters) {
    }
}
