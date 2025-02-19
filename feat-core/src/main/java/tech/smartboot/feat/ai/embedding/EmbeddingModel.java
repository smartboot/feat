package tech.smartboot.feat.ai.embedding;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.vector.chroma.ValidationError;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EmbeddingModel {
    private final EmbeddingOptions options = new EmbeddingOptions();

    public List<float[]> embed(List<String> documents) {
        JSONObject object = new JSONObject();
        object.put("model", options.getModel());
        object.put("input", documents);
        object.put("encoding_format", "float");
        object.put("dimensions", 1);
        byte[] bytes = object.toJSONString().getBytes();
        HttpClient httpClient = Feat.httpClient(options.baseUrl(), opts -> {
            opts.debug(options.isDebug());
        });
        HttpPost httpPost = httpClient.post("/v1/embeddings").header(header -> {
            if (StringUtils.isNotBlank(options.getApiKey())) {
                header.add(HeaderNameEnum.AUTHORIZATION.getName(), "Bearer " + options.getApiKey());
            }
            header.add(HeaderNameEnum.CONTENT_TYPE.getName(), "application/json").add(HeaderNameEnum.CONTENT_LENGTH.getName(), bytes.length);
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
                ValidationError err = JSON.parseObject(response.body(), ValidationError.class);
                throw new FeatException(err.getMessage());
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
