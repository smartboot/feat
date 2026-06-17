package tech.smartboot.feat.cloud.aot.serializer.orm;

import tech.smartboot.feat.cloud.annotation.orm.Delete;
import tech.smartboot.feat.cloud.annotation.orm.Insert;
import tech.smartboot.feat.cloud.annotation.orm.Options;
import tech.smartboot.feat.cloud.annotation.orm.Results;
import tech.smartboot.feat.cloud.annotation.orm.Select;
import tech.smartboot.feat.cloud.annotation.orm.Update;
import tech.smartboot.feat.cloud.aot.serializer.orm.ResultMapRegistry;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

/**
 * 统一封装 Mapper 方法上的 SQL 注解解析结果。
 *
 * <p>把注解差异收敛到该模型后，{@link FeatMapperMethodGenerator}
 * 只需按 {@link Kind} 和是否动态 SQL 分发即可，避免单文件膨胀。</p>
 */
public final class MapperMethodSql {
    public enum Kind {
        SELECT, INSERT, UPDATE, DELETE
    }

    private final Kind kind;
    private final String sql;
    private final Results results;
    private final TypeMirror explicitResultType;
    private final boolean useGeneratedKeys;
    private final String keyProperty;
    private final String keyColumn;

    private MapperMethodSql(Kind kind, String sql, Results results, TypeMirror explicitResultType,
                            boolean useGeneratedKeys, String keyProperty, String keyColumn) {
        this.kind = kind;
        this.sql = sql;
        this.results = results;
        this.explicitResultType = explicitResultType;
        this.useGeneratedKeys = useGeneratedKeys;
        this.keyProperty = keyProperty;
        this.keyColumn = keyColumn;
    }

    public static MapperMethodSql resolve(ExecutableElement method, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        Select select = method.getAnnotation(Select.class);
        if (select != null) {
            Results results = ResultMapRegistry.resolve(method, select, ResultMapRegistry.build(mapperElement));
            return new MapperMethodSql(Kind.SELECT, select.value(), results,
                    extractResultType(select), false, null, null);
        }
        Insert insert = method.getAnnotation(Insert.class);
        if (insert != null) {
            return buildInsert(method, insert.value());
        }
        Update update = method.getAnnotation(Update.class);
        if (update != null) {
            return new MapperMethodSql(Kind.UPDATE, update.value(), null, null, false, null, null);
        }
        Delete delete = method.getAnnotation(Delete.class);
        if (delete != null) {
            return new MapperMethodSql(Kind.DELETE, delete.value(), null, null, false, null, null);
        }
        return null;
    }

    private static MapperMethodSql buildInsert(ExecutableElement method, String sql) {
        Options options = method.getAnnotation(Options.class);
        boolean generatedKeys = false;
        String keyProperty = "";
        String keyColumn = "";
        Insert insert = method.getAnnotation(Insert.class);
        if (insert != null) {
            generatedKeys = insert.useGeneratedKeys();
            keyProperty = insert.keyProperty();
            keyColumn = insert.keyColumn();
        }
        if (options != null) {
            if (options.useGeneratedKeys()) {
                generatedKeys = true;
            }
            if (!options.keyProperty().isEmpty()) {
                keyProperty = options.keyProperty();
            }
            if (!options.keyColumn().isEmpty()) {
                keyColumn = options.keyColumn();
            }
        }
        return new MapperMethodSql(Kind.INSERT, sql, null, null, generatedKeys, keyProperty, keyColumn);
    }

    private static TypeMirror extractResultType(Select select) {
        try {
            select.resultType();
        } catch (MirroredTypeException e) {
            TypeMirror type = e.getTypeMirror();
            if (!"void".equals(type.toString())) {
                return type;
            }
        }
        return null;
    }

    public Kind getKind() {
        return kind;
    }

    public String getSql() {
        return sql;
    }

    public Results getResults() {
        return results;
    }

    public TypeMirror getExplicitResultType() {
        return explicitResultType;
    }

    public boolean isUseGeneratedKeys() {
        return useGeneratedKeys;
    }

    public String getKeyProperty() {
        return keyProperty;
    }

    public String getKeyColumn() {
        return keyColumn;
    }
}
