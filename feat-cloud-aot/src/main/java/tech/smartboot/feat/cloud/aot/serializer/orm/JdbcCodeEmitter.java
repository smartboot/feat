
package tech.smartboot.feat.cloud.aot.serializer.orm;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.io.PrintWriter;
 import java.util.List;
import tech.smartboot.feat.cloud.annotation.orm.ResultSetType;
import tech.smartboot.feat.cloud.annotation.orm.StatementType;

/**
 * 负责生成 JDBC 相关的公共代码片段。
  */
 public final class JdbcCodeEmitter {
     private JdbcCodeEmitter() {
     }

     /**
      * 输出方法签名。
      */
     public static void emitMethodSignature(PrintWriter printWriter, ExecutableElement method) {
         String returnType = method.getReturnType().toString();
         printWriter.print("\t\t\tpublic " + returnType + " " + method.getSimpleName() + "(");
         boolean first = true;
         for (javax.lang.model.element.VariableElement param : method.getParameters()) {
             if (first) {
                 first = false;
             } else {
                 printWriter.print(",");
             }
             printWriter.print(param.asType().toString() + " " + param.getSimpleName());
         }
         printWriter.println(") {");
     }

     /**
      * 输出参数设置代码。
      */
     public static void emitParameterBindings(PrintWriter printWriter, List<String> parameters, List<ParameterMetadata> paramInfos) {
         int index = 1;
         for (String parameter : parameters) {
             String expression = ParameterResolver.toExpression(parameter, paramInfos);
             printWriter.println("\t\t\t\t\t_ps.setObject(" + index++ + ", " + expression + ");");
         }
     }

     /**
      * 输出 catch 块。
      */
     public static void emitCatch(PrintWriter printWriter) {
         printWriter.println("\t\t\t\t} catch (Exception e) {");
         printWriter.println("\t\t\t\t\tthrow new RuntimeException(e);");
         printWriter.println("\t\t\t\t}");
     }

     /**
      * 转义 SQL 字符串以嵌入 Java 字符串字面量。
      */
    public static String escapeSql(String sql) {
         return sql.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\r\n", "\\n")
                 .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    public static String typeHandlerResultExpr(TypeMirror javaType, String handlerType, String indexExpr) {
        String box = JdbcTypeMapping.boxType(javaType);
        String expr = "new " + handlerType + "().getResult(_rs, " + indexExpr + ")";
        if (javaType.getKind().isPrimitive()) {
            String unbox;
            switch (javaType.toString()) {
                case "boolean":
                    unbox = "booleanValue";
                    break;
                case "byte":
                    unbox = "byteValue";
                    break;
                case "short":
                    unbox = "shortValue";
                    break;
                case "int":
                    unbox = "intValue";
                    break;
                case "long":
                    unbox = "longValue";
                    break;
                case "float":
                    unbox = "floatValue";
                    break;
                case "double":
                    unbox = "doubleValue";
                    break;
                case "char":
                    unbox = "charValue";
                    break;
                default:
                    unbox = "intValue";
            }
            return "((" + box + ") " + expr + ")." + unbox + "()";
        }
        return "((" + box + ") " + expr + ")";
    }

    public static String generatedKeyGetter(String rsVar, TypeMirror keyType, String keyColumn) {
        String getter = JdbcTypeMapping.resultSetGetter(keyType);
        String indexExpr = (keyColumn == null || keyColumn.isEmpty()) ? "1" : "\"" + escapeSql(keyColumn) + "\"";
        if ("getObject".equals(getter)) {
            return rsVar + ".getObject(" + indexExpr + ", " + keyType.toString() + ".class)";
        }
        if (getter != null) {
            return rsVar + "." + getter + "(" + indexExpr + ")";
        }
        return rsVar + ".getObject(" + indexExpr + ", " + keyType.toString() + ".class)";
    }

    /**
     * 根据 statementType、生成键、keyColumn 和 resultSetType 生成语句创建参数表达式。
     */
    public static String statementPrepareArgs(StatementType statementType, boolean generatedKeys, String keyColumn, ResultSetType resultSetType) {
        StringBuilder args = new StringBuilder();
        boolean hasType = resultSetType != null && resultSetType != ResultSetType.FORWARD_ONLY;
        if (statementType == StatementType.STATEMENT) {
            if (hasType) {
                switch (resultSetType) {
                    case SCROLL_INSENSITIVE:
                        args.append("ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY");
                        break;
                    case SCROLL_SENSITIVE:
                        args.append("ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY");
                        break;
                    default:
                        args.append("ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY");
                        break;
                }
            }
            return args.toString();
        }
        if (hasType) {
            switch (resultSetType) {
                case SCROLL_INSENSITIVE:
                    args.append(", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY");
                    break;
                case SCROLL_SENSITIVE:
                    args.append(", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY");
                    break;
                default:
                    args.append(", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY");
                    break;
            }
        }
        if (generatedKeys && statementType == StatementType.PREPARED) {
            if (keyColumn != null && !keyColumn.isEmpty()) {
                args.append(", new String[]{\"").append(escapeSql(keyColumn)).append("\"}");
            } else {
                args.append(", Statement.RETURN_GENERATED_KEYS");
            }
        }
        return args.toString();
    }

    /**
     * 根据语句类型返回 JDBC Statement 类名。
     */
    public static String statementClass(StatementType statementType) {
        if (statementType == StatementType.STATEMENT) {
            return "java.sql.Statement";
        }
        if (statementType == StatementType.CALLABLE) {
            return "java.sql.CallableStatement";
        }
        return "java.sql.PreparedStatement";
    }

    /**
     * 根据语句类型返回 Connection 上的创建方法名。
     */
    public static String prepareMethod(StatementType statementType) {
        if (statementType == StatementType.STATEMENT) {
            return "createStatement";
        }
        if (statementType == StatementType.CALLABLE) {
            return "prepareCall";
        }
        return "prepareStatement";
    }

    /**
     * 根据语句类型返回 _ps.executeQuery(...) 表达式（STATEMENT 需要传入 SQL）。
     */
    public static String executeQueryExpr(StatementType statementType, String sqlExpr) {
        if (statementType == StatementType.STATEMENT) {
            return "_ps.executeQuery(" + sqlExpr + ")";
        }
        return "_ps.executeQuery()";
    }

    /**
     * 根据语句类型返回 _ps.executeUpdate(...) 表达式（STATEMENT 需要传入 SQL）。
     */
    public static String executeUpdateExpr(StatementType statementType, String sqlExpr, boolean generatedKeys, String keyColumn) {
        if (statementType == StatementType.STATEMENT) {
            if (generatedKeys) {
                if (keyColumn != null && !keyColumn.isEmpty()) {
                    return "_ps.executeUpdate(" + sqlExpr + ", new String[]{\"" + escapeSql(keyColumn) + "\"})";
                }
                return "_ps.executeUpdate(" + sqlExpr + ", Statement.RETURN_GENERATED_KEYS)";
            }
            return "_ps.executeUpdate(" + sqlExpr + ")";
        }
        return "_ps.executeUpdate()";
    }

    /**
     * 动态 SQL 下根据语句类型返回 executeQuery 表达式。
     */
    public static String dynamicExecuteQueryExpr(StatementType statementType) {
        if (statementType == StatementType.STATEMENT) {
            return "_ps.executeQuery(_sql.toString())";
        }
        return "_ps.executeQuery()";
    }

    /**
     * 动态 SQL 下根据语句类型返回 executeUpdate 表达式。
     */
    public static String dynamicExecuteUpdateExpr(StatementType statementType, boolean generatedKeys, String keyColumn) {
        if (statementType == StatementType.STATEMENT) {
            if (generatedKeys) {
                if (keyColumn != null && !keyColumn.isEmpty()) {
                    return "_ps.executeUpdate(_sql.toString(), new String[]{\"" + escapeSql(keyColumn) + "\"})";
                }
                return "_ps.executeUpdate(_sql.toString(), Statement.RETURN_GENERATED_KEYS)";
            }
            return "_ps.executeUpdate(_sql.toString())";
        }
        return "_ps.executeUpdate()";
    }

    /**
     * 生成 _ps.setFetchSize(...) 调用（仅当 fetchSize > 0 时输出）。
     */
    public static void emitFetchSize(PrintWriter printWriter, int fetchSize, int tabCount) {
        if (fetchSize > 0) {
            StringBuilder tabs = new StringBuilder();
            for (int i = 0; i < tabCount; i++) {
                tabs.append('\t');
            }
            printWriter.println(tabs + "_ps.setFetchSize(" + fetchSize + ");");
        }
    }
    public static void validateConfiguration(String sql, boolean generatedKeys, String keyColumn,
                                              ResultSetType resultSetType, StatementType statementType,
                                              MapperMethodSql.Kind kind) {
        if (statementType == StatementType.STATEMENT && hasNamedPlaceholder(sql)) {
            throw new RuntimeException("StatementType.STATEMENT 不支持 #{...} 占位符，请改用 ${...} 或调整为 PREPARED");
        }
        if (generatedKeys) {
            if (statementType == StatementType.CALLABLE) {
                throw new RuntimeException("CALLABLE 语句不支持 useGeneratedKeys");
            }
            if (resultSetType != ResultSetType.FORWARD_ONLY && statementType != StatementType.STATEMENT) {
                throw new RuntimeException("useGeneratedKeys 与 resultSetType 不能同时用于 PREPARED/CALLABLE 语句");
            }
        }
    }

    private static boolean hasNamedPlaceholder(String sql) {
        return !StaticSqlParser.extractParameters(sql).isEmpty();
    }

}
