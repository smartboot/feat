package tech.smartboot.feat.ai.chat;

import com.alibaba.fastjson2.JSONObject;

import java.util.function.Consumer;

public interface ThinkOption {
    interface Qwen {
        Consumer<JSONObject> ENABLE = (jsonObject) -> {
            jsonObject.put("enable_thinking", true);
        };
        Consumer<JSONObject> DISABLE = (jsonObject) -> {
            jsonObject.put("enable_thinking", false);
        };
    }

    interface DeepSeek {

        Consumer<JSONObject> ENABLE = (jsonObject) -> {
            jsonObject.put("thinking", JSONObject.of("type", "enabled"));
        };


        Consumer<JSONObject> DISABLE = (jsonObject) -> {
            jsonObject.put("thinking", JSONObject.of("type", "disabled"));
        };
    }
}
