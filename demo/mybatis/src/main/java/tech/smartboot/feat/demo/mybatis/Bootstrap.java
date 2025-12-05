/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.mybatis;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Bean;

import java.io.IOException;
import java.io.InputStream;

@Bean
public class Bootstrap {
    @Bean
    public SqlSessionFactory sessionFactory() throws IOException {
        InputStream inputStream = Resources.getResourceAsStream("mybatis/mybatis-config.xml");
        SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        ScriptRunner runner = new ScriptRunner(sessionFactory.openSession().getConnection());
        runner.setLogWriter(null);
        runner.runScript(Resources.getResourceAsReader("mybatis/ddl/schema.sql"));
        return sessionFactory;
    }


    public static void main(String[] args) {
        FeatCloud.cloudServer().listen();
    }
}