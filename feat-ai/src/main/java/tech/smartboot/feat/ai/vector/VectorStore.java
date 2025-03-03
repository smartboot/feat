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

import tech.smartboot.feat.ai.vector.expression.Expression;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public interface VectorStore {

    static ChromaVectorStore chroma(Consumer<ChromaVectorOptions> consumer) {
        return new ChromaVectorStore(consumer);
    }

    static MilvusVectorStore milvus(Consumer<MilvusVectorOptions> consumer) {
        return new MilvusVectorStore(consumer);
    }

    /**
     * 增加文档
     *
     * @param document 文档
     * @return void
     */
    default void add(Document document) {
        add(Collections.singletonList(document));
    }

    /**
     * 增加文档
     *
     * @param documents 文档
     * @return void
     */
    void add(List<Document> documents);

    default void delete(String id) {
        delete(Collections.singletonList(id));
    }

    void delete(List<String> idList);

    void delete(Expression filter);


    default List<Document> similaritySearch(String query) {
        throw new FeatException("not support");
    }

    List<Document> similaritySearch(SearchRequest request);

}
