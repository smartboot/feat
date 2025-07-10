/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.reranker;

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
import java.util.List;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class RerankerModel {
    private final RerankerOptions options = new RerankerOptions();

    public List<float[]> rerank(String query, List<String> documents) {
        return rerank(query, documents, 3);
    }

    public List<float[]> rerank(String query, List<String> documents, int topN) {
        JSONObject object = new JSONObject();
        object.put("model", options.getModel());
        object.put("query", query);
        object.put("top_n", topN);
        object.put("documents", documents);
        byte[] bytes = object.toJSONString().getBytes();
        HttpClient httpClient = Feat.httpClient(options.baseUrl(), opts -> {
            opts.debug(options.isDebug());
        });
        HttpPost httpPost = httpClient.post("/v1/embeddings").header(header -> {
            if (FeatUtils.isNotBlank(options.getApiKey())) {
                header.add(HeaderName.AUTHORIZATION, "Bearer " + options.getApiKey());
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

    public RerankerOptions options() {
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
