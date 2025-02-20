package tech.smartboot.feat.ai.vector;

import tech.smartboot.feat.ai.vector.chroma.Chroma;
import tech.smartboot.feat.ai.vector.chroma.Collection;
import tech.smartboot.feat.ai.vector.expression.Filter;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ChromaVectorStore implements VectorStore {
    private final ChromaVectorOptions options = new ChromaVectorOptions();
    private Chroma chroma;
    private Collection collection;

    public ChromaVectorStore(Consumer<ChromaVectorOptions> consumer) {
        consumer.accept(options);
        if (StringUtils.isBlank(options.getCollectionName())) {
            throw new FeatException("Collection name is required");
        }
        chroma = new Chroma(options.getUrl(), opt -> {
            opt.debug(options.isDebug())
                    .embeddingModel(options.embeddingModel())
                    .setApiVersion(ChromaVectorOptions.API_VERSION_2);
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
    public void delete(Filter filter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Document> similaritySearch(String query) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Document> similaritySearch(Filter query) {
        query.build(filter -> {
//            filter.
        });
        return Collections.emptyList();
    }
}
