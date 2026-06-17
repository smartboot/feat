package tech.smartboot.feat.cloud.aot.serializer.orm;

import tech.smartboot.feat.cloud.annotation.orm.SelectKey;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.io.PrintWriter;
import java.util.List;

/**
 * 生成 @SelectKey 主键查询与回写代码。
 */
public final class SelectKeyGenerator {

    private SelectKeyGenerator() {
    }

    public static void emit(PrintWriter printWriter, ExecutableElement method, SelectKey selectKey, int indent) {
        String keyProperty = selectKey.keyProperty();
        if (keyProperty == null || keyProperty.isEmpty()) {
            return;
        }
        List<ParameterMetadata> paramInfos = ParameterResolver.resolve(method);
        GeneratedKeyResolver.GeneratedKey key = GeneratedKeyResolver.resolveKey(method, paramInfos, keyProperty);
        if (key == null || key.getSetter() == null) {
            throw new RuntimeException("无法解析 @SelectKey 的 keyProperty: " + keyProperty);
        }
        TypeMirror resultType = extractResultType(selectKey);
        TypeMirror keyType = resultType != null ? resultType : key.getType();
        if (keyType == null) {
            throw new RuntimeException("无法确定 @SelectKey 主键类型");
        }

        String keyColumn = selectKey.keyColumn();

        SqlText sqlText = FeatSqlParser.parse(selectKey.statement());
        String keySql = JdbcCodeEmitter.escapeSql(sqlText.getSql());
        printWriter.println(tabs(indent) + "try (java.sql.PreparedStatement _keyPs = _connection.prepareStatement(\"" + keySql + "\")) {");
        int idx = 1;
        for (String param : sqlText.getParameters()) {
            printWriter.println(tabs(indent + 1) + "_keyPs.setObject(" + idx + ", " + ParameterResolver.toExpression(param, paramInfos) + ");");
            idx++;
        }
        printWriter.println(tabs(indent + 1) + "try (java.sql.ResultSet _keyRs = _keyPs.executeQuery()) {");
        printWriter.println(tabs(indent + 2) + "if (_keyRs.next()) {");
        printWriter.println(tabs(indent + 3) + key.getSetter() + "(" + JdbcCodeEmitter.generatedKeyGetter("_keyRs", keyType, keyColumn) + ");");
        printWriter.println(tabs(indent + 2) + "}");
        printWriter.println(tabs(indent + 1) + "}");
        printWriter.println(tabs(indent) + "}");
    }

    private static TypeMirror extractResultType(SelectKey selectKey) {
        try {
            selectKey.resultType();
        } catch (MirroredTypeException e) {
            TypeMirror type = e.getTypeMirror();
            if (!"void".equals(type.toString())) {
                return type;
            }
        }
        return null;
    }

    private static String tabs(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append('\t');
        }
        return sb.toString();
    }
}
