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

import tech.smartboot.feat.ai.vector.expression.Convert;
import tech.smartboot.feat.ai.vector.expression.Expression;
import tech.smartboot.feat.ai.vector.expression.SimpleExpression;
import tech.smartboot.feat.ai.vector.milvus.Collection;
import tech.smartboot.feat.ai.vector.milvus.Milvus;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class MilvusVectorStore implements VectorStore {
    private final MilvusVectorOptions options = new MilvusVectorOptions();
    public static final Convert<StringBuilder> convert = new Convert<StringBuilder>() {
        private String convertString(String input) {
            return input.replace("\"", "\\\"");
        }

        @Override
        public void build(StringBuilder sb, SimpleExpression expression) {
            switch (expression.getType()) {
                case EQ:
                    sb.append(expression.getKey()).append(" == ");
                    if (expression.getValue().getClass() == String.class) {
                        sb.append("\"").append(convertString(expression.getValue().toString())).append("\"");
                    } else {
                        sb.append(expression.getValue());
                    }

                    break;
                case IN:
                    sb.append(expression.getKey()).append(" in (");
                    if (expression.getValue().getClass() == String[].class) {
                        for (String str : (String[]) expression.getValue()) {
                            sb.append("\"").append(convertString(str)).append("\"");
                        }
                    } else if (expression.getValue().getClass() == long[].class) {
                        for (long v : (long[]) expression.getValue()) {
                            sb.append("\"").append(v).append("\"");
                        }
                    } else {
                        throw new IllegalArgumentException("Unsupported type: " + expression.getValue().getClass());
                    }
                    sb.setCharAt(sb.length() - 1, ')');
                    break;
                default:
                    throw new UnsupportedOperationException(expression.getType() + " Not supported yet.");
            }
        }

        @Override
        public void and(StringBuilder sb, List<Expression> filters) {
            int i = 0;
            sb.append(" ( ");
            for (Expression expression : filters) {
                if (i++ > 0) {
                    sb.append(" and ");
                }
                expression.build(sb, this);
            }
            sb.append(" )");
        }

        @Override
        public void or(StringBuilder sb, List<Expression> filters) {
            int i = 0;
            sb.append(" ( ");
            for (Expression expression : filters) {
                if (i++ > 0) {
                    sb.append(" or ");
                }
                expression.build(sb, this);
            }
            sb.append(" )");
        }
    };
    private Milvus milvus;
    private Collection collection;

    public MilvusVectorStore(Consumer<MilvusVectorOptions> consumer) {
        consumer.accept(options);
        if (StringUtils.isBlank(options.getCollectionName())) {
            throw new FeatException("Collection name is required");
        }
        milvus = new Milvus(options.getUrl(), opt -> {
            opt.debug(options.isDebug()).embeddingModel(options.embeddingModel());
        });
        collection = milvus.getCollection(options.getCollectionName());
    }

    @Override
    public void add(List<Document> documents) {
        collection.add(documents);
    }

    @Override
    public void delete(List<String> idList) {
        collection.delete(idList.toArray(new String[idList.size()]));
    }

    @Override
    public void delete(Expression expression) {
        collection.delete(expression);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        return Collections.emptyList();
    }
}
