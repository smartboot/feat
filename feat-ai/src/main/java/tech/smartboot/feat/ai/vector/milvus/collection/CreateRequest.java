package tech.smartboot.feat.ai.vector.milvus.collection;

public class CreateRequest {
    private String dbName;
    private String collectionName;
    private int dimension;
    private String metricType;
    private String idType;
    private String autoId;
    private String primaryFieldName;
    private String vectorFieldName;

    public CreateRequest(String collectionName) {
        this(collectionName, 5);
    }

    public CreateRequest(String collectionName, int dimension) {
        this.collectionName = collectionName;
        this.dimension = dimension;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getAutoId() {
        return autoId;
    }

    public void setAutoId(String autoId) {
        this.autoId = autoId;
    }

    public String getPrimaryFieldName() {
        return primaryFieldName;
    }

    public void setPrimaryFieldName(String primaryFieldName) {
        this.primaryFieldName = primaryFieldName;
    }

    public String getVectorFieldName() {
        return vectorFieldName;
    }

    public void setVectorFieldName(String vectorFieldName) {
        this.vectorFieldName = vectorFieldName;
    }
}
