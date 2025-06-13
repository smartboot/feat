/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.vector;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.vector.chroma.Chroma;
import tech.smartboot.feat.ai.vector.chroma.Collection;
import tech.smartboot.feat.ai.vector.chroma.collection.Query;
import tech.smartboot.feat.ai.vector.expression.Convert;
import tech.smartboot.feat.ai.vector.expression.Expression;
import tech.smartboot.feat.ai.vector.expression.SimpleExpression;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.FeatUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class ChromaVectorStore implements VectorStore {
    public static final Convert<JSONObject> convert = new Convert<JSONObject>() {
        @Override
        public void build(JSONObject object, SimpleExpression expression) {
            switch (expression.getType()) {
                case EQ:
                    object.put(expression.getKey(), Collections.singletonMap("$eq", expression.getValue()));
                    break;
                case NE:
                    object.put(expression.getKey(), Collections.singletonMap("$ne", expression.getValue()));
                    break;
                case LT:
                    object.put(expression.getKey(), Collections.singletonMap("$lt", expression.getValue()));
                    break;
                case LTE:
                    object.put(expression.getKey(), Collections.singletonMap("$lte", expression.getValue()));
                    break;
                case GT:
                    object.put(expression.getKey(), Collections.singletonMap("$gt", expression.getValue()));
                    break;
                case GTE:
                    object.put(expression.getKey(), Collections.singletonMap("$gte", expression.getValue()));
                    break;
                default:
                    throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        @Override
        public void and(JSONObject object, List<Expression> filters) {
            JSONArray jsonArray = new JSONArray();
            for (Expression expression : filters) {
                JSONObject jsonObject = new JSONObject();
                expression.build(jsonObject, this);
                jsonArray.add(jsonObject);
            }
            object.put("$and", jsonArray);
        }

        @Override
        public void or(JSONObject object, List<Expression> filters) {
            JSONArray jsonArray = new JSONArray();
            for (Expression expression : filters) {
                JSONObject jsonObject = new JSONObject();
                expression.build(jsonObject, this);
                jsonArray.add(jsonObject);
            }
            object.put("$or", jsonArray);
        }
    };
    private final ChromaVectorOptions options = new ChromaVectorOptions();
    private Chroma chroma;
    private Collection collection;

    public ChromaVectorStore(Consumer<ChromaVectorOptions> consumer) {
        consumer.accept(options);
        if (FeatUtils.isBlank(options.getCollectionName())) {
            throw new FeatException("Collection name is required");
        }
        chroma = new Chroma(options.getUrl(), opt -> {
            opt.debug(options.isDebug()).embeddingModel(options.embeddingModel()).setApiVersion(ChromaVectorOptions.API_VERSION_2);
        });
        collection = chroma.getCollection(options.getCollectionName());
    }

    @Override
    public void add(List<Document> documents) {
        collection.add(documents);
    }

    @Override
    public void delete(List<String> idList) {
        collection.delete(idList);
    }

    @Override
    public void delete(Expression filter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Document> similaritySearch(String query) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        Query query = new Query();
        List<String> include = new ArrayList<>();
        if (request.getExpression() != null) {
            JSONObject jsonObject = new JSONObject();
            request.getExpression().build(jsonObject, convert);
            query.where(jsonObject);
            include.add("metadatas");
        }
        query.setQueryText(request.getQuery());
        if (FeatUtils.isNotEmpty(include)) {
            query.setInclude(include);
        }
        collection.query(query);
        return Collections.emptyList();
    }
}
