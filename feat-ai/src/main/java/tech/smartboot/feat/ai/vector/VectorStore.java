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
