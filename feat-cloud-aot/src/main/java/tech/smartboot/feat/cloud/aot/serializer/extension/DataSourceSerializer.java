 /*
  *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
  *
  *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
  *
  *   Enterprise users are required to use this project reasonably
  *  and legally in accordance with the Apache-2.0 open source agreement
  *  without special permission from the smartboot organization.
  */
 
 package tech.smartboot.feat.cloud.aot.serializer.extension;
 
 import com.alibaba.fastjson2.JSONPath;
 import org.apache.ibatis.datasource.pooled.PooledDataSource;
 import tech.smartboot.feat.core.common.exception.FeatException;
 
 import javax.annotation.processing.ProcessingEnvironment;
 import javax.sql.DataSource;
 import java.io.PrintWriter;
 
 import static tech.smartboot.feat.cloud.aot.controller.JsonSerializer.headBlank;
 
 /**
  * Feat 原生 JDBC 数据源配置。
  *
  * <pre>
  *   feat:
  *     datasource:
  *       driver: org.h2.Driver
  *       url: jdbc:h2:mem:feat
  *       username: sa
  *       password:
  * </pre>
  */
 public class DataSourceSerializer extends ExtensionSerializer {
 
     public DataSourceSerializer(ProcessingEnvironment processingEnv, String config, PrintWriter printWriter) {
         super(processingEnv, config, printWriter);
         if (JSONPath.eval(config, "$.feat.datasource") == null) {
             throw new FeatException("feat.datasource is null");
         }
     }
 
     @Override
     public void serializeImport() {
         printWriter.println("import " + DataSource.class.getName() + ";");
         printWriter.println("import " + PooledDataSource.class.getName() + ";");
     }
 
     @Override
     public void serializeLoadBean() {
         String driver = stringValue("$.feat.datasource.driver");
         String url = stringValue("$.feat.datasource.url");
         String username = stringValue("$.feat.datasource.username");
         String password = stringValue("$.feat.datasource.password");
         printWriter.append(headBlank(0)).println("DataSource dataSource = new PooledDataSource(" + driver + ", " + url + ", " + username + ", " + password + ");");
         printWriter.append(headBlank(0)).println("applicationContext.addBean(\"dataSource\", dataSource);");
     }
 
     private String stringValue(String path) {
         Object obj = JSONPath.eval(config, path);
         return obj == null ? "null" : "\"" + obj.toString() + "\"";
     }
 }
