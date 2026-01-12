/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer.extension;

import com.alibaba.fastjson2.JSONPath;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import tech.smartboot.feat.core.common.exception.FeatException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.sql.DataSource;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;

import static tech.smartboot.feat.cloud.aot.controller.JsonSerializer.headBlank;

/**
 * <pre>
 *   feat
 *     mybatis:
 *       path: resources/mybatis_config.xml
 *       initial-sql: init_db.sql
 * </pre>
 *
 * @author 三刀
 * @version v1.0 1/12/26
 */
public class MybatisSerializer extends ExtensionSerializer {
    private final Configuration configuration;
    private final String xmlPath;

    public MybatisSerializer(ProcessingEnvironment processingEnv, String config, PrintWriter printWriter) throws IOException {
        super(processingEnv, config, printWriter);
        Object obj = JSONPath.eval(config, "$.feat.mybatis.path");
        if (obj == null) {
            throw new FeatException("feat.mybatis.path is null");
        }
        xmlPath = obj.toString();
        // 在编译时通过filer获取资源文件并解析XML
        FileObject fileObject = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", xmlPath);
        File file = new File(fileObject.toUri());
        if (!file.isFile()) {
            throw new FeatException("feat.mybatis.path:" + xmlPath + " is not exist");
        }
        InputStream inputStream = fileObject.openInputStream();
        // 在编译期间解析XML配置
        XMLConfigBuilder xmlConfigBuilder = new XMLConfigBuilder(inputStream, null, null);
        configuration = xmlConfigBuilder.parse();
    }

    @Override
    public void serializeImport() {
        printWriter.println("import " + Configuration.class.getName() + ";");
        printWriter.println("import " + Environment.class.getName() + ";");
        printWriter.println("import " + TransactionFactory.class.getName() + ";");
        printWriter.println("import " + JdbcTransactionFactory.class.getName() + ";");
        printWriter.println("import " + PooledDataSource.class.getName() + ";");
        printWriter.println("import " + JdbcType.class.getName() + ";");
        printWriter.println("import " + XMLLanguageDriver.class.getName() + ";");
        printWriter.println("import " + JavassistProxyFactory.class.getName() + ";");
        printWriter.println("import " + SqlSessionFactoryBuilder.class.getName() + ";");
        printWriter.println("import " + AutoMappingBehavior.class.getName() + ";");
        printWriter.println("import " + ExecutorType.class.getName() + ";");
        printWriter.println("import " + SqlSessionFactory.class.getName() + ";");
        printWriter.println("import " + ScriptRunner.class.getName() + ";");
        printWriter.println("import " + Resources.class.getName() + ";");
    }

    @Override
    public void serializeLoadBean() {
        Environment environment = configuration.getEnvironment();

        // 生成静态代码，直接将解析到的配置值写入
        printWriter.append(headBlank(0)).println("// Static configuration generated at compile time from MyBatis XML: " + xmlPath);
        // 处理数据源 - 从已解析的配置中获取数据源配置参数并重新构建
        DataSource dataSource = environment.getDataSource();
        String dataSourceCode = "";
        // 尝试获取PooledDataSource的配置参数
        if (dataSource instanceof PooledDataSource) {
            PooledDataSource pooledDataSource = (PooledDataSource) dataSource;
            dataSourceCode = "new PooledDataSource(" + (pooledDataSource.getDriver() == null ? "null" : "\"" + pooledDataSource.getDriver() + "\"") + ", " + (pooledDataSource.getUrl() == null ? "null" : "\"" + pooledDataSource.getUrl() + "\"") + ", " + (pooledDataSource.getUsername() == null ? "null" : "\"" + pooledDataSource.getUsername() + "\"") + ", " + (pooledDataSource.getPassword() == null ? "null" : "\"" + pooledDataSource.getPassword() + "\"") + ")";
        } else {
            throw new FeatException("unSupport datasource: " + dataSource);
        }


        printWriter.append(headBlank(0)).println("Environment environment = new Environment(\"" + environment.getId() + "\",");
        // 处理事务管理器
        if (environment.getTransactionFactory() != null) {
            printWriter.append(headBlank(0)).println("\tnew " + environment.getTransactionFactory().getClass().getName() + "(),");
        } else {
            printWriter.append(headBlank(0)).println("\tnew JdbcTransactionFactory(),");
        }

        printWriter.append(headBlank(0)).println("\t" + dataSourceCode + ");");

        printWriter.append(headBlank(0)).println("Configuration configuration = new Configuration(environment);");

        // 设置解析出的配置选项
        printWriter.append(headBlank(0)).println("configuration.setLazyLoadingEnabled(" + configuration.isLazyLoadingEnabled() + ");");
        printWriter.append(headBlank(0)).println("configuration.setAggressiveLazyLoading(" + configuration.isAggressiveLazyLoading() + ");");
        printWriter.append(headBlank(0)).println("configuration.setMultipleResultSetsEnabled(" + configuration.isMultipleResultSetsEnabled() + ");");
        printWriter.append(headBlank(0)).println("configuration.setUseColumnLabel(" + configuration.isUseColumnLabel() + ");");
        printWriter.append(headBlank(0)).println("configuration.setUseGeneratedKeys(" + configuration.isUseGeneratedKeys() + ");");
        printWriter.append(headBlank(0)).println("configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(\"" + configuration.getAutoMappingBehavior().name() + "\"));");
        printWriter.append(headBlank(0)).println("configuration.setDefaultExecutorType(ExecutorType.valueOf(\"" + configuration.getDefaultExecutorType().name() + "\"));");
        printWriter.append(headBlank(0)).println("configuration.setJdbcTypeForNull(org.apache.ibatis.type.JdbcType.valueOf(\"" + configuration.getJdbcTypeForNull().name() + "\"));");
        // 设置默认脚本语言
//            LanguageDriver defaultLanguageDriver = parsedConfiguration.getDefaultScriptingLanguageInstance();
//            if (defaultLanguageDriver != null) {
//                printWriter.println("configuration.setDefaultScriptingLanguage(new " + defaultLanguageDriver.getClass().getName() + "());");
//            } else {
//                printWriter.println("configuration.setDefaultScriptingLanguage(new " + XMLLanguageDriver.class.getName() + "());");
//            }

        // 设置代理工厂
        printWriter.append(headBlank(0)).println("configuration.setProxyFactory(new " + configuration.getProxyFactory().getClass().getName() + "());");

        // 添加所有解析到的映射器
        Collection<Class<?>> mapperClasses = configuration.getMapperRegistry().getMappers();
        for (Class<?> mapperClass : mapperClasses) {
            printWriter.append(headBlank(0)).println("configuration.addMapper(" + mapperClass.getName() + ".class);");
        }
        printWriter.append(headBlank(0)).println("SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(configuration);");

        Object obj = JSONPath.eval(config, "$.feat.mybatis['initial-sql']");
        if (obj != null) {
            printWriter.append(headBlank(0)).println("ScriptRunner runner = new ScriptRunner(sessionFactory.openSession().getConnection());");
            printWriter.append(headBlank(0)).println("runner.setLogWriter(null);");
            printWriter.append(headBlank(0)).println("runner.runScript(Resources.getResourceAsReader(\"" + obj + "\"));");
        }
        printWriter.append(headBlank(0)).println("applicationContext.addBean(\"sessionFactory\", sessionFactory);");

    }
}
