 package tech.smartboot.feat.cloud.aot.serializer.orm;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 /**
  * 解析 MyBatis 风格的 #{...} 占位符。
  */
 public final class FeatSqlParser {
 
     private static final Pattern PARAM_PATTERN =
             Pattern.compile("#\\{\\s*([a-zA-Z0-9_\\.]+)\\b[^}]*\\}");
 
     private FeatSqlParser() {
     }
 
     public static SqlText parse(String sql) {
         Matcher matcher = PARAM_PATTERN.matcher(sql);
         StringBuilder builder = new StringBuilder();
         List<String> parameters = new ArrayList<>();
         int last = 0;
         while (matcher.find()) {
             builder.append(sql, last, matcher.start());
             builder.append("?");
             parameters.add(matcher.group(1));
             last = matcher.end();
         }
         builder.append(sql.substring(last));
         return new SqlText(builder.toString(), parameters);
     }
 }
