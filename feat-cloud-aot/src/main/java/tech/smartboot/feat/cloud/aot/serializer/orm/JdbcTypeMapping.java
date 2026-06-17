 package tech.smartboot.feat.cloud.aot.serializer.orm;
 
 import javax.lang.model.type.TypeMirror;
 import java.math.BigDecimal;
 import java.sql.Timestamp;
 import java.time.LocalDateTime;
 import java.util.Arrays;
 import java.util.Date;
 import java.util.HashSet;
 import java.util.Set;
 
 /**
  * JDBC 类型映射。
  */
 public final class JdbcTypeMapping {
 
     private static final Set<String> SCALAR_TYPES = new HashSet<>(Arrays.asList(
             "boolean", "byte", "short", "int", "long", "float", "double",
             Boolean.class.getName(), Byte.class.getName(), Short.class.getName(),
             Integer.class.getName(), Long.class.getName(), Float.class.getName(),
             Double.class.getName(), String.class.getName(), BigDecimal.class.getName(),
             Date.class.getName(), java.sql.Date.class.getName(),
             Timestamp.class.getName(), LocalDateTime.class.getName()
     ));
 
     private JdbcTypeMapping() {
     }
 
     public static boolean isScalar(TypeMirror type) {
         return type.getKind().isPrimitive() || SCALAR_TYPES.contains(type.toString());
     }
 
     public static String resultSetGetter(TypeMirror type) {
         switch (type.toString()) {
             case "boolean":
             case "java.lang.Boolean":
                 return "getBoolean";
             case "byte":
             case "java.lang.Byte":
                 return "getByte";
             case "short":
             case "java.lang.Short":
                 return "getShort";
             case "int":
             case "java.lang.Integer":
                 return "getInt";
             case "long":
             case "java.lang.Long":
                 return "getLong";
             case "float":
             case "java.lang.Float":
                 return "getFloat";
             case "double":
             case "java.lang.Double":
                 return "getDouble";
             case "java.lang.String":
                 return "getString";
             case "java.util.Date":
             case "java.sql.Date":
                 return "getDate";
             case "java.sql.Timestamp":
                 return "getTimestamp";
             case "java.math.BigDecimal":
                 return "getBigDecimal";
             case "java.time.LocalDateTime":
                 return "getObject";
             default:
                 return null;
         }
     }
 
    public static String primitiveDefault(TypeMirror type) {
         switch (type.toString()) {
             case "boolean":
                 return "false";
             case "long":
                 return "0L";
             case "float":
                 return "0F";
             case "double":
                 return "0D";
             default:
                 return "0";
        }
    }

    public static String boxType(TypeMirror type) {
        if (!type.getKind().isPrimitive()) {
            return type.toString();
        }
        switch (type.toString()) {
            case "boolean":
                return Boolean.class.getName();
            case "byte":
                return Byte.class.getName();
            case "short":
                return Short.class.getName();
            case "int":
                return Integer.class.getName();
            case "long":
                return Long.class.getName();
            case "float":
                return Float.class.getName();
            case "double":
                return Double.class.getName();
            case "char":
                return Character.class.getName();
            default:
                return type.toString();
        }
    }
}
