 package tech.smartboot.feat.cloud.aot.serializer.orm;
 
 import java.util.Collections;
 import java.util.List;
 
 /**
  * 解析后的 SQL 文本。
  */
 public final class SqlText {
     private final String sql;
     private final List<String> parameters;
 
     public SqlText(String sql, List<String> parameters) {
         this.sql = sql;
         this.parameters = parameters;
     }
 
     public String getSql() {
         return sql;
     }
 
     public List<String> getParameters() {
         return Collections.unmodifiableList(parameters);
     }
 }
