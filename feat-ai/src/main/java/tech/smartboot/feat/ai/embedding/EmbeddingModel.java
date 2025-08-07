/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.embedding;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class EmbeddingModel {
    private final EmbeddingOptions options = new EmbeddingOptions();

    public List<float[]> embed(List<String> documents) {
        JSONObject object = new JSONObject();
        object.put("model", options.model());
        object.put("input", documents);
        object.put("encoding_format", "float");
        object.put("dimensions", 1);
        byte[] bytes = object.toJSONString().getBytes();
        HttpClient httpClient = Feat.httpClient(options.baseUrl(), opts -> {
            opts.debug(options.isDebug());
        });
        HttpPost httpPost = httpClient.post("/v1/embeddings").header(header -> {
            if (FeatUtils.isNotBlank(options.apiKey())) {
                header.add(HeaderName.AUTHORIZATION, "Bearer " + options.apiKey());
            }
            header.add(HeaderName.CONTENT_TYPE, "application/json").add(HeaderName.CONTENT_LENGTH, bytes.length);
        });

        httpPost.body().write(bytes);
        String rsp = execute(httpPost);
        JSONObject responseObject = JSON.parseObject(rsp);
        JSONArray array = responseObject.getJSONArray("data");
        float[][] embedding = new float[array.size()][];
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            embedding[obj.getIntValue("index")] = obj.getObject("embedding", float[].class);
        }
        return Arrays.asList(embedding);
    }

    public float[] embed(String document) {
        return embed(Collections.singletonList(document)).get(0);
    }

    public EmbeddingOptions options() {
        return options;
    }

    private String execute(HttpRest rest) {
        return execute(rest, String.class);
    }

    private <T> T execute(HttpRest rest, Class<T> clazz) {
        try {
            HttpResponse response = rest.submit().get();
            if (response.statusCode() >= 400) {
                throw new FeatException(response.body());
            }
            if (clazz == String.class) {
                return (T) response.body();
            }
            return JSON.parseObject(response.body(), clazz);
        } catch (FeatException e) {
            throw e;
        } catch (Exception e) {
            throw new FeatException(e);
        }
    }
}
